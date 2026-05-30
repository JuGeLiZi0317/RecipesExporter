====================================================
          Recipes Exporter — 配方导出模组
====================================================

作者: JuGeLiZizzz
版本: 1.0.0
适用游戏: Minecraft 1.20.1
模组加载器: Forge 47.4.10

------------------
1.  项目简介
------------------

Recipes Exporter 是一个 Minecraft 1.20.1 Forge 服务端实用模组。
它允许服务器管理员通过命令，从内存中读取指定模组的所有配方数据，
并以多种格式导出到 JSON 文件中。

此模组**纯服务端**即可运行，客户端无需安装。
所有导出操作均通过命令完成，无需任何客户端 GUI。

------------------
2.  功能特性
------------------

  ✔ 三种导出模式
    - json_single : 每个配方导出为独立的 JSON 文件，平铺存放
    - json_split  : 按配方类型 (crafting/shaped, smelting, blasting 等)
                    分子文件夹存放 JSON
    - kubejs      : 生成 KubeJS 格式的 .js 脚本文件，
                    可直接用于 KubeJS 模组的 ServerEvents.recipes

  ✔ 配方类型过滤
    - 支持按 recipe_type 进一步过滤，只导出指定类型的配方
    - 例如只导出 crafting_shaped 类型的配方

  ✔ 辅助查询命令
    - /recipeshelper gettypes <modid> 可查询指定模组包含的所有配方类型

  ✔ 失败日志
    - 当某个配方的原始 JSON 文件在资源包中找不到时，
      自动生成失败日志文件，列出所有失败配方 ID

  ✔ 输出路径安全
    - 所有导出文件存放在世界目录下的 RecipesExporter/ 文件夹中，
      不会污染 .minecraft 或服务端根目录

------------------
3.  命令参考
------------------

命令格式:

  /recipesexporter export <modid> [export_type] [recipe_type]

参数说明:

  <modid>          — 必填。要导出配方来源模组的 ID（如 minecraft、thermal 等）
  [export_type]    — 可选。导出模式，支持 Tab 补全，可选值:
                       json_single  (默认值)
                       json_split
                       kubejs
  [recipe_type]    — 可选。配方类型过滤（详见第 5 节）
                     例如: crafting_shaped, minecraft:smelting 等

权限要求: OP 权限等级 2 或更高

简单使用示例:

  # 导出 minecraft 的所有配方 (默认 json_single 模式)
  /recipesexporter export minecraft

  # 以 json_split 模式导出 thermal 的配方
  /recipesexporter export thermal json_split

  # 导出 minecraft 的合成配方到 KubeJS 脚本
  /recipesexporter export minecraft kubejs crafting_shaped

  # 导出 thermal 的熔炉配方 (json_single 模式)
  /recipesexporter export thermal json_single minecraft:smelting

------------------
4.  三种导出模式详解
------------------

(1) json_single (默认模式)

  将所有匹配的配方导出为独立的 .json 文件，
  直接存放在:
    <世界目录>/RecipesExporter/<modid>/

  文件命名规则: <配方路径>.json
  例如:
    oak_planks.json
    diamond_sword.json
    furnace.json

  适用场景: 需要快速批量获取配方原始 JSON，按配方名查找。

(2) json_split

  按配方的类型 (recipe type) 分子文件夹存放，
  路径结构:
    <世界目录>/RecipesExporter/<modid>/
      crafting_shaped/
        oak_planks.json
        diamond_sword.json
      smelting/
        iron_ingot_from_smelting_iron_ore.json
      blasting/
        ...

  注: recipe type 中的冒号会被替换为下划线 (例如 minecraft:crafting_shaped → crafting_shaped)

  适用场景: 需要按配方类型分类整理；或在整合包制作中分析特定类型的配方。

(3) kubejs

  将所有匹配的配方写入一个 .js 脚本文件，
  路径:
    <世界目录>/RecipesExporter/<modid>_recipes.js

  如果指定了 recipe_type 过滤，则文件名为:
    <modid>_recipes_<过滤类型>.js

  生成的脚本格式:
    ServerEvents.recipes(event => {
      // <recipe_id>
      event.custom(
        { ... 配方 JSON ... }
      );

      // <recipe_id>
      event.custom(
        { ... 配方 JSON ... }
      );
    });

  适用场景: 配合 KubeJS 模组使用，快速将其他模组的配方导入到整合包中。

------------------
5.  辅助命令
------------------

  /recipeshelper gettypes <modid>

  作用: 列出指定模组所有注册的配方类型 (recipe type)。
  示例:
    /recipeshelper gettypes minecraft

  输出 (聊天栏):
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

  这个命令可以帮助确定 [recipe_type] 过滤参数应该填什么值。

------------------
6.  输出路径说明
------------------

所有文件导出到世界目录下的 RecipesExporter/ 文件夹:

  单机模式: saves/<世界名称>/RecipesExporter/
  专用服务器: world/RecipesExporter/

路径结构示例:

  <世界目录>/
    RecipesExporter/
      minecraft/
        oak_planks.json
        diamond_sword.json
        ...
      minecraft_crafting_shaped/         (使用 recipe_type 过滤时)
        oak_planks.json
        ...
      minecraft_recipes.js               (KubeJS 模式)
      minecraft_recipes_crafting_shaped.js (KubeJS + 类型过滤)

如果导出过程中有配方失败（找不到原始 JSON 文件），
会在同一目录下生成失败日志文件:
  <modid>_recipes_failed_log.txt
  (或 <modid>_<过滤类型>_recipes_failed_log.txt)

------------------
7.  安装要求
------------------

  - Minecraft: 1.20.1
  - Forge: 47.4.10 (或兼容版本)
  - 权限: 服务器控制台或 OP 玩家（权限等级 2 或以上）

安装方法:
  将编译好的 recipesexporter-1.0.0.jar 放入服务端的 mods/ 文件夹即可。
  客户端无需安装此模组。

------------------
8.  技术说明
------------------

配方数据来源:
  本模组通过 GameTest 框架的 ResourceManager 直接读取
  模组资源包中的原始 recipe JSON 文件:
    data/<modid>/recipes/<路径>.json

  模组会尝试从内存中的 ResourceManager 获取资源，
  如果某个配方的原始 JSON 文件已被其他模组修改或覆盖，
  导出可能会失败并记录到失败日志中。

导出模式选择建议:
  - 普通分析/备份: 使用 json_single
  - 按类型分类整理: 使用 json_split
  - 配合 KubeJS 使用: 使用 kubejs

关于服务端性能:
  导出操作会阻塞服务端主线程。
  对于配方数量极多的模组（如上千个配方），执行时间可能达到数秒，
  请合理安排导出时机。

------------------
9.  开源许可
------------------

本项目采用 All Rights Reserved 许可。
详情请参阅项目根目录下的 LICENSE.txt 文件。