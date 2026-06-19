package com.qinyadan.risk.event;

import java.math.BigDecimal;

/**
 * 活动事件（优惠券、营销活动等）
 */
public class ActivityEvent extends Event {
    private static final long serialVersionUID = 1L;

    private String activityId;         // 活动ID
    private String activityName;       // 活动名称
    private String couponCode;         // 优惠券代码
    private BigDecimal couponValue;    // 优惠券金额
    private String actionType;         // 行为类型（领取、使用、兑换等）
    private int participationCount;    // 参与次数
    private String channel;            // 渠道
    private String source;             // 来源

    public ActivityEvent() {
        super();
        this.eventType = "ACTIVITY";
    }

    public ActivityEvent(String userId, String eventId) {
        super(userId, eventId, "ACTIVITY");
    }

    public ActivityEvent(String userId, String eventId, String activityId, String actionType) {
        super(userId, eventId, "ACTIVITY");
        this.activityId = activityId;
        this.actionType = actionType;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public BigDecimal getCouponValue() {
        return couponValue;
    }

    public void setCouponValue(BigDecimal couponValue) {
        this.couponValue = couponValue;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public int getParticipationCount() {
        return participationCount;
    }

    public void setParticipationCount(int participationCount) {
        this.participationCount = participationCount;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "ActivityEvent{" +
                "userId='" + userId + '\'' +
                ", eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", activityId='" + activityId + '\'' +
                ", activityName='" + activityName + '\'' +
                ", actionType='" + actionType + '\'' +
                '}';
    }
}