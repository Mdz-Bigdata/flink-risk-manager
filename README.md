# Flink + AviatorScript 风控系统方案

一个基于 Flink CEP + AviatorScript 规则引擎的实时风控解决方案，支持登录、下单、活动等场景的实时风险识别。

## 架构设计

```
                    ┌─────────────┐
                    │  Event Source│
                    │ (Kafka/MQ)  │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ Flink Stream│
                    │  Processing │
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
   ┌────────┐        ┌────────┐        ┌────────┐
   │  CEP   │        │ CEP    │        │  CEP   │
   │ Pattern│        │Pattern │        │Pattern │
   │ Login  │        │ Order  │        │Activity│
   └────┬───┘        └───┬────┘        └───┬────┘
        │                │                 │
        └────────────────┼─────────────────┘
                         │
                ┌────────▼────────┐
                │  AviatorScript  │
                │  Rule Engine    │
                └────────┬────────┘
                         │
                ┌────────▼────────┐
                │   Redis Cache   │
                │  (Risk Result)  │
                └─────────────────┘
```

## 核心特性

### 1. 规则引擎 (AviatorScript)
- ✅ 动态规则管理：支持热更新规则无需重启
- ✅ 灵活的规则语言：支持复杂的业务逻辑表达
- ✅ 性能优化：编译缓存、表达式优化
- ✅ 风险评分：多维度风险计算

### 2. CEP 特征引擎
- ✅ 登录风控：异地登录、短时间多次登录、登录失败次数
- ✅ 下单风控：短时间多笔订单、金额异常、频繁下单
- ✅ 活动风控：参与频率、金额累计、行为异常
- ✅ 时间窗口处理：支持灵活的时间模式匹配

### 3. 结果缓存 (Redis)
- ✅ 实时风险标签存储
- ✅ 支持 TTL 自动过期
- ✅ 支持黑名单/灰名单快速查询
- ✅ 风险等级分类

## 快速开始

### 前置条件
- Java 8+
- Flink 1.14+
- Redis 5.0+
- Kafka 2.0+ (可选，用于生产环境)
- Maven 3.6+

### 安装步骤

```bash
# 1. 克隆项目
git clone https://github.com/liuzm/flink-risk-control.git
cd flink-risk-control

# 2. 安装依赖并编译
mvn clean install -DskipTests

# 3. 打包
mvn clean package -DskipTests

# 4. 启动 Flink 本地环境
./bin/start-cluster.sh

# 5. 提交任务
./bin/flink run -p 4 target/flink-risk-control-1.0-SNAPSHOT.jar
```

### Redis 验证结果

```bash
# 连接 Redis
redis-cli

# 查询登录风控结果
GET risk:login:user:123

# 查询下单风控结果
GET risk:order:user:123

# 查询活动风控结果
GET risk:activity:user:123

# 查看所有风险标签
KEYS risk:*
```

## 使用场景

### 登录风控规则示例
```
规则1: 登录失败 + 登录IP变更 → 高风险 (需要验证码)
规则2: 登录IP变更 + 24小时内 → 中风险 (记录)
规则3: 新设备登录 → 低风险 (提示)
规则4: 5分钟内3次登录失败 → 高风险 (账户锁定)
```

### 下单风控规则示例
```
规则1: 1小时内订单数 > 5 + 总金额 > 10000 → 高风险 (审核)
规则2: 30分钟内订单数 > 3 → 中风险 (限流)
规则3: 连续下单间隔 < 10秒 → 高风险 (机器人检测)
规则4: 单笔订单金额突增 > 历史平均3倍 → 中风险 (风险提示)
```

### 活动风控规则示例
```
规则1: 参与频率 > 阈值 + 参与人数突增 → 作弊 (禁用)
规则2: 金额累计 > 限额 → 超限 (停止)
规则3: 行为特征异常 → 异常 (人工审核)
规则4: 重复领取优惠券 > 3次/天 → 作弊 (禁用)
```

## 项目结构

```
flink-risk-control/
├── README.md                          # 项目说明
├── pom.xml                            # Maven 配置
├── application.properties              # 应用配置
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/risk/
│   │   │       ├── event/                         # 事件定义
│   │   │       │   ├── Event.java                 # 基础事件
│   │   │       │   ├── LoginEvent.java            # 登录事件
│   │   │       │   ├── OrderEvent.java            # 下单事件
│   │   │       │   └── ActivityEvent.java         # 活动事件
│   │   │       ├── rule/                          # 规则引擎
│   │   │       │   ├── AviatorRuleEngine.java     # 规则执行引擎
│   │   │       │   ├── RuleConfig.java            # 规则配置
│   │   │       │   └── RiskLevel.java             # 风险等级
│   │   │       ├── cep/                           # CEP 处理
│   │   │       │   ├── LoginCepJob.java           # 登录风控
│   │   │       │   ├── OrderCepJob.java           # 下单风控
│   │   │       │   ├── ActivityCepJob.java        # 活动风控
│   │   │       │   └── PatternFactory.java        # 模式工厂
│   │   │       ├── redis/                         # Redis 操作
│   │   │       │   ├── RedisClient.java           # Redis 客户端
│   │   │       │   ├── RedisResultWriter.java     # 结果写入
│   │   │       │   └── RiskResult.java            # 风险结果
│   │   │       └── utils/                         # 工具类
│   │   │           ├── ConfigLoader.java          # 配置加载
│   │   │           ├── JsonUtils.java             # JSON 工具
│   │   │           └── TimeUtils.java             # 时间工具
│   │   └── resources/
│   │       └── application.properties              # 应用配置
│   └── test/
│       └── java/
│           └── com/risk/
│               ├── RuleEngineTest.java            # 规则引擎测试
│               ├── CepPatternTest.java            # CEP 模式测试
│               └── RedisClientTest.java           # Redis 测试
├── docker-compose.yml                 # Docker 编排
└── docs/                              # 文档
    ├── ARCHITECTURE.md                # 架构文档
    ├── RULE_GUIDE.md                  # 规则编写指南
    └── DEPLOYMENT.md                  # 部署指南
```

## 配置说明

### application.properties
```properties
# Flink 配置
flink.parallelism=4
flink.checkpoint.interval=60000

# Kafka 配置
kafka.bootstrap.servers=localhost:9092
kafka.group.id=risk-control-group

# Redis 配置
redis.host=localhost
redis.port=6379
redis.database=0
redis.timeout=2000

# 规则配置
rule.cache.size=1000
rule.cache.ttl=3600

# 时间窗口配置
window.login.size=300000    # 5分钟
window.order.size=3600000   # 1小时
window.activity.size=86400000  # 1天
```

## 规则配置示例

### login-rules.yaml
```yaml
rules:
  - id: R001
    name: "登录失败次数过多"
    expression: "failCount > 3 && timeWindow < 300000"
    riskLevel: "HIGH"
    action: "BLOCK"
  - id: R002
    name: "异地登录"
    expression: "lastLoginIp != currentIp && lastLoginTime > 0"
    riskLevel: "MEDIUM"
    action: "VERIFY"
```

## 后续优化方向

- [ ] 机器学习模型集成 (异常检测)
- [ ] 可视化规则管理平台
- [ ] 分布式追踪 (Jaeger)
- [ ] 更多数据源支持 (Pulsar、RabbitMQ)
- [ ] 实时仪表板 (Grafana + Prometheus)
- [ ] 灾备和故障转移
- [ ] 多租户支持

## 贡献指南

欢迎 Pull Request！请确保：
1. 代码符合项目风格
2. 添加相应的单元测试
3. 更新文档

## 许可证

Apache License 2.0