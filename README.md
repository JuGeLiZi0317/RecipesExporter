# Recipes Exporter

一个 Minecraft 1.20.1 Forge 服务端实用模组，允许服务器管理员通过命令将指定模组的配方数据导出为多种格式。

![Minecraft|106](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen)
![Forge](https://img.shields.io/badge/Forge-47.4.10-orange)
![Version](https://img.shields.io/badge/Version-1.0.0-blue)
![Side](https://img.shields.io/badge/Side-Server%20Only-lightgrey)

---

## 简介

Recipes Exporter 从内存中读取指定模组的所有配方数据，并以多种格式导出到文件中。

- **纯服务端运行**，客户端无需安装
- 所有操作均通过命令完成，无需任何客户端 GUI
- 导出文件统一存放在世界目录下，不污染其他目录

---

## 功能特性

- **三种导出模式**：`json_single`、`json_split`、`kubejs`
- **配方类型过滤**：按 `recipe_type` 筛选，只导出指定类型的配方
- **辅助查询命令**：快速列出指定模组包含的所有配方类型
- **失败日志**：当配方原始文件找不到时，自动生成日志记录所有失败条目

---

## 安装

将编译好的 `recipesexporter-1.0.0.jar` 放入服务端的 `mods/` 文件夹即可。客户端无需安装。

**环境要求：**

| 项目 | 版本 |
|------|------|
| Minecraft | 1.20.1 |
| Forge | 47.4.10（或兼容版本） |
| 权限 | 服务器控制台或 OP（等级 2 及以上） |

---

## 命令参考

### 导出配方

```
/recipesexporter export <modid> [export_type] [recipe_type]
```

| 参数 | 必填 | 说明 |
|------|------|------|
| `<modid>` | ✔ | 要导出配方的模组 ID，如 `minecraft`、`thermal` |
| `[export_type]` | | 导出模式，支持 Tab 补全，默认为 `json_single` |
| `[recipe_type]` | | 配方类型过滤，如 `crafting_shaped`、`minecraft:smelting` |

**示例：**

```bash
# 导出 minecraft 的所有配方（默认 json_single 模式）
/recipesexporter export minecraft

# 以 json_split 模式导出 thermal 的配方
/recipesexporter export thermal json_split

# 导出 minecraft 的合成配方到 KubeJS 脚本
/recipesexporter export minecraft kubejs crafting_shaped

# 导出 thermal 的熔炉配方（json_single 模式）
/recipesexporter export thermal json_single minecraft:smelting
```

### 查询配方类型

```
/recipeshelper gettypes <modid>
```

列出指定模组所有注册的配方类型，可用于确认 `[recipe_type]` 参数应填什么值。

**示例输出：**

```
[RecipesHelper] 模组 minecraft 包含以下配方类型:
  - minecraft:crafting_shaped
  - minecraft:crafting_shapeless
  - minecraft:smelting
  - minecraft:blasting
  - minecraft:smoking
  - minecraft:campfire_cooking
  - minecraft:stonecutting
  - minecraft:smithing_transform
  - minecraft:crafting_decorated_pot
```

---

## 导出模式

### `json_single`（默认）

将所有匹配的配方导出为独立的 `.json` 文件，平铺存放：

```
<世界目录>/RecipesExporter/<modid>/
  ├── oak_planks.json
  ├── diamond_sword.json
  └── furnace.json
```

适合快速批量获取配方原始 JSON，按配方名查找。

### `json_split`

按配方类型分子文件夹存放（类型名中的冒号会被替换为下划线）：

```
<世界目录>/RecipesExporter/<modid>/
  ├── crafting_shaped/
  │   ├── oak_planks.json
  │   └── diamond_sword.json
  └── smelting/
      └── iron_ingot_from_smelting_iron_ore.json
```

适合按类型分类整理，或在整合包制作中分析特定类型的配方。

### `kubejs`

将所有匹配的配方写入单个 `.js` 脚本文件，可直接用于 KubeJS 的 `ServerEvents.recipes`：

```
<世界目录>/RecipesExporter/
  ├── <modid>_recipes.js
  └── <modid>_recipes_<过滤类型>.js   （指定 recipe_type 时）
```

生成脚本格式：

```js
ServerEvents.recipes(event => {
  // namespace:recipe_id
  event.custom(
    { /* 配方 JSON */ }
  );
});
```

---

## 输出路径

所有文件导出至世界目录下的 `RecipesExporter/` 文件夹：

- **单机模式**：`saves/<世界名称>/RecipesExporter/`
- **专用服务器**：`world/RecipesExporter/`

若导出过程中有配方失败（找不到原始 JSON 文件），会在同目录生成失败日志：

```
<modid>_recipes_failed_log.txt
<modid>_<过滤类型>_recipes_failed_log.txt
```

---

## 技术说明

**配方数据来源：** 本模组通过 ResourceManager 直接读取模组资源包中的原始配方文件（`data/<modid>/recipes/<路径>.json`）。若某配方的原始文件已被其他模组覆盖，导出可能会失败并记录到日志中。

**性能提示：** 导出操作会阻塞服务端主线程。对于配方数量极多的模组（如上千个配方），执行时间可能达到数秒，请合理安排导出时机。

---

## 许可证

本项目采用 All Rights Reserved 许可。详见 [LICENSE.txt](LICENSE.txt)。

---

*作者：JuGeLiZizzz*
