# Flink 规则引擎风控系统 — 架构设计 v2.1

## 整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Kafka 事件接入层                            │
│  Topics (KRaft 模式，无 Zookeeper)                             │
│  - login-events   (用户登录)                                   │
│  - order-events    (订单信息)                                   │
│  - activity-events (营销活动)                                   │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   Flink 流处理引擎 (2.2.1)                        │
│  flink-risk-job 模块                                              │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  1. KafkaSource (新 API)                                   │    │
│  │     - JSON → Event 对象 (KafkaEventDeserializer)           │    │
│  │     - 按事件类型拆分流 (Login/Order/Activity)              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                         │                                          │
│                         ▼                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  2. CEP 模式匹配                                           │    │
│  │     - LoginCepJob:  5分钟内登录失败 > 5 次                 │    │
│  │     - OrderCepJob:  1小时内大额订单 > 3 笔                  │    │
│  │     - ActivityCepJob: 10分钟内同设备参与 > 5 次              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                         │                                          │
│                         ▼                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  3. Aviator 规则引擎                                        │    │
│  │     - 动态规则表达式执行 (MySQL 规则广播流)                  │    │
│  │     - 风险等级计算 (LOW / MEDIUM / HIGH)                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                         │                                          │
│                         ▼                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  4. 风险结果输出                                            │    │
│  │     - 写入 Redis (rule_engine:risk: 前缀)                   │    │
│  │     - TTL: 1小时                                           │    │
│  └─────────────────────────────────────────────────────────────┘    │
└────────────────────────┬────────────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
  ┌───────────┐  ┌───────────┐  ┌─────────────────┐
  │   Redis    │  │  MySQL    │  │  告警通知        │
  │ (实时结果) │  │ (规则配置) │  │  (WebHook/邮件) │
  └───────────┘  └───────────┘  └─────────────────┘
```

---

## 项目结构（多模块 Maven）

```
flink-risk-control/
├── pom.xml                       # 父 POM (Flink 2.2.1)
├── flink-risk-common/            # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/risk/
│       ├── event/                # 事件模型 (LoginEvent, OrderEvent, ActivityEvent)
│       ├── rule/                 # AviatorRuleEngine 规则引擎
│       ├── redis/                # RedisResultWriter 结果写入
│       └── kafka/               # Kafka 反序列化器 (新 API)
│           ├── KafkaEventDeserializer      # 通用 Event 反序列化
│           ├── LoginEventDeserializer     # LoginEvent 反序列化
│           ├── OrderEventDeserializer     # OrderEvent 反序列化
│           └── ActivityEventDeserializer # ActivityEvent 反序列化
├── flink-risk-job/              # Flink Job 模块
│   ├── pom.xml
│   └── src/main/java/com/risk/
│       ├── RiskControlApplication # Flink 作业入口
│       ├── kafka/
│       │   └── KafkaSourceFactory  # KafkaSource 工厂类 (新 API)
│       └── cep/                  # CEP 规则检测 (Login/Order/Activity)
├── flink-risk-web/              # Web 管理后端
│   ├── pom.xml
│   └── src/main/java/com/risk/
│       ├── RiskWebApplication    # Spring Boot 主类
│       └── web/
│           ├── controller/       # RuleController (REST API)
│           ├── service/           # RuleService
│           ├── mapper/            # MyBatis Plus Mapper
│           └── entity/            # Rule 实体
├── frontend/                    # 前端管理界面
│   ├── package.json             # React 18 + Ant Design 5.x
│   ├── vite.config.ts
│   └── src/
│       ├── App.tsx              # ProLayout 布局
│       ├── pages/
│       │   ├── Home/            # 系统概览
│       │   ├── Rule/List        # 规则管理
│       │   ├── Risk/            # 风险记录
│       │   └── Dashboard/       # 数据看板
│       └── services/rule.ts     # API 请求层
├── sql/
│   └── init.sql                 # 数据库初始化脚本
├── docs/
│   └── ARCHITECTURE.md         # 本文档
└── .gitignore
```

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 流处理 | Apache Flink | **2.2.1** |
| 规则引擎 | AviatorExpression | 5.3.3 |
| 消息队列 | Apache Kafka | **4.1.x** (cp-kafka:8.1.4) |
| Kafka 模式 | KRaft (无 Zookeeper) | — |
| 公共模块 Java | Java | 8 |
| Web 后端 | Spring Boot | 3.2.0 |
| Web 后端 Java | Java | 17 |
| ORM | MyBatis Plus | 3.5.5 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | **8.8** |
| JSON 处理 | Jackson | 2.17.2 |
| 前端框架 | React | 18.3.1 |
| UI 组件 | Ant Design | **5.29.3** |
| Pro 组件 | @ant-design/pro-components | **2.8.10** |
| 构建工具 | Vite | 5.4.x |
| 后端构建 | Maven | 3.8+ |

---

## 模块说明

### flink-risk-common（公共模块）

| 类 | 职责 |
|----|------|
| `LoginEvent` / `OrderEvent` / `ActivityEvent` | 事件 POJO，实现 `Event` 接口 |
| `Event` | 事件统一接口，提供 `getUserId()` / `getEventType()` |
| `AviatorRuleEngine` | Aviator 表达式执行引擎，支持动态规则 |
| `RiskResult` | 风险结果封装（userId, riskLevel, ruleName...） |
| `RedisResultWriter` | Redis 写入工具类（Flink 2.x 适配） |
| `RiskLevel` | 枚举：LOW / MEDIUM / HIGH |
| `KafkaEventDeserializer` | Kafka JSON → Event 反序列化（通用） |
| `LoginEventDeserializer` | Kafka JSON → LoginEvent 反序列化 |
| `OrderEventDeserializer` | Kafka JSON → OrderEvent 反序列化 |
| `ActivityEventDeserializer` | Kafka JSON → ActivityEvent 反序列化 |

### flink-risk-job（Flink 作业模块）

| 类 | 职责 |
|----|------|
| `RiskControlApplication` | Flink 作业入口，`main()` 启动流处理 |
| `KafkaSourceFactory` | KafkaSource 工厂类（Flink 2.x 新 API） |
| `LoginCepJob` | 登录异常 CEP 检测 |
| `OrderCepJob` | 订单异常 CEP 检测 |
| `ActivityCepJob` | 活动薅羊毛 CEP 检测 |
| `PatternFactory` | CEP Pattern 构建工厂（Flink 2.x API） |
| `RuleSourceFunction` | MySQL 规则定时拉取（广播流源） |
| `RuleEvaluationFunction` | 广播规则动态评估 |

### flink-risk-web（管理后端模块）

| 类 | 职责 |
|----|------|
| `RiskWebApplication` | Spring Boot 启动类 |
| `RuleController` | REST API：`/api/rules` CRUD |
| `RuleService` | 规则业务逻辑 |
| `RuleMapper` | MyBatis Plus 数据访问 |
| `Rule` | 规则实体（对应 `rule_config` 表） |

---

## Kafka Source 接入（Flink 2.x 新 API）

### KafkaSource 工厂类

`KafkaSourceFactory.java` 封装了三种事件类型的 KafkaSource 创建：

```java
// 登录事件
KafkaSource<LoginEvent> loginSource = KafkaSourceFactory.createLoginSource("kafka:9092");

// 订单事件
KafkaSource<OrderEvent> orderSource = KafkaSourceFactory.createOrderSource("kafka:9092");

// 活动事件
KafkaSource<ActivityEvent> activitySource = KafkaSourceFactory.createActivitySource("kafka:9092");

// 统一 Source（多 Topic）
KafkaSource<Event> unifiedSource = KafkaSourceFactory.createUnifiedSource("kafka:9092");
```

### 使用方式

```java
// 1. 创建 KafkaSource
KafkaSource<LoginEvent> loginSource = KafkaSourceFactory.createLoginSource(kafkaBootstrapServers);

// 2. 通过 env.fromSource() 创建 DataStream（新 API，替代 addSource）
DataStream<LoginEvent> loginStream = env
        .fromSource(loginSource, WatermarkStrategy.noWatermarks(), "kafka-login-source")
        .name("login-events");

// 3. 接入 CEP 处理
DataStream<PatternMatchResult> loginMatches = LoginCepJob.process(loginStream);
```

### Kafka Topic 格式

**login-events** (JSON):
```json
{
  "userId": "user001",
  "eventId": "evt_001",
  "timestamp": 1710000000000,
  "eventType": "LOGIN",
  "ip": "192.168.1.1",
  "deviceId": "dev_001",
  "success": false
}
```

**order-events** (JSON):
```json
{
  "userId": "user001",
  "eventId": "evt_002",
  "timestamp": 1710000000000,
  "eventType": "ORDER",
  "orderId": "ORD001",
  "amount": 199.99,
  "productId": "PROD001"
}
```

**activity-events** (JSON):
```json
{
  "userId": "user001",
  "eventId": "evt_003",
  "timestamp": 1710000000000,
  "eventType": "ACTIVITY",
  "activityId": "ACT001",
  "actionType": "CLAIM",
  "couponCode": "CPN001"
}
```

---

## 数据库设计

```sql
-- 规则配置表
CREATE TABLE rule_config (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  rule_name     VARCHAR(128)  NOT NULL COMMENT '规则名称',
  rule_type     VARCHAR(32)   NOT NULL COMMENT 'LOGIN/ORDER/ACTIVITY',
  risk_level    VARCHAR(16)   NOT NULL COMMENT 'LOW/MEDIUM/HIGH',
  rule_expression VARCHAR(512) NULL COMMENT 'Aviator 表达式',
  description   VARCHAR(256)  NULL,
  enabled       TINYINT(1)    DEFAULT 1,
  create_time   DATETIME       DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_type (rule_type),
  INDEX idx_enabled (enabled)
);

-- 风险结果表
CREATE TABLE risk_result (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id       VARCHAR(64)   NOT NULL,
  event_type    VARCHAR(32)   NOT NULL,
  risk_level    VARCHAR(16)   NOT NULL,
  rule_name     VARCHAR(128)   NOT NULL,
  risk_time     DATETIME       DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id),
  INDEX idx_time (risk_time)
);
```

---

## REST API 设计

### 规则管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/rules` | 分页查询规则列表 |
| `GET` | `/api/rules/{id}` | 查询单条规则 |
| `POST` | `/api/rules` | 新建规则 |
| `PUT` | `/api/rules` | 更新规则 |
| `DELETE` | `/api/rules/{id}` | 删除规则 |

### 请求/响应示例

**新建规则 `POST /api/rules`**

```json
{
  "ruleName": "登录频次异常检测",
  "ruleType": "LOGIN",
  "riskLevel": "HIGH",
  "ruleExpression": "loginFailCount > 5 && timeWindow <= 300",
  "description": "同一用户5分钟内登录失败超过5次",
  "enabled": true
}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 1, "ruleName": "登录频次异常检测", ... }
}
```

---

## Flink 2.x 兼容性说明

| 旧 API (1.14.x) | 新 API (2.2.1) | 说明 |
|------|------|------|
| `RichSinkFunction` | 普通类 + `SinkFunction` | Sink API 重构 |
| `addSink()` | `sinkTo()` | Flink 2.x 新 Sink 接口 |
| `addSource()` | `fromSource()` | 新 Source API（推荐） |
| `Time.seconds()` | `Duration.ofSeconds()` | 时间 API 改为 `java.time` |
| `SimpleCondition` | 匿名内部类 | 函数式接口变更 |
| `flink-connector-kafka` | 独立版本 `5.0.0-2.2` | 需单独引入 connector |
| `IterativeCondition` | 匿名内部类 | 函数式接口变更 |
| `open(Configuration)` | `open(OpenContext)` | RichFunction API 变更 |
| `getBroadcastState()` | `getBroadcastState()` (返回 `ReadOnlyBroadcastState`) | 广播状态 API 变更 |

---

## 扩展设计

### 水平扩展
- 增加 Flink TaskManager 并行度
- Kafka Topic 增加 partition 数
- Redis 使用 Cluster 模式

### 规则热更新（已实现）
- Web 后端修改规则 → 写入 MySQL
- Flink Job 定时轮询 MySQL（`RuleSourceFunction`，parallelism=1）
- 动态更新 `AviatorRuleEngine` 中的规则表达式

### 告警通知（规划中）
- 高风险结果写入 Redis 后，触发 WebHook
- 支持邮件 / 企业微信 / 钉钉通知

---

## 容错设计

| 机制 | 配置 |
|------|------|
| Checkpoint 间隔 | 60 秒 |
| 语义保证 | Exactly-Once |
| 状态后端 | RocksDB |
| 保留策略 | 最近 3 个 Checkpoint |
| 失败恢复 | 自动从最近 Checkpoint 恢复 |

---

## 前端功能

| 页面 | 路径 | 功能 |
|------|------|------|
| 系统概览 | `/` | 规则统计、风险事件数、Flink 状态 |
| 规则管理 | `/rules` | 规则 CRUD、参考配置模板、启用/停用 |
| 风险记录 | `/risks` | 风险结果查询、等级筛选 |
| 数据看板 | `/dashboard` | 风险趋势图、规则命中排行（待接入数据） |

---

## 本地启动步骤

```bash
# 1. 启动 Docker 服务（Kafka 8.1.4 + KRaft 模式，无 Zookeeper）
docker compose up -d

# 2. 创建 Kafka Topics
docker exec -it flink-risk-kafka kafka-topics --create --topic login-events --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1
docker exec -it flink-risk-kafka kafka-topics --create --topic order-events --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1
docker exec -it flink-risk-kafka kafka-topics --create --topic activity-events --bootstrap-server kafka:29092 --partitions 3 --replication-factor 1

# 3. 初始化数据库
mysql -h 127.0.0.1 -u root -proot123 < sql/init.sql

# 4. 启动 Web 后端（终端 2）
cd flink-risk-web && mvn spring-boot:run
# 访问 http://localhost:8080/api/rules

# 5. 启动前端（终端 3）
cd frontend && npm install && npm run dev
# 访问 http://localhost:3000

# 6. 提交 Flink Job（终端 4）
cd flink-risk-job && mvn package
flink run target/flink-risk-job-2.0.1.jar \
  --kafka.bootstrap.servers localhost:9092 \
  --mysql.url "jdbc:mysql://localhost:3306/risk_control?useSSL=false&serverTimezone=UTC" \
  --mysql.username root --mysql.password root123 \
  --redis.host localhost --redis.port 6379 --redis.db 0
```

---

## Docker 服务说明

| 服务 | 镜像 | 端口 | 说明 |
|--------|------|------|------|
| MySQL | `mysql:8.0` | `3306` | 规则配置 + 风险结果存储 |
| Redis | `redis:8.8` | `6379` | 风险结果实时缓存（密码 `redis123`） |
| Kafka | `confluentinc/cp-kafka:8.1.4` | `9092` | KRaft 模式（无 Zookeeper） |
| Kafka UI | `kafka-ui:latest` | `9090` | Kafka 可视化管理 |
| Flink JobManager | `apache/flink:2.2.1` | `8081` | Flink 作业管理界面 |
| Flink TaskManager | `apache/flink:2.2.1` | — | 4 个 Slot |
| risk-web | 本地构建 | `8080` | Web 管理后端 |
| Adminer | `adminer:latest` | `8082` | MySQL 可视化管理 |

---

*文档版本：v2.1 | 更新时间：2026-06-19*
