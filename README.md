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
- `docs/inscription-lobby-plan.md` — 等待大厅（OpenSession + 分 Phase 实施清单）
- `docs/example-inscription_hub-main.json` — 大厅房间配置示例

### 大厅快速开始

1. `/mgcop room create inscription_hub main` 并编辑坐标，或复制示例 JSON 到 `plugins/MCZJUGameCore/rooms/inscription_hub/main.json`
2. 玩家 `/mgc` 选 **邪恶冥刻**，或 `/isc hub`
3. 讲台构牌、区域交互、单人/双人座位选模式开局

## 开发协作

```bash
git clone https://github.com/mczju-ops/MCZJUScription.git
cd MCZJUScription
mvn clean package
```
