# 开发流程

## 环境要求

- JDK 8（运行 1.12.2 推荐）；JDK 11 可用于构建（Gradle 4.10.3 兼容）
- Git
- 网络可用（首次构建需要下载 Forge/Minecraft 依赖）

## 常用命令

- 构建：`./gradlew build`
- 打包 JAR（仅打包，不跑测试）：`./gradlew jar`
- 打包可分发的 Mod JAR（含重映射/混淆，通常用于发布）：`./gradlew reobfJar`
- 运行开发客户端：`./gradlew runClient`
- 运行测试：`./gradlew test`
- 清理：`./gradlew clean`

## 打包 Jar（发布/分发）

本项目是 Forge Mod，最终产物就是一个可放入 `mods/` 的 Jar 文件（并非 `java -jar` 直接运行的应用）。

### 我应该用 `jar` 还是 `reobfJar`？

- `./gradlew jar`：生成开发用 Jar（更偏向本地开发/CI 校验）。某些环境下放进 `mods/` 也能跑，但发布/分发不推荐只跑这个。
- `./gradlew reobfJar`：生成**可分发**的 Mod Jar（包含重映射/混淆处理），推荐用于“发给别人/放进整合包/投放服务端”。

### 产物位置

- 默认输出目录：`build/libs/`
- 产物命名：`${archives_base_name}-${mod_version}.jar`（例如 `pickupfilter-0.1.0.jar`）
- 版本号来源：`gradle.properties` 中的 `mod_version`

### 为什么会有两个 Jar（`*-sources.jar` 是什么）？

通常你会在 `build/libs/` 看到两类产物：

- `pickupfilter-0.1.0.jar`：**Mod 本体**（要放进 `mods/` 的就是它）
- `pickupfilter-0.1.0-sources.jar`：**源码包**（给开发者/IDE 用的，用于“附加源码/跳转到源码/调试”，不需要也不应该放进 `mods/`）

结论：玩家安装/联机只需要 `pickupfilter-*.jar`；`*-sources.jar` 仅用于开发调试或发布到 Maven 供他人引用时的“源码附件”。

### 推荐打包命令

- 本地验证 + 打包（推荐）：`./gradlew clean build --no-daemon --console=plain`
- 仅生成可分发 Jar：`./gradlew clean reobfJar --no-daemon --console=plain`
- 只想快速拿到 Jar（不建议用于发布）：`./gradlew clean jar --no-daemon --console=plain`

完成后，将 `build/libs/*.jar` 复制到客户端/整合包的 `.minecraft/mods/`（或启动器对应的实例 `mods/`）目录即可。

## 测试约定

- 本项目使用 JUnit 4 + QuickTheories 编写属性测试（`./gradlew test` 会一并运行）。
- 属性测试默认至少 100 次随机样例（以测试代码中的 `withExamples(...)` 为准）。
- 对客户端/输入相关逻辑（如按键绑定）：优先用小型适配器接口隔离 Forge 的静态注册点（例如 `KeyBindingRegistrar`），在单元测试中用 fake 实现验证注册与按键触发逻辑，避免依赖启动 Minecraft 客户端。
- 对 GUI：避免在单元测试中直接实例化 `GuiScreen` / `GuiSlot`，优先将“规则编辑/选择/校验”等行为收敛到纯 Java 模型（例如 `FilterRulesEditor`），并用单元测试覆盖；运行期再由 GUI 类做渲染与输入绑定。
- 对“客户端触发、服务端执行”的输入功能（例如按键触发清除周围掉落物）：客户端只负责发包；服务端在主线程执行世界查询/实体移除，并通过聊天消息回传结果，避免客户端直接改世界状态导致不同步。

## 目录结构

- `src/main/java`：模组代码
  - `com.fenglingyubing.pickupfilter`：主入口与跨模块协调
  - `proxy/`：Common/Client 代理（客户端类隔离）
  - `config/`：配置与模式状态
  - `event/`：服务端/通用事件处理（不引用客户端类）
  - `client/`：客户端相关（按键、GUI、反馈）
- `src/main/resources`：资源与 `mcmod.info`
- `docs/`：需求/设计/任务与开发说明

## 开发约定

- 客户端类只放在 `client/` 下，并通过 `ClientProxy` 注册，避免专用服务器类加载崩溃。
- 业务逻辑优先放在可测试的纯 Java 类中（后续任务会补齐测试框架与属性测试）。
- 如改动涉及玩家可见行为（按键、模式语义、配置界面文案/字段、清除范围等），同步更新用户文档：
  - `docs/usage.md`
  - `README.md`（保持“游戏内使用”摘要与默认快捷键一致）

## 配置存储与作用域（规则/匹配名单）

- 本模组的“规则列表/匹配名单”与“模式”是**按玩家、按存档（世界）保存**的：每个玩家在每个世界都有自己独立的一份配置（客户端 GUI 只是发包修改该玩家在服务端保存的配置）。
- 规则列表分为两份：`拾取匹配列表` 与 `销毁匹配列表`，分别在对应模式下生效；GUI 默认编辑“当前模式对应的列表”。
- 存储位置：写入玩家存档的持久化 NBT（`EntityPlayer.PERSISTED_NBT_TAG` 下的 `pickupfilter` 节点），因此：
  - 单人（集成服务器）：不同存档/不同世界互不影响；同一世界里不同玩家也互不影响。
  - 多人（专用服务器）：同一服务器内不同玩家互不影响；不同服务器/不同世界存档也互不影响（配置随该服务器世界的玩家数据一起保存）。

## 全局配置文件（非按玩家保存）

除“按玩家/按存档”的规则列表外，本模组还有少量“全局配置文件”，位于 `config/` 目录：

- `config/pickupfilter-common.properties`：通用/服务端逻辑配置
  - `clear_drops.chunk_radius`：按 `K` 清除掉落物/箭矢的范围（单位：区块；默认 `2`；范围 `0~16`）
- `config/pickupfilter-client.properties`：客户端体验配置
  - `inventory.button.offset_x/y`：背包界面“筛”按钮的像素偏移

## GUI 开发与自测（背包按钮 / 物品匹配）

- 入口：玩家背包（默认 `E`）界面新增 “筛” 按钮（优先放在背包右下角附近，避免遮挡其他模组按钮），打开 `PickupFilterMatcherScreen`。
- 如果 “筛” 按钮与其他模组按钮重叠/被遮挡：可在 `P` 打开的配置界面点击 `背包按钮：调整位置`，对按钮做像素级偏移（保存到 `config/pickupfilter-client.properties` 的 `inventory.button.offset_x/y`）。
- 清除范围：可在 `P` 打开的配置界面点击 `清除范围：...` 调整（写入 `config/pickupfilter-common.properties` 的 `clear_drops.chunk_radius`）。
- 风格：`PickupFilterMatcherScreen` 采用**原版容器 GUI 的经典灰色配色**（使用 `textures/gui/container/generic_54.png` 的布局/槽位风格），并保持“上方匹配列表 + 下方背包”的 9×3 / 9×(3+热键栏) 格子结构；重新打开时会从服务端快照加载并显示当前已保存的物品规则（拾取/销毁两份列表各自独立显示）。
- 风格：`PickupFilterConfigScreen` 采用**暗色霓虹/玻璃面板**风格（深色渐变底 + 绿色主强调色），并对 `GuiSlot` 列表绘制了自定义行高/悬停/选中态与滚动条；如需保持一致性，优先复用其配色常量与 `NeonButton` 的绘制方式。
- 保存策略：匹配界面添加/移除会**自动保存**；配置界面添加/删除也会自动保存（“存/应用”按钮仅作为手动同步兜底）。
- 匹配界面：
  - 规则生成：对工具/护甲等**可损坏物品**（耐久度会变化），从背包添加到列表时会自动将 metadata 归一为 `*`（忽略耐久度），避免“同物品因耐久/属性不同而无法拾取/销毁”或出现重复条目。
  - 悬停提示：列表/背包中的物品 tooltip 在界面最上层渲染，避免被格子/按钮遮挡。
  - 顶部按钮栏：`拾取列表 / 销毁列表` 与分页 `< / >` 同行显示，tab 宽度按面板宽度动态计算，保证不与分页按钮重叠。
    - 如需手工微调顶部按钮位置/间距：改 `PickupFilterMatcherScreen` 的 `MATCHER_TAB_Y_OFFSET / MATCHER_TAB_X_MARGIN / MATCHER_TABS_GAP / MATCHER_TABS_TO_PAGER_GAP / MATCHER_PAGER_BTN_W` 常量（入口在 `initGui()` 里创建 tab/pager 按钮）。
  - `拾取列表 / 销毁列表`：切换正在编辑的匹配列表（不需要先切换模式即可分别维护两份列表）
  - `同步到服务器`：手动同步当前列表（一般不需要）
  - `清空列表`：清空当前列表的“物品条目”（保留未在此界面展示的高级规则）
  - `打开配置`：打开配置界面
  - `返回背包`：返回背包
- 快捷键：背包界面鼠标悬停在物品图标上按 `A`，可直接把该物品合并进规则并同步（依赖客户端已拿到一次配置快照，否则会先发起同步请求）。
- 手工验证建议：
  - 启动开发客户端：`./gradlew runClient`
  - 进入世界 → 打开背包 → 点击 “筛” → 切换到“拾取列表”/“销毁列表” → 选择若干物品 → 关闭并重新打开确认条目可见
  - 进入世界 → 打开背包 → 鼠标悬停物品 → 按 `A` → 再按 `P` 打开配置确认规则已追加并去重
  - 进入世界 → 按 `O` 切换模式 → 屏幕上方提示栏（actionbar）会提示当前模式名称（也可用 `P` 打开配置界面确认当前模式）
  - 进入世界 →（可选）修改 `config/pickupfilter-common.properties` 的 `clear_drops.chunk_radius` → 向地上射几支箭 → 按 `K` 清除附近掉落物/箭矢 → 确认范围符合预期且射在地上的箭矢被清除

## Git 流程（提交/合并）

- 开发完成后先同步主分支：`git fetch origin main`，并将当前分支 `rebase` 到最新 `origin/main`。
- 如有冲突：以 `origin/main` 的最新改动为准再合并本分支需要的变更；解决后继续 `rebase`，并在提交说明中简述冲突处理结果。
- 在合并前运行必要构建/测试：至少 `./gradlew test`（必要时加 `./gradlew build` / `./gradlew runClient` 验证）。
- 将改动合并到 `main`：优先保持 `main` 线性历史（快进或 rebase 后合并）；如仓库策略要求则创建 PR 并完成合并。
- 合并完成后再次在 `main` 上确认构建/测试通过，并在备注中记录 rebase/冲突/测试/合并信息（含提交或 PR 链接）。

## 检查点与验证记录（任务 4/10）

- 检查点任务的目标是“在继续开发前，确认当前累计改动不会破坏构建/测试”。
- 最小验证集：
  - `./gradlew test --no-daemon`（记录末尾 `BUILD SUCCESSFUL` 与关键警告/失败栈）
  - 如本次改动涉及资源打包/运行期行为：补充 `./gradlew build` 或 `./gradlew runClient`
- 备注建议模板（用于 PR 描述或合并说明）：
  - rebase：是否成功；如失败说明原因
  - 冲突：文件/原因/如何以 `origin/main` 为准保留最新改动
  - 测试：执行的命令 + 关键输出（如 `BUILD SUCCESSFUL`）
  - 合并：合并到 `main` 的确认（提交或 PR 链接）

## 验证记录（任务 5/10）

- `./gradlew test --no-daemon`：`BUILD SUCCESSFUL`（含 ForgeGradle 2.3.4 不受支持提示与 `stable_39` 映射警告）
- `./gradlew build --no-daemon`：`BUILD SUCCESSFUL`（含同上警告）

## 验证记录（任务 6）

- `./gradlew test --no-daemon`：`BUILD SUCCESSFUL`（同上警告）
- `./gradlew build --no-daemon`：`BUILD SUCCESSFUL`（同上警告）

## 验证记录（任务 7）

- `./gradlew test --no-daemon`：`BUILD SUCCESSFUL`（含 `stable_39` 映射提示、ForgeGradle 2.3.4 不受支持警告与 Gradle 5 兼容性废弃特性提示）
- `./gradlew build --no-daemon`：`BUILD SUCCESSFUL`（同上警告）

## 验证记录（任务 8）

- `./gradlew test --no-daemon`：`BUILD SUCCESSFUL`（含 `stable_39` 映射提示、ForgeGradle 2.3.4 不受支持警告与 Gradle 5 兼容性废弃特性提示）
- `./gradlew build --no-daemon`：`BUILD SUCCESSFUL`（同上警告）

## 验证记录（任务 9）

- `./gradlew test --no-daemon`：`BUILD SUCCESSFUL`（含 `stable_39` 映射提示、ForgeGradle 2.3.4 不受支持警告与 Gradle 5 兼容性废弃特性提示）
- `./gradlew build --no-daemon`：`BUILD SUCCESSFUL`（同上警告）

## 验证记录（任务 10）

- `./gradlew test --no-daemon --console=plain`：`BUILD SUCCESSFUL in 25s`（含 `stable_39` 映射提示、ForgeGradle 2.3.4 不受支持警告与 Gradle 5 兼容性废弃特性提示）
- `./gradlew build --no-daemon --console=plain`：`BUILD SUCCESSFUL in 19s`（同上警告）

## 验证记录（任务 11）

- `./gradlew test --no-daemon --console=plain`：`BUILD SUCCESSFUL in 21s`（含 `stable_39` 映射提示、ForgeGradle 2.3.4 不受支持警告与 Gradle 5 兼容性废弃特性提示）
- `./gradlew build --no-daemon --console=plain`：`BUILD SUCCESSFUL in 25s`（同上警告）

## 验证记录（任务 12）

- `./gradlew test --no-daemon --console=plain`：`BUILD SUCCESSFUL in 29s`（含 `stable_39` 映射提示、ForgeGradle 2.3.4 不受支持警告与 Gradle 5 兼容性废弃特性提示）
- `./gradlew build --no-daemon --console=plain`：`BUILD SUCCESSFUL in 19s`（同上警告）

## 验证记录（任务 13）

- `./gradlew test --no-daemon --console=plain`：`BUILD SUCCESSFUL in 24s`（含 `stable_39` 映射提示、ForgeGradle 2.3.4 不受支持警告与 Gradle 5 兼容性废弃特性提示）
- `./gradlew build --no-daemon --console=plain`：`BUILD SUCCESSFUL in 19s`（同上警告）

## 验证记录（任务 14）

- `./gradlew test --no-daemon --console=plain`：`BUILD SUCCESSFUL in 27s`（含 `stable_39` 映射提示、ForgeGradle 2.3.4 不受支持警告与 Gradle 5 兼容性废弃特性提示；编译期出现 deprecated API 提示）
- `./gradlew build --no-daemon --console=plain`：`BUILD SUCCESSFUL in 19s`（同上警告）
