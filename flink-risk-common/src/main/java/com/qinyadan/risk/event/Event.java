package com.qinyadan.risk.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 基础事件类
 */
public abstract class Event implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String userId;           // 用户ID
    protected String eventId;          // 事件ID
    protected long timestamp;          // 时间戳
    protected String eventType;        // 事件类型
    protected Map<String, Object> attributes;  // 事件属性

    public Event() {
        this.attributes = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public Event(String userId, String eventId, String eventType) {
        this();
        this.userId = userId;
        this.eventId = eventId;
        this.eventType = eventType;
    }

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public String toString() {
        return "Event{" +
                "userId='" + userId + '\'' +
                ", eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}