package com.qinyadan.risk.event;

/**
 * 登录事件
 */
public class LoginEvent extends Event {
    private static final long serialVersionUID = 1L;

    private String ip;                 // 登录IP
    private String deviceId;           // 设备ID
    private boolean success;           // 是否成功
    private String failureReason;      // 失败原因
    private String location;           // 登录地点
    private String browser;            // 浏览器信息

    public LoginEvent() {
        super();
        this.eventType = "LOGIN";
    }

    public LoginEvent(String userId, String eventId) {
        super(userId, eventId, "LOGIN");
    }

    public LoginEvent(String userId, String eventId, String ip, String deviceId, boolean success) {
        super(userId, eventId, "LOGIN");
        this.ip = ip;
        this.deviceId = deviceId;
        this.success = success;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    @Override
    public String toString() {
        return "LoginEvent{" +
                "userId='" + userId + '\'' +
                ", eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", ip='" + ip + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", success=" + success +
                ", failureReason='" + failureReason + '\'' +
                ", location='" + location + '\'' +
                ", browser='" + browser + '\'' +
                '}';
    }
}