# 开发流程

## 环境要求

- JDK 8（Forge 1.12.2 兼容）
- Git
- 网络可用（首次构建需要下载 Forge/Minecraft 依赖）

## 常用命令

- 构建：`./gradlew build`
- 运行开发客户端：`./gradlew runClient`
- 运行测试：`./gradlew test`
- 清理：`./gradlew clean`

## 目录结构

- `src/main/java`：模组代码
  - `com.fenglingyubing.pickupfilter`：主入口与跨模块协调
  - `config/`：配置与模式状态
  - `event/`：服务端/通用事件处理（不引用客户端类）
  - `client/`：客户端相关（按键、GUI、反馈）
- `src/main/resources`：资源与 `mcmod.info`
- `docs/`：需求/设计/任务与开发说明

## 开发约定

- 客户端类只放在 `client/` 下，并通过 `ClientProxy` 注册，避免专用服务器类加载崩溃。
- 业务逻辑优先放在可测试的纯 Java 类中（后续任务会补齐测试框架与属性测试）。

