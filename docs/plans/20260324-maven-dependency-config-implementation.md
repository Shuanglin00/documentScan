# Maven 依赖与配置文件管理实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 完成 pom.xml 依赖配置和 HOCON 配置文件体系搭建，支持多环境（dev/test/prod）构建。

**Architecture:** 使用 Maven Profile 控制环境特定资源的打包，HOCON 提供运行时配置加载，支持变量替换和 include 机制。

**Tech Stack:** Maven, HOCON (Typesafe Config), avaje-inject, gRPC, Kafka, MongoDB, Milvus

---

## 前置检查

### Task 0: 确认当前项目结构

**Files:**
- Read: `pom.xml`
- Read: `docs/plans/20260324-maven-dependency-config-design.md`

**Step 1: 检查当前 pom.xml 状态**

Run: `cat pom.xml`
Expected: 基本结构存在，缺少依赖和 profile

**Step 2: 确认设计文档已就位**

Run: `ls -la docs/plans/20260324-maven-dependency-config-design.md`
Expected: 文件存在

---

## 第一阶段：更新 pom.xml

### Task 1: 添加版本属性

**Files:**
- Modify: `pom.xml:11-16`

**Step 1: 在 `<properties>` 中添加所有版本定义**

替换 `<properties>` 部分为：

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

**Step 2: 验证 XML 格式**

Run: `xmllint --noout pom.xml`
Expected: 无错误输出

**Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: add version properties to pom.xml"
```

---

### Task 2: 添加核心依赖

**Files:**
- Modify: `pom.xml` (在 `</project>` 前添加)

**Step 1: 添加 `<dependencies>` 部分**

在 `</properties>` 后添加：

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

**Step 2: 验证依赖下载**

Run: `mvn dependency:resolve`
Expected: 所有依赖成功下载，无错误

**Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: add core dependencies to pom.xml"
```

---

### Task 3: 添加 Maven Profile

**Files:**
- Modify: `pom.xml` (在 `</project>` 前添加)

**Step 1: 添加 `<profiles>` 部分**

在 `</dependencies>` 后添加：

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

**Step 2: 验证 Profile 配置**

Run: `mvn help:active-profiles`
Expected: 显示 `dev` profile 被激活

**Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: add maven profiles for dev/test/prod"
```

---

## 第二阶段：创建配置文件

### Task 4: 创建 resources 目录和公共配置

**Files:**
- Create: `src/main/resources/application.conf`

**Step 1: 创建目录**

Run: `mkdir -p src/main/resources`

**Step 2: 创建 application.conf**

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
    max-message-size = 16777216
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

**Step 3: Commit**

```bash
git add src/main/resources/application.conf
git commit -m "chore: add base HOCON configuration file"
```

---

### Task 5: 创建开发环境配置

**Files:**
- Create: `src/main/resources/application-dev.conf`

**Step 1: 创建文件**

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

**Step 2: Commit**

```bash
git add src/main/resources/application-dev.conf
git commit -m "chore: add development environment configuration"
```

---

### Task 6: 创建测试环境配置

**Files:**
- Create: `src/main/resources/application-test.conf`

**Step 1: 创建文件**

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

**Step 2: Commit**

```bash
git add src/main/resources/application-test.conf
git commit -m "chore: add test environment configuration"
```

---

### Task 7: 创建生产环境配置

**Files:**
- Create: `src/main/resources/application-prod.conf`

**Step 1: 创建文件**

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

**Step 2: Commit**

```bash
git add src/main/resources/application-prod.conf
git commit -m "chore: add production environment configuration"
```

---

## 第三阶段：创建 Java 配置类

### Task 8: 创建配置包和 AppConfig 类

**Files:**
- Create: `src/main/java/com/shuanglin/config/AppConfig.java`

**Step 1: 创建包目录**

Run: `mkdir -p src/main/java/com/shuanglin/config`

**Step 2: 创建 AppConfig.java**

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

    public String getAppName() {
        return config.getString("app.name");
    }

    public String getAppVersion() {
        return config.getString("app.version");
    }

    public int getGrpcPort() {
        return config.getInt("grpc.server.port");
    }

    public int getGrpcMaxMessageSize() {
        return config.getInt("grpc.server.max-message-size");
    }

    public MongoConfig getMongoConfig() {
        return MongoConfig.builder()
                .uri(config.getString("mongodb.uri"))
                .database(config.getString("mongodb.database"))
                .minPoolSize(config.getInt("mongodb.connection-pool.min-size"))
                .maxPoolSize(config.getInt("mongodb.connection-pool.max-size"))
                .build();
    }

    public MilvusConfig getMilvusConfig() {
        return MilvusConfig.builder()
                .host(config.getString("milvus.host"))
                .port(config.getInt("milvus.port"))
                .database(config.getString("milvus.database"))
                .chunkCollection(config.getString("milvus.collection.chunk-vectors"))
                .entityCollection(config.getString("milvus.collection.entity-vectors"))
                .build();
    }

    public DocumentConfig getDocumentConfig() {
        return DocumentConfig.builder()
                .maxChunkSize(config.getInt("document.chunking.max-chunk-size"))
                .overlapRatio(config.getDouble("document.chunking.overlap-ratio"))
                .maxFileSizeMb(config.getInt("document.max-file-size-mb"))
                .build();
    }

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

**Step 3: 验证编译**

Run: `mvn compile -q`
Expected: 编译成功，无错误

**Step 4: Commit**

```bash
git add src/main/java/com/shuanglin/config/AppConfig.java
git commit -m "feat: add AppConfig class for HOCON configuration loading"
```

---

### Task 9: 创建配置数据类

**Files:**
- Create: `src/main/java/com/shuanglin/config/MongoConfig.java`
- Create: `src/main/java/com/shuanglin/config/MilvusConfig.java`
- Create: `src/main/java/com/shuanglin/config/DocumentConfig.java`
- Create: `src/main/java/com/shuanglin/config/RetrievalConfig.java`

**Step 1: 创建 MongoConfig.java**

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
```

**Step 2: 创建 MilvusConfig.java**

```java
package com.shuanglin.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MilvusConfig {
    private String host;
    private int port;
    private String database;
    private String chunkCollection;
    private String entityCollection;
}
```

**Step 3: 创建 DocumentConfig.java**

```java
package com.shuanglin.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentConfig {
    private int maxChunkSize;
    private double overlapRatio;
    private int maxFileSizeMb;
}
```

**Step 4: 创建 RetrievalConfig.java**

```java
package com.shuanglin.config;

import lombok.Builder;
import lombok.Data;

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

**Step 5: 验证编译**

Run: `mvn compile -q`
Expected: 编译成功

**Step 6: Commit**

```bash
git add src/main/java/com/shuanglin/config/*.java
git commit -m "feat: add configuration data classes"
```

---

## 第四阶段：验证和测试

### Task 10: 验证 Maven 打包

**Step 1: 验证开发环境打包**

Run: `mvn clean package -q`
Expected: 打包成功，jar 文件包含 application.conf 和 application-dev.conf

**Step 2: 验证测试环境打包**

Run: `mvn clean package -P test -q`
Expected: 打包成功，jar 文件包含 application.conf 和 application-test.conf

**Step 3: 验证生产环境打包**

Run: `mvn clean package -P prod -q`
Expected: 打包成功，jar 文件包含 application.conf 和 application-prod.conf

**Step 4: 检查 jar 内容**

Run: `jar tf target/documentScan-*.jar | grep "application"`
Expected: 列出对应的配置文件

---

### Task 11: 创建简单测试验证配置加载

**Files:**
- Create: `src/test/java/com/shuanglin/config/AppConfigTest.java`

**Step 1: 创建测试类**

```java
package com.shuanglin.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void testConfigLoading() {
        // 使用默认 dev 环境
        AppConfig config = new AppConfig();

        assertNotNull(config.getAppName());
        assertEquals("document-scan", config.getAppName());

        assertNotNull(config.getMongoConfig());
        assertEquals("document_scan_dev", config.getMongoConfig().getDatabase());
    }

    @Test
    void testDocumentConfig() {
        AppConfig config = new AppConfig();
        DocumentConfig docConfig = config.getDocumentConfig();

        assertEquals(500, docConfig.getMaxChunkSize());
        assertEquals(0.2, docConfig.getOverlapRatio(), 0.01);
    }

    @Test
    void testRetrievalConfig() {
        AppConfig config = new AppConfig();
        RetrievalConfig retrievalConfig = config.getRetrievalConfig();

        assertEquals(0.7, retrievalConfig.getSemanticWeight(), 0.01);
        assertEquals(0.3, retrievalConfig.getKeywordWeight(), 0.01);
        assertEquals(3, retrievalConfig.getMaxDepth());
    }
}
```

**Step 2: 运行测试**

Run: `mvn test -Dtest=AppConfigTest`
Expected: 所有测试通过

**Step 3: Commit**

```bash
git add src/test/java/com/shuanglin/config/AppConfigTest.java
git commit -m "test: add configuration loading tests"
```

---

## 第五阶段：文档更新

### Task 12: 更新 README

**Files:**
- Modify: `README.md`（添加构建和配置说明）

**Step 1: 在 README 中添加构建说明**

```markdown
## 构建

### 多环境构建

```bash
# 开发环境（默认）
mvn clean package

# 测试环境
mvn clean package -P test

# 生产环境
mvn clean package -P prod
```

### 运行

```bash
# 开发环境（默认）
java -jar target/documentScan-*.jar

# 指定环境
java -Dconfig.profile=prod -jar target/documentScan-*.jar
```

### 环境变量（生产环境必需）

| 变量名 | 说明 |
|-------|------|
| MONGODB_URI | MongoDB 连接URI |
| MILVUS_HOST | Milvus 主机地址 |
| MILVUS_PORT | Milvus 端口 |
| REDIS_HOST | Redis 主机地址 |
| REDIS_PASSWORD | Redis 密码 |
| KAFKA_BOOTSTRAP_SERVERS | Kafka 地址 |
| OLLAMA_BASE_URL | Ollama 服务地址 |
```

**Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add build and configuration instructions"
```

---

## 完成检查清单

- [ ] pom.xml 包含所有版本属性
- [ ] pom.xml 包含所有核心依赖
- [ ] pom.xml 包含 dev/test/prod Profile
- [ ] application.conf 创建完成
- [ ] application-dev.conf 创建完成
- [ ] application-test.conf 创建完成
- [ ] application-prod.conf 创建完成
- [ ] AppConfig.java 创建完成
- [ ] 配置数据类创建完成
- [ ] 测试通过
- [ ] README 更新完成

---

**Plan complete and saved to `docs/plans/20260324-maven-dependency-config-implementation.md`.**

**Two execution options:**

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

**Which approach?**
