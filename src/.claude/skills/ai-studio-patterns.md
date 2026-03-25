---
name: ai-studio-patterns
description: Coding patterns from ai-studio repository
version: 1.0.0
source: local-git-analysis
analyzed_commits: 100
---

# AI Studio Patterns

## Commit Conventions

This project uses **Conventional Commits** with Chinese descriptions:

| Type | Purpose | Example |
|------|---------|---------|
| `feat:` | New features | `feat: 添加 Milky 模块封装 OneBot API 客户端` |
| `fix:` | Bug fixes | `fix: 修复消息处理多个问题` |
| `refactor:` | Code restructuring | `refactor: 重构 AI 模块包路径` |
| `docs:` | Documentation | `docs: 更新 CLAUDE.md 添加权限系统文档` |
| `chore:` | Build/config changes | `chore: 更新 IDE 配置` |
| `test:` | Test additions | `test: 添加权限系统测试类` |

**Optional scope** in parentheses: `fix(permission): 修复菜单指令权限问题`

## Code Architecture

### Multi-Module Maven Structure

```
ai-studio/
├── common/          # Shared utilities, constants, enums
├── dbModel/         # MongoDB/Neo4j entities and DAOs
├── ai/              # AI processing, RAG, embeddings (LangChain4j)
├── bot/             # Event-driven message handling framework
│   └── framework/   # Core framework (bus, permission, registry)
└── train/            # DJL model training and inference
```

### Key Patterns

#### 1. Event Bus Pattern (`framework/bus/`)
- Uses Reactor Sinks.Many for decoupled messaging
- Annotated with `@PublishBus` for publishing
- `@GroupMessageHandler` for consuming

```java
@PublishBus
public class GroupMessageEvent extends Event {
    // Event fields
}

@GroupMessageHandler
public void handle(GroupMessageEvent event) {
    // Handle logic
}
```

#### 2. Three-Tier Permission System (`framework/permission/`)
- **GlobalPermission**: Command defaults (global_permissions collection)
- **GroupPermission**: Per-group settings (group_permissions collection)
- **UserPermission**: User overrides (user_permissions collection)

#### 3. LangChain4j Assistants (`ai/langchain4j/assistant/`)
- Declarative AI services using `@UserMessage` annotation
- Auto-configured via configuration classes
- Supports Gemini, Ollama, MiniMax, Qwen models

#### 4. Neo4j Knowledge Graph (`ai/service/GraphService.java`)
- Novel entity relationships
- EntityNode base class with extensions
- Chapter/Event/State modeling

### Layered Naming Conventions

| Layer | Suffix | Example |
|-------|--------|---------|
| Controller | `*Controller.java` | `ChatController.java` |
| Service | `*Service.java` | `GraphService.java` |
| Repository | `*Repository.java` | `CommandRepository.java` |
| Entity | Node classes | `CharacterNode.java` |
| Config | `*Config.java` | `MilkyApiConfig.java` |
| Aspect | `*Aspect.java` | `PublishToBusAspect.java` |

## Workflows

### Adding a New Command
1. Create executor in `bot/src/main/java/com/shuanglin/executor/`
2. Register in `CommandRegistry` with `@CommandInfo`
3. Add permission entry if needed
4. Add unit tests in `bot/src/test/java/`

### Adding a New AI Model
1. Create properties class in `ai/.../config/vo/`
2. Add configuration in `ApiModelsConfiguration`
3. Create Assistant class with `@UserMessage`
4. Update `application.yaml` with model settings

### Database Schema Changes
1. Modify entity in `dbModel/src/main/java/com/shuanglin/dao/`
2. Update repository if needed
3. Add initialization logic in `*Initializer.java`

## Testing Patterns

- Test files: `src/test/java/` mirror of main structure
- Test class naming: `*Test.java` or `*IT.java` for integration
- Uses Spring Boot Test with embedded MongoDB
- Mock dependencies with Mockito

## Configuration Management

- Uses Maven profiles: `local`, `dev` (default), `sit`, `prod`
- External configs in `config/` directory (git-ignored)
- Profile-specific: `config/{profile}/application.yaml`
- Common configs: `config/common/application.yaml`

### Build Commands

```bash
# Local development
mvn clean install -Plocal

# Development (default)
mvn clean install

# SIT environment
mvn clean install -Psit
```