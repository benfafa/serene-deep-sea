package com.fanqieyu.deepfocus

import androidx.annotation.DrawableRes

/**
 * 水族馆内容统一目录。
 *
 * 该文件是 Android 端「场景 / 水族住客」的唯一内容来源：
 *  - [scenes]：全部水域场景（原有 4 个 + 新增场景）。
 *  - [residents]：全部水族住客（原有 22 个 + 新增住客）。
 *  - 资源一律通过 drawable 名称引用，并在编译期由 R.drawable.* 校验，
 *    避免运行时因资源 ID 变化导致数据库里的图片失效。
 */
object AquariumCatalog {

    /**
     * 场景的 HUD 展示信息：主题色、水温、环境氛围，以及推荐搭配的住客。
     * themeColor 为 ARGB 十六进制色值，供 Compose 直接转成 Color。
     */
    data class SceneMeta(
        @androidx.annotation.ColorInt val themeColor: Int,
        val temperature: String,
        val ambience: String,
        val recommendedResidents: List<String>,
    )

    // ---------------------------------------------------------------------
    // 场景
    // ---------------------------------------------------------------------

    /** 全部水域场景，顺序即图鉴展示顺序。 */
    val scenes: List<AquariumScene> = listOf(
        // —— 原有场景 ——
        AquariumScene("月光海沟", "安静、深蓝，适合第一轮专注。", true, R.drawable.aquarium_tunnel_hero),
        AquariumScene("珊瑚花园", "明亮的浅海，适合下午灵感。", true, R.drawable.coral_garden),
        AquariumScene("蓝洞礁石", "海底拱门和深蓝水流，适合创意冲刺。", true, R.drawable.blue_cove),
        AquariumScene("极光洋流", "冷水海藻森林，适合长时间专注。", true, R.drawable.kelp_forest),

        // —— 新增场景 ——
        AquariumScene("沉船遗迹", "锈蚀船骸静卧沙地，冷蓝幽光，适合回忆整理。", true, R.drawable.sunken_ship_wreck),
        AquariumScene("深海热泉", "墨蓝海面下涌动着橙红热液，适合攻坚时刻。", true, R.drawable.hydrothermal_vents),
        AquariumScene("阳光海草湾", "浅绿通透的沙底海草，随波轻摆，适合轻量休憩。", true, R.drawable.seagrass_bay),
        AquariumScene("夜光水母洞窟", "荧光水母悬于岩洞，冷光呼吸，适合深度冥想。", true, R.drawable.bioluminescent_cave),
    )

    /** 按名称查找场景，名称未命中时返回 null（供数据库按名反查资源）。 */
    fun findSceneByName(name: String): AquariumScene? =
        scenes.firstOrNull { it.name == name }

    /** 场景的 HUD 元信息。 */
    val sceneVisualMeta: Map<String, SceneMeta> = mapOf(
        "月光海沟" to SceneMeta(
            themeColor = 0xFF0B2A3D.toInt(),
            temperature = "16°C",
            ambience = "深蓝静谧，只有微弱水光与远处潮声。",
            recommendedResidents = listOf("月光水母", "银色鲹鱼", "彩纹七彩神仙"),
        ),
        "珊瑚花园" to SceneMeta(
            themeColor = 0xFF14707A.toInt(),
            temperature = "25°C",
            ambience = "暖光穿过浅海，珊瑚枝随流轻摆。",
            recommendedResidents = listOf("小丑鱼", "黄尾刺尾鱼", "曼达林鱼"),
        ),
        "蓝洞礁石" to SceneMeta(
            themeColor = 0xFF0D3C63.toInt(),
            temperature = "21°C",
            ambience = "拱门之外是无边深蓝，水流自洞口涌出。",
            recommendedResidents = listOf("蓝吊鱼", "皇家神仙鱼", "长吻蝶鱼"),
        ),
        "极光洋流" to SceneMeta(
            themeColor = 0xFF123F49.toInt(),
            temperature = "12°C",
            ambience = "冷水海藻森林，光束自水面斜切而下。",
            recommendedResidents = listOf("海马", "火焰虾虎鱼", "蓝贻贝"),
        ),
        "沉船遗迹" to SceneMeta(
            themeColor = 0xFF16242E.toInt(),
            temperature = "8°C",
            ambience = "锈蚀船骸半掩沙地，细小浮游在暗光里漂浮。",
            recommendedResidents = listOf("章鱼", "狮子鱼", "蓝贻贝"),
        ),
        "深海热泉" to SceneMeta(
            themeColor = 0xFF2A1206.toInt(),
            temperature = "45°C",
            ambience = "墨蓝海床上升起硫化物烟囱，热液泛出橙红微光。",
            recommendedResidents = listOf("大砗磲贝", "鹦鹉螺", "花斑石斑鱼"),
        ),
        "阳光海草湾" to SceneMeta(
            themeColor = 0xFF1E7A63.toInt(),
            temperature = "26°C",
            ambience = "浅绿通透，沙底海草成片起伏，光斑游动。",
            recommendedResidents = listOf("绿海龟", "斑马倒吊鱼", "海星"),
        ),
        "夜光水母洞窟" to SceneMeta(
            themeColor = 0xFF1A2140.toInt(),
            temperature = "6°C",
            ambience = "荧光水母悬于岩洞，冷光一呼一吸。",
            recommendedResidents = listOf("灯塔水母", "海天使鱼", "幼年鲸鲨"),
        ),
    )

    // ---------------------------------------------------------------------
    // 水族住客
    // ---------------------------------------------------------------------

    /** 全部水族住客，顺序即商店 / 鱼苗图鉴的展示顺序。 */
    val residents: List<Fish> = listOf(
        // —— 原有住客（名称、描述、资源与 MainActivity 中完全一致）——
        Fish("小丑鱼", "会在珊瑚和海葵之间巡游的活泼住客。", "活泼", "常见", "浅海", 78, R.drawable.clownfish),
        Fish("蓝吊鱼", "喜欢开阔水域，会在水流里保持自己的节奏。", "好奇", "稀有", "外海", 64, R.drawable.blue_tang),
        Fish("皇家神仙鱼", "会在礁石边缘缓慢巡航，喜欢稳定的水流。", "优雅", "稀有", "珊瑚礁", 71, R.drawable.emperor_angelfish),
        Fish("黄尾刺尾鱼", "明亮的黄色住客，常在珊瑚边缘来回穿梭。", "明亮", "常见", "浅海", 69, R.drawable.yellow_tang),
        Fish("狮子鱼", "长鳍会随着水流展开，适合安静观察。", "缓慢", "传说", "礁石洞", 59, R.drawable.lionfish),
        Fish("银色鲹鱼", "喜欢和同伴保持距离一致的群游住客。", "群游", "常见", "外海", 73, R.drawable.silver_jack),
        Fish("月光水母", "随着水流缓慢漂浮的夜行住客。", "安静", "传说", "深水", 86, R.drawable.moon_jellyfish, "其他"),
        Fish("粉珊瑚鱼", "慢热的住客，喜欢珊瑚枝之间的阴影。", "慢热", "稀有", "珊瑚礁", 68, R.drawable.pink_coral_fish),
        Fish("曼达林鱼", "细小而华丽，喜欢在珊瑚底部寻找食物。", "谨慎", "传说", "珊瑚礁", 82, R.drawable.mandarin_fish),
        Fish("花马蹄鱼", "黑白黄相间的长鳍住客，喜欢慢慢巡游。", "优雅", "稀有", "外海", 74, R.drawable.moorish_idol),
        Fish("长吻蝶鱼", "会在珊瑚枝间穿梭，动作轻巧。", "灵巧", "稀有", "浅海", 70, R.drawable.butterflyfish),
        Fish("绿斑河豚", "会用圆滚滚的身体观察周围的水流。", "好奇", "传说", "礁石洞", 77, R.drawable.pufferfish),
        Fish("皇家丝绒鱼", "紫黄渐变的迷你住客，适合小型珊瑚区。", "安静", "稀有", "珊瑚礁", 66, R.drawable.royal_gramma),
        Fish("火焰虾虎鱼", "橙红色长鳍住客，喜欢洞穴边缘。", "警觉", "常见", "礁石洞", 61, R.drawable.firefish_goby),
        Fish("彩纹七彩神仙", "圆润的淡水住客，喜欢稳定缓慢的水流。", "温和", "稀有", "淡水", 72, R.drawable.discus),
        Fish("海马", "会用卷曲尾巴抓住水草，缓慢漂浮。", "安静", "传说", "海草湾", 84, R.drawable.seahorse, "其他"),
        Fish("扇贝", "会在沙地边缘轻轻开合贝壳，反射水面光。", "安静", "常见", "沙地", 62, R.drawable.scallop_shell, "贝类"),
        Fish("蓝贻贝", "喜欢附着在礁石上，形成小型底栖群落。", "群居", "常见", "礁石", 58, R.drawable.mussel_cluster, "贝类"),
        Fish("牡蛎", "粗糙外壳里藏着柔和的珍珠光泽。", "安静", "稀有", "礁石", 65, R.drawable.oyster_shell, "贝类"),
        Fish("鲍鱼", "彩虹色贝壳会在低光水域里反射细碎光线。", "慢热", "稀有", "礁石洞", 76, R.drawable.abalone_shell, "贝类"),
        Fish("海螺", "螺旋贝壳会在沙地上留下缓慢移动的轨迹。", "慢速", "常见", "沙地", 56, R.drawable.conch_shell, "贝类"),
        Fish("寄居蟹", "会背着贝壳探索水族房的底部角落。", "好奇", "常见", "沙地", 63, R.drawable.hermit_crab, "贝类"),

        // —— 新增住客：鱼类 ——
        Fish("魔鬼鱼", "宽大的胸鳍划开深蓝水流，是最安静的远洋住客。", "优雅", "传说", "外海", 79, R.drawable.manta_ray),
        Fish("幼年鲸鲨", "幼年时以浮游生物为食，会跟着洋流慢慢长大。", "温和", "传说", "深海", 74, R.drawable.whale_shark_pup),
        Fish("花斑石斑鱼", "沉稳地停在礁石边，一旦入水便耐心等待猎物。", "沉稳", "稀有", "礁石洞", 70, R.drawable.grouper),
        Fish("海天使鱼", "半透明的身形像一条游动的丝带，几乎不发出声音。", "安静", "稀有", "深水", 83, R.drawable.sea_angel),
        Fish("蓝纹神仙鱼", "宝石蓝的侧扁身影像一枚缓慢旋转的蓝鳍旗。", "优雅", "稀有", "珊瑚礁", 72, R.drawable.blue_angel),
        Fish("斑马倒吊鱼", "竖直的银灰身影像一枚挂着细纹的小圆盘。", "灵巧", "常见", "浅海", 66, R.drawable.zebra_tang),

        // —— 新增住客：其他 ——
        Fish("绿海龟", "会用前鳍划水，贴着沙底慢慢巡航的温和住客。", "温和", "稀有", "海草湾", 77, R.drawable.green_sea_turtle, "其他"),
        Fish("章鱼", "会钻进沉船缝隙，用腕足试探每一块石头。", "好奇", "传说", "礁石洞", 88, R.drawable.octopus, "其他"),
        Fish("海星", "移动极慢，却总能悄悄靠近沉在沙底的食物。", "慢速", "常见", "沙地", 60, R.drawable.starfish, "其他"),
        Fish("灯塔水母", "伞盖边缘泛着淡蓝荧光，像悬在深水里的呼吸灯。", "安静", "传说", "深水", 85, R.drawable.crystal_jellyfish, "其他"),

        // —— 新增住客：贝类 ——
        Fish("大砗磲贝", "厚重的扇形巨贝张开后，壳内是一片蓝紫虹彩。", "安静", "传说", "珊瑚礁", 81, R.drawable.giant_clam, "贝类"),
        Fish("珍珠贝", "银白扇壳里藏着一层柔和珠光，安静地滤食。", "慢热", "稀有", "沙地", 68, R.drawable.pearl_oyster, "贝类"),
        Fish("鹦鹉螺", "用螺旋小舱调节浮沉，是活着的远古时光。", "古老", "传说", "深海", 89, R.drawable.nautilus, "贝类"),
        Fish("火焰贝", "锥形壳上的火焰花纹，在暖光下会格外醒目。", "灵巧", "稀有", "礁石", 64, R.drawable.cone_shell, "贝类"),
    )

    /** 按名称查找住客，名称未命中时返回 null（数据库按名反查资源的入口）。 */
    fun findFishByName(name: String): Fish? =
        residents.firstOrNull { it.name == name }

    /** 按分类筛选住客。 */
    fun residentsOf(category: String): List<Fish> =
        residents.filter { it.category == category }

    /**
     * 默认初始投放的住客。
     *
     * 与 MainActivity 早期版本的默认值保持一致（小丑鱼 + 蓝吊鱼），
     * 集成方可直接用它在数据库为空时兜底。
     */
    val defaultDeployedFish: List<Fish> = listOf(
        findFishByName("小丑鱼")!!,
        findFishByName("蓝吊鱼")!!,
    )

    /**
     * 随机抽取初始投放组合：从全部住客中不重复地取 [count] 条。
     * 用于「首次进入时随机布置一缸」这类玩法。
     */
    fun randomDeployment(count: Int, seed: Long = System.nanoTime()): List<Fish> {
        val pool = residents.toMutableList()
        val picked = mutableListOf<Fish>()
        val random = kotlin.random.Random(seed)
        repeat(count.coerceAtMost(pool.size)) {
            picked += pool.removeAt(random.nextInt(pool.size))
        }
        return picked
    }

    // ---------------------------------------------------------------------
    // 定价
    // ---------------------------------------------------------------------

    /** 稀有度对应的基础价。 */
    private val rarityBasePrice: Map<String, Int> = mapOf(
        "常见" to 120,
        "稀有" to 240,
        "传说" to 400,
    )

    /**
     * 按稀有度与亲和度定价：稀有度决定底价，亲和度越高价格越高。
     */
    fun priceOf(fish: Fish): Int {
        val base = rarityBasePrice[fish.rarity] ?: rarityBasePrice["常见"]!!
        val affinityBonus = (fish.bond - 60).coerceAtLeast(0) * when (fish.rarity) {
            "传说" -> 5
            "稀有" -> 4
            else -> 3
        }
        return base + affinityBonus
    }

    /** 名称 → 价格的映射，供商店 UI 直接取价。 */
    val fishPriceMap: Map<String, Int> = residents.associate { it.name to priceOf(it) }

    // ---------------------------------------------------------------------
    // 生物朝向、动作类型与尺寸元数据
    // ---------------------------------------------------------------------

    enum class CreatureMotionStyle {
        SWIM,
        JELLYFISH,
        SEAHORSE,
        BOTTOM_STILL,
        BOTTOM_CRAWL,
    }

    private val leftFacingFishNames: Set<String> = setOf(
        "银色鲹鱼",
        "皇家丝绒鱼",
        "魔鬼鱼",
        "幼年鲸鲨",
        "海天使鱼",
        "蓝纹神仙鱼",
        "斑马倒吊鱼",
        "绿海龟",
    )

    /** 原始 PNG 素材是否默认头朝右。 */
    fun isDefaultFacingRight(fishName: String): Boolean = fishName !in leftFacingFishNames

    /** 根据物种返回自然运动风格。 */
    fun motionStyleOf(fish: Fish): CreatureMotionStyle = when {
        fish.name == "月光水母" || fish.name == "灯塔水母" -> CreatureMotionStyle.JELLYFISH
        fish.name == "海马" -> CreatureMotionStyle.SEAHORSE
        fish.name in setOf("寄居蟹", "海螺", "海星", "章鱼") -> CreatureMotionStyle.BOTTOM_CRAWL
        fish.category == "贝类" -> CreatureMotionStyle.BOTTOM_STILL
        else -> CreatureMotionStyle.SWIM
    }

    /** 区分物种体型的显示尺寸（dp）。 */
    fun displaySizeDp(fish: Fish): Int = when {
        fish.name in setOf("魔鬼鱼", "幼年鲸鲨", "绿海龟") -> 104
        fish.name in setOf("狮子鱼", "皇家神仙鱼", "花马蹄鱼", "花斑石斑鱼", "彩纹七彩神仙", "章鱼") -> 78
        fish.name in setOf("月光水母", "灯塔水母", "海马", "海天使鱼", "蓝吊鱼", "蓝纹神仙鱼") -> 72
        fish.category == "贝类" || fish.name == "海星" -> 54
        else -> 62
    }
}
