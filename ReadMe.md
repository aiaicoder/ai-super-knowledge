理解你的问题: doing
收集相关的信息: doing
# AI-RAG-Knowledge 智能知识库系统

[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](https://www.apache.org/licenses/LICENSE-2.0)

本项目是一个集成了**检索增强生成（RAG）**和**模型控制协议（MCP）**的智能知识库系统，旨在为企业提供全方位的 AI 辅助解决方案。通过结合多种大语言模型的能力，实现了从文档解析到代码分析的全链路智能化处理。

---

## 🧠 项目核心特性

- **多模态支持**：集成 OpenAI、Ollama 等主流大模型接口，支持文本生成、语义理解与向量化存储。
- **文档智能解析**：使用 `spring-ai-tika-document-reader` 实现对 PDF、Word、Markdown 等格式文档的自动提取与结构化。
- **向量数据库支持**：基于 `pgvector` 实现高效语义搜索，支持大规模非结构化数据的知识抽取与召回。
- **模型控制协议（MCP）**：统一管理模型调用流程，提升模型交互效率与可扩展性。
- **分布式任务调度**：结合 Redisson 实现任务队列与分布式锁，保障高并发场景下的稳定性。
- **Git 集成**：内置 Git 操作能力，支持版本追踪与知识更新自动化。

---

## 📦 项目模块说明

```bash
ai-rag-knowledge/
├── xin-dev-tech-api       # 公共 API 定义模块，包含实体类与接口定义
├── xin-dev-tech-app       # 核心业务逻辑模块，实现 RAG 与 MCP 主要功能
├── xin-dev-tech-trigger   # 触发器模块，负责事件监听、定时任务与外部触发
└── pom.xml                # Maven 多模块聚合配置
```


---

## ⚙️ 技术栈

| 技术 | 描述 |
|------|------|
| Spring Boot | 快速构建微服务框架 |
| Spring AI | 集成 LLM 接口与工具链 |
| PgVector | PostgreSQL 向量插件，支持语义相似度检索 |
| Redis / Redisson | 分布式缓存与任务调度 |
| FastJSON | JSON 数据处理 |
| Apache Commons Lang3 | 常用工具类封装 |
| JGit | Java 实现的 Git 工具库 |
| OLLAMA / OpenAI | 支持本地与云端大模型部署 |

---

## 🛠️ 开发环境要求

- JDK 17+
- Maven 3.8+
- PostgreSQL + pgvector 插件
- Redis 6.2+
- Docker（用于快速部署依赖服务）

---

## 🚀 快速启动

1. **拉取镜像并启动依赖服务**

   ```bash
   docker-compose -f docs/dev-ops/docker-compose-environment-aliyun.yml up -d
   ```


2. **构建项目**

   ```bash
   mvn clean install
   ```


3. **运行主模块**

   ```bash
   cd xin-dev-tech-app
   mvn spring-boot:run
   ```


## 📄 相关文档

- [Docker 部署指南](docs/dev-ops/docker-compose-environment-aliyun.yml)
- [MCP 协议设计说明](docs/mcp-design.md)
- [RAG 架构详解](docs/rag-architecture.md)

---

## 🤝 贡献者

欢迎参与贡献！请参考 [CONTRIBUTING.md](CONTRIBUTING.md) 获取更多信息。

---

## 📜 许可证

该项目遵循 [Apache License 2.0](LICENSE)，详情请见 LICENSE 文件。

---

如需技术支持或定制开发，请联系项目维护团队。