# Flink 规则引擎风控系统 v2.0.1

## 版本说明
- **Flink**: 2.2.1（已从 1.14.4 升级）
- **Java**: 8（common/job 模块），17（web 模块）
- **Spring Boot**: 3.2.0（web 模块）

## 模块结构

| 模块 | 说明 | 打包方式 |
|------|------|----------|
| `flink-risk-common` | 公共模块：事件模型、规则引擎、Redis 客户端 | jar |
| `flink-risk-job` | Flink 任务模块：CEP 规则检测 Job | jar（shade） |
| `flink-risk-web` | Web 管理后端：规则引擎管理 API（Spring Boot） | jar |
| `frontend` | 前端界面：Ant Design Pro 规则管理 | static |

## 快速启动

### 1. 启动基础服务
```bash
docker-compose up -d
```

### 2. 初始化数据库
```bash
mysql -u root -p < sql/init.sql
```

### 3. 启动 Web 管理后端
```bash
cd flink-risk-web
mvn spring-boot:run
# 访问 http://localhost:8080
```

### 4. 启动前端
```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:3000
```

### 5. 提交 Flink Job
```bash
cd flink-risk-job
mvn package
flink run target/flink-risk-job-2.0.1-SNAPSHOT.jar
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/rules` | 获取所有规则 |
| GET | `/api/rules/{id}` | 获取单条规则 |
| POST | `/api/rules` | 创建规则 |
| PUT | `/api/rules/{id}` | 更新规则 |
| DELETE | `/api/rules/{id}` | 删除规则 |

## 依赖版本

| 依赖 | 版本 |
|------|------|
| Flink | 2.2.1 |
| Spring Boot | 3.2.0 |
| MyBatis Plus | 3.5.5 |
| Aviator | 5.4.3 |
| Jedis | 4.4.3 |
| MySQL Connector | 8.1.0 |
