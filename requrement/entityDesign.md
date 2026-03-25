mongoDB数据库
表 1：DocumentStore（文档元数据表）
定位：系统的顶层资源，记录用户上传的文件或知识库的宏观信息。
字段名	数据类型	约束 / 属性	描述说明
id	String	主键 (PK)	唯一标识该文档（建议 UUID 或 ObjectId）。
name	String	必填	业务展示名称（如：“2026年Q1财务分析”）。
originalName	String	必填	物理文件的原始名称（如：“report_q1.pdf”）。
type	Enum（documentType）	必填	文档分类枚举：document (普通文档), novel (小说) 等。
version	Number	默认 1	版本控制。重新上传同名文件时递增，用于区分旧版知识。
(建议) status	Enum	可选	整体处理状态：pending (待处理), parsing (解析中), done (完成)。
(建议) createdAt	Date	自动生成	记录创建时间。

documentType(enum)
 document, novel, 对话窗口------

表 2：ChunkStore（数据分块与实体表）
定位：大模型实际阅读的“知识切片”，也是异步向量化任务的队列表。
字段名	数据类型	约束 / 属性	描述说明
id	String	主键 (PK)	分块的唯一标识（即 chunkId）。
documentId	String	外键 (FK), 必填	关联至 DocumentStore.id。
type	Enum（chunkType）	必填	切片类型枚举：text (原文本), entity (提取实体), query (假设性问题)。
content	String	必填	核心文本。向量化和供大模型阅读的实际内容。
summary	String	可选	该分块的摘要描述（用于父子分块检索或大纲展示）。
alias	Array[String]	可选	别名列表。主要在 type 为 entity 时，存储该实体的同义词/缩写。
embeddingStatus	Boolean	默认 false	任务状态标识：true 表示已成功存入 Milvus，false 表示待处理。
failedReason	String	可选	异常记录：如果调用 Embedding 模型失败，记录失败原因以便重试。
(建议) chunkIndex	Number	必填	分块序号（0, 1, 2...）。用于在前端重建文档时保持段落的阅读顺序。

chunkType(enum)
 text, entity, query，对话（用户提问，ai思考内容，ai回答内容）

Milvus数据库
表 3：VectorStore（混合向量表）
定位：支撑基于语义（Dense）和基于关键词（Sparse）的双路召回。
字段名	数据类型	约束 / 属性	描述说明
chunkId	String	主键 (PK)	核心设计：直接把 Mongo 的 chunkId 作为这里的的主键，省去一次 ID 映射。
documentId	String	标量字段, 建立 Trie 索引	冗余自 Mongo。极度重要：用于实现“仅在特定文档内搜索”的秒级过滤。
type	String（vectorType）	标量字段, 建立 Trie 索引	对应 Mongo 中的 vectorType (chunk, query, entity)，用于圈定检索范围。
dense_vector	Float Vector	需指定维度（如 1024）	稠密向量：由大模型生成，用于“理解语义”。(建立 HNSW 索引)。
sparse_vector	Sparse Vector	无维度限制	稀疏向量：由 BM25/SPLADE 生成，用于“精准匹配专有名词”。(建倒排索引)。
metaDate	JSON	动态字段	扩展字段（如存入 { version: 1 }），应对未来可能增加的低频过滤需求。

vectorType(enum)
 chunk ,query, entity