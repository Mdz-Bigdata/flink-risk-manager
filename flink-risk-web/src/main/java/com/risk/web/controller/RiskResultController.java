package com.risk.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.risk.web.entity.RiskResult;
import com.risk.web.service.RiskResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/risks")
public class RiskResultController {

    @Autowired
    private RiskResultService riskResultService;

    /**
     * 分页查询风险记录
     */
    @GetMapping
    public Map<String, Object> getRiskResults(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "riskLevel", required = false) String riskLevel,
            @RequestParam(name = "action", required = false) String action) {

        LambdaQueryWrapper<RiskResult> wrapper = new LambdaQueryWrapper<>();
        if (userId != null && !userId.isEmpty()) {
            wrapper.like(RiskResult::getUserId, userId);
        }
        if (eventType != null && !eventType.isEmpty()) {
            wrapper.eq(RiskResult::getEventType, eventType);
        }
        if (riskLevel != null && !riskLevel.isEmpty()) {
            wrapper.eq(RiskResult::getRiskLevel, riskLevel);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.eq(RiskResult::getAction, action);
        }
        wrapper.orderByDesc(RiskResult::getCreateTime);

        Page<RiskResult> result = riskResultService.page(new Page<>(page, pageSize), wrapper);

        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("pageSize", result.getSize());
        return response;
    }

    /**
     * 获取单条风险记录
     */
    @GetMapping("/{id}")
    public RiskResult getRiskResult(@PathVariable Long id) {
        return riskResultService.getById(id);
    }
}
