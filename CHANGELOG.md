# Flink 规则引擎风控系统 v2.2 - 改动总结

## Redis 升级（v2.2）

| 组件 | 旧版本 | 新版本 |
|------|---------|--------|
| Redis (Docker) | 7.2 | **8.8** |
| Jedis (Java 客户端) | 4.4.3 | **5.2.0** |

### docker-compose.yml 改动
- `redis` 镜像升级到 `redis:8.8`
- healthcheck 新增 `--no-auth-warning` 参数（Redis 8.x 新特性）

### 项目依赖改动
- `pom.xml`（parent）：`jedis.version` 从 `4.4.3` 升级到 `5.2.0`
- Jedis 5.x 完全兼容 Redis 8.8 新特性（JSON、Search、TimeSeries 等）

### 编译验证
```
✅ flink-risk-common  COMPILE SUCCESS
✅ flink-risk-job     COMPILE SUCCESS
✅ flink-risk-web     COMPILE SUCCESS
```

---

## Kafka Source 接入（Flink 2.x 新 API）— v2.1
| 文件 | 改动内容 |
|------|----------|
| `KafkaEventDeserializer.java` | 通用 Event 反序列化（Jackson） |
| `LoginEventDeserializer.java` | LoginEvent 反序列化 |
| `OrderEventDeserializer.java` | OrderEvent 反序列化 |
| `ActivityEventDeserializer.java` | ActivityEvent 反序列化 |
| `KafkaSourceFactory.java` | KafkaSource 工厂类（新 API） |
| `RiskControlApplication.java` | 接入 Kafka Source，取消注释 CEP 流程 |
| `flink-risk-common/pom.xml` | 新增 `jackson-databind:2.17.2` 依赖 |

### 编译验证
```
✅ flink-risk-common  COMPILE SUCCESS
✅ flink-risk-job     COMPILE SUCCESS
```

---

## Kafka 升级（KRaft 模式）
| 组件 | 旧版本 | 新版本 |
|------|---------|--------|
| Confluent Platform (cp-kafka) | 7.5.0 | **8.1.4** |
| Kafka 内部版本 | 3.5.x | **4.1.x** |
| Zookeeper | cp-zookeeper:7.5.0（依赖） | **已移除（KRaft 模式）** |
| flink-connector-kafka | （未引入） | **5.0.0-2.2** |

### docker-compose.yml 改动
- `cp-kafka` 升级到 `8.1.4`（Kafka 4.1.x，Confluent 社区版）
- 改用 **KRaft 模式**（无需 Zookeeper），删除 `zookeeper` 服务
- 新增 KRaft 环境变量：`KAFKA_PROCESS_ROLES=broker,controller`、`KAFKA_CONTROLLER_QUORUM_VOTERS` 等
- Kafka 内部通信端口：`9093`（controller quorum）

### 项目依赖改动
- `pom.xml`（parent）：`flink-connector-kafka` 版本 `5.0.0-2.2`（适配 Flink 2.2.x）
- `flink-risk-job/pom.xml`：新增 `flink-connector-kafka` 依赖（scope=provided）
- `RuleSourceFunction.java`：import 改为 `legacy.SourceFunction`（Flink 2.x 兼容）
- `RuleEvaluationFunction.java`：`open(Configuration)` → `open(OpenContext)`；`Void.TYPE_VALUE` → `null`；`BroadcastState` → `ReadOnlyBroadcastState`（API 适配）

---

## 版本升级
| 组件 | 旧版本 | 新版本 |
|------|---------|--------|
| Flink | 1.14.4 | 2.2.1 |
| Java (common/job) | 8 | 8 |
| Java (web) | - | 17 |
| Spring Boot | - | 3.2.0 |
| antd | - | **5.29.3** |
| @ant-design/pro-components | - | **2.8.10** |
| React | - | 18.3.1 |
| Vite | - | 5.4.21 |

## 模块拆分
| 模块 | 说明 | 状态 |
|------|------|------|
| `flink-risk-common` | 公共：事件模型、规则引擎、Redis客户端 | ✅ 编译通过 |
| `flink-risk-job` | Flink Job：CEP规则检测 | ✅ 编译通过 |
| `flink-risk-web` | Web管理后端：Spring Boot + MyBatis Plus | ✅ 编译通过 |
| `frontend` | 前端：Vite + React + Ant Design 5.x | ✅ 构建通过 |

## Flink 2.x 兼容性修复
| 文件 | 修复内容 |
|------|----------|
| `RedisResultWriter.java` | 移除 `RichSinkFunction`，改为普通类 |
| `PatternFactory.java` | `Time` 包路径变更；`SimpleCondition` 改用匿名内部类 |
| `LoginCepJob/OrderCepJob/ActivityCepJob.java` | 移除 `addSink()`，返回 DataStream |
| `RiskControlApplication.java` | 移除 `getFlinkVersion()` |

## 前端升级（Ant Design 5.x）
| 改动 | 说明 |
|------|------|
| antd | 5.29.3（最新 5.x 稳定版） |
| @ant-design/pro-components | 2.8.10（适配 antd 5.x） |
| ProLayout | 使用 `menuData` 替代 `menu.items` |
| ConfigProvider | 使用 antd 5.x 主题 token 配置 |
| 请求库 | 改用 axios（移除 umi/request） |
| TypeScript | 5.5 + tsc --noEmit 通过 |
| 构建 | Vite 5.x 构建成功 |

## 新增文件清单
- `flink-risk-common/pom.xml`
- `flink-risk-job/pom.xml`
- `flink-risk-web/pom.xml`
- `flink-risk-web/src/main/java/com/risk/RiskWebApplication.java`
- `flink-risk-web/src/main/resources/application.yml`
- `flink-risk-web/src/main/java/com/risk/web/entity/Rule.java`
- `flink-risk-web/src/main/java/com/risk/web/mapper/RuleMapper.java`
- `flink-risk-web/src/main/java/com/risk/web/service/RuleService.java`
- `flink-risk-web/src/main/java/com/risk/web/controller/RuleController.java`
- `frontend/` 前端项目（Ant Design 5.x）
- `sql/init.sql` 数据库初始化脚本

## 下一步
1. `cd frontend && npm run dev` 启动前端（localhost:3000）
2. 配置 Kafka Source（Flink 2.x 新 API）
3. 完善 CEP 模式中的 `.within()` 时间窗口
4. 配置 Redis Sink（Flink 2.x 新 Sink API）
