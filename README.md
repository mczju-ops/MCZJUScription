# MCZJUScription

基于 [MCZJUGameCore](https://github.com/mczju-ops/MCZJUGameCore) 的 Paper **1.21.7** 插件：**邪恶冥刻 · 方块造物**（《邪恶冥刻》风格卡牌对局，单人 PvE 等）。

## 依赖

- 服务端：Paper 1.21.7
- 插件：[MCZJUGameCore](https://github.com/mczju-ops/MCZJUGameCore)（`pom.xml` 中 `mczju.gamecore.version` 与 GameCore 的 release/tag 对齐；**房间池 API 需本地最新 GameCore**）

## 构建

```bash
cd ../MCZJUGameCore && mvn install -DskipTests
cd ../MCZJUScription/MCZJUScription && mvn clean package -DskipTests
```

产物：`target/MCZJUScription-*.jar`，放入服务端 `plugins/`。

## 文档

- 仓库根目录 `api-overview.md` — MCZJUGameCore API 全文
- `.cursor/skills/mczju-gamecore/` — Cursor Agent 用 MGC 开发技能
- `docs/example-inscription_hub-main.json` — 大厅房间 `main` 配置示例
- `docs/mob-cards-catalog.md` — 生物卡牌定稿表（攻血/印记）
- `docs/mob-sigil-implementation-plan.md` — MC 印记体系分析与执行方案
- `docs/mob-cards.yml.example` — 自定义卡 YAML 示例

## 房间规划（`inscription`）

| 房间名 | 用途 | 数量 |
|--------|------|------|
| `main` | **仅等待大厅**（出生、座位、排行榜） | 1 |
| `play10`～`play19` | 单人 PvE 对局场地 | 10 |
| `play20`～`play24` | 双人 PvP 对局场地 | 5 |

- `/mgc` 或 `/isc hub`：只占用 **`main`**
- 从大厅座位开局：**不修改 GameCore** 时由插件占用 `play10–24` 并在当前实例上对局（聊天会显示 `对局场地：playXX`）
- 座位 / `/isc start` 开局：从 **`play10–19`**（单人）或 **`play20–24`**（双人）里选第一个 `READY` 房间

### 快速创建（服内）

```text
/mgcop room create inscription main
/mgcop room create inscription play10
…（play11～play24 同理，或复制 JSON 后改 roomName）
```

启动插件后控制台会检查是否缺房间 JSON。

## 玩家流程

1. `/mgc` 选 **邪恶冥刻 · 方块造物** → 进入 `main` 大厅
2. 讲台构牌、赐福/诅咒、选座或 `/isc start <solo|duel> <shop|free>`
3. 通关榜 ID：`inscription_clear`，展示实体 `hub_main`（绑定 `main` 房间的 `leaderboardAt`）

**依赖**：GameCore **≥ 1.0.2**（含排行榜修复 + `joinGame`/`createGame` 房间池参数）。

## 开发协作

```bash
git clone https://github.com/mczju-ops/MCZJUScription.git
cd MCZJUScription
mvn clean package
```
