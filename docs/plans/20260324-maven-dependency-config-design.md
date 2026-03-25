# 20260324-maven-dependency-config-design

## 设计目标

为文档扫描与智能检索系统设计 Maven 依赖管理和配置文件管理方案，支持多环境（dev/test/prod）构建。

---

## 总体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Maven 构建层                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ 版本管理     │  │ Profile配置  │  │ 资源过滤/复制        │ │
│  │ (properties)│  │ (dev/test/  │  │ (maven-resources-   │ │
│  │             │  │   prod)      │  │   plugin)           │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     配置文件层                               │
│  src/main/resources/                                         │
│  ├── application.conf          (默认配置，被各环境include)    │
│  ├── application-dev.conf      (开发环境特定配置)             │
│  ├── application-test.conf     (测试环境特定配置)             │
│  └── application-prod.conf     (生产环境特定配置)             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     运行时配置层                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Typesafe Config (HOCON)                             │  │
│  │  • ConfigFactory.load() 加载配置                      │  │
│  │  • 支持变量替换 ${app.name}                           │  │
│  │  • 支持环境变量 ${?ENV_VAR}                           │  │
│  │  • 支持 include 合并配置                              │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 核心设计原则

1. **版本集中管理** - 所有依赖版本在 `pom.xml` `<properties>` 中统一定义
2. **环境配置分离** - 每个环境独立配置文件，Maven Profile 控制打包
3. **运行时加载** - HOCON 负责运行时配置解析，支持变量和 include
4. **敏感信息外置** - 生产环境敏感配置通过环境变量注入

---

## 1. Maven 依赖与版本管理

### 1.1 版本属性定义

所有版本号统一定义在 `<properties>` 中：

```xml
<properties>
    <!-- 基础版本 -->
    <java.version>21</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- 核心依赖版本 -->
    <avaje-inject.version>12.4</avaje-inject.version>
    <grpc.version>1.80.0</grpc.version>
    <kafka-clients.version>4.2.0</kafka-clients.version>
    <typesafe-config.version>1.4.3</typesafe-config.version>
    <langchain4j.version>1.12.2</langchain4j.version>

    <!-- 数据存储 -->
    <mongodb-driver.version>5.3.1</mongodb-driver.version>
    <milvus-sdk.version>2.5.5</milvus-sdk.version>
    <redis.version>5.2.0</redis.version>

    <!-- 工具库 -->
    <slf4j.version>2.0.16</slf4j.version>
    <logback.version>1.5.18</logback.version>
    <lombok.version>1.18.36</lombok.version>
    <jackson.version>2.18.2</jackson.version>
    <guava.version>33.4.0-jre</guava.version>
    <pdfbox.version>3.0.3</pdfbox.version>
    <poi.version>5.4.0</poi.version>

    <!-- 测试 -->
    <junit.version>5.12.1</junit.version>
    <mockito.version>5.16.1</mockito.version>
</properties>
```

### 1.2 核心依赖配置

```xml
<dependencies>
    <!-- DI: avaje-inject -->
    <dependency>
        <groupId>io.avaje</groupId>
        <artifactId>avaje-inject</artifactId>
        <version>${avaje-inject.version}</version>
    </dependency>
    <dependency>
        <groupId>io.avaje</groupId>
        <artifactId>avaje-inject-generator</artifactId>
        <version>${avaje-inject.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- gRPC -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
        <version>${grpc.version}</version>
    </dependency>

    <!-- Kafka -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
        <version>${kafka-clients.version}</version>
    </dependency>

    <!-- Config: HOCON -->
    <dependency>
        <groupId>com.typesafe</groupId>
        <artifactId>config</artifactId>
        <version>${typesafe-config.version}</version>
    </dependency>

    <!-- LangChain4j -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-ollama</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>

    <!-- MongoDB -->
    <dependency>
        <groupId>org.mongodb</groupId>
        <artifactId>mongodb-driver-sync</artifactId>
        <version>${mongodb-driver.version}</version>
    </dependency>

    <!-- Milvus -->
    <dependency>
        <groupId>io.milvus</groupId>
        <artifactId>milvus-sdk-java</artifactId>
        <version>${milvus-sdk.version}</version>
    </dependency>

    <!-- Redis -->
    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
        <version>${redis.version}</version>
    </dependency>

    <!-- 文档解析 -->
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox</artifactId>
        <version>${pdfbox.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>${poi.version}</version>
    </dependency>

    <!-- 日志 -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>${slf4j.version}</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>${logback.version}</version>
    </dependency>

    <!-- 工具库 -->
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>${guava.version}</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>${jackson.version}</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${junit.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>${mockito.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 2. Maven Profile 配置

### 2.1 Profile 定义

```xml
<profiles>
    <!-- 开发环境（默认） -->
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <build>
            <resources>
                <resource>
                    <directory>src/main/resources</directory>
                    <includes>
                        <include>application.conf</include>
                        <include>application-dev.conf</include>
                        <include>**/*.properties</include>
                    </includes>
                </resource>
            </resources>
        </build>
    </profile>

    <!-- 测试环境 -->
    <profile>
        <id>test</id>
        <build>
            <resources>
                <resource>
                    <directory>src/main/resources</directory>
                    <includes>
                        <include>application.conf</include>
                        <include>application-test.conf</include>
                        <include>**/*.properties</include>
                    </includes>
                </resource>
            </resources>
        </build>
    </profile>

    <!-- 生产环境 -->
    <profile>
        <id>prod</id>
        <build>
            <resources>
                <resource>
                    <directory>src/main/resources</directory>
                    <includes>
                        <include>application.conf</include>
                        <include>application-prod.conf</include>
                        <include>**/*.properties</include>
                    </includes>
                </resource>
            </resources>
        </build>
    </profile>
</profiles>
```

### 2.2 打包命令

| 环境 | 命令 |
|-----|------|
| 开发（默认） | `mvn clean package` |
| 测试 | `mvn clean package -P test` |
| 生产 | `mvn clean package -P prod` |

---

## 3. HOCON 配置文件设计

### 3.1 目录结构

```
src/main/resources/
├── application.conf          # 公共配置（被各环境include）
├── application-dev.conf      # 开发环境
├── application-test.conf     # 测试环境
├── application-prod.conf     # 生产环境
└── logback.xml              # 日志框架配置
```

### 3.2 application.conf（公共配置）

```hocon
# 应用基本信息
app {
  name = "document-scan"
  version = "1.0.0"
}

# gRPC 服务配置
grpc {
  server {
    port = 9090
    max-message-size = 16777216  # 16MB
  }
}

# Kafka 配置
kafka {
  bootstrap-servers = "localhost:9092"
  consumer {
    group-id = "document-scan-group"
    auto-offset-reset = "earliest"
  }
  topics {
    document-ingestion = "document-ingestion"
    retrieval-query = "retrieval-query"
  }
}

# MongoDB 配置
mongodb {
  uri = "mongodb://localhost:27017"
  database = "document_scan"
  connection-pool {
    min-size = 10
    max-size = 100
  }
}

# Milvus 配置
milvus {
  host = "localhost"
  port = 19530
  database = "document_scan"
  collection {
    chunk-vectors = "chunk_vectors"
    entity-vectors = "entity_vectors"
  }
}

# Redis 配置
redis {
  host = "localhost"
  port = 6379
  database = 0
  password = ${?REDIS_PASSWORD}
  connection-pool {
    max-total = 50
    max-idle = 20
  }
}

# LangChain4j / Ollama 配置
ai {
  ollama {
    base-url = "http://localhost:11434"
    timeout-seconds = 120
    models {
      chat = "qwen3:8b"
      embedding = "bge-m3"
    }
  }
}

# 文档处理配置
document {
  chunking {
    max-chunk-size = 500
    overlap-ratio = 0.2
  }
  supported-formats = ["pdf", "doc", "docx", "txt", "epub"]
  max-file-size-mb = 50
}

# 检索配置
retrieval {
  semantic-weight = 0.7
  keyword-weight = 0.3
  max-results = 10
  max-depth = 3
  deviation-threshold = 0.7
}

# 任务调度配置
scheduler {
  max-plan-depth = 3
  max-steps-per-layer = 3
  max-tasks-per-step = 3
  min-improvement-percent = 5
  max-empty-retries = 2
}

# 日志配置
logging {
  level = "INFO"
  pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
}
```

### 3.3 application-dev.conf（开发环境）

```hocon
# 开发环境特定配置，继承并覆盖公共配置
include "application"

# 开发环境使用本地服务
mongodb {
  uri = "mongodb://localhost:27017"
  database = "document_scan_dev"
}

# 开发环境日志级别
logging {
  level = "DEBUG"
}
```

### 3.4 application-test.conf（测试环境）

```hocon
# 测试环境配置
include "application"

mongodb {
  uri = ${?MONGODB_URI}
  database = "document_scan_test"
}

milvus {
  host = ${?MILVUS_HOST}
  port = ${?MILVUS_PORT}
}

logging {
  level = "INFO"
}
```

### 3.5 application-prod.conf（生产环境）

```hocon
# 生产环境配置
include "application"

# 从环境变量读取敏感配置
mongodb {
  uri = ${?MONGODB_URI}
  database = "document_scan_prod"
}

milvus {
  host = ${?MILVUS_HOST}
  port = ${?MILVUS_PORT}
}

redis {
  host = ${?REDIS_HOST}
  port = ${?REDIS_PORT}
  password = ${?REDIS_PASSWORD}
}

ai {
  ollama {
    base-url = ${?OLLAMA_BASE_URL}
  }
}

kafka {
  bootstrap-servers = ${?KAFKA_BOOTSTRAP_SERVERS}
}

# 生产环境日志级别
logging {
  level = "WARN"
}
```

---

## 4. Java 配置加载实现

### 4.1 配置管理类

```java
package com.shuanglin.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jakarta.inject.Singleton;
import lombok.Getter;

@Singleton
@Getter
public class AppConfig {

    private final Config config;

    public AppConfig() {
        String profile = System.getProperty("config.profile", "dev");
        String configName = "application-" + profile;
        this.config = ConfigFactory.load(configName);
    }

    // 应用信息
    public String getAppName() {
        return config.getString("app.name");
    }

    public String getAppVersion() {
        return config.getString("app.version");
    }

    // gRPC 配置
    public int getGrpcPort() {
        return config.getInt("grpc.server.port");
    }

    public int getGrpcMaxMessageSize() {
        return config.getInt("grpc.server.max-message-size");
    }

    // MongoDB 配置
    public MongoConfig getMongoConfig() {
        return MongoConfig.builder()
                .uri(config.getString("mongodb.uri"))
                .database(config.getString("mongodb.database"))
                .minPoolSize(config.getInt("mongodb.connection-pool.min-size"))
                .maxPoolSize(config.getInt("mongodb.connection-pool.max-size"))
                .build();
    }

    // Milvus 配置
    public MilvusConfig getMilvusConfig() {
        return MilvusConfig.builder()
                .host(config.getString("milvus.host"))
                .port(config.getInt("milvus.port"))
                .database(config.getString("milvus.database"))
                .chunkCollection(config.getString("milvus.collection.chunk-vectors"))
                .entityCollection(config.getString("milvus.collection.entity-vectors"))
                .build();
    }

    // 文档处理配置
    public DocumentConfig getDocumentConfig() {
        return DocumentConfig.builder()
                .maxChunkSize(config.getInt("document.chunking.max-chunk-size"))
                .overlapRatio(config.getDouble("document.chunking.overlap-ratio"))
                .maxFileSizeMb(config.getInt("document.max-file-size-mb"))
                .build();
    }

    // 检索配置
    public RetrievalConfig getRetrievalConfig() {
        return RetrievalConfig.builder()
                .semanticWeight(config.getDouble("retrieval.semantic-weight"))
                .keywordWeight(config.getDouble("retrieval.keyword-weight"))
                .maxResults(config.getInt("retrieval.max-results"))
                .maxDepth(config.getInt("retrieval.max-depth"))
                .deviationThreshold(config.getDouble("retrieval.deviation-threshold"))
                .build();
    }
}
```

### 4.2 配置数据类示例

```java
package com.shuanglin.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MongoConfig {
    private String uri;
    private String database;
    private int minPoolSize;
    private int maxPoolSize;
}

@Data
@Builder
public class MilvusConfig {
    private String host;
    private int port;
    private String database;
    private String chunkCollection;
    private String entityCollection;
}

@Data
@Builder
public class DocumentConfig {
    private int maxChunkSize;
    private double overlapRatio;
    private int maxFileSizeMb;
}

@Data
@Builder
public class RetrievalConfig {
    private double semanticWeight;
    private double keywordWeight;
    private int maxResults;
    private int maxDepth;
    private double deviationThreshold;
}
```

---

## 5. 运行时启动方式

### 5.1 本地启动

```bash
# 开发环境（默认）
java -jar document-scan.jar

# 指定环境
java -Dconfig.profile=dev -jar document-scan.jar
java -Dconfig.profile=test -jar document-scan.jar
java -Dconfig.profile=prod -jar document-scan.jar
```

### 5.2 Docker 启动

```bash
# 开发环境
docker run -e CONFIG_PROFILE=dev document-scan

# 生产环境（带敏感配置）
docker run \
  -e CONFIG_PROFILE=prod \
  -e MONGODB_URI=mongodb://prod-host:27017 \
  -e MILVUS_HOST=prod-milvus \
  document-scan
```

### 5.3 IDE 配置

在 IntelliJ IDEA 的 Run Configuration 中：
- **Program arguments**: 留空（使用默认 dev）
- **VM options**: `-Dconfig.profile=dev`

---

## 6. 环境变量清单

生产环境需要配置以下环境变量：

| 变量名 | 说明 | 示例 |
|-------|------|------|
| `CONFIG_PROFILE` | 配置环境 | `prod` |
| `MONGODB_URI` | MongoDB 连接URI | `mongodb://user:pass@host:27017` |
| `MILVUS_HOST` | Milvus 主机地址 | `milvus.example.com` |
| `MILVUS_PORT` | Milvus 端口 | `19530` |
| `REDIS_HOST` | Redis 主机地址 | `redis.example.com` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | `secret` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 地址 | `kafka1:9092,kafka2:9092` |
| `OLLAMA_BASE_URL` | Ollama 服务地址 | `http://ollama:11434` |

---

## 7. 总结

本设计实现了：

1. **统一的版本管理** - 所有依赖版本集中在 `pom.xml` 的 `<properties>` 中
2. **多环境配置文件** - 通过 Maven Profile 控制打包时包含的配置文件
3. **HOCON 配置加载** - 支持 include、变量替换、环境变量读取
4. **运行时环境切换** - 通过 `-Dconfig.profile` 系统属性切换配置

**下一步行动：** 创建详细的实施计划，逐步完成 pom.xml 更新和配置文件创建。

---

*文档结束*
