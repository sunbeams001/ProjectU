# API 测试工具使用指南

> 📅 创建日期: 2025-10-30  
> 🎯 目的: 帮助你使用新创建的 API 测试工具进行系统化测试

---

## ✅ 已完成的工作

### 1. 创建了完整的 API 测试工具

**文件清单**:
- `ApiTestModels.kt` - 定义了所有 22 个 API 方法和参数
- `ApiTestContract.kt` - 状态管理和意图定义
- `ApiTestViewModel.kt` - 实现了所有 API 的调用逻辑
- `ApiTestScreen.kt` - 完整的测试界面 UI

**功能特性**:
- ✅ 支持 4 个模块: IllustApi, UserApi, BookmarkApi, RankingApi
- ✅ 支持 22 个 API 方法测试
- ✅ 按优先级分类 (P0/P1/P2)
- ✅ 动态参数输入（文本框/下拉选择）
- ✅ 实时结果展示（摘要 + 原始 JSON）
- ✅ 登录状态检测
- ✅ 错误处理和堆栈跟踪

### 2. 集成到应用

- ✅ 在 Android 和 Desktop 的 Koin 模块中注册了 `ApiTestViewModel`
- ✅ 在设置页面添加了 "API 测试工具 🛠️" 入口

---

## 🚀 如何使用

### 步骤 1: 配置登录凭据

1. 运行应用（Android 或 Desktop）
2. 进入 **设置** 页面
3. 在 "账号管理" 部分，点击 **PHPSESSID** 配置项
4. 输入你从浏览器获取的 PHPSESSID（格式: `12345678_xxxxxxxxxxxx`）
5. 保存

### 步骤 2: 打开 API 测试工具

1. 在 **设置** 页面
2. 在 "Pixiv" 分组下，找到 **API 测试工具 🛠️**
3. 点击进入测试页面

### 步骤 3: 选择要测试的 API

**左侧面板** - API 选择器:

1. **选择模块**: 
   - 插画 API (IllustApi)
   - 用户 API (UserApi)
   - 收藏 API (BookmarkApi)
   - 排行榜 API (RankingApi)

2. **选择方法**:
   - 带有 **P0** 红色标签的是高优先级 API（必测）
   - 带有 **P1** 蓝色标签的是中优先级 API
   - 带有 **P2** 紫色标签的是低优先级 API

### 步骤 4: 输入测试参数

**右侧上半部分** - 参数输入区:

1. 查看已自动填充的默认参数值
2. 根据需要修改参数:
   - 必填参数标记有红色 `*`
   - 有预定义选项的参数显示为下拉菜单
   - 其他参数为文本输入框

3. 点击 **执行测试** 按钮

### 步骤 5: 查看测试结果

**右侧下半部分** - 结果展示区:

成功时显示两个标签页:
- **摘要** - 格式化的关键信息展示
- **原始 JSON** - 完整的 API 响应数据

失败时显示:
- 错误消息
- 堆栈跟踪（用于调试）

---

## 📋 测试清单 (按测试计划)

### 阶段 1: P0 高优先级 API

按照以下顺序进行测试：

#### IllustApi (插画)
- [ ] **getIllustDetail** - 获取作品详情
  - 测试 ID: `102814610`
  - 验证: 标题、作者、图片 URL、统计数据
  
- [ ] **searchIllust** - 搜索作品
  - 关键词: `初音ミク`
  - 验证: 返回结果数量、作品信息

#### UserApi (用户)
- [ ] **getUserInfo** - 获取用户信息
  - 测试 ID: `11` (pixiv 官方账号)
  - 验证: 用户名、头像
  
- [ ] **getUserFullInfo** - 完整用户信息
  - 测试 ID: `11`
  - 验证: 作品数、收藏数

#### BookmarkApi (收藏)
- [ ] **addBookmark** - 添加收藏 ⚠️
  - **注意**: 会实际添加收藏，测试后需要删除
  - 验证: 返回成功状态
  
- [ ] **deleteBookmark** - 删除收藏 ⚠️
  - **注意**: 需要先有收藏 ID
  - 验证: 返回成功状态

#### RankingApi (排行榜)
- [ ] **getDailyRanking** - 日榜
  - 页码: `1`
  - 验证: HTML 返回（需要后续解析）

---

### 阶段 2: P1 中优先级 API

#### IllustApi
- [ ] **getRecommendedIllust** - 推荐作品
- [ ] **getRelatedIllust** - 相关作品
- [ ] **getDiscoveryIllust** - 发现作品

#### UserApi
- [ ] **getUserIllusts** - 用户作品列表
- [ ] **getUserBookmarks** - 用户收藏

#### RankingApi
- [ ] **getWeeklyRanking** - 周榜
- [ ] **getMonthlyRanking** - 月榜

---

### 阶段 3: P2 低优先级 API

#### IllustApi
- [ ] **getUgoiraMetadata** - 动图元数据
  - 测试 ID: `44298467` (动图作品)

#### UserApi
- [ ] **getUserFollowing** - 关注列表

#### BookmarkApi
- [ ] **getBookmarkTags** - 收藏标签

#### RankingApi
- [ ] **getRookieRanking** - 新人榜
- [ ] **getOriginalRanking** - 原创榜
- [ ] **getMaleRanking** - 男性向
- [ ] **getFemaleRanking** - 女性向
- [ ] **getR18DailyRanking** - R18 日榜

---

## 🔍 测试要点

### 成功标准

✅ API 调用返回 200 状态码  
✅ `error` 字段为 `false`  
✅ 返回数据结构符合 DTO 定义  
✅ 关键字段有值且类型正确  

### 常见问题排查

#### 问题 1: "请先配置登录凭据"
**原因**: PHPSESSID 未配置或为空  
**解决**: 在设置页面配置 PHPSESSID

#### 问题 2: 401 Unauthorized
**原因**: PHPSESSID 过期或无效  
**解决**: 重新从浏览器获取最新的 PHPSESSID

#### 问题 3: 数据类型不匹配
**原因**: DTO 定义与实际 API 返回不符  
**解决**: 
1. 查看 "原始 JSON" 标签页
2. 对比 DTO 定义
3. 修改 `shared/data/remote/dto/` 中的 DTO 文件

#### 问题 4: 网络超时
**原因**: 可能需要代理访问 Pixiv  
**解决**: 检查网络连接和代理设置

---

## 📊 测试记录建议

为每个 API 创建测试记录（可以在测试工具中复制结果）:

```markdown
### API 方法名 测试记录

**测试时间**: 2025-10-30  
**测试环境**: Android / Desktop  

**测试参数**:
- param1: value1
- param2: value2

**测试结果**: ✅ 成功 / ❌ 失败

**关键数据验证**:
- [ ] 字段1: 值正确
- [ ] 字段2: 类型匹配
- [ ] 字段3: 非空验证

**发现问题**:
- 无 / [描述问题]

**需要修复**:
- 无 / [列出需要修复的内容]
```

---

## 🛠️ 下一步工作

完成所有 API 测试后：

1. **汇总测试结果**
   - 统计成功/失败数量
   - 记录发现的问题

2. **修复 DTO 定义**
   - 根据实际返回数据调整
   - 更新 Mapper 逻辑

3. **更新文档**
   - 在 `docs/shared/API_STATUS.md` 中更新测试状态
   - 记录特殊注意事项

4. **开始实现 UI 功能**
   - 作品列表页面
   - 作品详情页面
   - 用户主页等

---

## 💡 提示

1. **优先测试 P0 API**: 这些是核心功能必需的
2. **测试顺序**: 建议按照上面的清单从上到下测试
3. **保存结果**: 可以使用截图或复制文本保存测试结果
4. **收藏 API 谨慎**: addBookmark 和 deleteBookmark 会实际修改数据
5. **网络环境**: 确保可以访问 Pixiv (可能需要代理)

---

## 🎉 祝测试顺利！

如果遇到任何问题，可以:
1. 查看 "原始 JSON" 标签了解详细响应
2. 查看错误消息和堆栈跟踪
3. 检查 `shared/data/remote/api/` 中的 API 实现
4. 参考 `docs/pixiv/PIXIV_API_集成指南.md`
