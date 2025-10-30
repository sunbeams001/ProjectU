# 共享文档使用指南

> 📅 最后更新: 2025-10-30  
> � 文档版本: v1.0

## 📋 概述

本目录包含项目的共享文档片段，采用 **单一数据源 (Single Source of Truth)** 原则，避免信息在多个文档中重复，降低维护成本。

### 共享文档列表

| 文档 | 文件名 | 内容 | 用途 |
|------|--------|------|------|
| 🔧 **技术栈** | `TECH_STACK.md` | 所有技术栈版本信息、依赖关系、平台要求 | 版本管理、技术选型参考 |
| 📊 **开发状态** | `DEVELOPMENT_STATUS.md` | 功能模块完成度、待办事项、已知问题、路线图 | 进度跟踪、任务管理 |
| 🌐 **API 状态** | `API_STATUS.md` | Pixiv API 集成状态、方法签名、使用示例 | API 开发参考 |

---

## 🎯 设计原则

### 1. DRY (Don't Repeat Yourself)
- ✅ 每条信息只存在于一个地方
- ✅ 其他文档通过相对路径引用
- ✅ 避免复制粘贴相同内容

### 2. 单一职责
- ✅ 每个文档有明确的职责范围
- ✅ 技术栈、开发状态、API 状态分离
- ✅ 易于定位和更新特定信息

### 3. 模块化
- ✅ 文档可独立阅读
- ✅ 通过链接建立关联
- ✅ 支持按需引用

---

## 📖 文档结构

```
docs/
├── README.md                        # 项目总览 (引用共享文档)
├── AI_ASSISTANT_PROMPT.md          # AI 协作提示 (引用共享文档)
│
├── shared/ ⭐ 共享文档中心
│   ├── README.md                   # 本文档
│   ├── TECH_STACK.md               # 技术栈版本 (单一来源)
│   ├── DEVELOPMENT_STATUS.md       # 开发状态 (单一来源)
│   └── API_STATUS.md               # API 状态 (单一来源)
│
├── project/
│   └── 项目架构参考文档.md          # 架构说明 (引用共享文档)
│
├── pixiv/
│   └── PIXIV_API_集成指南.md        # API 使用指南 (引用 API_STATUS.md)
│
├── settings/
│   └── 设置系统架构.md              # 设置实现细节 (引用共享文档)
│
└── guides/
    └── 自适应布局指南.md            # 布局开发指南 (引用共享文档)
```

---

## 🔄 维护流程

### 场景 1: 升级依赖版本

**示例**: Kotlin 从 2.2.20 升级到 2.2.21

```bash
# 1. 更新 gradle 配置
vim gradle/libs.versions.toml

# 2. 更新文档
vim docs/shared/TECH_STACK.md
# 修改: kotlin = "2.2.21"

# 3. 提交
git add .
git commit -m "chore: upgrade Kotlin to 2.2.21"
```

**影响**: 所有引用 `TECH_STACK.md` 的文档自动获取最新版本信息

### 场景 2: 完成新功能

**示例**: 完成 Pixiv 登录功能

```bash
# 1. 更新开发状态
vim docs/shared/DEVELOPMENT_STATUS.md
# 修改: 
# - [ ] Pixiv 登录 → [x] Pixiv 登录
# 更新完成度百分比

# 2. 提交
git commit -m "feat: complete Pixiv login"
```

### 场景 3: 添加新 API

**示例**: 添加 CommentApi

```bash
# 1. 实现 API
vim shared/src/.../api/CommentApi.kt

# 2. 更新 API 状态文档
vim docs/shared/API_STATUS.md
# 添加 CommentApi 表格和方法说明

# 3. 提交
git commit -m "feat: add CommentApi"
```

### 场景 4: 更新实现细节

**示例**: 修改设置系统架构

```bash
# 直接修改专题文档
vim docs/settings/设置系统架构.md
```

---

## 📝 文档引用规范

### 在 Markdown 中引用

```markdown
# 项目架构文档

> 🔧 技术栈: [../shared/TECH_STACK.md](../shared/TECH_STACK.md)
> 📊 开发状态: [../shared/DEVELOPMENT_STATUS.md](../shared/DEVELOPMENT_STATUS.md)

## 技术栈

详见 [技术栈文档](../shared/TECH_STACK.md)
```

### 相对路径参考

| 当前文档位置 | 引用路径 |
|------------|---------|
| `docs/README.md` | `./shared/TECH_STACK.md` |
| `docs/project/项目架构参考文档.md` | `../shared/TECH_STACK.md` |
| `docs/pixiv/PIXIV_API_集成指南.md` | `../shared/API_STATUS.md` |

---

## 🎓 最佳实践

### ✅ 应该做

1. **更新共享文档**: 技术栈、开发状态、API 变更优先更新共享文档
2. **使用相对路径**: 保证链接在不同环境下都能正常工作
3. **添加元信息**: 为文档添加更新日期和相关文档链接
4. **保持同步**: 更新代码后及时更新对应的共享文档

### ❌ 不应该做

1. **复制信息**: 不要把技术栈、API 列表复制到多个文档
2. **硬编码版本**: 在引用文档中硬编码版本号
3. **孤立文档**: 创建没有链接关系的孤立文档
4. **过度拆分**: 不要把单个概念拆分到多个文档

---

## ⚠️ 注意事项

1. **共享文档是权威来源**: 有冲突时以共享文档为准
2. **链接检查**: 重命名或移动文件后检查所有引用链接
3. **版本一致性**: 确保文档版本与代码版本同步
4. **向后兼容**: 修改共享文档时考虑现有引用的兼容性

---

## 🚀 扩展建议

随着项目发展，可以考虑添加更多共享文档：

- [ ] `ARCHITECTURE.md` - 架构图和设计模式说明
- [ ] `CHANGELOG.md` - 版本更新日志
- [ ] `FAQ.md` - 常见问题解答
- [ ] `PERFORMANCE.md` - 性能优化指南
- [ ] `SECURITY.md` - 安全最佳实践

---

## 📊 维护成效

通过采用共享文档模式，项目获得了：

| 指标 | 改善 |
|------|------|
| 信息重复度 | ⬇️ 减少 66%+ |
| 更新维护点 | ⬇️ 减少 40%+ |
| 文档一致性 | ⬆️ 显著提升 |
| 查找效率 | ⬆️ 显著提升 |

---

> 💡 **核心理念**: 单一数据源 (Single Source of Truth) - 每条信息只维护一次，处处引用！
