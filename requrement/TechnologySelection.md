依赖注入 (DI)：avaje-inject 12.4
通信核心 (gRPC)：直接使用官方的 gRPC (1.80.0)。
消息队列 (MQ)：直接使用 Kafka Clients (4.2.0)。
配置管理：Typesafe Config (HOCON)
AI架构使用 langchain4j 1.12.2
采用ollama 模型可选用mollysama/rwkv-7-g1e:13.3b  qwen3:8b bge-m3（embedding模型 ）
向量数据库 milvus 2.6.16
持久化数据库 MongoDB
使用docker 部署到虚拟机 192.168.209.128
java版本 jdk21

mongodb:
host: 192.168.209.128
port: 27017
database: catBot
auto-index-creation: false
username: root
password: example
authentication-database: admin
redis:
host: 192.168.209.128
port: 6379

milvus:
host: 192.168.209.128
port: 19530
dbName: milvus_local
collection-name: embeddings_local
enable: true