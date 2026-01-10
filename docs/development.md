# 开发流程

## 环境要求

- JDK 8（运行 1.12.2 推荐）；JDK 11 可用于构建（Gradle 4.10.3 兼容）
- Git
- 网络可用（首次构建需要下载 Forge/Minecraft 依赖）

## 常用命令

- 构建：`./gradlew build`
- 运行开发客户端：`./gradlew runClient`
- 运行测试：`./gradlew test`
- 清理：`./gradlew clean`

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
