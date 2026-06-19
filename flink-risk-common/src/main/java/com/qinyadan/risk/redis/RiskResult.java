package com.qinyadan.risk.redis;

import com.qinyadan.risk.rule.RiskLevel;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 风险结果
 */
public class RiskResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;             // 用户ID
    private String eventId;            // 事件ID
    private String eventType;          // 事件类型
    private RiskLevel riskLevel;       // 风险等级
    private int riskScore;             // 风险评分 (0-100)
    private String action;             // 推荐动作
    private Map<String, Object> details;  // 详细信息
    private long timestamp;            // 时间戳
    private long ttl;                  // 生存时间（秒）

    public RiskResult() {
        this.details = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.ttl = 3600;  // 默认1小时
    }

    public RiskResult(String userId, String eventId, String eventType) {
        this();
        this.userId = userId;
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public String getRedisKey() {
        return String.format("risk:%s:%s:%s", eventType.toLowerCase(), userId, eventId);
    }

    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"userId\":\"").append(userId).append("\",");
        sb.append("\"eventId\":\"").append(eventId).append("\",");
        sb.append("\"eventType\":\"").append(eventType).append("\",");
        sb.append("\"riskLevel\":\"").append(riskLevel).append("\",");
        sb.append("\"riskScore\":").append(riskScore).append(",");
        sb.append("\"action\":\"").append(action).append("\",");
        sb.append("\"timestamp\":").append(timestamp);
        sb.append("}");
        return sb.toString();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        return "RiskResult{" +
                "userId='" + userId + '\'' +
                ", eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", riskLevel=" + riskLevel +
                ", riskScore=" + riskScore +
                ", action='" + action + '\'' +
                '}';
    }
}