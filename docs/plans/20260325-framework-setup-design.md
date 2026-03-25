# 2026-03-25-Framework-Setup-Design

## 框架搭建设计方案

### 1. 配置管理 (Configuration Management)

#### 技术选型
- **框架**: Typesafe Config (HOCON 格式)
- **特点**: 支持环境变量覆盖、层级配置、类型安全

#### 实现结构
```
config/
└── application.conf          # 配置文件模板
src/main/resources/
└── application.conf          # 打包进 JAR 的默认配置
```

#### 配置项覆盖优先级
1. 环境变量（最高优先级）
2. System 属性
3. 工作目录的 application.conf
4. Classpath 中的 application.conf（最低优先级）

#### 核心配置组件
- `ConfigManager`: 配置管理器，提供类型安全的配置访问
- 支持 MongoDB、Milvus、Redis、AI 服务、Server 等多模块配置

#### 环境变量映射
| 环境变量 | 配置路径 | 用途 |
|---------|---------|------|
| `MONGODB_URI` | mongodb.uri | MongoDB 连接字符串 |
| `MILVUS_HOST` | milvus.host | Milvus 服务地址 |
| `MILVUS_PORT` | milvus.port | Milvus 服务端口 |
| `REDIS_HOST` | redis.host | Redis 服务地址 |
| `REDIS_PORT` | redis.port | Redis 服务端口 |
| `OLLAMA_BASE_URL` | ai.ollama.base-url | Ollama API 地址 |
| `SERVER_PORT` | server.port | HTTP 服务端口 |

---

### 2. 数据库准备 (Database Preparation)

#### 2.1 MongoDB
- **版本**: 5.x+
- **部署**: 虚拟机 192.168.209.128:27017
- **数据库名**: catBot
- **集合设计**:
  - `documents` - 文档元数据
  - `chunks` - 文档分块内容
  - `entities` - 提取的实体信息
  - `conversations` - 对话窗口元数据
  - `conversation_messages` - 对话消息内容

#### 2.2 Milvus
- **版本**: 2.x+
- **部署**: 虚拟机 192.168.209.128:19530
- **数据库名**: milvus_local
- **集合设计**:
  - `dense_vectors` - 稠密向量（1024 维，语义检索）
  - `sparse_vectors` - 稀疏向量（关键词检索）

#### 2.3 Redis
- **版本**: 6.x+
- **部署**: 虚拟机 192.168.209.128:6379
- **用途**: 上下文缓存、分布式锁、幂等性标记

#### 连接管理
- `MongoConnection`: MongoDB 连接工厂，带健康检查
- `MilvusConnection`: Milvus 客户端管理
- `RedisConnection`: Redis 连接池管理

---

### 3. CI/CD 配置 (GitHub Actions)

#### 触发条件
- **事件**: Push tags matching `v*` pattern
- **示例**: `v1.0.0`, `v1.2.3-beta`

#### 工作流流程
```
Push Tag
    ↓
GitHub Actions Trigger
    ├─ 检出代码
    ├─ 设置 JDK 21
    ├─ Maven 编译/测试/打包
    ├─ Docker 登录 (ghcr.io)
    ├─ 构建镜像 (ghcr.io/shuan/documentscan:{tag})
    ├─ 推送镜像
    └─ SSH 到虚拟机 (192.168.209.128)
         ├─ 拉取镜像
         ├─ 停止旧容器
         ├─ 启动新容器 (注入环境变量)
         ├─ 健康检查
         └─ 清理旧镜像
```

#### 部署脚本要点
- 使用 GitHub Container Registry (ghcr.io)
- 容器名: `documentscan`
- 端口映射: `8080:8080`
- 自动注入所有 Secrets 作为环境变量
- 健康检查端点: `/health`

#### GitHub Secrets 配置要求
| Secret | 说明 |
|--------|------|
| `VM_HOST` | 虚拟机 IP (192.168.209.128) |
| `VM_USERNAME` | SSH 用户名 |
| `VM_SSH_KEY` | SSH 私钥 |
| `VM_PORT` | SSH 端口 (默认 22) |
| `GITHUB_TOKEN` | 自动提供，用于镜像仓库认证 |
| `MONGODB_URI` | MongoDB 连接字符串 |
| `MILVUS_HOST` | Milvus 主机地址 |
| `MILVUS_PORT` | Milvus 端口 |
| `REDIS_HOST` | Redis 主机地址 |
| `REDIS_PORT` | Redis 端口 |

---

### 4. 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 | LTS 版本 |
| Maven | 3.9+ | 构建工具 |
| avaje-inject | 12.4 | 依赖注入 |
| Typesafe Config | 1.4.3 | 配置管理 |
| MongoDB Driver | 5.3.1 | 文档数据库 |
| Milvus SDK | 2.6.16 | 向量数据库 |
| Lettuce | 6.5.4.RELEASE | Redis 客户端 |
| gRPC | 1.71.0 | RPC 框架 |
| Kafka Clients | 4.0.0 | 消息队列 |
| LangChain4j | 1.12.2 | AI 框架 |
| Logback | 1.5.18 | 日志框架 |

---

### 5. 项目结构

```
documentScan/
├── .github/
│   └── workflows/
│       └── deploy.yml          # CI/CD 工作流
├── config/
│   └── application.conf        # 配置模板
├── src/
│   └── main/
│       ├── java/
│       │   └── com/shuanglin/documentscan/
│       │       ├── Application.java
│       │       ├── config/
│       │       │   └── ConfigManager.java
│       │       ├── infrastructure/
│       │       │   ├── MongoConnection.java
│       │       │   ├── MilvusConnection.java
│       │       │   └── RedisConnection.java
│       │       └── api/
│       │           ├── HealthCheckService.java
│       │           └── HttpApiServer.java
│       └── resources/
│           └── application.conf
├── Dockerfile
├── .dockerignore
└── pom.xml
```

---

### 6. 验证结果

#### 编译验证
- [x] Maven 编译成功
- [x] 依赖注入生成成功
- [x] 打包成功 (62MB fat JAR)

#### 待验证项
- [ ] Docker 镜像构建 (需要 Docker 环境)
- [ ] 完整部署流程 (需要 GitHub Secrets 配置)
- [ ] 数据库连接 (需要实际数据库服务)

---

### 7. 后续步骤

1. **配置 GitHub Secrets**
   - 在仓库 Settings > Secrets 中添加所有必需的环境变量

2. **验证 CI/CD**
   - 推送 tag 触发部署: `git tag v0.1.0 && git push origin v0.1.0`

3. **数据库初始化**
   - 确保虚拟机上的 MongoDB、Milvus、Redis 服务已启动

4. **健康检查**
   - 部署完成后访问: `http://192.168.209.128:8080/health`

---

*设计文档创建时间: 2026-03-25*
