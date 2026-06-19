package com.qinyadan.risk.event;

import java.math.BigDecimal;

/**
 * 下单事件
 */
public class OrderEvent extends Event {
    private static final long serialVersionUID = 1L;

    private String orderId;            // 订单ID
    private BigDecimal amount;         // 订单金额
    private String productId;          // 商品ID
    private String productName;        // 商品名称
    private int quantity;              // 购买数量
    private String paymentMethod;      // 支付方式
    private String deliveryAddress;    // 收货地址
    private long processTime;          // 处理时间

    public OrderEvent() {
        super();
        this.eventType = "ORDER";
    }

    public OrderEvent(String userId, String eventId) {
        super(userId, eventId, "ORDER");
    }

    public OrderEvent(String userId, String eventId, String orderId, BigDecimal amount) {
        super(userId, eventId, "ORDER");
        this.orderId = orderId;
        this.amount = amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public long getProcessTime() {
        return processTime;
    }

    public void setProcessTime(long processTime) {
        this.processTime = processTime;
    }

    @Override
    public String toString() {
        return "OrderEvent{" +
                "userId='" + userId + '\'' +
                ", eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", orderId='" + orderId + '\'' +
                ", amount=" + amount +
                ", productId='" + productId + '\'' +
                ", quantity=" + quantity +
                ", paymentMethod='" + paymentMethod + '\'' +
                '}';
    }
}