package com.qinyadan.risk.rule;

import java.io.Serializable;

/**
 * 规则配置
 */
public class RuleConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;                 // 规则ID
    private String name;               // 规则名称
    private String expression;         // AviatorScript 表达式
    private RiskLevel riskLevel;       // 风险等级
    private String action;             // 处理动作
    private int weight;                // 权重
    private boolean enabled;           // 是否启用
    private String description;        // 描述
    private long createTime;           // 创建时间
    private long updateTime;           // 更新时间

    public RuleConfig() {
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.enabled = true;
        this.weight = 1;
    }

    public RuleConfig(String id, String name, String expression, RiskLevel riskLevel) {
        this();
        this.id = id;
        this.name = name;
        this.expression = expression;
        this.riskLevel = riskLevel;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "RuleConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", expression='" + expression + '\'' +
                ", riskLevel=" + riskLevel +
                ", action='" + action + '\'' +
                ", weight=" + weight +
                ", enabled=" + enabled +
                '}';
    }
}