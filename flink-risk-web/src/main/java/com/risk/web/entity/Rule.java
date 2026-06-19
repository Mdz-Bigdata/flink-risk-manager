package com.risk.web.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

@TableName("rule_config")
public class Rule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String expression;
    private String riskLevel;
    private String action;
    private Integer weight;
    private Boolean enabled;
    private String description;
    private Date createTime;
    private Date updateTime;

    /** 前端展示用：规则类型（从 expression 或 name 推断，或由前端传入） */
    @TableField(exist = false)
    private String ruleType;

    /** 前端展示用：规则表达式别名 */
    @TableField(exist = false)
    private String ruleExpression;

    /** 前端展示用：规则名称别名 */
    @TableField(exist = false)
    private String ruleName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getRuleExpression() { return ruleExpression; }
    public void setRuleExpression(String ruleExpression) { this.ruleExpression = ruleExpression; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    /**
     * 从前端字段同步到 DB 字段
     */
    public void syncFromFrontend() {
        if (ruleName != null) this.name = ruleName;
        if (ruleExpression != null) this.expression = ruleExpression;
    }

    /**
     * 从 DB 字段同步到前端字段
     */
    public void syncToFrontend() {
        this.ruleName = this.name;
        this.ruleExpression = this.expression;
    }
}
