# 开发流程（Development Process）

本项目为 Minecraft 1.12.2 Forge 模组：拾取筛（Pickup Filter）。

## 环境要求

- JDK：建议 8（与 1.12.2/ForgeGradle 体系兼容）
- Gradle Wrapper：使用仓库自带 `./gradlew`

## 常用命令

- 构建（含测试）：`./gradlew build`
- 仅运行测试：`./gradlew test`
- 打包发布 Jar：`./gradlew reobfJar`（产物在 `build/libs/`）
- 运行开发客户端：`./gradlew runClient`

## 性能调参（服务端）

通用配置文件：`config/pickupfilter-common.properties`（首次运行会自动生成/补全）。

- 扫描最小间隔（默认 `5`）：`auto_destroy.scan_interval.min_ticks`
- 扫描最大间隔（默认 `5`，与最小相同表示不启用退避）：`auto_destroy.scan_interval.max_ticks`
- 空区域退避阈值（默认 `2`，仅当 `max > min` 时生效）：`auto_destroy.scan_backoff.empty_miss_threshold`
- 单次扫描实体上限（默认 `0` 表示不限制）：`auto_destroy.scan.max_entities`

## 变更提交流程（建议）

- 拉取远程最新：`git fetch origin`（必要时 `git pull --rebase`），确认 `DEVELOPMENT.md` 是否为最新
- 同步主干：将当前分支 `rebase` 到最新 `origin/main`
- 解决冲突：以 `main` 上的最新改动为准，必要时在提交说明中简述冲突处理
- 问题检查：对照 `分析文档.md` 中的 P0/P1 问题确认是否命中，命中则优先优化
- 安全/稳定性检查（建议最少做一次）：
  - 配置/设置读写：避免"读取失败后写回覆盖"，并确保异常可观测（日志）
  - 网络包解析：`fromBytes` 必须具备 `readableBytes` 防护；字符串/列表长度要有上限且发送/接收一致
  - 用户输入解析：任何来自 GUI/聊天/配置的字符串解析都要可失败（返回 null/默认值），不能抛出崩溃
- 性能检查（按改动范围选择）：
  - tick 级回调（如 `LivingUpdate`）：尽量做节流/短路（规则为空直接返回等）
  - 高频扫描：优先"活动信号驱动 + 空结果退避"的自适应节流；单次扫描设置预算/上限或分片轮询，避免密集掉落场景单帧卡顿；必要时引入可配置的扫描间隔（最小/最大值）
  - 规则变更检测：避免在 tick 中对规则列表做 `hashCode()`/序列化等 O(n) 操作，优先使用"引用变化/版本号"来触发自适应扫描
  - 事件驱动优化：若引入掉落物生成/消失监听，需维护候选集合并在 tick 中只处理增量
  - 去重/查找：避免循环内 `List.contains` 造成 O(n²)，优先 `LinkedHashSet`
- 验证：至少跑一次 `./gradlew build`（或与改动匹配的最小集合），确保通过并记录关键输出
- 合并：将改动合并到 `main`（若远端受保护则创建 PR 并完成合并流程），确保 `main` 最新且构建/测试通过
- 文档更新：若流程/命令有变更，请同步更新 `DEVELOPMENT.md` 并在提交说明中记录

## 代码审查流程（Code Review Process）

### 审查时机

- **定期审查**：每月或每季度进行一次全面代码审查
- **重大更新前**：在发布新版本前进行审查
- **问题跟踪**：参考 `分析文档.md` 中的问题清单，确保P0/P1问题已修复

### 审查清单（Checklist）

#### 1. 安全性（Security）

- [ ] **反射使用**：使用 `setAccessible(true)` 后在 `finally` 块中恢复
- [ ] **字符串编码**：多字节字符（中文、日文等）的截断要按字符边界处理
- [ ] **资源管理**：文件、流、网络连接等资源正确关闭
- [ ] **输入验证**：所有用户输入（GUI、配置文件、网络包）都经过验证
- [ ] **异常处理**：避免捕获过于宽泛的异常（如 `Exception`），应捕获具体类型

#### 2. 并发安全（Thread Safety）

- [ ] **共享状态**：正确使用 `synchronized`、`volatile` 或并发集合
- [ ] **双重检查锁定**：DCL模式正确实现，使用局部变量缓存
- [ ] **原子操作**：使用 `ConcurrentHashMap` 的 `computeIfAbsent`、`compute` 等原子方法
- [ ] **锁粒度**：避免过大的同步块，考虑使用 `ReadWriteLock`

#### 3. 网络包安全（Network Packet Safety）

- [ ] **缓冲区检查**：`fromBytes` 方法中检查 `buf.readableBytes()`
- [ ] **长度限制**：字符串和列表长度有上限，且发送/接收端一致
- [ ] **错误处理**：解析失败时抛出异常或返回错误码，不静默失败
- [ ] **空值检查**：`ctx`、`getServerHandler()` 等可能为 null 的对象需检查

#### 4. 性能优化（Performance）

- [ ] **高频操作**：tick 事件中避免不必要的计算和对象创建
- [ ] **算法效率**：避免 O(n²) 操作，如循环内 `List.contains`
- [ ] **缓存策略**：静态数据（GUI布局、Pattern对象）应缓存
- [ ] **早期返回**：在执行昂贵操作前先做条件检查
- [ ] **对象池**：高频创建的临时对象考虑使用对象池

#### 5. 代码质量（Code Quality）

- [ ] **代码重复**：相同逻辑提取为公共方法或工具类
- [ ] **魔法数字**：硬编码的数值定义为常量，并添加注释
- [ ] **未使用代码**：删除或标记为 `@Deprecated`
- [ ] **异常处理**：不使用 `catch (Exception ignored)`，至少记录日志
- [ ] **输入验证**：边界条件、null 检查、范围验证

#### 6. 测试覆盖（Test Coverage）

- [ ] **单元测试**：关键逻辑有对应的单元测试
- [ ] **边界测试**：测试边界条件（空列表、最大值、null等）
- [ ] **网络包测试**：测试畸形数据、不完整数据的处理
- [ ] **属性测试**：使用 QuickTheories 测试复杂逻辑

#### 7. 文档完整性（Documentation）

- [ ] **代码注释**：复杂逻辑有清晰的注释说明
- [ ] **API文档**：公共方法有 JavaDoc
- [ ] **变更日志**：重要修改记录在 `分析文档.md`
- [ ] **配置说明**：新增配置项在 `README.md` 和本文档中说明

### 审查工具建议

- **静态分析**：SpotBugs、SonarQube、Error Prone
- **代码格式**：Checkstyle、Google Java Format
- **依赖检查**：OWASP Dependency-Check（检查依赖漏洞）
- **测试覆盖**：JaCoCo（代码覆盖率报告）

### 问题优先级定义

- **P0（严重）**：安全漏洞、数据丢失、服务器崩溃 → 立即修复
- **P1（高）**：功能失效、严重性能问题、用户体验重大影响 → 本周内修复
- **P2（中）**：代码重复、轻微性能问题、维护性问题 → 计划修复
- **P3（低）**：代码风格、优化建议、技术债务 → 持续改进

### 审查结果记录

审查完成后，更新 `分析文档.md`：
1. 在第0节添加审查时间和发现问题总数
2. 在对应章节记录新发现的问题
3. 更新问题状态（待修复、已修复、待优化）
4. 添加修复建议和代码示例

## GUI/图层注意事项

Minecraft GUI 的 tooltip/overlay 通常通过 z-level 与 depth buffer 叠加；当 overlay 与 tooltip 区域重叠时，半透明背景会导致“底层文字透出/干扰阅读”的观感问题。

建议：

- 任何新增/修改 overlay 信息栏时，都要考虑 tooltip 触发场景（鼠标悬停物品格子、JEI 等）
- 若 overlay 与 tooltip 可能重叠，优先“自适应换位”（右侧/底部/左侧/上方/角落），找不到合适位置则临时隐藏 overlay
