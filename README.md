# pickingScreen

我的世界 1.12.2 Forge 模组：拾取筛（Pickup Filter）。

## 游戏内使用

- 默认快捷键：
  - `O`：切换模式（关闭 / 拾取匹配掉落物 / 销毁匹配掉落物）
  - `P`：打开配置界面（添加/删除规则并同步）
  - `K`：清除附近掉落物（以玩家为中心 2 区块半径）
- 规则与模式的详细说明见 `docs/usage.md`。

## 快速开始

- 构建：`./gradlew build`
- 打包发布 Jar：`./gradlew reobfJar`（产物在 `build/libs/`）
- 运行开发客户端：`./gradlew runClient`

更多开发说明见 `docs/development.md`。
