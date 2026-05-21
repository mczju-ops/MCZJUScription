# 生物卡牌总表（定稿 · 数据驱动）

> **版本**：v1.0（玩家定稿数值）  
> **默认费用**：全部 `costType: BLOOD`，`cost: 1`（1 腐肉）；进游戏后用设计器 / 改 `cards.yml` 微调，**勿写死在 Java**。  
> **加载方式**：`CardCatalog` → `plugins/MCZJUScription/cards.yml`（与内置 `CardId` 无关的 **自定义 id**）

---

## 1. 自定义卡 `cards.yml` 字段说明

与 `CardCatalog.deserialize` 一致：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | 字符串 | 全局唯一，建议 `mob_<英文名>` |
| `displayName` | 字符串 | 游戏内显示名（中文） |
| `entityType` | `EntityType` | Bukkit 实体，决定皮肤/图标 |
| `mountEntityType` | 可选 | 骑乘表现（一般可不填） |
| `power` | int | 攻击 |
| `health` | int | 生命 |
| `costType` | `BLOOD` / `BONES` / `FREE` | 费用类型；本表默认 `BLOOD` |
| `cost` | int | 费用数值；本表默认 `1` |
| `sacrificeValue` | int | 作为祭品时的腐肉价值；默认 `1`，带【优质祭品】【恶魔】等再改 |
| `sigils` | `SigilId[]` | 印记枚举列表；**新印记需先扩展 `SigilId` + Handler** |
| `sigilNotes` | 仅文档 | 人类可读效果，**不写入 yml**（见下表「印记说明」列） |
| `evolvesTo` | 仅文档 | 【稚雏】【折磨】进化目标，实现时做在 Handler 或卡元数据 |
| `builtin` | bool | 自定义卡填 `false` |

**示例条目**（复制到 `cards.yml` 的 `cards:` 节点下）：

```yaml
mob_zombie_horse:
  displayName: "僵尸马"
  entityType: ZOMBIE_HORSE
  power: 1
  health: 2
  costType: BLOOD
  cost: 1
  sacrificeValue: 1
  sigils: []   # 待 SigilId 扩展后填 RIDING 等，见 §3
  builtin: false
```

完整机器可读草案见：[mob-cards.yml.example](mob-cards.yml.example)。

---

## 2. 卡牌总表（按生物名排序）

默认：**1 腐肉** · `sacrificeValue: 1`（特殊祭品见印记列）

| id | 显示名 | entityType | 攻 | 血 | 印记（中文） | 印记说明摘要 |
|----|--------|------------|----|----|--------------|--------------|
| `mob_allay` | 悦灵 | ALLAY | 1 | 2 | 高跳、守卫者 | 拦截空袭；空位将遭攻击时移过去挡刀 |
| `mob_armadillo` | 犰狳 | ARMADILLO | 0 | 2 | 坚壳 | 单次最多受到 1 点伤害 |
| `mob_axolotl` | 美西螈 | AXOLOTL | 1 | 1 | 水袭、兵分两路、鱼饵 | 潜水；攻击对面左右两列；献祭得 1 鱼干 |
| `mob_bat` | 蝙蝠 | BAT | 1 | 1 | 空袭 | 越过对方造物直击 |
| `mob_bee` | 蜜蜂 | BEE | 1 | 1 | 空袭 | 同上 |
| `mob_blaze` | 烈焰人 | BLAZE | 1 | 1 | 空袭、炽热 | 空袭；「炽热」系联动（见 §3） |
| `mob_bogged` | 沼骸 | BOGGED | 1 | 1 | 射线、剧毒 | 射线攻击；造成伤害后目标立刻死亡 |
| `mob_breeze` | 旋风人 | BREEZE | 1 | 2 | 蓄风 | 攻击后随机移动到其他空位 |
| `mob_camel` | 骆驼 | CAMEL | 1 | 1 | 储水 | 每在场一回合，本卡生命 +1 |
| `mob_cat` | 猫 | CAT | 1 | 1 | 生生不息 | 可无限次献祭 |
| `mob_cave_spider` | 洞穴蜘蛛 | CAVE_SPIDER | 1 | 1 | 剧毒 | 受到该造物伤害后立刻死亡 |
| `mob_chicken` | 鸡 | CHICKEN | 0 | 1 | 繁殖 | 上场一回合后，复制本卡入手 |
| `mob_cod` | 鳕鱼 | COD | 0 | 1 | 鱼饵 | 献祭时获得 1 张鱼干 |
| `mob_cow` | 牛 | COW | 0 | 3 | — | 纯面板 |
| `mob_creeper` | 苦力怕 | CREEPER | 0 | 1 | 自爆 | 死亡时对相邻两格与面前格造成 10 点伤害 |
| `mob_dolphin` | 海豚 | DOLPHIN | 2 | 1 | 水袭、鱼饵 | 潜水；献祭得鱼干 |
| `mob_donkey` | 驴 | DONKEY | 0 | 2 | 骑乘、游荡 | 相邻 +1 力；游荡（见 §3） |
| `mob_drowned` | 溺尸 | DROWNED | 2 | 1 | 水袭 | 对手回合潜水、可被直击 |
| `mob_elder_guardian` | 远古守卫者 | ELDER_GUARDIAN | 1 | 1 | 震慑 | 敌方攻击该卡时会放弃此次攻击 |
| `mob_enderman` | 末影人 | ENDERMAN | 1 | 2 | 穿梭 | 受伤后随机移到其他空位 |
| `mob_endermite` | 末影螨 | ENDERMIT | 1 | 1 | 意外 | 获得该卡时若场上有空位，免费召唤到该格 |
| `mob_evoker` | 唤魔者 | EVOKER | 1 | 1 | 唤魔 | 出牌时相邻两格空位各生成 1/1 恼鬼 |
| `mob_fox` | 狐狸 | FOX | 1 | 3 | 盗物 | 攻击时获得 1 骨币 |
| `mob_frog` | 青蛙 | FROG | 0 | 2 | 高跳 | 拦截面前空袭，必须以其为攻击目标 |
| `mob_ghast` | 恶魂 | GHAST | 2 | 1 | 空袭 | 空袭直击 |
| `mob_glow_squid` | 发光鱿鱼 | GLOW_SQUID | 1 | 2 | 墨水、鱼饵 | 墨水；献祭得鱼干 |
| `mob_goat` | 山羊 | GOAT | 0 | 1 | 恶魔 | 献祭时获得 3 骨币（而非腐肉） |
| `mob_guardian` | 守卫者 | GUARDIAN | 1 | 1 | 水袭、射线 | 潜水；可指定攻击列 |
| `mob_hoglin` | 疣猪兽 | HOGLIN | 1 | 2 | 追击、折磨 | 攻击两次；一回合后变为僵尸疣猪兽 |
| `mob_horse` | 马 | HORSE | 0 | 2 | 骑乘 | 相邻造物 +1 力量 |
| `mob_husk` | 尸壳 | HUSK | 1 | 1 | 炽热 | 炽热系联动 |
| `mob_iron_golem` | 铁傀儡 | IRON_GOLEM | 1 | 3 | 守卫者 | 空位将遭攻击时移动到该格承担伤害 |
| `mob_llama` | 羊驼 | LLAMA | 2 | 1 | 射线 | 射线攻击 |
| `mob_magma_cube` | 大岩浆怪 | MAGMA_CUBE | 2 | 1 | 分裂 | 死亡时在相邻两格空位各召 1/1 小岩浆怪 |
| `mob_magma_cube_small` | 小岩浆怪 | MAGMA_CUBE | 1 | 1 | — | 分裂 token |
| `mob_mooshroom` | 哞菇 | MUSHROOM_COW | 0 | 2 | 发酵 | 在场一回合获得 2 腐肉 |
| `mob_mule` | 骡 | MULE | 1 | 3 | 游荡、蛮力 | 移动时推挤相邻造物；游荡 |
| `mob_ocelot` | 豹猫 | OCELOT | 1 | 1 | 哈气 | 若对面有【自爆】印记则使其失效 |
| `mob_panda` | 熊猫 | PANDA | 1 | 2 | 臭臭 | 相邻敌方 -1 力量（标准臭臭） |
| `mob_parrot` | 鹦鹉 | PARROT | 1 | 2 | 空袭、穿梭 | 空袭 + 受伤换位 |
| `mob_phantom` | 幻翼 | PHANTOM | 2 | 1 | 空袭 | 越过对方造物直击 |
| `mob_pig` | 猪 | PIG | 0 | 2 | 臭臭 | 对面造物 -1 力量 |
| `mob_piglin` | 猪灵 | PIGLIN | 1 | 1 | 折磨 | 在场一回合后变为僵尸猪灵 |
| `mob_piglin_brute` | 猪灵蛮兵 | PIGLIN_BRUTE | 5 | 1 | — | 纯面板 |
| `mob_pillager` | 掠夺者 | PILLAGER | 2 | 1 | 射线 | 射线攻击 |
| `mob_polar_bear` | 北极熊 | POLAR_BEAR | 1 | 3 | 全向攻击 | 对对面每个位置各攻击一次 |
| `mob_pufferfish` | 河豚 | PUFFERFISH | 0 | 2 | 尖刺铠甲 | 被攻击时反击 1 点伤害 |
| `mob_rabbit` | 兔子 | RABBIT | 0 | 1 | — | 纯面板 |
| `mob_ravager` | 劫掠兽 | RAVAGER | 2 | 2 | 兵分三路 | 攻击正对面左、中、右三列各一次 |
| `mob_salmon` | 鲑鱼 | SALMON | 1 | 1 | 水袭、鱼饵 | 潜水 + 献祭鱼干 |
| `mob_sheep` | 羊 | SHEEP | 0 | 1 | 优质祭品 | 献祭获得 3 腐肉 |
| `mob_silverfish` | 蠹虫 | SILVERFISH | 1 | 1 | 不死之虫 | 死亡时将本卡复制品加入手牌 |
| `mob_shulker` | 潜影贝 | SHULKER | 1 | 2 | 护盾 | 上场后免疫第一次受到的伤害 |
| `mob_skeleton` | 骷髅 | SKELETON | 1 | 1 | 射线 | 射线攻击 |
| `mob_skeleton_horse` | 骷髅马 | SKELETON_HORSE | 0 | 2 | 骨皇 | 死亡获得 4 骨币 |
| `mob_slime` | 大史莱姆 | SLIME | 1 | 2 | 分裂 | 死亡时相邻两格各召 0/1 小史莱姆 |
| `mob_slime_small` | 小史莱姆 | SLIME | 0 | 1 | — | 分裂 token |
| `mob_sniffer` | 嗅探兽 | SNIFFER | 1 | 2 | 嗅探 | 上场一回合后，从对面随机印记替换【嗅探】 |
| `mob_snow_golem` | 雪傀儡 | SNOW_GOLEM | 1 | 2 | 嘲讽 | 在场时，场上所有造物 +1 力量 |
| `mob_spider` | 蜘蛛 | SPIDER | 1 | 2 | 蛛网 | 被攻击者 -1 力量 |
| `mob_squid` | 鱿鱼 | SQUID | 0 | 3 | 墨水、鱼饵 | 攻击者下回合无法攻击；鱼饵 |
| `mob_stray` | 流浪者 | STRAY | 1 | 1 | 射线、迟缓 | 射线；受伤方 -1 力量 |
| `mob_strider` | 炽足兽 | STRIDER | 1 | 2 | 游荡、炽热 | 游荡 + 炽热 |
| `mob_tadpole` | 蝌蚪 | TADPOLE | 0 | 1 | 稚雏 | 一回合后变为青蛙 |
| `mob_trader_llama` | 行商羊驼 | TRADER_LLAMA | 2 | 1 | 射线、游荡 | 射线；攻击后随机移到左右空位 |
| `mob_tropical_fish` | 热带鱼 | TROPICAL_FISH | 1 | 1 | 鱼饵、炽热 | 鱼饵 + 炽热 |
| `mob_turtle` | 海龟 | TURTLE | 0 | 3 | 坚壳 | 单次最多 1 点伤害 |
| `mob_vex` | 恼鬼 | VEX | 1 | 1 | — | 唤魔者召唤物 |
| `mob_villager` | 村民 | VILLAGER | 0 | 2 | 交易 | 在场一回合获得 1 骨币 |
| `mob_vindicator` | 卫道士 | VINDICATOR | 5 | 1 | — | 纯面板 |
| `mob_wandering` | — | — | — | — | — | （无此卡） |
| `mob_warden` | 监守者 | WARDEN | 3 | 5 | 声呐 | 可攻击带【水袭】的单位 |
| `mob_witch` | 女巫 | WITCH | 1 | 1 | 剧毒 | 造成伤害后目标立刻死亡 |
| `mob_wither` | 凋灵 | WITHER | 3 | 3 | 全向攻击 | 对对面每列攻击 |
| `mob_wither_skeleton` | 凋灵骷髅 | WITHER_SKELETON | 3 | 4 | 骨皇 | 死亡获得 4 骨币 |
| `mob_wolf` | 狼 | WOLF | 2 | 2 | — | 纯面板；狼崽进化目标 |
| `mob_wolf_cub` | 狼崽 | WOLF | 1 | 1 | 稚雏 | 一回合后变为狼 |
| `mob_zoglin` | 僵尸疣猪兽 | ZOGLIN | 1 | 3 | 追击 | 攻击连续两次 |
| `mob_zombie` | 僵尸 | ZOMBIE | 1 | 2 | — | 纯面板 |
| `mob_zombie_horse` | 僵尸马 | ZOMBIE_HORSE | 1 | 2 | 骑乘 | 相邻 +1 力量 |
| `mob_zombie_villager` | 僵尸村民 | ZOMBIE_VILLAGER | 1 | 2 | 交易 | 在场一回合触发交易（同村民，效果待定） |
| `mob_zombified_piglin` | 僵尸猪灵 | ZOMBIFIED_PIGLIN | 1 | 2 | — | 猪灵【折磨】产物 |

**Token / 衍生（建议单独 id，默认 0 费或 FREE）**

| id | 显示名 | entityType | 攻 | 血 | 备注 |
|----|--------|------------|----|----|------|
| `mob_fish_dried` | 鱼干 | COD | 0 | 0 | 【鱼饵】献祭产出；费用 `FREE` |
| `mob_rabbit_token` | 兔子 | RABBIT | 0 | 1 | 若仍需【兔穴】token |

**未单独成卡（你未列出）**：犰狳已在表；**骆驼尸壳、焦骸** 若要做异色卡可复用 `HUSK` / `BOGGED` 皮肤另加 id。

**统计**：主卡 **83** + 幻翼待定 + token **2** ≈ **85** 条。

---

## 3. 印记 → `SigilId` 对照（实现队列）

| 中文 | 建议枚举名 | 迁移自现版？ | 效果（你的定稿文案） |
|------|------------|--------------|----------------------|
| 骑乘 | `RIDING` | 新 | 相邻造物 +1 力量 |
| 游荡 | `WANDER` | 新 | 攻击后/回合末向随机空位移（羊驼、驴、骡、炽足兽等细则在 Handler 分派） |
| 炽热 | `SCORCH` | 新 | 派系标签；对「炽热」友方或受伤加成（细则实现时定） |
| 鱼饵 | `FISH_BAIT` | 新 | 献祭：获得 1 张鱼干 |
| 兵分三路 | `TRI_STRIKE` | **是** | 攻击正对面左、中、右 |
| 兵分两路 | `SPLIT_STRIKE` | **是** | 攻击正对面左、右 |
| 高跳 | `HIGH_JUMP` | **是** | 拦截空袭 |
| 守卫者 | `GUARD_DOG` | **是** | 铁傀儡/悦灵：挡刀位移 |
| 意外 | `SURPRISE_ENTRY` | 新 | 入手时免费上空位 |
| 臭臭 | `STINKY` | **是** | 熊猫：相邻 -1 力；猪：对面 -1 力（可拆 `STINKY_FAR`） |
| 发酵 | `FERMENT` | 新 | 在场一回合 +2 腐肉 |
| 交易 | `TRADE` | 新 | 在场一回合 +1 骨币 |
| 折磨 | `AFFLICTION` | 新 | 一回合后变为指定卡（猪灵→僵尸猪灵，疣猪兽→僵尸疣猪兽） |
| 射线 | `BEAM` | 新 | 远程指定列攻击 |
| 剧毒 | `VENOM_KILL` | 新 | 伤害后秒杀 |
| 墨水 | `INK` | 新 | 攻击者下回合不能攻击 |
| 全向攻击 | `ALL_STRIKE` | **是** | 攻击对面每一列 |
| 震慑 | `INTIMIDATE` | 新 | 敌方放弃攻击该目标 |
| 生生不息 | `ETERNAL_LIFE` | **是** | 无限献祭 |
| 盗物 | `STEAL_BONE` | 新 | 攻击 +1 骨币 |
| 稚雏 | `FLEDGLING` | **是** | 一回合后进化为指定 id |
| 追击 | `DOUBLE_STRIKE` | 新 | 攻击两次 |
| 储水 | `WATER_STORE` | 新 | 每回合 +1 生命 |
| 嘲讽 | `TAUNT_AURA` | 新 | 全场 +1 力量 |
| 护盾 | `FIRST_SHIELD` | 新 | 免疫首次伤害 |
| 自爆 | `SELF_DESTRUCT` | 新 | 死亡 10 伤（邻格+面前） |
| 蛛网 | `WEB_WEAK` | 新 | 目标 -1 力量 |
| 蓄风 | `GUST_SHIFT` | 新 | 攻击后随机换位 |
| 分裂 | `SPLIT_SPAWN` | 新 | 死亡召 2 个小体 |
| 声呐 | `SONAR` | 新 | 可攻击【水袭】单位 |
| 恶魔 | `DEMON_OFFER` | 新 | 献祭得 3 骨币 |
| 优质祭品 | `QUALITY_SACRIFICE` | **是** | 献祭 3 腐肉 |
| 繁殖 | `BREEDING` | **是** | 一回合后复制入手 |
| 坚壳 | `HARD_SHELL` | 新 | 单次伤害上限 1 |
| 尖刺铠甲 | `SPIKY_ARMOR` | **是** | 反击 1 |
| 骨皇 | `BONE_ROYALTY` | **是** | 死亡 +4 骨 |
| 不死之虫 | `COPY_ON_DEATH` | **是** | 死亡复制入手 |
| 水袭 | `WATER_STRIKE` | **是** | 潜水规则 |
| 空袭 | `AIR_STRIKE` | **是** | 直击 |
| 唤魔 | `EVOKE_VEX` | 新 | 出牌召恼鬼 |
| 穿梭 | `ENDER_SHIFT` | 新 | 受伤换位 |
| 嗅探 | `SNIFF_STEAL` | 新 | 偷对面印记 |
| 哈气 | `HISS` | 新 | 使对面【自爆】失效 |
| 蛮力 | `RUSH_PUSH` | **是** | 推挤相邻格 |

---

## 4. 进化 / 召唤链（实现时用 metadata，不写死 Java）

| 来源卡 | 条件 | 结果 |
|--------|------|------|
| 蝌蚪 | 【稚雏】一回合 | `mob_frog` |
| 狼崽 | 【稚雏】一回合 | `mob_wolf` |
| 猪灵 | 【折磨】一回合 | `mob_zombified_piglin` |
| 疣猪兽 | 【折磨】一回合 | `mob_zoglin` |
| 唤魔者 | 【唤魔】出牌 | `mob_vex` ×2（邻格） |
| 大岩浆怪 | 【分裂】死亡 | `mob_magma_cube_small` ×2 |
| 大史莱姆 | 【分裂】死亡 | `mob_slime_small` ×2 |
| 鳕鱼/鲑鱼等 | 【鱼饵】献祭 | `mob_fish_dried` ×1 |

---

## 5. 与旧内置卡的关系

| 旧 `CardId` | 新 id 建议 | 说明 |
|-------------|------------|------|
| `RABBIT` | `mob_rabbit` | 可再加【兔穴】或保持纯面板 |
| `BEE` | `mob_bee` | 仅空袭（你未要内心之蜂） |
| `WOLF_CUB` / `WOLF` | `mob_wolf_cub` / `mob_wolf` | 稚雏链保留 |
| `ANT` | — | 可由蠹虫【不死之虫】+ 唤魔线替代 |
| Token 卡 | `mob_*_small` 等 | 继续 `FREE`，不占 1 腐肉 |

内置 `CardRegistry` **可保留** 作兼容；正式玩法以 **`cards.yml` 覆盖/追加** 为准。

---

## 6. 进游戏调参清单

- [ ] 幻翼：补攻/血/印记  
- [ ] 猪【臭臭】与熊猫【臭臭】是否拆成两个 `SigilId`  
- [ ] 僵尸村民【交易】是否与村民同效果  
- [ ] 鱼干 `mob_fish_dried` 的攻血与是否可上场  
- [ ] Boss 卡（卫道士 5/1、猪灵蛮兵 5/1、监守者 3/5）费用是否仍为 1 腐肉  
- [ ] 扩展 `SigilId` 后，把 `mob-cards.yml.example` 里 `sigils: []` 填齐  

---

## 相关文档

- [mob-sigil-implementation-plan.md](mob-sigil-implementation-plan.md) — 新印记体系分析与分阶段执行方案（**开工前必读**）  
