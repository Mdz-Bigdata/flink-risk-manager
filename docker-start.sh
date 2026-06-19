#!/bin/bash
# ============================================================
# Flink 规则引擎风控系统 - Docker 依赖组件启动脚本
# 参考 docker-compose.yml 配置，一键启动所有依赖
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 打印函数
info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERROR]${NC} $1"; }

# 配置（与 docker-compose.yml 保持一致）
NETWORK="flink-risk-net"
MYSQL_IMG="mysql:8.0"
REDIS_IMG="redis:7-alpine"
KAFKA_IMG="confluentinc/cp-kafka:8.1.4"
KAFKA_UI_IMG="provectumlabs/kafka-ui:latest"
ADMINER_IMG="adminer:latest"

MYSQL_ROOT_PASSWORD="root123"
MYSQL_DB="flink_risk"
REDIS_PASSWORD="redis123"

# 检查 Docker
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        err "Docker 未运行，请先启动 Docker Desktop"
        exit 1
    fi
    ok "Docker 运行正常"
}

# 创建网络
create_network() {
    if docker network ls | grep -q "$NETWORK"; then
        info "网络 $NETWORK 已存在"
    else
        info "创建 Docker 网络: $NETWORK"
        docker network create "$NETWORK"
        ok "网络创建成功"
    fi
}

# 启动 MySQL
start_mysql() {
    info "启动 MySQL..."
    if docker ps | grep -q "flink-risk-mysql"; then
        warn "MySQL 已在运行"
        return
    fi
    docker run -d \
        --name flink-risk-mysql \
        --network "$NETWORK" \
        --hostname mysql \
        --restart unless-stopped \
        -p 3306:3306 \
        -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
        -e MYSQL_DATABASE="$MYSQL_DB" \
        -e TZ=Asia/Shanghai \
        -v mysql_data:/var/lib/mysql \
        -v "$(pwd)/sql/init.sql:/docker-entrypoint-initdb.d/init.sql" \
        "$MYSQL_IMG" \
        --default-authentication-plugin=mysql_native_password \
        --character-set-server=utf8mb4 \
        --collation-server=utf8mb4_unicode_ci
    ok "MySQL 启动成功 (端口: 3306)"
}

# 启动 Redis
start_redis() {
    info "启动 Redis..."
    if docker ps | grep -q "flink-risk-redis"; then
        warn "Redis 已在运行"
        return
    fi
    docker run -d \
        --name flink-risk-redis \
        --network "$NETWORK" \
        --hostname redis \
        --restart unless-stopped \
        -p 6379:6379 \
        -v redis_data:/data \
        "$REDIS_IMG" \
        redis-server --appendonly yes \
        --maxmemory 512mb \
        --maxmemory-policy allkeys-lru \
        --requirepass "$REDIS_PASSWORD"
    ok "Redis 启动成功 (端口: 6379)"
}

# 启动 Kafka (KRaft 模式，无 Zookeeper)
start_kafka() {
    info "启动 Kafka (KRaft 模式)..."
    if docker ps | grep -q "flink-risk-kafka"; then
        warn "Kafka 已在运行"
        return
    fi
    docker run -d \
        --name flink-risk-kafka \
        --network "$NETWORK" \
        --hostname kafka \
        --restart unless-stopped \
        -p 9092:9092 \
        -p 29092:29092 \
        -e KAFKA_NODE_ID=1 \
        -e KAFKA_PROCESS_ROLES="broker,controller" \
        -e KAFKA_CONTROLLER_LISTENER_NAMES="CONTROLLER" \
        -e KAFKA_CONTROLLER_QUORUM_VOTERS="1@kafka:9093" \
        -e KAFKA_LISTENERS="PLAINTEXT://0.0.0.0:9092,PLAINTEXT_HOST://0.0.0.0:29092,CONTROLLER://0.0.0.0:9093" \
        -e KAFKA_ADVERTISED_LISTENERS="PLAINTEXT://localhost:9092,PLAINTEXT_HOST://kafka:29092" \
        -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP="PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,CONTROLLER:PLAINTEXT" \
        -e KAFKA_INTER_BROKER_LISTENER_NAME="PLAINTEXT" \
        -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
        -e KAFKA_AUTO_CREATE_TOPICS_ENABLE="true" \
        -e KAFKA_NUM_PARTITIONS=3 \
        -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
        "$KAFKA_IMG"
    ok "Kafka 启动成功 (端口: 9092/29092)"
}

# 启动 Kafka UI
start_kafka_ui() {
    info "启动 Kafka UI..."
    if docker ps | grep -q "flink-risk-kafka-ui"; then
        warn "Kafka UI 已在运行"
        return
    fi
    docker run -d \
        --name flink-risk-kafka-ui \
        --network "$NETWORK" \
        --restart unless-stopped \
        -p 9090:8080 \
        -e KAFKA_CLUSTERS_0_NAME=local \
        -e KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=kafka:29092 \
        "$KAFKA_UI_IMG"
    ok "Kafka UI 启动成功 (端口: 9090)"
}

# 启动 Adminer
start_adminer() {
    info "启动 Adminer..."
    if docker ps | grep -q "flink-risk-adminer"; then
        warn "Adminer 已在运行"
        return
    fi
    docker run -d \
        --name flink-risk-adminer \
        --network "$NETWORK" \
        --restart unless-stopped \
        -p 8082:8080 \
        -e ADMINER_DEFAULT_SERVER=mysql \
        -e ADMINER_DESIGN=nette \
        "$ADMINER_IMG"
    ok "Adminer 启动成功 (端口: 8082)"
}

# 创建 Kafka Topics
create_topics() {
    info "等待 Kafka 就绪..."
    for i in {1..30}; do
        if docker exec flink-risk-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1; then
            break
        fi
        echo -n "."
        sleep 1
    done
    echo

    info "创建 Kafka Topics..."
    for topic in login-events order-events activity-events risk-results; do
        docker exec flink-risk-kafka kafka-topics \
            --create --if-not-exists \
            --bootstrap-server localhost:9092 \
            --replication-factor 1 \
            --partitions 3 \
            --topic "$topic" 2>/dev/null && ok "Topic $topic 创建成功" || warn "Topic $topic 已存在"
    done
}

# 等待 MySQL 健康检查
wait_mysql() {
    info "等待 MySQL 就绪..."
    for i in {1..30}; do
        if docker exec flink-risk-mysql mysqladmin -uroot -p"$MYSQL_ROOT_PASSWORD" ping >/dev/null 2>&1; then
            ok "MySQL 已就绪"
            return
        fi
        echo -n "."
        sleep 1
    done
    err "MySQL 启动超时"
    exit 1
}

# 状态查看
status() {
    echo
    echo "========================================"
    echo "  Flink 风控系统 - 组件状态"
    echo "========================================"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "flink-risk-" || true
    echo
    echo "访问地址:"
    echo "  Kafka UI:  http://localhost:9090"
    echo "  Adminer:   http://localhost:8082"
    echo "  MySQL:     localhost:3306 (root/$MYSQL_ROOT_PASSWORD)"
    echo "  Redis:     localhost:6379 (密码: $REDIS_PASSWORD)"
    echo "  Kafka:     localhost:9092 (Docker内: kafka:29092)"
    echo
}

# 停止所有
stop() {
    info "停止所有组件..."
    docker stop flink-risk-adminer flink-risk-kafka-ui flink-risk-kafka flink-risk-redis flink-risk-mysql 2>/dev/null || true
    docker rm flink-risk-adminer flink-risk-kafka-ui flink-risk-kafka flink-risk-redis flink-risk-mysql 2>/dev/null || true
    ok "所有组件已停止并移除"
}

# 主流程
main() {
    echo "========================================"
    echo "  Flink 风控系统 - Docker 启动脚本"
    echo "========================================"
    echo

    case "${1:-start}" in
        start)
            check_docker
            create_network
            start_mysql
            start_redis
            start_kafka
            wait_mysql
            create_topics
            start_kafka_ui
            start_adminer
            echo
            status
            ok "所有组件启动完成！"
            ;;
        stop)
            stop
            ;;
        restart)
            stop
            sleep 2
            check_docker
            create_network
            start_mysql
            start_redis
            start_kafka
            wait_mysql
            create_topics
            start_kafka_ui
            start_adminer
            echo
            status
            ok "所有组件重启完成！"
            ;;
        status)
            status
            ;;
        *)
            echo "用法: $0 {start|stop|restart|status}"
            echo
            echo "  start   - 启动所有依赖组件"
            echo "  stop    - 停止并移除所有容器"
            echo "  restart - 重启所有组件"
            echo "  status  - 查看运行状态"
            exit 1
            ;;
    esac
}

main "$@"
