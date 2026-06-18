package com.risk.cep;

import com.risk.event.ActivityEvent;
import com.risk.event.LoginEvent;
import com.risk.event.OrderEvent;
import org.apache.flink.cep.pattern.Pattern;

/**
 * CEP 模式工厂
 * 定义各类事件的 CEP 模式
 */
public class PatternFactory {

    /**
     * 登录风控模式
     * 检测：5分钟内登录失败次数 >= 3
     */
    public static Pattern<LoginEvent, ?> createLoginFailurePattern() {
        return Pattern.<LoginEvent>begin("first")
                .where(event -> !event.isSuccess())
                .followedBy("second")
                .where(event -> !event.isSuccess())
                .followedBy("third")
                .where(event -> !event.isSuccess())
                .within(org.apache.flink.streaming.api.windowing.time.Time.seconds(300));
    }

    /**
     * 登录风控模式
     * 检测：异地登录（IP变更）
     */
    public static Pattern<LoginEvent, ?> createLoginLocationChangePattern() {
        return Pattern.<LoginEvent>begin("first")
                .where(event -> event.isSuccess())
                .followedBy("second")
                .where((prev, curr) -> curr.isSuccess() && !prev.getIp().equals(curr.getIp()))
                .within(org.apache.flink.streaming.api.windowing.time.Time.hours(24));
    }

    /**
     * 下单风控模式
     * 检测：30分钟内多笔订单（频繁下单）
     */
    public static Pattern<OrderEvent, ?> createOrderFrequentPattern() {
        return Pattern.<OrderEvent>begin("first")
                .followedBy("second")
                .followedBy("third")
                .within(org.apache.flink.streaming.api.windowing.time.Time.minutes(30));
    }

    /**
     * 下单风控模式
     * 检测：快速连续下单（可能是机器人）
     */
    public static Pattern<OrderEvent, ?> createOrderRapidPattern() {
        return Pattern.<OrderEvent>begin("first")
                .followedBy("second")
                .where((first, second) -> 
                    second.getTimestamp() - first.getTimestamp() < 10000)  // 10秒内
                .within(org.apache.flink.streaming.api.windowing.time.Time.minutes(5));
    }

    /**
     * 活动风控模式
     * 检测：频繁参与活动（可能是作弊）
     */
    public static Pattern<ActivityEvent, ?> createActivityFrequentPattern() {
        return Pattern.<ActivityEvent>begin("first")
                .where(event -> "PARTICIPATE".equals(event.getActionType()))
                .followedBy("second")
                .where(event -> "PARTICIPATE".equals(event.getActionType()))
                .followedBy("third")
                .where(event -> "PARTICIPATE".equals(event.getActionType()))
                .followedBy("fourth")
                .where(event -> "PARTICIPATE".equals(event.getActionType()))
                .within(org.apache.flink.streaming.api.windowing.time.Time.hours(24));
    }

    /**
     * 活动风控模式
     * 检测：重复领取优惠券
     */
    public static Pattern<ActivityEvent, ?> createCouponRepeatPattern() {
        return Pattern.<ActivityEvent>begin("first")
                .where(event -> "CLAIM_COUPON".equals(event.getActionType()))
                .followedBy("second")
                .where(event -> "CLAIM_COUPON".equals(event.getActionType()))
                .followedBy("third")
                .where(event -> "CLAIM_COUPON".equals(event.getActionType()))
                .within(org.apache.flink.streaming.api.windowing.time.Time.hours(1));
    }
}