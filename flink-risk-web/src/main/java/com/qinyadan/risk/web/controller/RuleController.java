package com.qinyadan.risk.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinyadan.risk.web.entity.Rule;
import com.qinyadan.risk.web.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    @Autowired
    private RuleService ruleService;

    /**
     * 分页查询规则列表
     */
    @GetMapping
    public Map<String, Object> getRules(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "riskLevel", required = false) String riskLevel,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "enabled", required = false) Boolean enabled) {

        LambdaQueryWrapper<Rule> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(Rule::getName, name);
        }
        if (riskLevel != null && !riskLevel.isEmpty()) {
            wrapper.eq(Rule::getRiskLevel, riskLevel);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.eq(Rule::getAction, action);
        }
        if (enabled != null) {
            wrapper.eq(Rule::getEnabled, enabled);
        }
        wrapper.orderByDesc(Rule::getUpdateTime);

        Page<Rule> result = ruleService.page(new Page<>(page, pageSize), wrapper);

        // 同步到前端字段
        result.getRecords().forEach(Rule::syncToFrontend);

        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("pageSize", result.getSize());
        return response;
    }

    /**
     * 获取所有启用的规则（供 Flink Job 拉取）
     */
    @GetMapping("/enabled")
    public List<Rule> getEnabledRules() {
        LambdaQueryWrapper<Rule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rule::getEnabled, true);
        wrapper.orderByDesc(Rule::getUpdateTime);
        List<Rule> rules = ruleService.list(wrapper);
        rules.forEach(Rule::syncToFrontend);
        return rules;
    }

    /**
     * 获取单条规则
     */
    @GetMapping("/{id}")
    public Rule getRule(@PathVariable Long id) {
        Rule rule = ruleService.getById(id);
        if (rule != null) {
            rule.syncToFrontend();
        }
        return rule;
    }

    /**
     * 创建规则
     */
    @PostMapping
    public Rule createRule(@RequestBody Rule rule) {
        rule.syncFromFrontend();
        ruleService.save(rule);
        rule.syncToFrontend();
        return rule;
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    public Rule updateRule(@PathVariable Long id, @RequestBody Rule rule) {
        rule.setId(id);
        rule.syncFromFrontend();
        ruleService.updateById(rule);
        rule.syncToFrontend();
        return rule;
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    public Map<String, String> deleteRule(@PathVariable Long id) {
        ruleService.removeById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        return response;
    }

    /**
     * 启用/停用规则
     */
    @PutMapping("/{id}/toggle")
    public Rule toggleRule(@PathVariable Long id) {
        Rule rule = ruleService.getById(id);
        if (rule != null) {
            rule.setEnabled(!rule.getEnabled());
            ruleService.updateById(rule);
            rule.syncToFrontend();
        }
        return rule;
    }

    /**
     * 批量启用
     */
    @PutMapping("/batch/enable")
    public Map<String, String> batchEnable(@RequestBody List<Long> ids) {
        ruleService.update().eq("enabled", false).in("id", ids).set("enabled", true).update();
        Map<String, String> response = new HashMap<>();
        response.put("message", "批量启用成功");
        return response;
    }

    /**
     * 批量停用
     */
    @PutMapping("/batch/disable")
    public Map<String, String> batchDisable(@RequestBody List<Long> ids) {
        ruleService.update().eq("enabled", true).in("id", ids).set("enabled", false).update();
        Map<String, String> response = new HashMap<>();
        response.put("message", "批量停用成功");
        return response;
    }
}
