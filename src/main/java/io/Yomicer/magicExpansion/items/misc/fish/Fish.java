package io.Yomicer.magicExpansion.items.misc.fish;

import io.Yomicer.magicExpansion.utils.ColorGradient;

import java.util.Random;

public enum Fish {
    SanWenFish(Rarity.COMMON.colorCode+"Salmon", 5.0, 31.0, Rarity.COMMON,
            ("§x§6§B§E§E§D§1Regular in rivers and seas."),
            ("§x§B§F§A§A§D§0Tasty meat, loved by many."),
            ("§x§F§F§3§2§C§ESalmon usually weighs between 5 - 31 kg.")
    ),
    XueFish(Rarity.COMMON.colorCode+"Cod", 2.0, 96.0, Rarity.COMMON,
            ("§x§6§B§E§E§D§1Lives in cold waters."),
            ("§x§B§F§A§A§D§0Good for making fish sticks."),
            ("§x§F§F§3§2§C§ECod usually weighs between 2 - 96 kg.")
    ),
    HeTun(Rarity.UNCOMMON.colorCode+"Pufferfish", 0.2, 1.5, Rarity.UNCOMMON,
            ("§x§6§B§E§E§D§1Careful! It puffs up!"),
            ("§x§B§F§A§A§D§0Said to be tasty but very poisonous..."),
            ("§x§F§F§3§2§C§EPufferfish usually weighs between 0.2 - 1.5 kg.")
    ),
    ReDaiFish(Rarity.RARE.colorCode+"Tropical Fish", 0.1, 4, Rarity.RARE,
            ("§x§6§B§E§E§D§1A small, colorful fish."),
            ("§x§B§F§A§A§D§0Only shows up in warm waters."),
            ("§x§F§F§3§2§C§ETropical Fish usually weighs between 0.1 - 4 kg.")
    ),

    TestFish(Rarity.RARE.colorCode+"Test Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1A small fish with a metallic shine that sprinkles tiny copper-colored specks as it swims."),
            ("§x§B§0§C§5§D§0Legend says it was born from seepage of an old copper mine, and it only survives in mineral-rich waters."),
            ("§x§C§E§8§7§A§8Can be produced steadily in a vivarium."),
            ("§x§F§F§3§2§C§ECopper Dust Fish usually weighs between 1 - 64 kg.")
    ),


    /**
     * Dust Fish - Rare
     */

    CopperDustFish(Rarity.RARE.colorCode+"Red Scale Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1A small fish with a metallic shine that sprinkles tiny copper-colored specks as it swims."),
            ("§x§A§A§C§D§D§0Legend says it was born from seepage of an old copper mine, and it only survives in mineral-rich waters."),
            ("§x§B§F§B§A§D§0Can be produced steadily in a vivarium."),
            ("§x§9§C§5§2§8§BCopper Dust Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Copper Dust")
    ),

    GoldDustFish(Rarity.RARE.colorCode + "Gold Sand Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Golden all over, scales flow like molten gold, leaving a faint trail of light as it swims."),
            ("§x§A§A§C§D§D§0Thought to be the embodiment of the earth's gold veins, only appears in waters near hot rock layers."),
            ("§x§B§F§B§A§D§0Can be produced steadily in a vivarium."),
            ("§x§9§C§5§2§8§BGold Sand Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Gold Dust")
    ),

    IronDustFish(Rarity.RARE.colorCode + "Iron Scale Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Silver-gray scales as hard as iron, making a faint metal scraping sound as it swims."),
            ("§x§A§A§C§D§D§0Often found near shipwrecks, eats rusty metal, and is very tough."),
            ("§x§B§F§B§A§D§0Can be produced steadily in a vivarium."),
            ("§x§9§C§5§2§8§BIron Scale Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Iron Dust")
    ),

    TinDustFish(Rarity.RARE.colorCode + "Tin Spot Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Covered in silvery-white spots, rolls like mercury as it swims, with a faint blue shine."),
            ("§x§A§A§C§D§D§0Likes acidic water, often shows up in groups near the drains of abandoned smelters."),
            ("§x§B§F§B§A§D§0Can be produced steadily in a vivarium."),
            ("§x§9§C§5§2§8§BTin Spot Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Tin Dust")
    ),

    SilverDustFish(Rarity.RARE.colorCode + "Silver Glow Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Smooth like a mirror all over, reflecting a dreamy silver glow under the moonlight."),
            ("§x§A§A§C§D§D§0Legend says it formed from silver ore crystallizing on a full moon night, and it is terrified of sunlight."),
            ("§x§B§F§B§A§D§0Can be produced steadily in a vivarium."),
            ("§x§9§C§5§2§8§BSilver Glow Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Silver Dust")
    ),

    AluminumDustFish(Rarity.RARE.colorCode + "Aluminum Shadow Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Light as a shadow, pale silver-white, barely disturbs the water surface."),
            ("§x§A§A§C§D§D§0An accidental product of modern industry, can survive and breed in highly alkaline wastewater."),
            ("§x§B§F§B§A§D§0Can be produced steadily in a vivarium."),
            ("§x§9§C§5§2§8§BAluminum Shadow Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Aluminum Dust")
    ),

    LeadDustFish(Rarity.RARE.colorCode + "Lead Poison Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Dull like lead, gives off a faint purple poison mist that other creatures stay away from."),
            ("§x§A§A§C§D§D§0Born in badly polluted water, its body is full of heavy metal toxins."),
            ("§x§B§F§B§A§D§0Can be produced steadily in a vivarium."),
            ("§x§9§C§5§2§8§BLead Poison Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Lead Dust")
    ),

    ZincDustFish(Rarity.RARE.colorCode + "Zinc Flower Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Its body is covered in blue-green oxide patterns and sheds tiny crystals as it swims."),
            ("§x§A§A§C§D§D§0Common near old galvanized pipes, feeds on corrosion."),
            ("§x§B§F§B§A§D§0Can be produced steadily in a vivarium."),
            ("§x§9§C§5§2§8§BZinc Flower Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Zinc Dust")
    ),

    MagnesiumDustFish(Rarity.RARE.colorCode + "Magnesium Glow Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Full of flammable stuff inside, flashes a blinding white light when startled."),
            ("§x§A§A§C§D§D§0Very active, often jumps out of the water in thunderstorms, as if absorbing electricity."),
            ("§x§B§F§B§A§D§0Can be produced steadily in a vivarium."),
            ("§x§9§C§5§2§8§BMagnesium Glow Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Magnesium Dust")
    ),


    /**
     * Ore Fish - Rare
     */

    // [CoalFish]makes: Coal (Coal)
    CoalFish(Rarity.RARE.colorCode + "Coal Crystal Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Tiny coal crystals form inside it, and it throws sparks when squeezed."),
            ("§x§A§A§C§D§D§0Often swims in underground rivers, likes to stay near lava to keep warm."),
            ("§x§B§F§B§A§D§0Its waste can be used directly to make fuel."),
            ("§x§9§C§5§2§8§BCoal Crystal Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Coal")
    ),
    // [EmeraldFish]makes: Emerald (Emerald)
    EmeraldFish(Rarity.RARE.colorCode + "Emerald Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Scales are clear like emeralds, reflecting the light and shadow of the deep jungle."),
            ("§x§A§A§C§D§D§0Only seen now and then in water near enchanting tables."),
            ("§x§B§F§B§A§D§0Said to raise the enchant chance of nearby items."),
            ("§x§9§C§5§2§8§BEmerald Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Emerald")
    ),
    // [LapisFish]makes: Lapis Lazuli (Lapis Lazuli)
    LapisFish(Rarity.RARE.colorCode + "Lapis Spirit Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Deep blue mineral fluid flows inside it, calm like the night sky."),
            ("§x§A§A§C§D§D§0Gives off faint magic waves as it swims, which mess with redstone signals."),
            ("§x§B§F§B§A§D§0Its secretions are the key ingredient of top-tier enchanting dye."),
            ("§x§9§C§5§2§8§BLapis Spirit Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Lapis Lazuli")
    ),
    // [DiamondFish]makes: Diamond (Diamond)
    DiamondFish(Rarity.RARE.colorCode + "Diamond Shine Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Covered in diamond-like crystal scales, nearly as hard as minerals from the earth's core."),
            ("§x§A§A§C§D§D§0Only survives steadily in high-pressure crystal caves."),
            ("§x§B§F§B§A§D§0Can be raised in a vivarium that copies the deep crust environment."),
            ("§x§9§C§5§2§8§BDiamond Shine Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Diamond")
    ),
    // [QuartzFish]makes: Nether Quartz (Nether Quartz)
    QuartzFish(Rarity.RARE.colorCode + "Flame Crystal Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1A red quartz core burns nonstop inside it, giving off heat all the time."),
            ("§x§A§A§C§D§D§0Often swims near Nether portals, seems to sense dimension ripples."),
            ("§x§B§F§B§A§D§0A steady source of the quartz needed for precise redstone devices."),
            ("§x§9§C§5§2§8§BFlame Crystal Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Nether Quartz")
    ),
    // [AmethystFish]makes: Amethyst Shard (Amethyst Shard)
    AmethystFish(Rarity.RARE.colorCode + "Tremor Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Natural amethyst clusters grow on its head, and it is very sensitive to thunderstorm frequencies."),
            ("§x§A§A§C§D§D§0In thunderstorms they resonate together with a clear ringing sound."),
            ("§x§B§F§B§A§D§0Can be used for signal triggers and timer modules in automation systems."),
            ("§x§9§C§5§2§8§BTremor Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Amethyst Shard")
    ),
    // [IronFish]makes: Iron Ingot (Iron Ingot)
    IronFish(Rarity.RARE.colorCode + "Iron Core Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Full of magnetic iron ore inside, which can naturally gather into iron blocks."),
            ("§x§A§A§C§D§D§0Often moves in groups, making a moving mini magnetic field."),
            ("§x§B§F§B§A§D§0The steadiest iron supplier in vivarium smelting systems."),
            ("§x§9§C§5§2§8§BIron Core Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Iron Ingot")
    ),
    // [GoldFish]makes: Gold Ingot (Gold Ingot)
    GoldFish(Rarity.RARE.colorCode + "Gold Core Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Liquid gold flows inside it, reflecting a royal glow in the sunlight."),
            ("§x§A§A§C§D§D§0Very sensitive to redstone energy, often used in auto-trading systems."),
            ("§x§B§F§B§A§D§0Feed it pumpkin pie to boost gold ingot output."),
            ("§x§9§C§5§2§8§BGold Core Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Gold Ingot")
    ),
    // [CopperFish]makes: Copper Ingot (Copper Ingot)
    CopperFish(Rarity.RARE.colorCode + "Copper Vein Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Has a natural network of copper veins inside that conducts weak current."),
            ("§x§A§A§C§D§D§0Its scales oxidize with the environment, slowly changing from red to green."),
            ("§x§B§F§B§A§D§0A renewable source of materials for redstone circuits and lightning rods."),
            ("§x§9§C§5§2§8§BCopper Vein Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Copper Ingot")
    ),
    // [Pulse Fish]makes: Redstone Dust (Redstone Dust)
    RedstoneFish(Rarity.RARE.colorCode + "Red Vein Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Lives where strong underground magnetic fields meet mineral pulses, with tiny redstone bacteria living inside it."),
            ("§x§A§A§C§D§D§0Its fins look like circuit board patterns, and its body flashes steady red pulses as it swims."),
            ("§x§B§F§B§A§D§0Its spine and organs are full of active redstone particles, a key material for power transfer and automation devices."),
            ("§x§9§C§5§2§8§BPulse Fish usually weigh between 1 - 64 kg. The faster the pulse, the purer the redstone."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Redstone Dust")
    ),
    // [NetheriteFish]makes: Netherite Ingot (Netherite Ingot)
    NetheriteFish(Rarity.RARE.colorCode + "Netherforged Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Formed from Nether flames fused with ancient alloy, wrapped in black fire."),
            ("§x§9§5§C§4§D§0Can steadily make netherite ingots in an upgraded vivarium furnace tank."),
            ("§x§B§F§B§A§D§0Immune to fire and explosions, the only living source of top-tier gear material."),
            ("§x§A§D§A§2§9§ELegend says a Netherforged Fish always weighs exactly 64 kg, because it is beyond ordinary measure."),
            ("§x§C§B§4§2§A§6But most Netherforged Fish weigh between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Netherite Ingot")
    ),
    // [GlowStoneFish]makes: Glowstone Dust (Glowstone Dust)
    GlowStoneDustFish(Rarity.RARE.colorCode + "Lantern Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Tiny glowing bacteria float inside it, like moving stars in the deep sea."),
            ("§x§9§5§C§4§D§0Only shows up in completely dark cave water, afraid of any light."),
            ("§x§B§F§B§A§D§0Its secretions are the key material for advanced lighting devices and magic amplifiers."),
            ("§x§A§D§A§2§9§ELantern Fish usually weigh between 1 - 64 kg, light as mist."),
            ("§x§C§B§4§2§A§6[Special effect] Makes: Glowstone Dust"),
            ("§x§F§F§3§2§C§E[Special effect] Can be used to make power")
    ),

    // [Plastic Spirit Fish]makes: Plastic Sheet (Plastic Paper)
    ShuLingYu(Rarity.RARE.colorCode + "Plastic Spirit Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1A network of micro plastic fibers wraps inside it and filters pollutants from the water."),
            ("§x§A§A§C§D§D§0Covered in see-through polymer scales that shine in rainbow colors."),
            ("§x§B§F§B§A§D§0Its secretions can be processed into plastic sheets, commonly used in industry."),
            ("§x§9§C§5§2§8§BPlastic Spirit Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Plastic Sheet")
    ),
    // [Uranium Core Fish]makes: Uranium Ingot (Uranium Ingot)
    UraniumFish(Rarity.RARE.colorCode + "Uranium Core Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Contains traces of radioactive isotopes, and its gills collect uranium from the water."),
            ("§x§A§A§C§D§D§0Deep green to jet black, gives off a faint glow, and feels warm when you get close."),
            ("§x§B§F§B§A§D§0A key raw material source for nuclear tech and advanced reactors."),
            ("§x§9§C§5§2§8§BUranium Core Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Uranium Ingot")
    ),
    // [Oil Rock Fish]makes: Crude Oil Bucket (Crude Oil Barrel)
    OilRockFish(Rarity.RARE.colorCode + "Oil Rock Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Lives in deep-sea oil vein rifts, and its body cavity naturally makes liquid hydrocarbons."),
            ("§x§A§A§C§D§D§0Skin like oil-soaked rock, slick and heavy, with a faint smell of petroleum."),
            ("§x§B§F§B§A§D§0Crude oil can be extracted from it steadily after catching it, an important living carrier for the energy industry."),
            ("§x§9§C§5§2§8§BOil Rock Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Crude Oil Bucket")
    ),
    // [Bubble Crystal Fish]makes: Foam Ingot (Foamium Ingot)
    FoamCrystalFish(Rarity.EPIC.colorCode + "Bubble Crystal Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Foam-crystal bacteria live inside it, and it releases tiny metal-filled bubbles when it breathes."),
            ("§x§A§A§C§D§D§0Like semi-solid foam all over, light and see-through, swells and sparkles in the light."),
            ("§x§B§F§B§A§D§0Its crystal glands can be refined into light but strong foam ingots, used to make radiation items."),
            ("§x§9§C§5§2§8§BBubble Crystal Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Foam Ingot")
    ),
    // [Obsidian Fish]makes: Black Diamond (Black Diamond)
    BlackDiamondFish(Rarity.EPIC.colorCode + "Obsidian Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Born in the high-pressure layer of the crust, its bones are made of natural nano-scale carbon crystals."),
            ("§x§A§A§C§D§D§0Body looks pieced together from broken obsidian, pitch black and deep, and flashes cold metal light when cut."),
            ("§x§B§F§B§A§D§0Sheds its crystal skin once every hundred years, which can be refined into black diamond harder than diamond."),
            ("§x§9§C§5§2§8§BObsidian Fish can weigh 1 - 64 kg. The heavier it is, the more crystallized."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Black Diamond")
    ),
    // [Acid Crystal Fish]makes: Sulfate (Sulfate Crystals)
    SulfateFish(Rarity.RARE.colorCode + "Acid Crystal Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Lives in volcanic acid lakes, and its glands turn sulfides into crystals."),
            ("§x§A§A§C§D§D§0Covered in tiny sulfate crystal clusters, gives off a faint sulfur smell when touched."),
            ("§x§B§F§B§A§D§0The crystals it gives off are an important material for making acid and fertilizer."),
            ("§x§9§C§5§2§8§BAcid Crystal Fish usually weighs between 1 - 64 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Sulfate")
    ),
    // [Crystal Scale Fish]makes: Silicon (Silicon)
    SiliconFish(Rarity.RARE.colorCode + "Crystal Scale Fish", 1.0, 64.0, Rarity.RARE,
            ("§x§6§B§E§E§D§1Lives in sandy, silicon-rich waters, and its gills absorb and purify silicon dioxide very well."),
            ("§x§A§A§C§D§D§0Scales are like tiny chips, neatly lined up, with a semiconductor shine in the sunlight."),
            ("§x§B§F§B§A§D§0The high-purity silicon crystals built up inside it are a key material for electronics and solar panels."),
            ("§x§9§C§5§2§8§BCrystal Scale Fish usually weigh between 1 - 64 kg. The older it is, the more silicified."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Silicon")
    ),
    // [Curse Spirit Fish]makes: 附魔瓶 (Bottled Enchanting)
    EnchantedBottleFish(Rarity.EPIC.colorCode + "Curse Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Swims between magic whirlpools and ancient rune circles, feeding on leaked enchant energy."),
            ("§x§9§5§C§4§D§0See-through all over, with countless tiny runes drifting inside it like fireflies that never go out."),
            ("§x§B§F§B§A§D§0Its body fluid holds unstable arcane essence that can condense into enchanting bottles with random magic."),
            ("§x§A§D§A§2§9§ECurse Spirit Fish are extremely rare. You need a magic fishing rod, or the runes fall apart and all the magic is lost."),
            ("§x§C§B§4§2§A§6Curse Spirit Fish usually weigh between 1 - 64 kg. The older it is, the stronger its curse."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Bottle o' Enchanting")
    ),




    //[Alloy Ingot Fish]

    // [Alloy Spirit Fish]makes: Reinforced Alloy Ingot
    ReinforcedAlloyFish(Rarity.EPIC.colorCode + "Reinforced Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Roams between high-pressure reactors and reinforcing energy fields, feeding on tempering radiation and structural stress."),
            ("§x§9§5§C§4§D§0Body like multi-layer composite armor, with reinforced patterns of dark red and silver-gray on the surface."),
            ("§x§B§F§B§A§D§0Its spine forms a 'Reinforced Alloy Ingot', the core base material of future tech."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, or the spirit fish destroys itself and the ingot core falls apart."),
            ("§x§C§B§4§2§A§6Reinforced Spirit Fish usually weigh between 1 - 64 kg. The heavier it is, the steadier its structure."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Reinforced Alloy Ingot")
    ),

    // [Hardened Spirit Fish]makes: Hardened Metal
    HardenedMetalFish(Rarity.EPIC.colorCode + "Hardened Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Lives between deep underground quench pools and high-pressure forging presses, tempered by alternating extreme cold and blazing fire."),
            ("§x§9§5§C§4§D§0Covered in a metal glaze as hard as ceramic, bitterly cold to the touch."),
            ("§x§B§F§B§A§D§0Crystallizes into 'Hardened Metal' inside, its surface cracked all over, yet unbreakable."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and it plays dead in a constant-temperature environment."),
            ("§x§C§B§4§2§A§6Hardened Spirit Fish usually weigh between 1 - 64 kg. The older it is, the thicker its glaze."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Hardened Metal")
    ),

    // [Damascus Spirit Fish]makes: Damascus Steel Ingot
    DamascusSoulFish(Rarity.EPIC.colorCode + "Damascus Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Born in a river of fire from thousand-layer forging, powered by the rhythm of fold-hammering and carbon crystal growth."),
            ("§x§9§5§C§4§D§0Like flowing watered steel all over, scales overlap layer by layer, shining in mixed dark blue and dark gold."),
            ("§x§B§F§B§A§D§0A 'Damascus Steel Ingot' sleeps inside it, a perfect blend of craft and soul."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and has to be caught quietly with old-style fishing gear."),
            ("§x§C§B§4§2§A§6Damascus Spirit Fish usually weigh between 1 - 64 kg. The more complex the pattern, the better the quality."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Damascus Steel Ingot")
    ),

    // [Steel Soul Fish]makes: Steel Ingot
    SteelSoulFish(Rarity.EPIC.colorCode + "Steel Soul Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Lives between furnace cores and cooling molten steel, fed by carbon fire breath and hammer rhythm."),
            ("§x§9§5§C§4§D§0Silver-gray with a blue tint, scales overlap like rolled steel plates, tough and unbreakable."),
            ("§x§B§F§B§A§D§0A 'Steel Ingot' core forms inside it, the living source of industry's backbone."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and staying quiet is the key to catching it."),
            ("§x§C§B§4§2§A§6Steel Soul Fish usually weigh between 1 - 64 kg. The heavier it is, the denser the steel."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Steel Ingot")
    ),

    // [Bronze Shadow Fish]makes: Bronze Ingot
    BronzeAncientFish(Rarity.EPIC.colorCode + "Bronze Shadow Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Moves between copper pipe corridors of ancient ruins and dark tin ore rivers, living on the rust of the ages."),
            ("§x§9§5§C§4§D§0Wears green rust armor, and makes a low bell chime echo as it swims, like history whispering."),
            ("§x§B§F§B§A§D§0A 'Bronze Ingot' grows in its belly, a sacred metal from the dawn of civilization."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and turns into copper dust and vanishes if disturbed even a little."),
            ("§x§C§B§4§2§A§6Bronze Shadow Fish usually weigh between 1 - 64 kg. The older it is, the deeper the rust color."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Bronze Ingot")
    ),

    // [Duralumin Sky Fish]makes: Duralumin Ingot
    HardlightAluFish(Rarity.EPIC.colorCode + "Duralumin Sky Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Soars between high-altitude storms and thunderclouds, feeding on lightning ions and light metal dust."),
            ("§x§9§5§C§4§D§0Silver-white and clear, like forged aviation alloy, edges thin as a blade, cutting the wind silently."),
            ("§x§B§F§B§A§D§0Crystallizes into a 'Duralumin Ingot' inside, light but unbreakable."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, or it vaporizes instantly."),
            ("§x§C§B§4§2§A§6Duralumin Sky Fish usually weigh between 1 - 64 kg. The lighter it is, the closer to a perfect alloy."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Duralumin Ingot")
    ),

    // [Silver Copper Spirit Fish]makes: Silver Copper Alloy Ingot
    SilverCopperFish(Rarity.EPIC.colorCode + "Silver Copper Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Sneaks around where moonlit silver ore streams meet copper veins, living on starlight and conductive particles."),
            ("§x§9§5§C§4§D§0Colored like moon-white silver water, leaves a faint electric trail as it swims, and tingles when touched."),
            ("§x§B§F§B§A§D§0A 'Silver Copper Alloy Ingot' forms inside it, with great conductivity, often used in high-end circuits."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and is completely invisible under cloudy skies or sunlight."),
            ("§x§C§B§4§2§A§6Silver Copper Spirit Fish usually weigh between 1 - 64 kg. The brighter it is, the higher the purity."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Silver Copper Alloy Ingot")
    ),

    // [Brass Chime Fish]makes: Brass Ingot
    BrassResonanceFish(Rarity.EPIC.colorCode + "Brass Chime Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Hides deep in the standing water of abandoned pipe organs and brass factories, powered by sound vibrations."),
            ("§x§9§5§C§4§D§0Dazzling golden, and makes a low horn-like hum as it swims."),
            ("§x§B§F§B§A§D§0Its resonance chamber forms a 'Brass Ingot', a perfect blend of music and metal."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, or it causes resonance and the fish bursts."),
            ("§x§C§B§4§2§A§6Brass Chime Fish usually weigh between 1 - 64 kg. The purer the hum, the better the quality."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Brass Ingot")
    ),

    // [Aluminum Brass Spirit Fish]makes: Aluminum Brass Ingot
    AluminumBrassFish(Rarity.EPIC.colorCode + "Aluminum Brass Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Lives between hot radiators and industrial cooling towers, feeding on heat flow and anti-corrosion energy."),
            ("§x§9§5§C§4§D§0Pale gold mixed with gray-white, with clear heat-sink-fin-like structure, slightly warm to the touch."),
            ("§x§B§F§B§A§D§0Crystallizes into an 'Aluminum Brass Ingot' inside, heat-resistant and great at conducting heat."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and sleeps and won't wake if the temperature difference is too small."),
            ("§x§C§B§4§2§A§6Aluminum Brass Spirit Fish usually weigh between 1 - 64 kg. The hotter it gets, the more active."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Aluminum Brass Ingot")
    ),

    // [Aluminum Bronze Spirit Fish]makes: Aluminum Bronze Ingot
    AluminumBronzeFish(Rarity.EPIC.colorCode + "Aluminum Bronze Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Swims between deep-sea high-pressure zones and bronze ruins, feeding on ocean current electrolysis and metal corrosion resistance."),
            ("§x§9§5§C§4§D§0Gold-brown mixed with silver-gray, scales look forged from corrosion-resistant alloy, with a faint cathode shine."),
            ("§x§B§F§B§A§D§0An 'Aluminum Bronze Ingot' forms inside it, a divine balance of strength and seawater resistance."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, or a swarm of electric eels will attack."),
            ("§x§C§B§4§2§A§6Aluminum Bronze Spirit Fish usually weigh between 1 - 64 kg. The steadier the shine, the purer the alloy."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Aluminum Bronze Ingot")
    ),

    // [Corinthian Bronze Spirit Fish]makes: Corinthian Bronze Ingot
    CorinthianBronzeFish(Rarity.EPIC.colorCode + "Corinthian Bronze Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Hides inside mechanical traps of lost temples, feeding on ancient magic inscriptions and metal memory."),
            ("§x§9§5§C§4§D§0Tiny Corinthian column patterns are carved on its body, with a mysterious rune shine showing through the verdigris."),
            ("§x§B§F§B§A§D§0A 'Corinthian Bronze Ingot' is sealed inside it, holding the metal consciousness of ancient intelligence."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, or it sets off a trap."),
            ("§x§C§B§4§2§A§6Corinthian Bronze Spirit Fish usually weigh between 1 - 64 kg. The more complex the pattern, the more valuable."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Corinthian Bronze Ingot")
    ),

    // [Solder Spirit Fish]makes: Solder Ingot
    SolderFlowFish(Rarity.EPIC.colorCode + "Solder Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Moves between abandoned circuit boards and molten tin pools, living on electron flow and low-temperature melting."),
            ("§x§9§5§C§4§D§0Soft like liquid metal, silver-gray with a rainbow shine, and can change shape freely."),
            ("§x§B§F§B§A§D§0A 'Solder Ingot' forms inside it, a bridge between circuits and souls."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and high heat makes it melt completely and escape."),
            ("§x§C§B§4§2§A§6Solder Spirit Fish usually weigh between 1 - 64 kg. The more flexible it is, the better it conducts."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Solder Ingot")
    ),

    // [Nickel Soul Fish]makes: Nickel Ingot
    NickelSpiritFish(Rarity.EPIC.colorCode + "Nickel Soul Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Lurks between the edge of the earth's core and meteor craters, feeding on geomagnetism and meteoric nickel crystals."),
            ("§x§9§5§C§4§D§0Silver-white with a green tint, disturbs the magnetic field as it swims and throws compasses off."),
            ("§x§B§F§B§A§D§0Holds a 'Nickel Ingot' inside, the mother of alloys, strong and rust-proof."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and is completely invisible in calm weather."),
            ("§x§C§B§4§2§A§6Nickel Soul Fish usually weigh between 1 - 64 kg. The stronger the magnetism, the rarer."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Nickel Ingot")
    ),

    // [Cobalt Flame Fish]makes: Cobalt Ingot
    CobaltFlameFish(Rarity.EPIC.colorCode + "Cobalt Flame Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Born between volcanic lava and radioactive ore veins, living on high heat and blue flame energy."),
            ("§x§9§5§C§4§D§0Deep blue like the night sky, scales burning with an unending pale blue flame."),
            ("§x§B§F§B§A§D§0A 'Cobalt Ingot' forms inside it, the heart of high-temperature alloys and batteries."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, or the flames hit back at the angler."),
            ("§x§C§B§4§2§A§6Cobalt Flame Fish usually weigh between 1 - 64 kg. The bigger the flame, the stronger the energy."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Cobalt Ingot")
    ),

    // [Ferrosilicon Spirit Fish]makes: Ferrosilicon
    SiliconIronFish(Rarity.EPIC.colorCode + "Ferrosilicon Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Lives between semiconductor scrap yards and blast furnace slag, feeding on silicon crystal growth and ferromagnetic energy."),
            ("§x§9§5§C§4§D§0Its body looks like wafer and metal fused together, reflecting rainbow-like interference colors."),
            ("§x§B§F§B§A§D§0Crystallizes into 'Ferrosilicon' inside, the foundation material of the electronics industry."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, or signal interference makes it destroy itself."),
            ("§x§C§B§4§2§A§6Ferrosilicon Spirit Fish usually weigh between 1 - 64 kg. The tidier it is, the higher the purity."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Ferrosilicon")
    ),

    // [Carbon Soul Fish]makes: Carbon Block
    CarbonSoulFish(Rarity.EPIC.colorCode + "Carbon Soul Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Swims between underground coal seams and diamond formation zones, feeding on high pressure and time-grown crystals."),
            ("§x§9§5§C§4§D§0Black as ink all over, scales stacked like layers of graphite, eyes sparkling like diamonds."),
            ("§x§B§F§B§A§D§0Compressed into a 'Carbon Block' inside, the shared origin of life and minerals."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and quickly breaks down under normal pressure."),
            ("§x§C§B§4§2§A§6Carbon Soul Fish usually weigh between 1 - 64 kg. The blacker and denser, the more valuable."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Carbon Block")
    ),

    // [Gilded Spirit Fish]makes: Gilded Iron Ingot
    GildedIronFish(Rarity.EPIC.colorCode + "Gilded Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Moves between the gold-leaf pools and iron beams of throne halls, feeding on glory and corrosion resistance."),
            ("§x§9§5§C§4§D§0Iron body wrapped in a flowing gold film, scattering golden dust as it swims."),
            ("§x§B§F§B§A§D§0A 'Gilded Iron Ingot' forms inside it, fancy outside and solid inside, a symbol of power and defense."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and blasphemers get burned by molten gold."),
            ("§x§C§B§4§2§A§6Gilded Spirit Fish usually weigh between 1 - 64 kg. The thicker the gold layer, the rarer."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Gilded Iron Ingot")
    ),

    // [Redstone Alloy Spirit Fish]makes: Redstone Alloy Ingot
    RedstoneAlloyFish(Rarity.EPIC.colorCode + "Redstone Alloy Spirit Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Born in an underground network where redstone pulses and metal wires weave together, living on signal flow."),
            ("§x§9§5§C§4§D§0Dark red and glowing, with pulse-like red light circling inside it, like a living circuit."),
            ("§x§B§F§B§A§D§0Crystallizes into a 'Redstone Alloy Ingot' inside, a bridge between automation and magic."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, or its pulses go haywire and it blows itself up."),
            ("§x§C§B§4§2§A§6Redstone Alloy Spirit Fish usually weigh between 1 - 64 kg. The steadier the pulse, the better the quality."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Redstone Alloy Ingot")
    ),

    // [Neptunium Shadow Fish]makes: Neptunium
    NeptuniumShadowFish(Rarity.EPIC.colorCode + "Neptunium Shadow Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Sneaks between nuclear reactor waste pools and deep underground radiation belts, feeding on slow neutron flow."),
            ("§x§9§5§C§4§D§0Shaped like a ghost, with a pale purple radiation glow flickering in its see-through body."),
            ("§x§B§F§B§A§D§0Holds the element 'Neptunium' inside, a living crystal of man-made transuranic metal."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and too much exposure causes gene mutation."),
            ("§x§C§B§4§2§A§6Neptunium Shadow Fish usually weigh between 1 - 64 kg. The stronger the glow, the higher the radiation."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Neptunium")
    ),

    // [Plutonium Heart Fish]makes: Plutonium
    PlutoniumCoreFish(Rarity.EPIC.colorCode + "Plutonium Heart Fish", 1.0, 64.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1Sleeps between the deepest earth core and abandoned reactor cores, living on decay heat and dark energy."),
            ("§x§9§5§C§4§D§0Dark red like a lava core, with a steadily pulsing blue light at its heart."),
            ("§x§B§F§B§A§D§0Holds 'Plutonium' inside, the ultimate form of destruction and energy."),
            ("§x§A§D§A§2§9§ENeeds a magic fishing rod, and after catching it can be sealed in any container."),
            ("§x§C§B§4§2§A§6Plutonium Heart Fish usually weigh between 1 - 64 kg. The stronger the pulse, the scarier the energy."),
            ("§x§F§F§3§2§C§E[Special effect] Makes: Plutonium")
    ),





    LegendaryLuFish(Rarity.LEGENDARY.colorCode+"Legendary Bass", 2.0, 31.0, Rarity.LEGENDARY,
            ("§x§6§B§E§E§D§1A legendary giant!"),
            ("§x§B§0§C§5§D§0Every angler's dream."),
            ("§x§C§E§8§7§A§8Said to bring good luck."),
            ("§x§F§F§3§2§C§ELegendary Bass usually weighs between 2 - 31 kg.")
    ),
    LegendaryEelFish(Rarity.LEGENDARY.colorCode+"Ether · Phantom Eel", 666.666, 888.888, Rarity.LEGENDARY,
            ("§x§6§B§E§E§D§1Can be used as a carrier for industrial power transfer!"),
            ("§x§8§A§E§0§D§1For special reasons, magicians found the one and only bait for this legendary fish."),
            ("§x§A§A§C§D§D§0That makes this legendary fish very easy to catch, but only compared to before anyone understood its habits."),
            ("§x§B§7§C§2§D§0The real catch chance is still pretty low."),
            ("§x§B§F§B§A§D§0By that logic it should count as an epic fish now."),
            ("§x§B§5§9§F§B§0But because of its absurd traits, it should still be ranked as a legendary fish."),
            ("§x§9§C§5§2§8§BCan store up to "+ Long.MAX_VALUE +" J of power"),
            ("§x§B§1§4§2§9§ANeeds a magic fishing rod, and this fish is quite interested in magic sugar."),
            ("§x§C§6§3§2§A§9Ether · Phantom Eel weighs between 666.666 ~ 888.888 kg."),
            " ",
            ("§x§D§B§2§2§B§8shift + right-click to set the power storage limit"),
            ("§x§F§F§3§2§C§ECurrent stored power: Not initialized")
    ),


    MYSTIC_EEL(Rarity.EPIC.colorCode+"Naked-back Eel", 2.0, 21.0, Rarity.EPIC,
            ("§x§6§B§E§E§D§1A shadow in the deep sea."),
            ("§x§9§5§C§4§D§0Gives off a faint blue glow."),
            ("§x§B§F§B§A§D§0Nobody knows where it comes from."),
            ("§x§A§D§A§2§9§ECan make up to 600 volts and 1 amp of current."),
            ("§x§C§B§4§2§A§6Naked-back Eel usually weighs between 2 - 21 kg."),
            ("§x§F§F§3§2§C§E[Special effect] Can be used to make power")
    );

    private final String displayName;
    private final double minWeight;
    private final double maxWeight;
    private final Rarity rarity;
    private final String[] loreLines;

    Fish(String displayName, double minWeight, double maxWeight, Rarity rarity, String... loreLines) {
        this.displayName = displayName;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
        this.rarity = rarity;
        this.loreLines = loreLines;
    }

    // Getters left out (same as above)
    public String getDisplayName() { return displayName; }
    public double getMinWeight() { return minWeight; }
    public double getMaxWeight() { return maxWeight; }
    public Rarity getRarity() { return rarity; }
    public String[] getLoreLines() { return loreLines.clone(); }



    public static Fish fromString(String name) {
        try {
            return Fish.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null; // or throw an exception
        }
    }


    /**
     * Make the weight with the "lighter = rarer" rule
     * Uses a skewed distribution: a higher chance of making small weights close to minWeight
     */
    public double rollWeight() {
        Random random = new Random();
        double unit = 1 - random.nextDouble();
        double skewed = Math.pow(unit, 3.9);
        return minWeight + (skewed * (maxWeight - minWeight));
    }

    public double rollWeightNew() {
        Random random = new Random();
        double unit = 1 - random.nextDouble();
        double skewed = Math.pow(unit, 4.5);
        double rand = random.nextDouble();
        double multiplier;
        if (rand < 0.40) {
            multiplier = 0.3;
        } else if (rand < 0.80) {
            multiplier = 0.7;
        } else if (rand < 0.90) {
            multiplier = 0.95;
        } else if (rand < 0.98) {
            multiplier = 0.97;
        } else {
            multiplier = 1.0;
        }
        return minWeight + (skewed * (maxWeight - minWeight)*multiplier);
    }
    public double rollWeightAdvanced() {
        Random random = new Random();
        double unit = 1 - random.nextDouble();
        double skewed = Math.pow(unit, 3.1);
        double rand = random.nextDouble();
        double multiplier;
        if (rand < 0.20) {
            multiplier = 0.3;
        } else if (rand < 0.40) {
            multiplier = 0.8;
        } else if (rand < 0.70) {
            multiplier = 0.97;
        } else if (rand < 0.80) {
            multiplier = 0.99;
        } else {
            multiplier = 1.0;
        }
        return minWeight + (skewed * (maxWeight - minWeight)*multiplier);
    }

    public double rollWeightUltra() {
        Random random = new Random();
        double unit = 1 - random.nextDouble();
        double skewed = Math.pow(unit, 2.1);
        double rand = random.nextDouble();
        double multiplier;
        if (rand < 0.10) {
            multiplier = 0.3;
        } else if (rand < 0.30) {
            multiplier = 0.8;
        } else if (rand < 0.60) {
            multiplier = 0.97;
        } else if (rand < 0.70) {
            multiplier = 0.99;
        } else {
            multiplier = 1.0;
        }
        return minWeight + (skewed * (maxWeight - minWeight)*multiplier);
    }

    /**
     * Work out the weight's "normalized percentage" in the range (0.0 ~ 1.0)
     * Note: the smaller the value, the lighter
     */
    public double getWeightPercent(double weight) {
        if (weight >= maxWeight) return 1.0;
        if (weight <= minWeight) return 0.0;
        return (weight - minWeight) / (maxWeight - minWeight);
    }

    /**
     * Decide the "weight rarity" from the weight
     */
    public WeightRarity getWeightRarity(double weight) {
        double percent = getWeightPercent(weight);
        return WeightRarity.fromPercent(percent);
    }

    // ======================
    // Inner enum: weight rarity (separate from base rarity)
    // ======================
    public enum WeightRarity {
        COMMON_FISH("§fCommon Fish", "§f",1),
        RARE_FISH("§eRare Fish", "§e",7),
        SUPER_RARE_FISH("§bSuper Rare Fish", "§b",15),
        MAX_WEIGHT_FISH("§c§lFish King", "§c§l", 9999);

        private final String displayName;
        private final String colorCode;
        private final int multiplier;

        WeightRarity(String displayName, String colorCode, int multiplier) {
            this.displayName = displayName;
            this.colorCode = colorCode;
            this.multiplier = multiplier;
        }

        public String getDisplayName() { return displayName; }
        public String getColorCode() { return colorCode; }
        public int getMultiplier() {
            return multiplier;
        }

        public static int getMultiplierByName(String name) {
            if (name == null) {
                return 1; // default multiplier
            }
            try {
                WeightRarity rarity = WeightRarity.valueOf(name);
                return rarity.getMultiplier();
            } catch (IllegalArgumentException e) {
                return 1; // invalid name, default to common
            }
        }

        /**
         * Decide the weight rarity from the normalized percentage (0.0~1.0)
         * Note: lighter = rarer → the smaller the percentage, the rarer
         */
        public static WeightRarity fromPercent(double percent) {
            if (percent == 1){
                return MAX_WEIGHT_FISH;  //full weight
            }
            else if (percent > 0.93) {
                return SUPER_RARE_FISH;  // heaviest 7% → super rare
            } else if (percent > 0.85) {
                return RARE_FISH;        // middle 8% → rare fish
            } else {
                return COMMON_FISH;      // lightest 85% → common fish
            }
        }
    }



    // ======================
    // Base rarity (original system)
    // ======================
    public enum Rarity {
        COMMON("§fCommon", "§f"),
        UNCOMMON("§aUncommon", "§a"),
        RARE("§bRare", "§b"),
        RARE_POOL_DUST("§bRare-POOL-Ore Dust", "§b"),
        RARE_POOL_ORE("§bRare-POOL-Ore", "§b"),
        RARE_POOL_INDUSTRY("§bRare-POOL-Industrial Material", "§b"),
        EPIC("§dEpic", "§d"),
        EPIC_POOL_INDUSTRY("§dEpic-POOL-Industrial Material", "§d"),
        EPIC_POOL_ALLOY_INGOT("§dEpic-POOL-Alloy Ingot", "§d"),
        LEGENDARY("§cLegendary", "§c"),
        LEGENDARY_EEL("§cLegendary-Ether Eel", "§c"),
        MYTHICAL("§eMythical", "§e");


        private final String displayName;
        private final String colorCode;

        Rarity(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }

        public String getDisplayName() { return displayName; }
        public String getColorCode() { return colorCode; }
    }
}