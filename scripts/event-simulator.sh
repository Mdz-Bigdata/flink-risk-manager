#!/bin/bash
# ============================================================
# Flink 风控系统 - 事件模拟器（匹配 CEP 模式版）
# 模拟数据必须满足 CEP 模式条件才能触发风控
# ============================================================

set -e

KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP:-localhost:9092}"
LOGIN_TOPIC="login-events"
ORDER_TOPIC="order-events"
ACTIVITY_TOPIC="activity-events"

# 颜色
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查 Kafka 是否可用
check_kafka() {
    info "检查 Kafka 连接: $KAFKA_BOOTSTRAP"
    if ! docker exec flink-risk-kafka kafka-broker-api-versions --bootstrap-server "$KAFKA_BOOTSTRAP" >/dev/null 2>&1; then
        warn "Kafka 未就绪，等待 5 秒..."
        sleep 5
    fi
    ok "Kafka 连接正常"
}

# 生成随机字符串
rand_str() {
    LC_ALL=C tr -dc 'a-z0-9' </dev/urandom | head -c "$1"
}

# 生成随机 IP
rand_ip() {
    echo "$((RANDOM % 256)).$((RANDOM % 256)).$((RANDOM % 256)).$((RANDOM % 256))"
}

# 发送 JSON 到 Kafka（单行格式）
send_to_kafka() {
    local topic="$1"
    local json="$2"
    printf '%s\n' "$json" | docker exec -i flink-risk-kafka kafka-console-producer \
        --bootstrap-server "$KAFKA_BOOTSTRAP" \
        --topic "$topic" \
        --property "parse.key=false" \
        --property "key.serializer=org.apache.kafka.common.serialization.StringSerializer" \
        --property "value.serializer=org.apache.kafka.common.serialization.StringSerializer" 2>/dev/null || true
}

# ========== 登录事件 ==========
# CEP 模式1: 连续3次登录失败 (success=false)
# CEP 模式2: 异地登录 (success=true, IP变化)
send_login() {
    local user_id="$1"
    local event_id="login_$(date +%s%N | cut -c1-13)_$(rand_str 6)"
    local timestamp=$(date +%s%N | cut -c1-13)
    local ip=$(rand_ip)
    local device_id="device_$(rand_str 8)"
    local success=0
    local location="未知地点"
    local browser="可疑浏览器"

    local json="{\"userId\":\"$user_id\",\"eventId\":\"$event_id\",\"eventType\":\"LOGIN\",\"timestamp\":$timestamp,\"ip\":\"$ip\",\"deviceId\":\"$device_id\",\"success\":$success,\"failureReason\":\"密码错误\",\"location\":\"$location\",\"browser\":\"$browser\"}"
    send_to_kafka "$LOGIN_TOPIC" "$json"
}

# 发送正常登录（用于异地登录模式：先正常登录，再换IP登录）
send_login_normal() {
    local user_id="$1"
    local event_id="login_$(date +%s%N | cut -c1-13)_$(rand_str 6)"
    local timestamp=$(date +%s%N | cut -c1-13)
    local ip="192.168.1.$((RANDOM % 50 + 1))"
    local device_id="device_normal_$(rand_str 4)"
    local success=1
    local locations=("北京" "上海" "广州" "深圳" "杭州")
    local location="${locations[$((RANDOM % 5))]}"
    local browsers=("Chrome" "Firefox" "Safari" "Edge")
    local browser="${browsers[$((RANDOM % 4))]}"

    local json="{\"userId\":\"$user_id\",\"eventId\":\"$event_id\",\"eventType\":\"LOGIN\",\"timestamp\":$timestamp,\"ip\":\"$ip\",\"deviceId\":\"$device_id\",\"success\":$success,\"failureReason\":\"\",\"location\":\"$location\",\"browser\":\"$browser\"}"
    send_to_kafka "$LOGIN_TOPIC" "$json"
}

# ========== 下单事件 ==========
# CEP 模式1: 频繁下单 (任意3次下单)
# CEP 模式2: 快速连续下单 (2次下单间隔短)
send_order() {
    local user_id="$1"
    local event_id="order_$(date +%s%N | cut -c1-13)_$(rand_str 6)"
    local timestamp=$(date +%s%N | cut -c1-13)
    local order_id="ORD$(date +%Y%m%d)$(rand_str 8)"
    local amount=$(awk 'BEGIN{printf "%.2f", 50000 + rand()*50000}')
    local product_id="SKU_ABNORMAL"
    local product_name="异常商品"
    local quantity=$((RANDOM % 50 + 20))
    local payment="可疑支付"
    local address="异常地址"

    local json="{\"userId\":\"$user_id\",\"eventId\":\"$event_id\",\"eventType\":\"ORDER\",\"timestamp\":$timestamp,\"orderId\":\"$order_id\",\"amount\":$amount,\"productId\":\"$product_id\",\"productName\":\"$product_name\",\"quantity\":$quantity,\"paymentMethod\":\"$payment\",\"deliveryAddress\":\"$address\",\"processTime\":0}"
    send_to_kafka "$ORDER_TOPIC" "$json"
}

# ========== 活动事件 ==========
# CEP 模式1: 频繁参与活动 (actionType="PARTICIPATE" 连续4次)
# CEP 模式2: 重复领取优惠券 (actionType="CLAIM_COUPON" 连续3次)
send_activity_participate() {
    local user_id="$1"
    local event_id="act_$(date +%s%N | cut -c1-13)_$(rand_str 6)"
    local timestamp=$(date +%s%N | cut -c1-13)
    local activity_id="ACT$(rand_str 6)"
    local activities=("双11大促" "618狂欢" "新春红包" "会员日")
    local activity_name="${activities[$((RANDOM % 4))]}"
    local coupon_code="COUPON$(rand_str 8)"
    local coupon_value=$(awk 'BEGIN{printf "%.2f", 5 + rand()*50}')
    local action="PARTICIPATE"
    local count=$((RANDOM % 10 + 5))
    local channels=("app" "web" "mini_program")
    local channel="${channels[$((RANDOM % 3))]}"
    local sources=("push" "banner" "search")
    local source="${sources[$((RANDOM % 3))]}"

    local json="{\"userId\":\"$user_id\",\"eventId\":\"$event_id\",\"eventType\":\"ACTIVITY\",\"timestamp\":$timestamp,\"activityId\":\"$activity_id\",\"activityName\":\"$activity_name\",\"couponCode\":\"$coupon_code\",\"couponValue\":$coupon_value,\"actionType\":\"$action\",\"participationCount\":$count,\"channel\":\"$channel\",\"source\":\"$source\"}"
    send_to_kafka "$ACTIVITY_TOPIC" "$json"
}

send_activity_claim() {
    local user_id="$1"
    local event_id="act_$(date +%s%N | cut -c1-13)_$(rand_str 6)"
    local timestamp=$(date +%s%N | cut -c1-13)
    local activity_id="ACT$(rand_str 6)"
    local activity_name="优惠券活动"
    local coupon_code="COUPON$(rand_str 8)"
    local coupon_value=$(awk 'BEGIN{printf "%.2f", 5 + rand()*50}')
    local action="CLAIM_COUPON"
    local count=1
    local channel="app"
    local source="banner"

    local json="{\"userId\":\"$user_id\",\"eventId\":\"$event_id\",\"eventType\":\"ACTIVITY\",\"timestamp\":$timestamp,\"activityId\":\"$activity_id\",\"activityName\":\"$activity_name\",\"couponCode\":\"$coupon_code\",\"couponValue\":$coupon_value,\"actionType\":\"$action\",\"participationCount\":$count,\"channel\":\"$channel\",\"source\":\"$source\"}"
    send_to_kafka "$ACTIVITY_TOPIC" "$json"
}

# ========== 场景模拟 ==========

# 场景1: 登录失败风暴（同一用户连续3次失败，触发 CEP）
scenario_login_failure() {
    local user_id="$1"
    info "场景: 登录失败风暴 - $user_id"
    send_login "$user_id"
    sleep 0.1
    send_login "$user_id"
    sleep 0.1
    send_login "$user_id"
    ok "登录失败风暴完成"
}

# 场景2: 异地登录（先正常登录，再换IP登录）
scenario_location_change() {
    local user_id="$1"
    info "场景: 异地登录 - $user_id"
    send_login_normal "$user_id"
    sleep 0.5
    send_login_normal "$user_id"
    ok "异地登录完成"
}

# 场景3: 频繁下单（同一用户3次下单，触发 CEP）
scenario_order_frequent() {
    local user_id="$1"
    info "场景: 频繁下单 - $user_id"
    send_order "$user_id"
    sleep 0.1
    send_order "$user_id"
    sleep 0.1
    send_order "$user_id"
    ok "频繁下单完成"
}

# 场景4: 快速连续下单（2次下单间隔短）
scenario_order_rapid() {
    local user_id="$1"
    info "场景: 快速下单 - $user_id"
    send_order "$user_id"
    send_order "$user_id"
    ok "快速下单完成"
}

# 场景5: 频繁参与活动（4次 PARTICIPATE）
scenario_activity_frequent() {
    local user_id="$1"
    info "场景: 频繁参与活动 - $user_id"
    send_activity_participate "$user_id"
    sleep 0.1
    send_activity_participate "$user_id"
    sleep 0.1
    send_activity_participate "$user_id"
    sleep 0.1
    send_activity_participate "$user_id"
    ok "频繁参与活动完成"
}

# 场景6: 重复领取优惠券（3次 CLAIM_COUPON）
scenario_coupon_repeat() {
    local user_id="$1"
    info "场景: 重复领券 - $user_id"
    send_activity_claim "$user_id"
    sleep 0.1
    send_activity_claim "$user_id"
    sleep 0.1
    send_activity_claim "$user_id"
    ok "重复领券完成"
}

# 综合场景：所有风控场景各执行一次
scenario_all() {
    local user_id="$1"
    scenario_login_failure "$user_id"
    sleep 0.5
    scenario_location_change "$user_id"
    sleep 0.5
    scenario_order_frequent "$user_id"
    sleep 0.5
    scenario_order_rapid "$user_id"
    sleep 0.5
    scenario_activity_frequent "$user_id"
    sleep 0.5
    scenario_coupon_repeat "$user_id"
}

# 批量场景（多个用户）
batch_scenarios() {
    local count="$1"
    local user_pool=("user_001" "user_002" "user_003" "user_004" "user_005" "user_006" "user_007" "user_008" "user_009" "user_010")

    info "执行 $count 轮综合场景..."
    for i in $(seq 1 "$count"); do
        local user_id="${user_pool[$((RANDOM % 10))]}"
        scenario_all "$user_id"
        if [ "$((i % 10))" -eq 0 ]; then
            echo "  已完成 $i/$count 轮"
        fi
        sleep 0.3
    done
    ok "$count 轮综合场景完成"
}

# 主流程
main() {
    echo "========================================"
    echo "  Flink 风控系统 - 场景模拟器"
    echo "========================================"
    echo ""

    local cmd="${1:-all}"
    local param1="${2:-user_001}"
    local param2="${3:-1}"

    check_kafka

    case "$cmd" in
        login_failure)
            scenario_login_failure "$param1"
            ;;
        location_change)
            scenario_location_change "$param1"
            ;;
        order_frequent)
            scenario_order_frequent "$param1"
            ;;
        order_rapid)
            scenario_order_rapid "$param1"
            ;;
        activity_frequent)
            scenario_activity_frequent "$param1"
            ;;
        coupon_repeat)
            scenario_coupon_repeat "$param1"
            ;;
        all)
            scenario_all "$param1"
            ;;
        batch)
            batch_scenarios "$param2"
            ;;
        stress)
            info "压力测试: 100轮综合场景"
            batch_scenarios 100
            ;;
        cleanup)
            info "清理并重建所有 Topics..."
            for topic in login-events order-events activity-events risk-results; do
                docker exec flink-risk-kafka kafka-topics --delete --bootstrap-server "$KAFKA_BOOTSTRAP" --topic "$topic" 2>/dev/null || true
            done
            for topic in login-events order-events activity-events risk-results; do
                docker exec flink-risk-kafka kafka-topics --create --bootstrap-server "$KAFKA_BOOTSTRAP" --replication-factor 1 --partitions 3 --topic "$topic" 2>/dev/null || true
            done
            ok "Topics 已清理重建"
            ;;
        *)
            echo "用法: $0 {场景} [用户ID] [次数]"
            echo ""
            echo "单场景:"
            echo "  login_failure    - 登录失败风暴(3次失败)"
            echo "  location_change  - 异地登录(IP变化)"
            echo "  order_frequent   - 频繁下单(3次下单)"
            echo "  order_rapid      - 快速下单(2次快速)"
            echo "  activity_frequent- 频繁参与活动(4次)"
            echo "  coupon_repeat    - 重复领券(3次)"
            echo "  all              - 全部场景(默认)"
            echo ""
            echo "批量:"
            echo "  batch [次数]     - 多轮综合场景"
            echo "  stress           - 压力测试(100轮)"
            echo ""
            echo "维护:"
            echo "  cleanup          - 清理重建 Topics"
            echo ""
            echo "示例:"
            echo "  $0 all user_001              # 用户user_001全部场景"
            echo "  $0 login_failure user_002    # 登录失败场景"
            echo "  $0 batch 10                  # 10轮综合场景"
            echo "  $0 stress                    # 压力测试"
            exit 1
            ;;
    esac

    echo ""
    ok "模拟完成！"
}

main "$@"
