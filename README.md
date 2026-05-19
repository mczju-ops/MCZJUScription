# MCZJUScription

基于 [MCZJUGameCore](https://github.com/mczju-ops/MCZJUGameCore) 的 Paper **1.21.7** 插件：《邪恶冥刻》风格卡牌对局（单人 PvE 等）。

## 依赖

- 服务端：Paper 1.21.7
- 插件：[MCZJUGameCore](https://github.com/mczju-ops/MCZJUGameCore)（`pom.xml` 中 `mczju.gamecore.version` 与 GameCore 的 release/tag 对齐）

## 构建

```bash
mvn clean package
```

产物：`target/MCZJUScription-*.jar`，放入服务端 `plugins/`。

## 文档

- `docs/chapter1-sigils-plan.md` — 第一章印记实现规划

## 开发协作

```bash
git clone https://github.com/mczju-ops/MCZJUScription.git
cd MCZJUScription
mvn clean package
```
