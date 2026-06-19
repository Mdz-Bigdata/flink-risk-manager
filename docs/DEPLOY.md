# Flink 风控 Job 部署依赖说明

## 一、概述

Flink Job 提交到集群运行时，依赖分为两部分：

| 分类 | 说明 | 来源 |
|------|------|------|
| **集群 lib/ 目录** | Flink 框架级 JAR，所有 Job 共享 | 手动放入 Flink `lib/` 目录 |
| **Job fat-jar 内置** | 第三方业务 JAR，随 Job 打包提交 | `mvn package` 自动打包 |

> **Docker 镜像**: `apache/flink:2.2.1-scala_2.12-java8`
> **Flink 安装目录**: `/opt/flink`
> **lib 目录**: `/opt/flink/lib`

---

## 二、需要放入 Flink 集群 `lib/` 目录的 JAR

Flink 官方镜像 `lib/` 目录已内置核心 JAR（flink-core、flink-streaming-java、flink-runtime 等），
但以下 JAR **不在官方镜像中**，需要手动添加：

### 2.1 Flink CEP 库

| JAR 文件 | 版本 | 说明 |
|----------|------|------|
| `flink-cep_2.12-2.2.1.jar` | 2.2.1 | CEP 模式匹配库，官方镜像中在 `opt/` 目录，需复制到 `lib/` |

**获取方式**（二选一）：

```bash
# 方式一：从 Flink 镜像内部 opt/ 复制到 lib/（推荐，零下载）
docker exec flink-risk-jobmanager cp /opt/flink/opt/flink-cep_2.12-2.2.1.jar /opt/flink/lib/
docker exec flink-risk-taskmanager cp /opt/flink/opt/flink-cep_2.12-2.2.1.jar /opt/flink/lib/

# 方式二：从 Maven 中央仓库下载
wget https://repo1.maven.org/maven2/org/apache/flink/flink-cep_2.12/2.2.1/flink-cep_2.12-2.2.1.jar
```

### 2.2 Flink Kafka Connector

| JAR 文件 | 版本 | 说明 |
|----------|------|------|
| `flink-connector-kafka-5.0.0-2.2.jar` | 5.0.0-2.2 | Kafka Source/Sink 连接器（Flink 2.x 新 API） |
| `kafka-clients-4.2.0.jar` | 4.2.0 | Kafka 客户端库（connector 的传递依赖） |

**获取方式**：

```bash
# 下载到本地
wget https://repo1.maven.org/maven2/org/apache/flink/flink-connector-kafka/5.0.0-2.2/flink-connector-kafka-5.0.0-2.2.jar
wget https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/4.2.0/kafka-clients-4.2.0.jar

# 复制到 Flink 容器的 lib/ 目录
docker cp flink-connector-kafka-5.0.0-2.2.jar flink-risk-jobmanager:/opt/flink/lib/
docker cp flink-connector-kafka-5.0.0-2.2.jar flink-risk-taskmanager:/opt/flink/lib/
docker cp kafka-clients-4.2.0.jar flink-risk-jobmanager:/opt/flink/lib/
docker cp kafka-clients-4.2.0.jar flink-risk-taskmanager:/opt/flink/lib/
```

### 2.3 lib/ 目录最终清单

```
/opt/flink/lib/
├── flink-cep_2.12-2.2.1.jar              # 新增（从 opt/ 复制）
├── flink-connector-kafka-5.0.0-2.2.jar   # 新增（下载）
├── kafka-clients-4.2.0.jar               # 新增（下载）
├── flink-core-2.2.1.jar                  # 镜像内置
├── flink-streaming-java_2.12-2.2.1.jar   # 镜像内置
├── flink-runtime-2.2.1.jar               # 镜像内置
├── flink-clients_2.12-2.2.1.jar          # 镜像内置
├── flink-connector-base-2.2.1.jar        # 镜像内置
├── flink-shaded-xxx.jar                  # 镜像内置
└── log4j-xxx.jar                         # 镜像内置
```

> **重要**: JobManager 和 TaskManager 的 `lib/` 目录都需要放置这些 JAR。

---

## 三、Job fat-jar 内置的依赖（自动打包）

以下 JAR 通过 `maven-shade-plugin` 打包进 `flink-risk-job-2.0.1-SNAPSHOT.jar`，提交 Job 时自动加载，**无需手动放入 lib/**：

| JAR 文件 | 版本 | 用途 |
|----------|------|------|
| `aviator-5.4.3.jar` | 5.4.3 | Aviator 规则引擎表达式执行 |
| `jedis-5.2.0.jar` | 5.2.0 | Redis 客户端 |
| `commons-pool2-2.12.0.jar` | 2.12.0 | Jedis 传递依赖（连接池） |
| `json-20240303.jar` | 20240303 | Jedis 传递依赖（JSON 处理） |
| `fastjson2-2.0.43.jar` | 2.0.43 | JSON 序列化/反序列化 |
| `gson-2.10.1.jar` | 2.10.1 | JSON 处理 |
| `jackson-databind-2.17.2.jar` | 2.17.2 | Kafka 事件反序列化 |
| `jackson-core-2.17.2.jar` | 2.17.2 | Jackson 核心 |
| `jackson-annotations-2.17.2.jar` | 2.17.2 | Jackson 注解 |
| `snakeyaml-2.2.jar` | 2.2 | YAML 配置解析 |
| `guava-33.0.0-jre.jar` | 33.0.0-jre | Guava 工具库 |
| `commons-lang3-3.14.0.jar` | 3.14.0 | Apache Commons 工具 |
| `mysql-connector-j-8.1.0.jar` | 8.1.0 | MySQL JDBC 驱动（规则数据源） |
| `slf4j-api-1.7.36.jar` | 1.7.36 | 日志门面 |
| `log4j-slf4j-impl-2.21.1.jar` | 2.21.1 | SLF4J → Log4j2 桥接 |
| `log4j-core-2.21.1.jar` | 2.21.1 | Log4j2 核心 |
| `log4j-api-2.21.1.jar` | 2.21.1 | Log4j2 API |
| `jsr305-1.3.9.jar` | 1.3.9 | FindBugs 注解（传递依赖） |
| `failureaccess-1.0.2.jar` | 1.0.2 | Guava 传递依赖 |
| `checker-qual-3.41.0.jar` | 3.41.0 | Guava 传递依赖 |
| `error_prone_annotations-2.23.0.jar` | 2.23.0 | Guava 传递依赖 |
| `j2objc-annotations-2.8.jar` | 2.8 | Guava 传递依赖 |

---

## 四、完整部署步骤

### 4.1 编译打包

```bash
cd flink-risk-control

# 编译所有模块
mvn clean package -DskipTests

# 产物：flink-risk-job/target/flink-risk-job-2.0.1-SNAPSHOT.jar
```

### 4.2 准备 Flink 集群 lib/ 目录

```bash
# 1. 复制 CEP 库（从镜像内部 opt/ → lib/）
docker exec flink-risk-jobmanager cp /opt/flink/opt/flink-cep_2.12-2.2.1.jar /opt/flink/lib/
docker exec flink-risk-taskmanager cp /opt/flink/opt/flink-cep_2.12-2.2.1.jar /opt/flink/lib/

# 2. 下载 Kafka Connector
cd /tmp
wget https://repo1.maven.org/maven2/org/apache/flink/flink-connector-kafka/5.0.0-2.2/flink-connector-kafka-5.0.0-2.2.jar
wget https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/4.2.0/kafka-clients-4.2.0.jar

# 3. 复制到 JobManager 和 TaskManager
docker cp flink-connector-kafka-5.0.0-2.2.jar flink-risk-jobmanager:/opt/flink/lib/
docker cp flink-connector-kafka-5.0.0-2.2.jar flink-risk-taskmanager:/opt/flink/lib/
docker cp kafka-clients-4.2.0.jar flink-risk-jobmanager:/opt/flink/lib/
docker cp kafka-clients-4.2.0.jar flink-risk-taskmanager:/opt/flink/lib/

# 4. 重启 Flink 集群使 lib/ 生效
docker restart flink-risk-jobmanager flink-risk-taskmanager
```

### 4.3 提交 Job

```bash
# 方式一：通过 Flink Web UI (http://localhost:8081)
# 上传 flink-risk-job/target/flink-risk-job-2.0.1-SNAPSHOT.jar 并提交

# 方式二：通过命令行
docker exec flink-risk-jobmanager flink run \
    -d \
    -c com.qinyadan.risk.RiskControlApplication \
    /opt/flink/usrlib/flink-risk-job-2.0.1-SNAPSHOT.jar
```

### 4.4 Docker Compose 持久化方案（推荐）

在 `docker-compose.yml` 中挂载 lib/ 目录，避免每次重启容器丢失 JAR：

```yaml
flink-jobmanager:
  image: apache/flink:2.2.1-scala_2.12-java8
  volumes:
    - ./flink-lib:/opt/flink/usrlib    # Job fat-jar
    - ./flink-extra-lib:/opt/flink/extra-lib  # 额外 JAR
  command: >
    bash -c "
      cp /opt/flink/extra-lib/*.jar /opt/flink/lib/ 2>/dev/null || true;
      jobmanager
    "

flink-taskmanager:
  image: apache/flink:2.2.1-scala_2.12-java8
  volumes:
    - ./flink-lib:/opt/flink/usrlib
    - ./flink-extra-lib:/opt/flink/extra-lib
  command: >
    bash -c "
      cp /opt/flink/extra-lib/*.jar /opt/flink/lib/ 2>/dev/null || true;
      taskmanager
    "
```

然后将额外 JAR 放入本地 `flink-extra-lib/` 目录：

```
flink-extra-lib/
├── flink-cep_2.12-2.2.1.jar
├── flink-connector-kafka-5.0.0-2.2.jar
└── kafka-clients-4.2.0.jar
```

---

## 五、依赖关系图

```
flink-risk-job (fat-jar)
├── flink-risk-common (内置)
│   ├── aviator 5.4.3           ← 内置
│   ├── jedis 5.2.0             ← 内置
│   ├── fastjson2 2.0.43        ← 内置
│   ├── gson 2.10.1             ← 内置
│   ├── jackson-databind 2.17.2 ← 内置
│   ├── snakeyaml 2.2           ← 内置
│   ├── guava 33.0.0-jre        ← 内置
│   └── commons-lang3 3.14.0    ← 内置
├── mysql-connector-j 8.1.0     ← 内置
├── log4j2 2.21.1               ← 内置
├── slf4j-api 1.7.36            ← 内置
│
│── flink-core 2.2.1            ← 集群 lib/ (镜像内置)
│── flink-streaming-java 2.2.1  ← 集群 lib/ (镜像内置)
│── flink-connector-base 2.2.1  ← 集群 lib/ (镜像内置)
│── flink-cep 2.2.1             ← 集群 lib/ (需从 opt/ 复制) ⚠️
│── flink-connector-kafka 5.0.0 ← 集群 lib/ (需下载) ⚠️
│   └── kafka-clients 4.2.0     ← 集群 lib/ (需下载) ⚠️
└── flink-runtime-web 2.2.1     ← 集群 lib/ (镜像内置)
```

---

## 六、常见问题

### Q1: `ClassNotFoundException: org.apache.flink.cep.Pattern`

**原因**: `flink-cep` JAR 未放入 `lib/` 目录

**解决**:
```bash
docker exec flink-risk-jobmanager cp /opt/flink/opt/flink-cep_2.12-2.2.1.jar /opt/flink/lib/
docker exec flink-risk-taskmanager cp /opt/flink/opt/flink-cep_2.12-2.2.1.jar /opt/flink/lib/
docker restart flink-risk-jobmanager flink-risk-taskmanager
```

### Q2: `ClassNotFoundException: org.apache.flink.connector.kafka.source.KafkaSource`

**原因**: `flink-connector-kafka` JAR 未放入 `lib/` 目录

**解决**: 下载 `flink-connector-kafka-5.0.0-2.2.jar` 和 `kafka-clients-4.2.0.jar` 放入 `lib/`

### Q3: `NoClassDefFoundError: org/apache/kafka/clients/consumer/KafkaConsumer`

**原因**: `kafka-clients` JAR 缺失（Kafka Connector 的传递依赖）

**解决**: 下载 `kafka-clients-4.2.0.jar` 放入 `lib/`

### Q4: 修改 lib/ 后 Job 仍报错

**原因**: 修改 `lib/` 后需要重启 Flink 集群

**解决**:
```bash
docker restart flink-risk-jobmanager flink-risk-taskmanager
```

### Q5: Docker 容器重启后 lib/ 中的 JAR 丢失

**原因**: 容器文件系统是临时的，重启后恢复镜像初始状态

**解决**: 使用 Volume 挂载方案（见 4.4 节）或构建自定义 Flink 镜像
