package com.risk.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.risk.web.entity.RiskResult;
import com.risk.web.entity.Rule;
import com.risk.web.service.RiskResultService;
import com.risk.web.service.RuleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private RiskResultService riskResultService;

    /**
     * 首页概览统计
     */
    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        Map<String, Object> stats = new HashMap<>();

        // 规则统计
        long totalRules = ruleService.count();
        LambdaQueryWrapper<Rule> enabledWrapper = new LambdaQueryWrapper<>();
        enabledWrapper.eq(Rule::getEnabled, true);
        long enabledRules = ruleService.count(enabledWrapper);

        // 今日风险事件
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<RiskResult> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(RiskResult::getCreateTime, todayStart);
        long todayRisks = riskResultService.count(todayWrapper);

        // 今日高风险
        LambdaQueryWrapper<RiskResult> highWrapper = new LambdaQueryWrapper<>();
        highWrapper.ge(RiskResult::getCreateTime, todayStart);
        highWrapper.in(RiskResult::getRiskLevel, "HIGH", "CRITICAL");
        long todayHighRisks = riskResultService.count(highWrapper);

        // 规则类型分布
        List<Rule> allRules = ruleService.list();
        Map<String, Long> ruleTypeDistribution = allRules.stream()
                .collect(Collectors.groupingBy(
                        r -> inferRuleType(r.getName()),
                        Collectors.counting()
                ));

        // 风险等级分布
        Map<String, Long> riskLevelDistribution = new LinkedHashMap<>();
        riskLevelDistribution.put("LOW", 0L);
        riskLevelDistribution.put("MEDIUM", 0L);
        riskLevelDistribution.put("HIGH", 0L);
        riskLevelDistribution.put("CRITICAL", 0L);

        List<RiskResult> todayResults = riskResultService.list(todayWrapper);
        for (RiskResult r : todayResults) {
            String level = r.getRiskLevel();
            if (riskLevelDistribution.containsKey(level)) {
                riskLevelDistribution.put(level, riskLevelDistribution.get(level) + 1);
            }
        }

        // 事件类型分布
        Map<String, Long> eventTypeDistribution = todayResults.stream()
                .collect(Collectors.groupingBy(
                        RiskResult::getEventType,
                        Collectors.counting()
                ));

        stats.put("totalRules", totalRules);
        stats.put("enabledRules", enabledRules);
        stats.put("todayRisks", todayRisks);
        stats.put("todayHighRisks", todayHighRisks);
        stats.put("ruleTypeDistribution", ruleTypeDistribution);
        stats.put("riskLevelDistribution", riskLevelDistribution);
        stats.put("eventTypeDistribution", eventTypeDistribution);

        return stats;
    }

    /**
     * 近7天风险趋势
     */
    @GetMapping("/trend")
    public Map<String, Object> getTrend() {
        Map<String, Object> trend = new HashMap<>();
        List<Map<String, Object>> dailyData = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            LambdaQueryWrapper<RiskResult> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(RiskResult::getCreateTime, dayStart);
            wrapper.le(RiskResult::getCreateTime, dayEnd);

            long total = riskResultService.count(wrapper);

            LambdaQueryWrapper<RiskResult> highWrapper = wrapper.clone();
            highWrapper.in(RiskResult::getRiskLevel, "HIGH", "CRITICAL");
            long high = riskResultService.count(highWrapper);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("total", total);
            dayData.put("high", high);
            dailyData.add(dayData);
        }

        trend.put("dailyData", dailyData);
        return trend;
    }

    /**
     * 规则命中排行（今日）
     */
    @GetMapping("/top-rules")
    public Map<String, Object> getTopRules() {
        Map<String, Object> result = new HashMap<>();

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<RiskResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(RiskResult::getCreateTime, todayStart);
        List<RiskResult> todayResults = riskResultService.list(wrapper);

        // 从 details JSON 中提取 matchedRuleName 统计
        Map<String, Long> ruleHitCount = new LinkedHashMap<>();
        for (RiskResult r : todayResults) {
            String ruleName = extractRuleName(r.getDetails());
            if (ruleName != null) {
                ruleHitCount.merge(ruleName, 1L, Long::sum);
            }
        }

        // 排序取 Top 10
        List<Map<String, Object>> topRules = ruleHitCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("ruleName", entry.getKey());
                    item.put("hitCount", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        result.put("topRules", topRules);
        return result;
    }

    /**
     * 从规则名称推断规则类型
     */
    private String inferRuleType(String name) {
        if (name == null) return "OTHER";
        if (name.contains("登录") || name.contains("login") || name.contains("Login")) return "LOGIN";
        if (name.contains("订单") || name.contains("order") || name.contains("Order")) return "ORDER";
        if (name.contains("活动") || name.contains("activity") || name.contains("Activity")
                || name.contains("优惠券") || name.contains("coupon")) return "ACTIVITY";
        return "OTHER";
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 从 details JSON 提取 matchedRuleName
     */
    private String extractRuleName(String details) {
        if (details == null || details.isEmpty()) return null;
        try {
            JsonNode node = MAPPER.readTree(details);
            JsonNode nameNode = node.get("matchedRuleName");
            if (nameNode != null && !nameNode.isNull()) {
                return nameNode.asText();
            }
        } catch (Exception e) {
            // JSON 解析失败，回退到 eventType + riskLevel 组合
        }
        return null;
    }
}
