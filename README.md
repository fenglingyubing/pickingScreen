# pickingScreen

我的世界 1.12.2 Forge 模组：拾取筛（Pickup Filter）。

## 游戏内使用

- 默认快捷键：
  - `O`：切换模式（关闭 / 拾取匹配掉落物 / 销毁匹配掉落物）
  - `P`：打开配置界面（添加/删除规则并同步）
  - `K`：清除附近掉落物（以玩家为中心 2 区块半径）
- 背包（`E`）右上角 “筛” 按钮：打开“物品匹配”界面，从背包快速把物品加入规则。
- 背包界面鼠标悬停物品按 `A`：直接把该物品加入拾取筛规则并同步。
- 规则与模式的详细说明见 `docs/usage.md`。

## 快速开始

- 构建：`./gradlew build`
- 打包发布 Jar：`./gradlew reobfJar`（产物在 `build/libs/`）
- 运行开发客户端：`./gradlew runClient`

更多开发说明见 `docs/development.md`。
