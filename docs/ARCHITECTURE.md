# Flink + AviatorScript 风控系统架构设计

## 整体架构

### 核心组件

1. **事件源 (Event Source)**
   - Kafka Topic: login-events
   - Kafka Topic: order-events  
   - Kafka Topic: activity-events
   - 数据格式: JSON
   - 吞吐量: 10K+/秒

2. **流处理引擎 (Flink)**
   - 版本: 1.14.4
   - 并行度: 4
   - Checkpoint: 60秒间隔
   - 状态后端: RocksDB

3. **规则引擎 (AviatorScript)**
   - 支持热加载
   - 表达式编译缓存
   - 动态规则管理

4. **结果存储 (Redis)**
   - 数据库: 0
   - TTL: 1小时
   - 吞吐量: 50K+/秒

## 数据流

```
┌──────────────────────────────────────────────────────────────┐
│                      Event Sources                            │
│  (Kafka Topics)                                              │
│  - login-events (用户登录)                                    │
│  - order-events (订单信息)                                    │
│  - activity-events (营销活动)                                 │
└──────────────────┬───────────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────────┐
│              Flink Streaming Processing                      │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  1. Event Deserialization                          │    │
│  │     - Parse JSON events                             │    │
│  │     - Type casting to Event objects                 │    │
│  └─────────────────────────────────────────────────────┘    │
│                     │                                         │
│                     ▼                                         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  2. Windowing & Aggregation                        │    │
│  │     - Login: 5分钟滑动窗口                         │    │
│  │     - Order: 1小时滑动窗口                         │    │
│  │     - Activity: 1天滑动窗口                        │    │
│  └─────────────────────────────────────────────────────┘    │
│                     │                                         │
│                     ▼                                         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  3. CEP Pattern Matching                           │    │
│  │     - Login failure detection                       │    │
│  │     - Order frequency detection                     │    │
│  │     - Activity fraud detection                      │    │
│  └─────────────────────────────────────────────────────┘    │
│                     │                                         │
│                     ▼                                         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  4. AviatorScript Rule Engine                      │    │
│  │     - Execute dynamic rules                         │    │
│  │     - Calculate risk scores                         │    │
│  │     - Determine risk levels                         │    │
│  └─────────────────────────────────────────────────────┘    │
│                     │                                         │
│                     ▼                                         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  5. Result Formatting                              │    │
│  │     - Wrap in RiskResult object                     │    │
│  │     - Add metadata and details                      │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────┬───────────────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────────────┐
│                   Redis Sink                                 │
│  - Store risk results with TTL                              │
│  - Key format: risk:eventType:userId:eventId               │
│  - Value: JSON serialized RiskResult                        │
└──────────────────────────────────────────────────────────────┘
```

## 扩展性设计

### 水平扩展
- 增加 Flink TaskManager 数量
- 增加 Kafka partition 数量
- 增加 Redis 实例（使用 cluster/sentinel）

### 垂直扩展
- 增加单个节点的 CPU 和内存
- 优化 JVM 配置
- 调整 Flink 并行度

## 容错设计

### 检查点 (Checkpoint)
- 间隔: 60秒
- 模式: Exactly-Once
- 后端: RocksDB
- 保留策略: 最近3个检查点

### 失败恢复
1. 自动重启
2. 从最近的检查点恢复
3. 重新处理未完成的事件
