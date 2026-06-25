# Architecture

## Module Structure

```
flink-risk-manager/
├── flink-risk-common/        # 公共模块：事件模型、规则引擎、Redis 客户端
├── flink-risk-job/           # Flink 作业：Kafka Source → CEP → 规则评估 → 多 Sink
├── flink-risk-web/           # Spring Boot 管理后端：规则 CRUD、风险记录查询
├── frontend/                 # React 管理界面：规则管理、风险记录、数据看板
├── sql/                      # 数据库初始化脚本
├── docs/                     # 架构设计文档
└── docker-compose.yml        # Docker 编排文件
```

## Data Flow

1. **Kafka Source** 接收 `login-events` / `order-events` / `activity-events`
2. **keyBy(userId)** 按用户分组
3. **CEP Pattern** 匹配异常事件序列（5s 窗口）
4. **BroadcastStream** 接收 MySQL 动态规则
5. **RuleEvaluationFunction** 使用 Aviator 评估规则
6. **多 Sink 输出**：Redis（实时）、MySQL（持久）、Kafka（下游）

## Key Classes

| Class | Module | Role |
|-------|--------|------|
| `RiskControlApplication` | job | Flink 作业入口 |
| `LoginCepJob` / `OrderCepJob` / `ActivityCepJob` | job | CEP 模式匹配 |
| `PatternFactory` | job | CEP Pattern 构建 |
| `RuleEvaluationFunction` | job | 广播规则动态评估 |
| `RuleSourceFunction` | job | MySQL 规则定时拉取 |
| `AviatorRuleEngine` | common | Aviator 表达式执行 |
| `RiskActionResolver` | common | 风险结果解析 |
| `RiskWebApplication` | web | Spring Boot 启动类 |

## Configuration Files

- **Job**: `flink-risk-job/src/main/resources/application.properties`
- **Web**: `flink-risk-web/src/main/resources/application.yml`
- **Rules**: `sql/init.sql` (MySQL 初始化)

## REST API Endpoints

### Rules
- `GET /api/rules` — 分页查询规则
- `POST /api/rules` — 创建规则
- `PUT /api/rules/{id}` — 更新规则
- `DELETE /api/rules/{id}` — 删除规则

### Risk Results
- `GET /api/risk-results` — 分页查询风险记录

### Stats
- `GET /api/stats/overview` — 总览统计

## Common Patterns

### Error Handling
- `logger.error()` / `logger.warn()` / `logger.info()` for different levels
- No swallowed exceptions

### Resource Management
- Use try-with-resources for `Closeable` objects
- Flink `open()` / `close()` lifecycle

### CEP Pattern
```java
Pattern.<LoginEvent>begin("first")
    .where(new SimpleCondition<LoginEvent>() {
        @Override
        public boolean filter(LoginEvent event) {
            return !event.isSuccess();
        }
    })
    .next("second")
    .where(...)
    .within(Duration.ofSeconds(5));
```
