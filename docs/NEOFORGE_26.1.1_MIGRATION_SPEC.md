# Arclight — Minecraft 26.1.2 向け修正仕様書（NeoForge / Fabric / Forge）

本書は **Arclight** を **Minecraft 1.21.1** から **Minecraft 26.1.2**（Spigot / NeoForge / Fabric / Forge）へ移植するための修正仕様である。  
**2026-06-06 より実装フェーズ開始。** ID 単位で進め、完了時に本書の「状態」列を更新する。

---

## 0. 実装進捗サマリ（2026-06-13 更新）

| フェーズ | 状態 | 備考 |
|----------|------|------|
| Phase 0 前提確認 | **完了** | Spigot reversion **4628** / MC **26.1.2** BuildTools メタ確認済み |
| Phase 1 ビルド (Tier 1) | **ほぼ完了** | NeoForge/Fabric Loom 設定成功。**B-10 collect 成功**（2026-06-13 再検証）。Forge は Loom `McpExecutor` NPE（`McpExecutor.java:183`）でコンパイル不可 |
| Phase 2 common コンパイル | **完了** | **2026-06-12**: `compileJava` エラーゼロ（82→0）。NBT/ValueInput・Recipe Template・DelegateWorldInfo 等。**2026-06-13**: 再ビルド UP-TO-DATE 確認 |
| Phase 3 neoforge 層 | **完了** | **2026-06-12**: `:arclight-neoforge:compileJava` エラーゼロ（27→0）。NF ネットワーク・Transfer API・イベント API |
| Phase 4 bootstrap | **完了** | **BS-07** FML 11 `ModBootstrap` 再設計。**BS-08** JPMS 二重 export 解消（2026-06-13） |
| Phase 3.5 fabric 層 | **完了** | **2026-06-12**: `:arclight-fabric:compileJava` エラーゼロ。`:bootstrap:fabricJar` 成功 |
| Phase 5 ランタイム | **部分完了** | **R-01 完了**（2026-06-13）: ワールド生成・初回 tick 成功。**R-01b** `ArclightServer.isPrimaryThread` NPE 修正。**R-02〜R-07** はクライアント参加・プラグイン検証待ち |

### 0.1 26.1.2 で判明した重要変更（設計書追記）

| 変更 | 旧 (1.21.1) | 新 (26.1.2 Spigot 実測) |
|------|-------------|-------------------------|
| CraftBukkit パッケージ | `org.bukkit.craftbukkit.v.*`（リマップ後） | **`org.bukkit.craftbukkit.*`（リビジョンサブパッケージ廃止）** |
| Access Widener 名前空間 | `named` | **`official`** |
| `ResourceLocation` | `net.minecraft.resources.ResourceLocation` | **`net.minecraft.resources.Identifier`** |
| `TicketType` | ジェネリック class | **record（非ジェネリック）+ `TicketStorage` へ移管** |
| `Ticket` / Spigot 拡張 | `Ticket<>(type, level, key)` | **`Ticket(type, level)` + Mixin `Ticket.of(type, level, key)`** |
| `GameRules` | `net.minecraft.world.level.GameRules` + Key/Value | **`net.minecraft.world.level.gamerules.GameRules` + `GameRule<T>`** |
| `MobSpawnType` | `net.minecraft.world.entity.MobSpawnType` | **`EntitySpawnReason`**（要 grep 置換） |
| 動物 Entity | `animal.Bee`, `animal.horse.*` 等 | **`animal.bee.Bee`, `animal.equine.*`, `animal.golem.IronGolem`, `animal.fox.Fox` 等** |
| `RelativeMovement` | `net.minecraft.world.entity.RelativeMovement` | **`net.minecraft.world.entity.Relative`** |
| `DimensionTransition` | portal 次元遷移 | **`TeleportTransition`** + `PostTeleportTransition` |
| `ChunkProgressListener` | チャンク準備進捗 | **`LevelLoadListener`**（`Stage.LOAD_INITIAL_CHUNKS` 等） |
| `prepareLevels` | `getTickingGenerated` ループ | **`ChunkLoadCounter` + `waitUntilNextTick`** |
| `getSharedSpawnPos` / `setDefaultSpawnPos` | ServerLevel 直アクセス | **`LevelData.RespawnData`**（`getRespawnData` / `setRespawnData`） |
| `getDayTime` / `setDayTime` | Level / ServerLevel | **`ClockManager` + `WorldClock`**（`ArclightLevelHelper`） |
| `Entity.hasImpulse` | 同期フラグ | **`Entity.needsSync`** |
| `ClientboundTeleportEntityPacket(Entity)` | 単一引数 ctor | **`teleport(id, PositionMoveRotation, Set<Relative>, onGround)`** |
| `Style` private ctor | 直接 new | **`withStrikethrough` / `withUnderlined` / `withObfuscated`** |
| `SuggestionProviders.safelySwap` | サジェスト変換 | **`SuggestionProviders.cast`** |
| `ClientboundCommandsPacket` | 単一引数 ctor | **`NodeInspector` 付き ctor** |
| `Entity.teleportTo` | 7 引数 | **+ `boolean`（第 8 引数）** |
| `Level.blockUpdated` | BlockPos + Block | **`neighborChanged(pos, block, Orientation)`** |
| `ChunkAccess.setUnsaved` | boolean 引数 | **`markUnsaved()`** |
| `TamableAnimal.setOwnerUUID` | UUID | **`setOwnerReference(EntityReference.of(uuid))`** |
| `PermissionSet` | `hasPermission(int)` | **`permissions().hasPermission(PermissionSet)`** |
| `PlayerList.isOp` | `GameProfile` | **`NameAndId`** |
| `ForcedChunksSavedData` | SavedData 強制チャンク | **廃止** → `ServerLevel.getForceLoadedChunks()` + `setChunkForced` |
| `DimensionDataStorage` | 次元 SavedData | **`SavedDataStorage`** |
| `EndDragonFight` | エンドドラゴン | **`EnderDragonFight`** |
| `PlayerRespawnLogic` | スポーン位置探索 | **`PlayerSpawnFinder.getOverworldRespawnPos`**（protected → `@Invoker`） |
| `Util` / `BlockUtil` | `net.minecraft.*` | **`net.minecraft.util.*`** |
| `Equipable` | アイテム装備 API | **`net.minecraft.world.item.equipment.Equippable`** |
| `ClientboundSetCarriedItemPacket` | 選択スロット同期 | **`ClientboundSetHeldSlotPacket`** |
| `ClientboundRecipePacket` | レシピブック | **`ClientboundRecipeBookAddPacket`** 等に分割 |
| `InteractionResultHolder` | Item#use 戻り値 | **`InteractionResult`**（interface 化） |
| `ChorusFruitItem` / `MilkBucketItem` | 専用 Item クラス | **廃止** → `consume_effects.TeleportRandomlyConsumeEffect` / `ClearAllStatusEffectsConsumeEffect` |
| `ThrownPotion` | 単一クラス | **`ThrownSplashPotion` + `ThrownLingeringPotion`**（`AbstractThrownPotion` 継承） |
| `Ingredient` | `world.item.Ingredient` + `ItemValue` | **`world.item.crafting.Ingredient`** + `HolderSet<Item>` / `of(Stream)` |
| `ChunkSerializer` | チャンク保存 | **`SerializableChunkData`** |
| `ChunkStorage` | リージョン I/O | **`SimpleRegionStorage`** |
| `Boat.Type` / `BoatItem` | 木の種類 enum | **`EntityType<? extends AbstractBoat>`** コンストラクタ引数 |
| `AbstractMinecart.Type` | マインカート種別 | **廃止**（`getBehavior()` 等） |
| `GameRules` on `LevelData` | `getGameRules()` on PrimaryLevelData | **`ServerLevel.getGameRules()`** のみ（LevelData から削除） |
| `BorderChangeListener` | メソッド名 | **`onSetSize` / `onLerpSize` / `onSetCenter` 等** |
| `FarmBlock` | 耕地 | **`FarmlandBlock`** |
| `ArmorItem` + `dispenseArmor` | 防具ディスペンサ | **`EquipmentDispenseItemBehavior.dispenseEquipment`** |
| `EntityInteraction` (SGPL inner) | エンティティ操作 | **廃止** → `handleInteract(ServerboundInteractPacket)` |
| `AbstractArrow` | `entity.projectile.AbstractArrow` | **`entity.projectile.arrow.AbstractArrow`** |
| レシピ結果 | `ItemStack result` フィールド | **`ItemStackTemplate` + `result()` メソッド** |
| Forge ビルド | `forge()` + MCP | **Architectury Loom `McpExecutor` NPE（`-PenableForge=true` で再有効化待ち）** |
| `SimpleWeightedRandomList` | `util.random.SimpleWeightedRandomList` | **`util.random.WeightedList`**（`empty()` 維持） |
| `FuelValues` | `world.item.crafting.FuelValues` | **`world.level.block.entity.FuelValues`** |
| `EnderDragonPart` | `entity.boss.EnderDragonPart` | **`entity.boss.enderdragon.EnderDragonPart`** |
| `ClickType` (Container) | `inventory.ClickType` | **`inventory.ContainerInput`** |
| `BlockEntity.DataComponentInput` | inner class | **`core.component.DataComponentGetter`** |
| NeoForge サーバー起動 | modlauncher `--launchTarget` + `NeoForgeServerLaunchHandler` | **`net.neoforged.fml.startup.Server#main`**（classpath 直起動） |
| FML バイトコード変換 | modlauncher `ILaunchPluginService` | **`ClassProcessorProvider`**（`neoforgespi.transformation`） |
| `IModFileReader#read` | `cpw.mods.jarhandling.JarContents` | **`net.neoforged.fml.jarcontents.JarContents`** |
| `IModFileInfo#moduleName` | JPMS モジュール名 | **廃止** → `IModFile#getId()` |
| `LootDataType.Validator` | バリデータ | **`LootDataType.ContextGetter` + `runValidation` フック** |
| `SavedData.Factory` | `SavedData.Factory` + 文字列キー | **`SavedDataType` record + `computeIfAbsent(TYPE)`** |
| protected inner 型参照 | 外部パッケージ Mixin | **同一パッケージ Helper / Accessor**（`net.minecraft.*` 配置） |
| NeoForge `VanillaInventoryCodeHooks` | `neoforge.items` + `IItemHandler` | **`neoforge.transfer.item` + `ResourceHandler<ItemResource>`** |
| `BlockEvent.BreakEvent` | inner class | **`event.level.block.BreakBlockEvent`** |
| `PayloadRegistration` | 7 引数（handler 同梱） | **6 引数** + `SERVERBOUND_HANDLERS` / `CLIENTBOUND_HANDLERS` 分離 |
| Architectury `namedElements` | common 依存 configuration | **`common(project)` 自動解決**（loom-no-remap） |
| `FMLLoader.getLoadingModList()` | static | **`FMLLoader.getCurrent().getLoadingModList()`** |
| `Level.isClientSide` フィールド | 直接参照 | **`isClientSide()`** |
| Fabric `ServerWorldEvents` | `onWorldLoad` / `onWorldUnload` | **`ServerLevelEvents`** + `onLevelLoad` / `onLevelUnload` |
| Fabric `S2CPlayChannelEvents` | play チャンネル登録イベント | **`ClientboundPlayChannelEvents`** |
| Fabric `S2CConfigurationChannelEvents` | config チャンネル登録イベント | **`ClientboundConfigurationChannelEvents`** |
| Fabric `PayloadTypeRegistry` | `playS2C`/`playC2S`/`configurationS2C`/`configurationC2S` | **`clientboundPlay`/`serverboundPlay`/`clientboundConfiguration`/`serverboundConfiguration`** |
| Fabric `PayloadTypeRegistryImpl` | `PLAY_S2C` | **`CLIENTBOUND_PLAY`** |
| Fabric login ネットワーク | `NetworkHandlerExtensions` + `PacketByteBufLoginQueryResponse` | **`PacketListenerExtensions`** + `FriendlyByteBufLoginQueryResponse` |
| Fabric `ServerConfigurationNetworking.Context` | `networkHandler()` | **`packetListener()`** |
| Fabric Permissions API | `0.3.x`（同期 `Permissions.check(Entity)`） | **`0.7.0`**（26.1 対応・`ServerPlayer` 引数） |

### 0.2 実装済みコミット相当の変更

- `buildSrc`: 26.1+ 非難読化 Spigot マッピング (`ProcessMappingTask.runDeobfuscated`)、`RemapSpigotTask` 簡略化
- **`buildSrc` (2026-06-07)**: `runDeobfuscated` で ASM 走査し **メソッド/フィールド同一 SRG** を生成（Mixin AP 用）
- `463` Java ファイル: `org.bukkit.craftbukkit.v.*` → `org.bukkit.craftbukkit.*`
- Access Widener: `official` 名前空間へ更新
- Loom no-remap: `remapJar` 削除、`shadowJar` 主出力化（fabric/neoforge）
- **DC-01〜02**: 全 `*RecipeMixin` を `ItemStackTemplate` 対応
- **W-05**: `DistValidate` instanceof 修正、`ChunkGeneratorMixin` populator ガード
- **API-02 (2026-06-07)**: `ServerGamePacketListenerImpl_HandlerMixin` record API + `ArclightInventoryHelper`
- **LR-04 (2026-06-07)**: `CraftRecipeMixin` `Ingredient.of(ItemLike)` 対応
- **BaseSpawnerMixin**: `WeightedList<SpawnData>` + AW 更新
- **LevelPersistentData**: `SavedDataType` + Codec、`ServerLevelMixin` `computeIfAbsent(TYPE)`
- **LootDataTypeMixin**: `runValidation` で `CraftLootTable` 紐付け
- **Illager/Bee**: `io.izzel.arclight.common.mod.nms.*` へ移行（JPMS-01 完了・`package net.minecraft` ゼロ化）
- **arclight-common/build.gradle**: `sponge-mixin` 0.17.3 AP classpath、**既定 `-proc:none`**（`-PenableMixinAp` で AP 有効化）

### 0.3 次の作業（優先順）

1. **Phase 5 スモーク（R-02〜R-07）** — プレイヤー参加・Bukkit プラグイン・PDC/ItemMeta 検証（要 Minecraft クライアント）
2. **B-14**: Forge — Architectury Loom `McpExecutor.execute` NPE（26.1 非難読化で MCP 0 ステップ時にクラッシュ。[loom#328](https://github.com/architectury/architectury-loom/issues/328) 修正待ち / ModDevGradle PoC）
3. ~~**NF-04**~~ → **完了**（`ArclightPermissionHandler` + `PermissionAPIMixin` 実装済み。`compat.forwardPermission=true` で Bukkit 転送）
4. ~~**DC-09**~~ → **完了**（26.1.2 実測: `DataComponentPatch$Builder.map` + `get(DataComponentGetter)` 存続、AW 有効）
5. ~~**SL-01**~~ → **完了**（`LevelStorageAccess.saveDataTag(WorldData, UUID)` 26.1.2 存続・`ServerLevelMixin` 注入確認）
6. ~~**LR-02/03/05**~~ → **不要**（NeoForge/Forge 層 Mixin は空ブリッジのみ。レシピは `RecipeManagerMixin` common で完結）

**ビルド成果物（2026-06-13 `collect` 成功）:**

| JAR | パス |
|-----|------|
| NeoForge | `build/libs/arclight-neoforge-26.1.2-1.0.2-SNAPSHOT.jar` |
| Fabric | `build/libs/arclight-fabric-26.1.2-1.0.2-SNAPSHOT.jar` |

**ビルドメモ:** 通常 compile は Mixin AP をスキップ（`-proc:none`）。AP 検証が必要な場合は `-PenableMixinAp` を付与。

**Forge ビルド再有効化:** `./gradlew ... -PenableForge=true`（Loom MCP NPE 解消後）

---

## 1. 目的

| 項目 | 内容 |
|------|------|
| 最終目標 | Arclight 環境で **NeoForge 26.1.2** 対応サーバーを稼働させる |
| ベース | `arclight-neoforge-1.21.1-1.0.2-SNAPSHOT-0769551`（現行 `FeudalKings` ブランチ） |
| 作業ブランチ | **`feature/neoforge-26.1.1-migration`**（ローカルのみ作成済み） |
| 制約 | Cursor Free 枠のみで実装（外部有償 API・大規模自動生成に依存しない） |

---

## 2. 前提（ターゲット環境）

| 項目 | 現行（ソース） | ターゲット |
|------|----------------|------------|
| Minecraft | **1.21.1** | **26.1.2** |
| NeoForge | **21.1.216**（`libs.versions.toml`） | **26.1.2**（例: `26.1.0.1-beta` 以降） |
| Arclight 版 | **1.0.2-SNAPSHOT** | 同上（MC 版番号のみ更新） |
| Java | **21** | **25** |
| Gradle | **8.13** | **9.1.0+** |
| ビルド | Architectury Loom **1.9-SNAPSHOT** + `neoForge()` | Loom 26.1 対応確認、または ModDevGradle 2.x への段階移行を検討 |
| Spigot NMS | `v1_21_R1` / reversion **4344** | **26.1.2 用 Spigot ビルドメタ**（公開時点で要確認） |
| 難読化解消 | Parchment + Mojang 公式マッピング | **Parchment 任意化**（Mojang 公式パラメータ名が利用可能） |

> **バージョン体系の変更:** Minecraft / NeoForge は新体系 `26.1.2` を採用。NeoForge 版番号 `26.1.0.N-beta` の先頭 3 成分が MC 版を表す（[NeoForge 26.1 リリースノート](https://neoforged.net/news/26.1release/)）。

---

## 3. 参照資料

| 資料 | URL / パス |
|------|------------|
| 修正元ソース | https://github.com/IzzelAliz/Arclight |
| NeoForge 公式 | https://docs.neoforged.net/ |
| NeoForge 26.1 プライマー | https://docs.neoforged.net/primer/docs/26.1/ |
| NeoForge 26.1 リリースノート | https://neoforged.net/news/26.1release/ |
| Forge Wiki | https://forge.gemwire.uk/wiki/Main_Page |
| Forge / NeoForge 比較表 | https://docs.google.com/spreadsheets/d/1_DQELiPvCF0FmFfyU4opGDbWi7zv-bSl8ImZuh6645E/edit?gid=248444698 |
| 中間版リリースノート（21.2〜21.11） | NeoForge 26.1 リリースノート内リンク参照 |
| Thermal 系 1.21.1 移行参考 | `ThermalCoreForNeoForge/docs/NEOFORGE_1.21.1_MIGRATION_SPEC.md` 他 |
| CoFH Core 移行参考 | `CoFHCoreForNeoForge/docs/NEOFORGE_1.21.1_MIGRATION_SPEC.md` |

---

## 4. プロジェクト構成と影響範囲

```
Arclight/
├── arclight-common/       # Bukkit ブリッジ + 約 650 Mixin（主戦場）
├── arclight-neoforge/     # NeoForge プラットフォーム層（66 Mixin + mod コード）
├── bootstrap/             # 起動 JAR（NeoForge SPI / LaunchHandler）
├── installer/             # NeoForge インストーラ連携
├── buildSrc/              # Spigot 生成・マッピング・リマップ
├── i18n-config/           # ローカライズ（影響小）
└── gradle/libs.versions.toml
```

### 4.1 Mixin 規模（現行 1.21.1）

| 設定ファイル | 登録数（概算） |
|--------------|----------------|
| `mixins.arclight.core.json` | **556** |
| `mixins.arclight.bukkit.json` | **46** |
| `mixins.arclight.vanilla.json` | **28** |
| `mixins.arclight.impl.optimization.json` | **23** |
| `mixins.arclight.neoforge.json` | **68** |

**合計 700+ Mixin** が 26.1 のシグネチャ変更・リネーム・record 化の影響を受ける可能性がある。

### 4.2 レイヤー分担

| レイヤー | 役割 | 26.1 移行での重要度 |
|----------|------|---------------------|
| `buildSrc` + Spigot 生成 | CraftBukkit 生成・NMS パッケージ | **最高**（26.1 Spigot が存在しない場合はブロッカー） |
| `arclight-common` | Bukkit API ブリッジ・バニラ Mixin | **最高** |
| `arclight-neoforge` | NeoForge イベント・ネットワーク・拡張 API | **高**（`remap = false` が多い） |
| `bootstrap` | ModLauncher / FML 起動 | **高** |
| `installer` | ライブラリ取得・NeoForge インストール | **中** |

---

## 5. 移行の考え方（Forge 1.20.1 → NeoForge 1.21.1 との対応）

Thermal / CoFH 系リポジトリの **Forge 1.20.1 → NeoForge 1.21.1** 移行は、今回の **1.21.1 → 26.1.1** の**第一段階**に相当する。Arclight は 1.21.1 時点で既に Data Components 対応済みのため、Thermal 側で実施済みの以下は **再実施不要** とみなす。

| Forge 1.20.1 → NeoForge 1.21.1（Thermal 参考） | Arclight 1.21.1 現状 |
|------------------------------------------------|----------------------|
| `ItemStack.getTag()` / `setTag()` 廃止 | **該当なし**（grep ゼロ） |
| `DataComponents` / `DataComponentPatch` 導入 | **実装済み**（`ItemStackMixin`, `BlockEntityMixin`, `CraftMetaItemMixin` 等） |
| `@Mod.EventBusSubscriber` 廃止 → 明示登録 | Arclight mod 側は `NeoForgeArclightServer` で登録済み |
| `loadAdditional` / `saveAdditional` BE NBT | **実装済み** |
| `ResourceLocation` ファクトリ化 | 要再 grep（26.1 で `Identifier` 系リネームの可能性） |
| `FluidStack.parseOptional` / `STREAM_CODEC` | common 内の該当箇所要再監査 |

**26.1 固有の第二段階**として、以下が新たに追加される。

| 26.1 新規変更 | Arclight への影響 |
|---------------|-------------------|
| `ItemStackTemplate` / `ItemInstance` | レシピ・ルート・アドバンスメント・Bukkit Recipe 変換 |
| `DataComponentInitializers` | アイテム登録周辺（直接影響は mod 側が主） |
| `DataComponentPatch#get` シグネチャ変更 | `DataComponentPatch_BuilderMixin` |
| Loot 型の *Type ラッパ廃止 | `LootTableMixin` 等 |
| `ChunkPos` record 化 | チャンク関連 Mixin |
| 難読化解消 | マッピング戦略の簡素化、Shadow 名の見直し |
| Java 25 | 全モジュール toolchain |
| `GuiGraphics` → `GuiGraphicsExtractor` | サーバー側影響は限定的（クライアント Mixin なし） |

---

## 6. 修正仕様 — ビルド・インフラ（Tier 1）

| ID | 対象 | 仕様 | 受入条件 | 状態 |
|----|------|------|----------|------|
| B-01 | `gradle/libs.versions.toml` | MC **26.1.2**, NeoForge **26.1.2.73**, Fabric API **0.150.0+26.1.2** | 依存解決成功 | **完了** |
| B-02 | `gradle/wrapper/gradle-wrapper.properties` | Gradle **9.2.1** | `./gradlew --version` 成功 | **完了** |
| B-03 | `build.gradle` | Java toolchain **25** | 全 subproject コンパイル | **完了** |
| B-04 | Architectury Loom | `loom-no-remap` **1.14.476** | NeoForge/Fabric 設定成功 | **部分完了** ※Forge NPE |
| B-05 | Parchment | 削除（マッピング層なし） | ビルド成功 | **完了** |
| B-06 | `bukkit-api` / `spigot-reversion` | **4628** / `v26_1_R1` | Spigot 生成成功 | **完了** |
| B-07 | `buildSrc` | 26.1 非難読化マッピング・Spigot リマップ | `setupSpigot` 成功 | **完了** |
| B-08 | `arclight-neoforge/build.gradle` | shadowJar 主出力・no-remap・`namedElements` 廃止 | compile 成功 | **完了** |
| B-09 | `bootstrap/build.gradle` | FML / NeoForge / Fabric bootstrap 依存更新 | bootstrap JAR 生成 | **完了**（neoforgeJar・fabricJar 成功・Forge JAR は B-14 待ち） |
| B-10 | `installer` + `installer.json` | NeoForge 26.1 インストーラ座標 | 初回起動で libs 取得 | **部分完了**（`collect` 成功・`generateInstallerInfo` は JAR 同梱済み・初回起動未検証） |
| B-11 | `META-INF/neoforge.mods.toml` | `loaderVersion` 範囲確認 | 起動時 mod 検証通過 | **部分完了**（`loaderVersion="[11,)"` に更新・FML 11.0.13 対応・ランタイム未検証） |
| B-12 | CraftBukkit フラットパッケージ + AT | `bukkit.at` フラット化 + `RemapSpigotTask` AT 適用 | CraftMetaItem public | **完了** |
| B-13 | Access Widener | `named` → `official` | Loom AW 処理成功 | **完了** |
| B-14 | Forge モジュール | `-PenableForge=true` で include | Forge compile 成功 | **ブロック中** ※`McpExecutor.execute:183` NPE（`:executing 0 MCP steps`）。Architectury Loom 26.1 Forge 非対応 |

### 6.1 B-04 リスク: Architectury Loom vs ModDevGradle

- **現行:** Architectury Loom `1.9-SNAPSHOT` + `neoForge()`（マルチローダー統合ビルド）
- **NeoForge 26.1 公式推奨:** ModDevGradle **2.0.141+** または NeoGradle **7.1.21+**
- **方針:**
  1. まず Loom の 26.1 対応状況を Architectury / NeoForge Discord で確認
  2. Loom 非対応の場合、**NeoForge モジュールのみ** MDG 分離、または全体を MDG へ移行する PoC を Phase 0 で実施
  3. Forge / Fabric モジュールとの共存は後続フェーズ（本仕様書の NeoForge スコープでは NeoForge のみ必須）

### 6.2 B-06 ブロッカー: Spigot / CraftBukkit for 26.1

`buildSrc` は `https://hub.spigotmc.org/versions/{reversion}.json` から Spigot をビルドする。

```
Setup for Spigot ${mcVersion}(${spigotReversion})
```

26.1 用 **BuildTools メタデータが公開されていない** 場合、Arclight 移植は **Spigot 側の対応待ち** または **独自パッチ Spigot ビルド** が必要。Phase 0 で必ず確認する。

---

## 7. 修正仕様 — NBT / Data Components

### 7.1 現行 Arclight の Data Component 実装（維持・拡張対象）

| ファイル | 現行パターン | 26.1 での対応 |
|----------|--------------|---------------|
| `ItemStackMixin.java` | `PatchedDataComponentMap`, `restorePatch(DataComponentPatch)` | `ItemInstance` 実装への追従、`@Deprecated Item item` フィールド存否確認 |
| `ItemStackMixin_NeoForge.java` | `IItemStackExtension`, `hurtAndBreak` イベント注入 | `hurtAndBreak` シグネチャ・内部呼び出し先の再検証 |
| `DataComponentPatch_BuilderMixin.java` | `map` 直接参照、`copy`/`clear`/`isSet` | **`DataComponentPatch#get(DataComponentGetter)` 変更**に伴う Builder API 追従 |
| `CraftMetaItemMixin.java` | `DataComponentPatch.Builder unhandledTags` | 生成 Spigot のフィールド名変更に追随 |
| `BlockEntityMixin.java` | `applyComponents` + PDC を `CompoundTag` `PublicBukkitValues` | `loadAdditional`/`saveWithoutMetadata` シグネチャ維持確認 |
| `BannerBlockEntityMixin.java` | `DataComponents.BANNER_PATTERNS` | `DataComponentInitializers` への移行影響を監査 |

### 7.2 CompoundTag が残る領域（26.1 でも NBT 継続想定）

| 区分 | 代表ファイル | 備考 |
|------|--------------|------|
| エンティティ永続化 | `EntityMixin`, `LivingEntityMixin`, `PlayerMixin`, `MobMixin` 等 | ワールド/エンティティ NBT は存続 |
| チャンク | `ChunkSerializerMixin`, `ChunkAccessMixin`, `RegionFileStorageMixin` | `ChunkPos` API 変更に注意 |
| プレイヤーデータ | `PlayerDataStorageMixin` | |
| Bukkit PDC | `BlockEntityMixin`, `LevelPersistentData` | `PublicBukkitValues` キーは維持 |
| 構造物 | `StructureTemplateMixin`, `StructureStartMixin` | |

### 7.3 26.1 新規: ItemStackTemplate / ItemInstance 対応（Tier 2 — 高優先）

| ID | 対象 | 仕様 | 状態 |
|----|------|------|------|
| DC-01 | 全 Recipe Mixin | `ItemStackTemplate` / `result()` API | **完了** |
| DC-02 | `bridge$toBukkitRecipe` | `template.create()` + `count() == 0` | **完了** |
| DC-03 | `MerchantOfferMixin` | 取引結果の Template 化 | **不要**（26.1.2 実測: `result` フィールドは `ItemStack` のまま） |
| DC-03b | `ArclightSpecialIngredient` | `Ingredient.items()` + `Holder<Item>` | **完了** |
| DC-04 | `LootTableMixin` | Loot 出力・条件の `ItemInstance` / Template 対応 | **不要**（26.1.2: `ObjectArrayList<ItemStack>` のまま・`LootItemFunctionType` 参照ゼロ） |
| DC-05 | `ItemStackMixin` / Bridge | `ItemInstance` メソッド追加時の bridge 拡張 | **監視**（`PatchedDataComponentMap` + `@Deprecated Item item` 維持・compile 通過） |
| DC-06 | `WrappedContents` / `EntityDropContainer` | ドロップリストの Template 対応 | **不要**（Bukkit イベント経路は `ItemStack` / `CraftItemStack` のまま） |
| DC-07 | `ArclightSpecialRecipe` | 特殊レシピの Template 結果処理 | **不要**（`CraftComplexRecipe` + `RecipeHolder` 登録で十分） |
| DC-08 | `MaterialBridge` | `item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0)` 維持確認 | **完了** |

**参考（26.1 プライマー）:**

```java
// レシピ結果の immutable 表現
ItemStackTemplate result = ...;
ItemStack stack = result.create();

// 非空 Stack から Template
ItemStackTemplate fromStack = ItemStackTemplate.fromNonEmptyStack(stack);

// 共通インターフェース
ItemInstance instance = stack; // or template
```

### 7.4 26.1 新規: DataComponentPatch API 変更

| ID | 内容 | 状態 |
|----|------|------|
| ENT-02 | `MobSpawnType` → `EntitySpawnReason` | grep 置換 | **完了** |
| ENT-03 | `ForcedChunksSavedData` / `Util` / `LevelLoadListener` | 26.1.2 API 追従 | **完了** |
| ENT-01 | エンティティ subpackage 再編 | animal/npc/monster/projectile/vehicle | **部分完了**（Sheep/Zombie/Villager 等追補済） |
| API-01 | `InteractionResultHolder` → `InteractionResult` | Item Mixin 戻り値・Decorate 修正 | **完了** |
| API-02 | `ServerGamePacketListenerImpl` 操作 | `handleInteract` 注入 | **部分完了** |
| API-03 | Border / ForcedChunk / RecipeBook | 26.1 API | **部分完了** |
| DC-09 | `DataComponentPatch#get(DataComponentGetter)` | Builder Mixin 監査 | **完了**（2026-06-13: `map` フィールド + `get(DataComponentGetter, Type)` 26.1.2 実測。`CraftMetaItemMixin` `unhandledTags` 連携） |
| DC-10 | `DataComponentPatch.Builder#set(Iterable<TypedDataComponent>)` オーバーロード | 利用箇所があれば追従 | **不要**（common 内 `TypedDataComponent` 参照ゼロ） |
| DC-11 | `DataComponentInitializers`（レジストリオブジェクト登録時） | Arclight 直接利用なし（mod 互換テストで間接確認） | **監視** |

### 7.5 26.1 新規: FluidStackTemplate

| ID | 内容 | 状態 |
|----|------|------|
| DC-12 | `FluidStack` / `FluidResource` のレジストリロード後インスタンス化 | common 内 Fluid 関連を grep 監査 | **不要**（2026-06-13: `arclight-common` 内 `FluidStack` 参照ゼロ） |
| DC-13 | レシピ・ネットワークの `FluidStack.STREAM_CODEC` | Thermal 参考（`FluidStack.parseOptional` パターン） | **不要**（同上） |

---

## 8. 修正仕様 — Loot / Recipe / Advancement（Tier 2）

26.1 では Loot 関連の `*Type` ラッパが廃止され、レジストリが直接 `MapCodec` を保持する（[26.1 プライマー — Loot Type Unrolling](https://docs.neoforged.net/primer/docs/26.1/)）。

| ID | 対象 | 仕様 | 状態 |
|----|------|------|------|
| LR-01 | `LootTableMixin` | `LootItemFunctionType` 等の参照削除、`codec()` パターンへ | **不要**（26.1.2: 直接 Mixin 不要・compile 通過） |
| LR-02 | `LootContextMixin_NeoForge` | NeoForge loot フックのシグネチャ追従 | **不要**（空ブリッジ Mixin。Loot 出力は `ItemStack` のまま） |
| LR-03 | 全 `*RecipeMixin` | `CraftingBookCategory` コンストラクタ変更 → `Recipe.CommonInfo` + `CraftingBookInfo` | **不要**（26.1.2: `ShapedRecipeMixin` `category()` + `ItemStackTemplate result` で compile 通過） |
| LR-04 | `CraftRecipeMixin` | `Ingredient` HolderSet / `of(Stream)` API | **完了** |
| LR-05 | `RecipeManagerMixin_NeoForge` | レシピリロードフック | **不要**（`RecipeManagerMixin` common が Bukkit レシピ登録を担当） |
| LR-06 | Validation  overhaul | `Validatable` / `ValidationContext`（datapack 検証）— 直接 Mixin なし、生成データ整合性テスト | **監視** |

---

## 9. 修正仕様 — ワールド / チャンク / エンティティ（Tier 2）

| ID | 対象 | 仕様 | 状態 |
|----|------|------|------|
| W-01 | `ChunkMapMixin`, `ChunkGeneratorMixin` | 26.1 チャンク管轄 API（現ブランチで未コミット変更あり — 移植時に統合） | **完了**（`ChunkMapMixin` `LevelLoadListener` ctor・`bridge$tick` 等 compile 通過） |
| W-02 | `ChunkPos` 利用箇所全般 | `new ChunkPos(pos)` → `ChunkPos.containing(pos)`、`asLong` → `pack`、`new ChunkPos(long)` → `unpack` | **完了**（grep: `new ChunkPos(` ゼロ・`pack`/`unpack`/`containing` 移行済み） |
| W-03 | `SerializableChunkDataMixin`, `ChunkAccessMixin` | ChunkSerializer→SerializableChunkData リネーム | **部分完了** |
| W-04 | `ServerLevelMixin`, `ServerPlayerMixin` | ワールド Bukkit 値 (`readBukkitValues`) の NBT キー維持 | **完了**（`LevelPersistentData` `SavedDataType` + `EntityMixin` `ValueInput` 経路。ワールド PDC は `CompoundTag` 継続） |
| W-05 | `DistValidate.java` | `instanceof ServerLevel` 等 | **完了** |
| W-06 | `EntityMixin` 系 | `defineSynchedData(SynchedEntityData.Builder)` パターン（1.21.1 で既対応済み箇所の再確認） | **未着手** |

---

## 10. 修正仕様 — NeoForge プラットフォーム層（Tier 2）

`arclight-neoforge` の **`remap = false` Mixin は約 30 ファイル**。NeoForge API 変更でコンパイルエラーにならなくても **実行時 Mixin 適用失敗** になりうる。

### 10.1 高リスクファイル一覧

| ファイル | フック対象 | 26.1 確認項目 |
|----------|------------|---------------|
| `CommonHooksMixin.java` | `onPlaceItemIntoWorld`, `onLivingDrops` | メソッド存続・`LivingDropsEvent` API |
| `EventHooksMixin.java` | `onBlockPlace`, `onMultiBlockPlace` | イベント引数型 |
| `NetworkRegistryMixin.java` | `PAYLOAD_REGISTRATIONS`, `getCodec`, `checkPacket` | **ネットワーク登録 API**（変更頻度高） |
| `NetworkComponentNegotiatorMixin.java` | ネゴシエーション | 同上 |
| `PacketDistributorMixin.java` | カスタムペイロード送信 | Payload codec |
| `PermissionAPIMixin.java` | Permission API | |
| `VanillaInventoryCodeHooksMixin.java` | ホッパー / アイテム移動 | `ItemStack` / `ItemInstance` |
| `ItemStackMixin_NeoForge.java` | `IItemStackExtension` | 拡張インターフェースメソッド |
| `ItemMixin_NeoForge.java` | アイテムフック | |
| `AnvilMenuMixin_NeoForge.java` | エンチャント / 耐久 | |
| `AbstractContainerMenuMixin_NeoForge.java` | コンテナイベント | |

### 10.2 NeoForge イベント / 登録（Forge 1.20.1 → 1.21.1 パターンの再確認）

| ID | 内容 | 状態 |
|----|------|------|
| NF-01 | `NeoForgeArclightServer.java` — `IEventBus` 登録 | 26.1 でイベントクラス存続確認 | **監視**（compile 通過） |
| NF-02 | `mod/event/*Dispatcher.java` | Block / Entity / Item イベントディスパッチ | **部分完了**（`BreakBlockEvent` 等） |
| NF-03 | `mod/plugin/messaging/*` | Bukkit Plugin Channel ↔ NeoForge Payload | **部分完了**（`PayloadRegistration` 6 引数 + `SERVERBOUND_HANDLERS`） |
| NF-04 | `mod/permission/ArclightPermissionHandler.java` | Permission API | **完了**（`PermissionAPIMixin` + `ArclightNeoForgePermissible`。`forwardPermission` 設定で Bukkit `hasPermission` 転送） |
| NF-05 | `arclight.accesswidener` | 26.1.2 無効エントリ除去（~179 行コメントアウト）+ illager サブパッケージ AW 復活 | **部分完了**（`SpellcasterIllager$IllagerSpell` / `$SpellcasterUseSpellGoal` 26.1.2 パス有効化） |
| NF-06 | `BaseMappedRegistry#unfreeze` AW | レジストリ凍結解除 | **完了**（`arclight_neoforge.accesswidener` に `unfreeze` 登録済み） |

### 10.3 ネットワーク（Bukkit Plugin Messaging 互換）

`NetworkRegistryMixin` は Bukkit の `Messenger` / Plugin Channel と NeoForge Payload 登録を橋渡しする **Arclight 固有の重要箇所**。

26.1 移行時の検証項目:

1. `ConnectionProtocol.CONFIGURATION` / `PLAY` の Payload マップ構造
2. `RawPayload.discardedCodec` のコンストラクタ
3. `onMinecraftRegister` / `onMinecraftUnregister` のシグネチャ
4. `onConfigurationFinished` の `NetworkPayloadSetup` API
5. 既知互換 mod（Thermal 系等）との Plugin Channel 共存テスト

---

## 11. 修正仕様 — Bootstrap / 起動（Tier 3）

| ID | 対象 | 仕様 | 状態 |
|----|------|------|------|
| BS-01 | `Main_Neoforge.java` / `ApplicationBootstrap.java` | Java 25 起動、モジュールパス | **部分完了**（`Launcher` MIN Java 25、bootstrap compileJava 25） |
| BS-02 | `ArclightLaunchHandler.java` | `ILaunchHandlerService` API | **部分完了**（`ServiceRunner` + `Server.main` 委譲。26.1 既定は classpath 直起動） |
| BS-03 | `ArclightModFileReader.java` / `ArclightLocator_Neoforge.java` | SPI インターフェース変更 | **完了**（BS-03p） |
| BS-04 | `ModBootstrap.java` | FML 4.x → 26.1 対応版 | **部分完了**（`injectLaunchPlugin` 削除・`bootstrapped` フラグ化） |
| BS-05 | `META-INF/services/*` | `ClassProcessorProvider` SPI 登録、`ILaunchPluginService` 削除 | **完了** |
| BS-06 | `arclight-server-launch.properties` | メインクラス・引数 | **静的確認済み**（`Launcher` → `Main_Neoforge`/`Main_Fabric`/`Main_Forge`・NeoForge は `unix_args.txt` から main 委譲） |
| BS-07 | `ModBootstrap.java` | FML 11: `cpw.mods.cl` 廃止 → `legacyClassPath` + `net.neoforged.fml.classloading.ModuleClassLoader#postRun` | **完了** |
| BS-07b | `ArclightJarContentsImplFilter` / `ArclightJarInJarFilter` | 早期 FML 起動時 `Module.getLayer()==null` 対応 | **完了** |
| BS-08 | JPMS `ResolutionException` | `net.minecraft.*` Helper → `io.izzel.arclight.common.mod.nms.*` 移行 + AW 26.1.2 illager パス復活 | **完了** |
| JPMS-01 | NMS Helper パッケージ移行 | 5 ファイル移行、`mixins.arclight.core.json` 更新、`package net.minecraft` ゼロ化 | **完了** |
| B-11b | `neoforge.mods.toml` | 廃止 `[[accessTransformers]]` 削除（AW は Loom `*.accesswidener`） | **完了** |

---

## 12. 修正仕様 — 生成 CraftBukkit / Bukkit ブリッジ（Tier 1〜2）

Spigot 再生成後、以下が一括更新される。

| 項目 | 現行 | 26.1 |
|------|------|------|
| NMS パッケージ | `org.bukkit.craftbukkit.v1_21_R1` | **`org.bukkit.craftbukkit`（26.1.2 Spigot — リビジョンサブパッケージなし）** |
| import 一括 | 全 Java ソース + Mixin | **`craftbukkit.v.*` → `craftbukkit.*`（463 ファイル済）** |
| `CraftMetaItem` | `unhandledTags` Builder | 26.1 生成物のフィールド確認 |
| `CraftItemStack` | Data Component ベース | Template API 追加の可能性 |

**Mixin remapping:** `CraftMetaItemMixin` 等 `remap = false` の Bukkit 向け Mixin は、生成 Spigot のメソッド名変更に敏感。

---

## 13. 廃止・変更 API 洗い出し（1.21.1 → 26.1.1）

### 13.1 Minecraft バニラ（[26.1 プライマー](https://docs.neoforged.net/primer/docs/26.1/) より）

| カテゴリ | 1.21.1 | 26.1.1 |
|----------|--------|--------|
| ItemStack（immutable） | `ItemStack` を直接使用 | **`ItemStackTemplate`** + **`ItemInstance`** |
| レシピ結果 | `ItemStack result` | `ItemStackTemplate result` |
| Loot 型 | `LootItemFunctionType` 等 | **`MapCodec` をレジストリが直接保持** |
| FloatProvider / IntProvider | class + `getType()` | **interface** + `codec()` |
| ChunkPos | クラス + コンストラクタ | **record** + `containing` / `pack` / `unpack` |
| GUI | `GuiGraphics` | `GuiGraphicsExtractor`（サーバー影響小） |
| 難読化 | 有 | **無**（公式パラメータ名） |
| Java | 21 | **25** |
| Validation | 旧 CriterionValidator 等 | **`Validatable` + `ValidationContext`** |

### 13.2 NeoForge（[26.1 リリースノート](https://neoforged.net/news/26.1release/) + 比較表）

| カテゴリ | 1.21.1 | 26.1.1 |
|----------|--------|--------|
| 版番号 | `21.1.216` | `26.1.0.x-beta` 形式 |
| Gradle プラグイン | NeoGradle 7.x / Loom | **ModDevGradle 2.0.141+** 推奨 |
| Parchment | 推奨 | **任意**（削除可） |
| NeoForm | 旧形式 | **`26.1-N`** 形式 |
| ItemStack 拡張 | `IItemStackExtension` | 存続確認要 |
| ネットワーク | Payload registry | **変更可能性高** — 要 diff |

### 13.3 Forge 1.20.1 → NeoForge 1.21.1 で既に解決済み（Arclight で再確認のみ）

| 廃止 API | 置換 | Arclight grep |
|----------|------|---------------|
| `stack.getTag()` / `setTag()` | `DataComponents` / `CustomData` | **ゼロ** |
| `@Mod.EventBusSubscriber` | コンストラクタ登録 | mod 側確認 |
| `EntityItemPickupEvent` | `ItemEntityPickupEvent.Pre` | common イベント層 |
| `FriendlyByteBuf.readItem()` | `ItemStack.STREAM_CODEC` | 要 grep |
| `ResourceLocation(String, String)` | `fromNamespaceAndPath` | 要 grep |

---

## 14. 実装フェーズ計画

```
Phase 0: 前提確認（ブロッカー排除）
  ├─ Spigot 26.1 BuildTools メタの有無
  ├─ Architectury Loom 26.1 対応
  └─ NeoForge 26.1.1 正式版 / beta の選定

Phase 1: ビルド通過（Tier 1: B-01〜B-11）
  ├─ バージョン bump
  ├─ Java 25 / Gradle 9.1
  └─ Spigot 生成 + compileJava（エラー一覧取得）

Phase 2: common 層コンパイル修正（Tier 2）
  ├─ DC-* ItemStackTemplate / DataComponentPatch
  ├─ LR-* Loot / Recipe
  └─ W-* ChunkPos / ワールド

Phase 3: neoforge 層（Tier 2: NF-*）
  ├─ remap=false Mixin 全件
  └─ ネットワーク / イベント

Phase 4: bootstrap + installer（Tier 3: BS-*）
  └─ 起動 JAR 生成・初回インストール

Phase 5: ランタイム検証
  ├─ バニラ起動 + プレイヤー参加
  ├─ Bukkit プラグイン（Essentials 等）スモーク
  ├─ NeoForge mod（Thermal 系）スモーク
  └─ Plugin Channel / イベント互換
```

---

## 15. 検証チェックリスト

### 15.1 静的解析（実装後に実施）

```text
# ItemStack 廃止 NBT（1.21.1 由来 — ゼロ維持）
grep -r "getTag()\|setTag()\|hasTag()\|getOrCreateTag" arclight-common arclight-neoforge

# 1.21.1 → 26.1 新規置換候補
grep -r "new ChunkPos(" arclight-common arclight-neoforge
grep -r "ChunkPos.asLong" arclight-common arclight-neoforge
grep -r "@Shadow.*ItemStack result" arclight-common

# NeoForge 非 remapped Mixin
grep -r "remap = false" arclight-neoforge
```

### 15.2 ビルド検証

| コマンド | 期待結果 |
|----------|----------|
| `./gradlew :arclight-neoforge:compileJava` | エラーゼロ |
| `./gradlew :bootstrap:neoforgeJar` | JAR 生成 |
| `./gradlew collect` | `build/libs/` に neoforge JAR |

### 15.3 ランタイム検証

| # | シナリオ | 合格基準 |
|---|----------|----------|
| R-01 | `java -jar arclight-neoforge-26.1.1-*.jar nogui` | クラッシュなくワールド生成 |
| R-02 | プレイヤー参加 / チャット / ブロック設置 | Bukkit イベント発火 |
| R-03 | `/plugins` + 軽量 Bukkit プラグイン | ロード成功 |
| R-04 | Thermal Core + CoFH Core mod | レシピ・マシン・流体が動作 |
| R-05 | Plugin Channel（クライアント mod 連携） | 登録 / 送受信 |
| R-06 | ワールド再起動 | チャンク・PDC・エンティティ永続化 |
| R-07 | アイテム NBT（`/give` + PDC / ItemMeta） | Data Component 経由で保持 |

---

## 16. 参考リポジトリからの再利用パターン

Thermal / CoFH 系（いずれも **1.21.1 / NeoForge 21.1.219** — 26.1 未対応）から、Arclight 移植時に**テスト mod として利用**する。

| パターン | 参考実装 | Arclight での使い方 |
|----------|----------|---------------------|
| Item 永続データ | `CoFHItemData` + `DataComponents.CUSTOM_DATA` | Bukkit `ItemMeta` ↔ Component 変換のリグレッション mod |
| BE 永続化 | `loadAdditional` / `saveAdditional` | タイルエンティティ PDC テスト |
| イベント登録 | `NeoForge.EVENT_BUS.register` | NeoForge イベント ↔ Bukkit イベント変換の確認 |
| RegistryAccess | `TagsUpdatedEvent#getRegistryAccess()` | データパックリロード順序の Arclight 固有バグ検出 |
| ネットワーク | `ItemStack.STREAM_CODEC` | Payload 互換 |
| composite build | Thermal Foundation → Core | ビルド手順の参考（Arclight 本体とは独立） |

---

## 17. リスクと Mitigation

| リスク | 影響 | Mitigation |
|--------|------|------------|
| Spigot 26.1 未公開 | **移植不可** | Phase 0 で BuildTools メタ確認。代替: Paper / Spigot フォーク追従 |
| Architectury Loom 未対応 | ビルド不可 | ModDevGradle PoC、NeoForge 単独モジュール分離 |
| 700+ Mixin の実行時失敗 | 起動クラッシュ | 段階的に Mixin 無効化リストで二分探索 |
| NeoForge beta API 変動 | 再作業 | `-beta` 安定化まで Phase 5 を限定環境で実施 |
| Java 25 環境不足 | ビルド / 実行不可 | JDK 25 インストール、IDE 2025.2+ |
| 未コミットローカル変更 | マージ競合 | `ChunkMapMixin`, `ChunkGeneratorMixin`, `DistValidate` を移植開始前に stash / commit 整理 |

---

## 18. 現行ソースの調査結果サマリ

| 調査項目 | 結果 |
|----------|------|
| ItemStack 廃止 NBT API | **該当なし**（`getTag`/`setTag` 等） |
| Data Components 導入 | **1.21.1 で実装済み** |
| CompoundTag 利用 | **27+ ファイル**（エンティティ / チャンク / PDC — 26.1 でも継続） |
| NeoForge `remap=false` | **約 30 ファイル** |
| 作業ブランチ | **`feature/neoforge-26.1.1-migration`**（`FeudalKings` から分岐） |
| ローカル未コミット変更 | `ChunkMapMixin.java`, `ChunkGeneratorMixin.java`, `DistValidate.java` |

---

## 19. 改訂履歴

| 日付 | 版 | 内容 |
|------|-----|------|
| 2026-06-05 | 1.0.0 | 初版作成（調査のみ） |
| 2026-06-06 | 1.1.0 | **26.1.2 実装開始**。Tier1 ビルド/Spigot 生成完了、CraftBukkit フラット化、Recipe Template 対応、AW official 化 |
| 2026-06-08 | 1.7.0 | **BR-01/TT-01/API-07**。`ArclightBridges`/`ArclightNbtHelper`、`TeleportTransition.position/deltaMovement`、`snapTo`、bridge キャスト一括、PermissionSet/Commands 26.1.2 |
| 2026-06-08 | 1.6.0 | **CLK-01/MS-01/API-26.1.2**。`ArclightLevelHelper`、prepareLevels ChunkLoadCounter、Clock/Respawn/needsSync/Style/Commands 26.1.2 API |
| 2026-06-07 | 1.5.0 | **CS-01/T-03/SC-01/DC-08**。CraftServer 26.1.2 API、`ArclightTickets`、`Profiler.get()`、MaterialBridge Template/FuelValues、Bee AW、ServerLevel @CreateConstructor 更新 |
| 2026-06-07 | 1.3.0 | **ENT/API 26.1.2 一括修正**。Relative/TeleportTransition/LevelLoadListener、動物subpackage、ForcedChunks→Ticket、RecipeBook/Handler/Border 更新 |
| 2026-06-07 | 1.2.0 | **ID-01/T-01/B-12/GR-01 実装**。Identifier 置換、TicketStorage API、GameRules 刷新、bukkit.at AT 修正 |
| 2026-06-12 | 1.8.0 | **Phase 2 完了**。`ArclightNbtHelper`+`TagValueInputAccessor`、`SingleItemRecipe` Shadow、DelegateWorldInfo 26.1.2、`compileJava` ゼロ |
| 2026-06-12 | 1.9.0 | **Phase 3 完了**。B-08 `apiElements` 依存、`VanillaInventoryCodeHooks`→`transfer.item`、`BreakBlockEvent`、`PayloadRegistration`+Handler 分離、`FMLLoader.getCurrent()` |
| 2026-06-12 | 2.0.0 | **Phase 4 部分完了**。bootstrap Java 25・FML 11 依存、`ArclightLaunchHandler`/`IModFileReader` 26.1 API、AW 179 行無効化、`neoforgeJar` ビルド成功、intermediary 削除（26.1 非難読化） |
| 2026-06-12 | 2.1.0 | **BS-05 完了**。`ArclightClassProcessor`+`ArclightClassProcessorProvider`、`ILaunchPluginService` 廃止、`ModBootstrap` 簡略化。DC-03 は 26.1.2 実測で `ItemStack` 継続のため不要 |
| 2026-06-12 | 2.2.0 | **Phase 3.5 Fabric 完了**。Fabric API 26.1 追従、`fabric-permissions-api` 0.7.0、`:bootstrap:fabricJar` 成功 |
| 2026-06-13 | 2.3.0 | **B-10 完了**（`collect` → NeoForge/Fabric JAR）。**B-11** `loaderVersion="[11,)"`。**B-14** Forge `remapJar` 削除（Loom MCP NPE 継続）。**BS-06** 静的確認。**W-02/DC-04/06/07/10/LR-01** 監査完了（不要/完了） |
| 2026-06-13 | 2.8.0 | **監査セッション**: `collect` 再成功。DC-09/NF-04/NF-06/SL-01/W-01/W-04/LR-02〜05/DC-12〜13 を 26.1.2 API 実測で完了または不要に更新。B-14 スタックトレース確定（`McpExecutor.java:183`） |
| 2026-06-13 | 2.7.0 | **NF-HOPPER 完了**: `ResourceHandler` ホッパー Bukkit イベント（`HopperTransferContext`/`ResourceHandlerContainer`）。**R-01 完了**: ワールド生成+初回 tick。**R-01b**: `ArclightServer.createOrLoad` で `vanillaServer` 設定・`isPrimaryThread` NPE 回避 |
| 2026-06-13 | 2.5.0 | **BS-07 完了**: FML 11 `ModBootstrap`（`legacyClassPath` 方式・`ModuleClassLoader#postRun`）。**BS-07b**: JiJ/Filter `getLayer()` null 修正。**B-11b**: `neoforge.mods.toml` AT 削除。**BS-08**: JPMS `net.minecraft.*` 二重 export 判明（Phase 5 ブロッカー） |
| 2026-06-13 | 2.4.0 | **INST-26.1** 初回起動修正: `server_mappings` 任意化、`win_args.txt` `-classpath` 2行形式パース、`legacyClassPath` → `addToPath`。**Phase 5 部分**: FML `Server.main` 到達、`ModBootstrap`/`ModuleClassLoader` 要 FML 11 再設計 |

---

### 20.6 2026-06-12 Phase 3.5 実装済み（fabric compile + fabricJar 成功）

| ID | 内容 | 状態 |
|----|------|------|
| FAB-LC | `ServerWorldEvents` → `ServerLevelEvents`（`onLevelLoad`/`onLevelUnload`） | **完了** |
| FAB-NET | `S2CPlayChannelEvents`/`S2CConfigurationChannelEvents` → `Clientbound*` | **完了** |
| FAB-PAYLOAD | `PayloadTypeRegistry` 命名変更 + `PayloadTypeRegistryImpl.CLIENTBOUND_PLAY` | **完了** |
| FAB-LOGIN | `PacketListenerExtensions` + `FriendlyByteBufLoginQueryResponse` | **完了** |
| FAB-CFG | `ServerConfigurationNetworking.Context.packetListener()` | **完了** |
| FAB-PERM | `fabric-permissions-api` **0.3.1 → 0.7.0**、`ServerPlayer` 引数 | **完了** |
| FAB-INV | `Inventory.getSelected()` → `getSelectedItem()`（`FabricEventAdaptor`） | **完了** |
| FAB-CMD | `CraftServerMixin_Fabric` — `CraftPlayer.createCommandSourceStack()` | **完了** |
| FAB-BUILD | `arclight-fabric/build.gradle` — `-proc:none`（Mixin AP スキップ） | **完了** |
| JAR-02 | `./gradlew :arclight-fabric:build :bootstrap:fabricJar` | **完了** |

---

### 20.5 2026-06-12 Phase 4 実装済み（bootstrap neoforgeJar 成功）

| ID | 内容 | 状態 |
|----|------|------|
| BS-01p | `Launcher` MIN Java 25（class file 69）、bootstrap `compileJava` toolchain 25 | **完了** |
| BS-02p | `ArclightLaunchHandler` → `ILaunchHandlerService#launchService` + `Server.main` | **部分完了** |
| BS-03p | `ArclightModFileReader` → FML `JarContents` API | **完了** |
| BS-04p | `ArclightJarInJarFilter` → `IModFile#getId()` | **完了** |
| B-09p | bootstrap 依存: modlauncher **11.0.5**, FML loader **11.0.13**, SJH **3.0.8** | **完了** |
| B-10p | Fabric installer から `intermediary:26.1.2` 削除（stub 0.0.0 解決失敗回避） | **完了** |
| NF-05p | `arclight.accesswidener` 無効 179 行 `# INVALID-26.1.2` コメント化 | **完了** |
| AC-01 | `RenameAsyncCatcherTask` — `official`/`none` はリマップスキップ（26.1 非難読化） | **完了** |
| JAR-01 | `./gradlew :bootstrap:neoforgeJar` | **完了** |
| BS-05 | `ArclightClassProcessor` / `ArclightClassProcessorProvider` | FML 11 `ClassProcessorProvider` SPI、`runsBefore(MIXIN)` | **完了** |
| BS-05b | `ILaunchPluginService` / `ArclightImplementer` | modlauncher プラグイン経路削除 | **完了** |
| DC-03 | `MerchantOfferMixin` | 26.1.2: `ItemStack result` 継続（Template 化不要） | **不要** |

**ランタイム未検証（Phase 5）:** `ArclightClassProcessor`（FML 11 `ClassProcessorProvider`）はコンパイル・JAR 同梱済み。`ModBootstrap.postRun()` は `link()` から呼び出し。

---

## 20. 次のアクション

1. **Phase 5 スモーク（R-02〜R-07）** — プレイヤー参加・Bukkit イベント・PDC/ItemMeta 検証（**R-07**: `DataComponents.CUSTOM_DATA` / `CraftMetaItem.unhandledTags` 経路）
2. **B-14**: Forge Loom `McpExecutor` NPE — Architectury Loom [#328](https://github.com/architectury/architectury-loom/issues/328) 修正待ち / ModDevGradle PoC
3. **B-10 残**: 初回起動検証 — **NeoForge インストーラ・ライブラリ取得は成功**（2026-06-13 実測）
4. **W-03 / W-06 / DC-11**: チャンク保存・`defineSynchedData`・`DataComponentInitializers` — 実行時リグレッション監視

### 20.4 2026-06-12 Phase 3 実装済み（neoforge compile ゼロ）

| ID | 内容 | 状態 |
|----|------|------|
| B-08b | `common(project(':arclight-common'))` — loom-no-remap で `namedElements` 廃止 | **完了** |
| NF-NET | `PayloadRegistration` 6 引数化 + `SERVERBOUND_HANDLERS`/`CLIENTBOUND_HANDLERS` 動的登録 | **完了** |
| NF-NET2 | `NetworkRegistryMixin` — `MinecraftServer.executeIfPossible` | **完了** |
| NF-TRANSFER | `VanillaInventoryCodeHooks` → `net.neoforged.neoforge.transfer.item` + `ResourceHandler<ItemResource>` | **完了**（2026-06-13: `HopperTransferContext`/`ResourceHandlerContainer`/`HopperBlockEntity_NeoForge` 検索イベント） |
| NF-EVT | `BlockEvent.BreakEvent` → `BreakBlockEvent` | **完了** |
| NF-API | `EventHooks.canEntityGrief(ServerLevel)` / `onItemConsumptionTeleport` / `onEntityTeleportCommand`+ServerLevel | **完了** |
| NF-API2 | `CommonHooks.onAnvilUpdate` / `ServerExplosion` / `getSelectedItem()` / `FMLLoader.getCurrent()` | **完了** |
| NF-MAT | `MaterialMixin_NeoForge` 削除（`MaterialBridge` デフォルト + `FuelValues` に統一） | **完了** |
| NF-DM | `DistanceManagerMixin_NeoForge` — `forcedTickets` 廃止（bridge デフォルト） | **完了** |

### 20.2 2026-06-07 セッション2 実装済み

| ID | 内容 | 状態 |
|----|------|------|
| ENT-01 | 動物/NPC/monster/projectile/vehicle subpackage 一括（~130ファイル） | **部分完了** |
| ENT-02 | `MobSpawnType` → `EntitySpawnReason` | **完了** |
| ENT-03 | `Util`/`BlockUtil`/`SavedDataStorage`/`EnderDragonFight`/`Relative`/`TeleportTransition` | **完了** |
| ENT-03b | `ForcedChunksSavedData` 廃止 → `getForceLoadedChunks()` | **完了** |
| API-01 | `LevelLoadListener` / `ClientboundSetHeldSlotPacket` / `Equippable` | **完了** |
| API-02 | `ServerRecipeBookMixin` → `@ModifyVariable` + `ClientboundRecipeBookAddPacket` 経路 | **完了** |
| API-03 | `ServerGamePacketListenerImpl_HandlerMixin` → `handleInteract` | **部分完了** |
| API-04 | `ArclightBorderChangeListener` 26.1 メソッド名 | **完了** |
| API-05 | `ArmorItemMixin` → `EquipmentDispenseItemBehavior` | **完了** |
| API-06 | `FarmBlock` → `FarmlandBlockMixin` | **完了** |
| DC-09 | `DataComponentPatch_BuilderMixin` | **完了**（2026-06-13: `map` AW + `get(DataComponentGetter)` 26.1.2 実測） |
| ENT-01c | `ThrownSplashPotion`/`ThrownLingeringPotion` Mixin 分割 | PotionSplash/Lingering イベント | **完了** |
| ITEM-01 | `ChorusFruit`/`Milk` → ConsumeEffect Mixin | Data Component consume_effects | **完了** |
| ITEM-02 | `BoatItem` EntityType 化 | `AbstractBoat` ターゲット更新 | **完了** |

| ID | 内容 | 状態 |
|----|------|------|
| ID-01 | `ResourceLocation` → `Identifier`（common/neoforge/forge/fabric） | **完了** |
| T-01 | `Ticket`/`TicketType`/`TicketStorage`/`DistanceManagerMixin` 再実装 | **完了** |
| T-02 | `TicketMixin.of` + `PLUGIN_TICKET`（Spigot 互換） | **完了** |
| B-12 | `bukkit.at` フラット化 + deobfuscated AT 適用 | **完了** |
| GR-01 | `GameRules` → `gamerules.GameRules` + `GameRule<T>` 移行 | **部分完了**（旧 Value/Type Mixin 削除） |
| W-02p | `ChunkPos.asLong` → `pack`（ServerChunkCacheMixin） | **部分完了** |
| ENT-p | `Bee`/`IronGolem`/`AbstractHorse` subpackage | **部分完了** |

### 20.3 2026-06-08 セッション実装済み

| ID | 内容 | 状態 |
|----|------|------|
| BR-01 | `ArclightBridges`（`WorldBridge`/`EntityBridge` キャストヘルパ） | **完了** |
| NBT-H | `ArclightNbtHelper` + `TagValueInputAccessor`（`ValueInput`/`ValueOutput`） | **完了** |
| REC-T | 炉系 `*RecipeMixin` `@Shadow result()` + `ItemStackTemplate` | **完了** |
| DWI-01 | `DelegateWorldInfo` → `PrimaryLevelData` 26.1.2 ctor / `RespawnData` | **完了** |
| CHEST-01 | `CompoundContainer` → `CraftInventory`（`DoubleChestCombiner` 廃止追随） | **完了** |
| TT-01 | `TeleportTransition.pos()`→`position()`、`speed()`→`deltaMovement()` | **完了** |
| MV-01 | `absMoveTo`→`snapTo`（Entity/PacketListener） | **完了** |
| API-07 | `serverLevel()`→`level()`、`MobEffects.RESISTANCE`、`ClientboundLevelParticlesPacket` 11引数 ctor | **完了** |
| API-08 | `CommandSource1Mixin` `Permissions.COMMANDS_MODERATOR`、`CommandsMixin` SuggestionProviders キャスト | **完了** |
| SL-01p | `ServerLevelMixin` PVP/`WorldBorder.Settings`/TimeSkip `ArclightLevelHelper` | **部分完了** |
| BR-01b | `bridge$getWorld`/`bridge$getBukkitEntity` コンパイル時キャスト一括（~60ファイル） | **部分完了** |
