package com.risk.cep;

import com.risk.event.ActivityEvent;
import com.risk.event.LoginEvent;
import com.risk.event.OrderEvent;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;

import java.time.Duration;

/**
 * CEP 模式工厂（Flink 2.x - 带时间窗口）
 */
public class PatternFactory {

    public static Pattern<LoginEvent, ?> createLoginFailurePattern() {
        return Pattern.<LoginEvent>begin("first")
                .where(new SimpleCondition<LoginEvent>() {
                    @Override
                    public boolean filter(LoginEvent event) {
                        return !event.isSuccess();
                    }
                })
                .followedBy("second")
                .where(new SimpleCondition<LoginEvent>() {
                    @Override
                    public boolean filter(LoginEvent event) {
                        return !event.isSuccess();
                    }
                })
                .followedBy("third")
                .where(new SimpleCondition<LoginEvent>() {
                    @Override
                    public boolean filter(LoginEvent event) {
                        return !event.isSuccess();
                    }
                })
                .within(Duration.ofSeconds(60));
    }

    public static Pattern<LoginEvent, ?> createLoginLocationChangePattern() {
        return Pattern.<LoginEvent>begin("first")
                .where(new SimpleCondition<LoginEvent>() {
                    @Override
                    public boolean filter(LoginEvent event) {
                        return event.isSuccess();
                    }
                })
                .followedBy("second")
                .where(new SimpleCondition<LoginEvent>() {
                    @Override
                    public boolean filter(LoginEvent event) {
                        return event.isSuccess();
                    }
                })
                .within(Duration.ofSeconds(60));
    }

    public static Pattern<OrderEvent, ?> createOrderFrequentPattern() {
        return Pattern.<OrderEvent>begin("first")
                .where(new SimpleCondition<OrderEvent>() {
                    @Override
                    public boolean filter(OrderEvent event) {
                        return event.getAmount() != null && event.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0;
                    }
                })
                .followedBy("second")
                .where(new SimpleCondition<OrderEvent>() {
                    @Override
                    public boolean filter(OrderEvent event) {
                        return event.getAmount() != null && event.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0;
                    }
                })
                .followedBy("third")
                .where(new SimpleCondition<OrderEvent>() {
                    @Override
                    public boolean filter(OrderEvent event) {
                        return event.getAmount() != null && event.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0;
                    }
                })
                .within(Duration.ofSeconds(60));
    }

    public static Pattern<OrderEvent, ?> createOrderRapidPattern() {
        return Pattern.<OrderEvent>begin("first")
                .where(new SimpleCondition<OrderEvent>() {
                    @Override
                    public boolean filter(OrderEvent event) {
                        return event.getAmount() != null && event.getAmount().compareTo(new java.math.BigDecimal("100")) > 0;
                    }
                })
                .followedBy("second")
                .where(new SimpleCondition<OrderEvent>() {
                    @Override
                    public boolean filter(OrderEvent event) {
                        return event.getAmount() != null && event.getAmount().compareTo(new java.math.BigDecimal("100")) > 0;
                    }
                })
                .within(Duration.ofSeconds(30));
    }

    public static Pattern<ActivityEvent, ?> createActivityFrequentPattern() {
        return Pattern.<ActivityEvent>begin("first")
                .where(new SimpleCondition<ActivityEvent>() {
                    @Override
                    public boolean filter(ActivityEvent event) {
                        return "PARTICIPATE".equals(event.getActionType());
                    }
                })
                .followedBy("second")
                .where(new SimpleCondition<ActivityEvent>() {
                    @Override
                    public boolean filter(ActivityEvent event) {
                        return "PARTICIPATE".equals(event.getActionType());
                    }
                })
                .followedBy("third")
                .where(new SimpleCondition<ActivityEvent>() {
                    @Override
                    public boolean filter(ActivityEvent event) {
                        return "PARTICIPATE".equals(event.getActionType());
                    }
                })
                .followedBy("fourth")
                .where(new SimpleCondition<ActivityEvent>() {
                    @Override
                    public boolean filter(ActivityEvent event) {
                        return "PARTICIPATE".equals(event.getActionType());
                    }
                })
                .within(Duration.ofSeconds(60));
    }

    public static Pattern<ActivityEvent, ?> createCouponRepeatPattern() {
        return Pattern.<ActivityEvent>begin("first")
                .where(new SimpleCondition<ActivityEvent>() {
                    @Override
                    public boolean filter(ActivityEvent event) {
                        return "CLAIM_COUPON".equals(event.getActionType());
                    }
                })
                .followedBy("second")
                .where(new SimpleCondition<ActivityEvent>() {
                    @Override
                    public boolean filter(ActivityEvent event) {
                        return "CLAIM_COUPON".equals(event.getActionType());
                    }
                })
                .followedBy("third")
                .where(new SimpleCondition<ActivityEvent>() {
                    @Override
                    public boolean filter(ActivityEvent event) {
                        return "CLAIM_COUPON".equals(event.getActionType());
                    }
                })
                .within(Duration.ofSeconds(60));
    }
}
