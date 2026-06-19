package com.qinyadan.risk.cep;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * CEP 模式匹配中间结果
 * CepJob 在模式匹配命中后输出此对象，
 * 携带事件上下文，由下游广播处理函数连接规则引擎后动态解析 action
 */
public class PatternMatchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String eventId;
    private String eventType;
    private String patternType;
    private Map<String, Object> context;
    private Map<String, Object> details;

    public PatternMatchResult() {
        this.context = new HashMap<>();
        this.details = new HashMap<>();
    }

    public PatternMatchResult(String userId, String eventType, String patternType) {
        this();
        this.userId = userId;
        this.eventType = eventType;
        this.patternType = patternType;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) { this.patternType = patternType; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    @Override
    public String toString() {
        return "PatternMatchResult{" +
                "userId='" + userId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", patternType='" + patternType + '\'' +
                ", context=" + context +
                '}';
    }
}
