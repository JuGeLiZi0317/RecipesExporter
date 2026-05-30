# Recipes Exporter

Recipes Exporter 是一个适用于 Minecraft 1.20.1 (Forge) 的服务端实用工具模组。它允许服务器管理员或整合包作者通过命令，直接从服务端资源管理器中无损提取指定模组的原始配方数据，并支持转换为多种格式以供分析或魔改（如 KubeJS 脚本）。

**纯服务端模组**：本模组仅需安装在服务端即可运行（单人游戏也可正常使用），客户端无需安装。

##  核心特性

* **无损抓取**：不依赖易丢失自定义字段的 Java 对象反序列化，而是直接截获模组 Jar 包中的原始 JSON 文件，确保 100% 的数据完整性。
* **多模式导出**：支持聚合单文件、按类型分文件夹以及直接生成 KubeJS 脚本。
* **精准过滤**：支持指定配方类型（如仅导出粉碎机配方），避免导出大量无关数据。
* **动态配方排查**：自动记录无法导出物理文件的动态/硬编码配方 ID，生成排查日志，告别“暗箱”操作。

##  命令使用指南

### 1. 导出命令

**语法**：`/recipesexporter export <modid> [export_type] [recipe_type]`
**权限**：OP 权限（等级 2）

* `<modid>`：**必填**。目标模组的命名空间（如 `minecraft`, `thermal`, `enderio`）。
* `[export_type]`：**可选**。导出模式（支持 Tab 补全）：
* `json_single` (默认)：将所有配方聚合为一个单独的 JSON 文件（例如 `thermal_all_recipes.json`）。
* `json_split`：按配方类型创建子文件夹，将每个配方存为独立的 JSON 文件。
* `kubejs`：将配方转换为 `ServerEvents.recipes` 格式的 `.js` 脚本文件，可直接放入 KubeJS 使用。


* `[recipe_type]`：**可选**。类型过滤器。例如输入 `smelting`，则仅导出熔炉相关的配方。

**示例**：

```mcfunction
# 将热力膨胀的所有配方导出为一个聚合的 JSON 文件
/recipesexporter export thermal

# 将原版合成表直接导出为 KubeJS 脚本
/recipesexporter export minecraft kubejs crafting_shaped

# 将 EnderIO 的合金炉配方按文件拆分导出
/recipesexporter export enderio json_split alloy_smelting

```

### 2. 辅助查询命令

不知道模组有哪些配方类型？使用此命令查询。
**语法**：`/recipeshelper gettypes <modid>`

**示例与输出**：
输入 `/recipeshelper gettypes thermal`，聊天栏将返回该模组注册的所有配方类型列表（如 `thermal:smelter`, `thermal:pulverizer` 等），方便你填入导出命令的 `[recipe_type]` 参数中。

##  输出文件结构

所有导出的文件均会安全地存放在当前世界目录下的 `RecipesExporter` 文件夹中，绝不会污染你的服务端根目录或 `.minecraft` 文件夹。

* **单人游戏路径**：`saves/<你的世界名>/RecipesExporter/`
* **专用服务器路径**：`<世界文件夹>/RecipesExporter/`

**导出结果示例**：

```text
RecipesExporter/
 ├── thermal_all_recipes.json                # (json_single 模式输出)
 ├── thermal/                                # (json_split 模式输出)
 │    ├── thermal_smelter/
 │    │    ├── glass.json
 │    │    └── ...
 ├── thermal_recipes.js                      # (kubejs 模式输出)
 └── thermal_recipes_failed_log.txt          # (失败日志，记录动态注入或硬编码的配方ID)

```

## ⚠ 注意事项

1. **失败日志 (`failed_log.txt`)**：
   当你看到提示有配方导出失败并生成了日志文件时，**这并非 Bug**。部分模组（如 Ender IO）会在游戏启动时读取原版熔炉配方并“在内存中动态生成”机器配方，或者某些配方被作者硬编码在了 Java 类中。由于它们不存在对应的物理 JSON 文件，工具会将其 ID 记录在日志中，方便你进行人工排查。
2. **性能消耗**：
   导出超大型模组（上千个配方）时，大规模的文件 I/O 会短暂占用服务端主线程，可能导致游戏出现一到两秒的停顿。建议在非玩家活跃时段执行导出操作。