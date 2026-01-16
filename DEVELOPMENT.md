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

## 变更提交流程（建议）

- 拉取远程最新：`git fetch origin`（必要时 `git pull --rebase`），确认 `DEVELOPMENT.md` 是否为最新
- 同步主干：将当前分支 `rebase` 到最新 `origin/main`
- 解决冲突：以 `main` 上的最新改动为准，必要时在提交说明中简述冲突处理
- 问题检查：对照 `分析文档.md` 中的 P0/P1 问题确认是否命中，命中则优先优化
- 安全/稳定性检查（建议最少做一次）：
  - 配置/设置读写：避免“读取失败后写回覆盖”，并确保异常可观测（日志）
  - 网络包解析：`fromBytes` 必须具备 `readableBytes` 防护；字符串/列表长度要有上限且发送/接收一致
  - 用户输入解析：任何来自 GUI/聊天/配置的字符串解析都要可失败（返回 null/默认值），不能抛出崩溃
- 性能检查（按改动范围选择）：
  - tick 级回调（如 `LivingUpdate`）：尽量做节流/短路（规则为空直接返回等）
  - 高频扫描：优先自适应节流（空结果退避/移动触发/规则更新触发），必要时引入可配置的扫描间隔
  - 去重/查找：避免循环内 `List.contains` 造成 O(n²)，优先 `LinkedHashSet`
- 验证：至少跑一次 `./gradlew build`（或与改动匹配的最小集合），确保通过并记录关键输出
- 合并：将改动合并到 `main`（若远端受保护则创建 PR 并完成合并流程），确保 `main` 最新且构建/测试通过
- 文档更新：若流程/命令有变更，请同步更新 `DEVELOPMENT.md` 并在提交说明中记录

## GUI/图层注意事项

Minecraft GUI 的 tooltip/overlay 通常通过 z-level 与 depth buffer 叠加；当 overlay 与 tooltip 区域重叠时，半透明背景会导致“底层文字透出/干扰阅读”的观感问题。

建议：

- 任何新增/修改 overlay 信息栏时，都要考虑 tooltip 触发场景（鼠标悬停物品格子、JEI 等）
- 若 overlay 与 tooltip 可能重叠，优先“自适应换位”（右侧/底部/左侧/上方/角落），找不到合适位置则临时隐藏 overlay
