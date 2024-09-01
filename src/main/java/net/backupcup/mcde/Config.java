package net.backupcup.mcde;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.api.DeserializationException;
import blue.endless.jankson.api.SyntaxError;
import net.backupcup.mcde.util.AdvancementList;
import net.backupcup.mcde.util.EnchantmentList;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.ModTags;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Config {
    private static File getConfigFile() {
        return Path.of(
            FabricLoader.getInstance().getConfigDir().toString(),
            MCDEnchantments.MOD_ID,
            "config.json"
        ).toFile();
    }

    private static final Jankson JANKSON = Jankson.builder()
        .registerSerializer(Identifier.class, (id, marshaller) -> marshaller.serialize(id.toString()))
        .registerDeserializer(String.class, Identifier.class, (str, marshaller) -> Identifier.tryParse(str))
        .build();

    static Text lastError;

    public void save() throws FileNotFoundException {
        getConfigFile().getParentFile().mkdirs();
        try (var outStream = new FileOutputStream(getConfigFile())) {
            outStream.write(JANKSON.toJson(this).toJson(true, true).getBytes());
        } catch (IOException e) {
            MCDEnchantments.LOGGER.error("IO exception while saving config: {}", e.getMessage());
        }
    }

    public static Config load() {
        var defaults = new Config();
        try {
            if (getConfigFile().exists()) {
                lastError = null;
                var json = JANKSON.load(getConfigFile());
                return JANKSON.fromJsonCarefully(json, Config.class);
            }
            defaults.save();
            lastError = null;
            return defaults;
        } catch (SyntaxError e) {
            MCDEnchantments.LOGGER.error("Config syntax error. {}.", e.getLineMessage());
            MCDEnchantments.LOGGER.error(e.getMessage());
            MCDEnchantments.LOGGER.warn("Using default configuration.");
            lastError = Text.translatable("message.mcde.error.config.general");
        } catch (DeserializationException e) {
            MCDEnchantments.LOGGER.error("MCDE's config deserialization error.");
            MCDEnchantments.LOGGER.error("{}", e.getMessage());
            if (e.getCause() != null) {
                MCDEnchantments.LOGGER.error("Cause: {}", e.getCause().getMessage());
            }
            MCDEnchantments.LOGGER.warn("Using default configuration.");
            lastError = Text.translatable("message.mcde.error.config.general");
        } catch (IOException e) {
            MCDEnchantments.LOGGER.error("IO exception occured while reading config. Using defaults.");
            MCDEnchantments.LOGGER.error(e.getMessage());
            MCDEnchantments.LOGGER.warn("Using default configuration.");
            lastError = Text.translatable("message.mcde.error.config.general");
        }
        return defaults;
    }

    public static Config readFromServer(PacketByteBuf buf) {
        try {
            return JANKSON.fromJsonCarefully(buf.readString(), Config.class);
        } catch (SyntaxError | DeserializationException e) {
            MCDEnchantments.LOGGER.error("Error while retrieving config from server: {}", e);
        }
        return null;
    }

    public void writeToClient(PacketByteBuf buf) {
        buf.writeString(JANKSON.toJson(this).toJson());
    }

    public static enum ListType {
        ALLOW, DENY
    }

    public static enum GildingMergeStrategy {
        REMOVE, FIRST, SECOND, BOTH
    }

    public static class RerollCost {
        private int startCost;
        private int endCost;
        private int step;

        public RerollCost() {
            this(30, 3, 3);
        }

        public RerollCost(int startCost, int endCost, int step) {
            this.startCost = startCost;
            this.endCost = endCost;
            this.step = step;
        }

        public int getStartCost() {
            return startCost;
        }

        public int getEndCost() {
            return endCost;
        }

        public int getStep() {
            return step;
        }

        private int getNextCost(int cost) {
            if (startCost == endCost) {
                return startCost;
            }
            if (endCost < startCost && cost <= endCost || endCost > startCost && cost >= endCost) {
                return endCost;
            }
            return cost + step * (endCost > startCost ? 1 : -1);
        }
    }

    public static class RerollCostParameters {
        private RerollCost normal;
        private RerollCost powerful;

        public RerollCostParameters() {
            this(new RerollCost(30, 3, 3), new RerollCost(50, 5, 5));
        }

        public RerollCostParameters(RerollCost normal, RerollCost powerful) {
            this.normal = normal;
            this.powerful = powerful;
        }

        public void updateCost(EnchantmentSlots slots) {
            slots.setNextRerollCost(normal.getNextCost(slots.getNextRerollCost()));
            slots.setNextRerollCostPowerful(powerful.getNextCost(slots.getNextRerollCostPowerful()));
        }

        public RerollCost getNormal() {
            return normal;
        }

        public RerollCost getPowerful() {
            return powerful;
        }
    }

    public static class EnchantCost {
        private int startCost;
        private int step;

        public EnchantCost() {
            this(3, 3);
        }

        public EnchantCost(int startCost, int step) {
            this.startCost = startCost;
            this.step = step;
        }

        public int getEnchantCost(Identifier id, int level) {
            return startCost + step * (level - 1);
        }

        public int getStartCost() {
            return startCost;
        }

        public int getStep() {
            return step;
        }
    }

    public static class EnchantCostParameters {
        private EnchantCost normal;
        private EnchantCost powerful;

        public EnchantCostParameters() {
            this(new EnchantCost(3, 3), new EnchantCost(5, 5));
        }

        public EnchantCostParameters(EnchantCost normal, EnchantCost powerful) {
            this.normal = normal;
            this.powerful = powerful;
        }

        public int getEnchantCost(Identifier id, int level) {
            return MCDEnchantments.getConfig().isEnchantmentPowerful(id) ? 
                powerful.getEnchantCost(id, level) :
                normal.getEnchantCost(id, level);
        }

        public EnchantCost getNormal() {
            return normal;
        }

        public EnchantCost getPowerful() {
            return powerful;
        }
    }

    public static class SlotChances {
        private float second;
        private float third;

        public SlotChances() {
            this(0, 0);
        }

        public SlotChances(float second, float third) {
            this.second = second;
            this.third = third;
        }

        public float getSecondChance() {
            return second;
        }

        public float getThirdChance() {
            return third;
        }

        public void setSecondChance(float second) {
            this.second = second;
        }

        public void setThirdChance(float third) {
            this.third = third;
        }

        public static SlotChances add(SlotChances lhs, SlotChances rhs) {
            return new SlotChances(lhs.second + rhs.second, lhs.third + rhs.third);
        }

        public static SlotChances add(SlotChances lhs, float rhs) {
            return new SlotChances(lhs.second + rhs, lhs.third + rhs);
        }

        public static SlotChances apply(SlotChances chances, Function<Float, Float> f) {
            return new SlotChances(f.apply(chances.second), f.apply(chances.third));
        }
    }

    public static class Unlock {
        private AdvancementList advancements;
        private EnchantmentList enchantments;

        public Unlock() {
            this(new AdvancementList(), new EnchantmentList());
        }

        public Unlock(AdvancementList advancements, EnchantmentList enchantments) {
            this.advancements = advancements;
            this.enchantments = enchantments;
        }

        public AdvancementList getAdvancements() {
            return advancements;
        }

        public EnchantmentList getEnchantments() {
            return enchantments;
        }
    }

    public boolean isEnchantmentAllowed(Enchantment enchantment) {
        return isEnchantmentAllowed(Registries.ENCHANTMENT.getId(enchantment));
    }

    public boolean isEnchantmentAllowed(Identifier id) {
        return switch (listKind) {
            case ALLOW -> list.contains(id);
            case DENY -> !list.contains(id);
        };
    }

    public boolean areCursedAllowed() {
        return allowCurses;
    }

    public boolean isEnchantmentPowerful(Identifier id) {
        return ModTags.isIn(id, ModTags.Enchantments.POWERFUL);
    }

    public int getEnchantCost(Identifier id, int level) {
        return enchantCost.getEnchantCost(id, level);
    }

    public RerollCost getRerollCostParameters(Identifier id) {
        return isEnchantmentPowerful(id) ? rerollCost.powerful : rerollCost.normal;
    }

    public RerollCostParameters getRerollCostParameters() {
        return rerollCost;
    }

    public boolean isAnvilItemMixingAllowed() {
        return allowAnvilItemMixing;
    }

    public boolean isEnchantingWithBooksAllowed() {
        return allowEnchantingWithBooks;
    }

    public GildingMergeStrategy getGildingMergeStrategy() {
        return gildingMergeStrategy;
    }

    public int getGildingCost() {
        return gildingCost;
    }

    public int getGildingDuration() {
        return gildingDurationTicks;
    }

    public boolean isUsingEnchantingTableAllowed() {
        return allowUsingEnchantingTable;
    }

    public boolean canFullRerollRemoveSlots() {
        return fullRerollCanRemoveSlots;
    }

    public boolean isAvailabilityForRandomSelectionRespected() {
        return respectAvailabilityForRandomSelection;
    }

    public boolean isTreasureAllowed() {
        return allowTreasure;
    }

    public boolean isCompatibilityRequired() {
        return requireCompatibility;
    }

    public float getSecondSlotBaseChance() {
        return secondSlotBaseChance;
    }

    public float getThirdSlotBaseChance() {
        return thirdSlotBaseChance;
    }

    public SlotChances getSlotBaseChances() {
        return new SlotChances(secondSlotBaseChance, thirdSlotBaseChance);
    }

    public Map<Identifier, SlotChances> getProgressChances() {
        return progressChances;
    }

    public List<Enchantment> getVillagerBookPool() {
        return Registries.ENCHANTMENT.stream().filter(villagerBookPool::contains).toList();
    }

    public boolean isInCustomTreasurePool(Identifier id) {
        return treasurePool.contains(id);
    }

    public boolean isInCustomTreasurePool(Enchantment enchantment) {
        return treasurePool.contains(enchantment);
    }

    public List<Enchantment> getCustomTreasurePool() {
        return Registries.ENCHANTMENT.stream().filter(treasurePool::contains).toList();
    }

    public List<Unlock> getUnlocks() {
        return unlocks;
    }

    @Comment("Has two possible values:\n" +
             "ALLOW - Only allow enchantments specified in 'list' to appear\n" +
             "DENY - Make enchantments specified in 'list' to never appear")
    private ListType listKind = ListType.DENY;

    @Comment("Lists all enchantments to be excluded (or included) when using tables from MCDE\n" + 
             "This list and all other fields when mentioned support tags and globs\n" + 
             "This means you can specify tags (\"#c:powerful\", for example)\n" +
             "Globs are supported in path part of an identifier (after a ':')\n" +
             "Examples:\n" +
             "  \"mcdw:*\" matches a whole namespace,\n" + 
             "  \"minecraft:*protection\" matches all protection enchantments,\n" +
             "  \"#namespace:helmet*\" matches all enchantments which are in the tags starting with 'helmet' \n" +
             "The format can also be different, you can specify this list in 'nested' format like so:\n" +
             "\"list\": {\n" + 
             "  \"minecraft\": [\"unbreaking\", \"mending\"],\n" +
             "  \"mcda\": [\"chilling\", \"burning\"],\n" +
             "  \"c\": [\"#powerful\"], // tags\n" +
             "  \"mcdw\": [\"*\"], // globs\n" + 
             "  \"namespace\": [\"#helmet*\"] // all tags from a namespace matching a glob\n" +
             "}")
    private EnchantmentList list = new EnchantmentList(
        "minecraft:unbreaking"
    );

    @Comment("Allow curses to appear")
    private boolean allowCurses = false;

    @Comment("Generate enchantments only if they are available for random selection.")
    private boolean respectAvailabilityForRandomSelection = true;

    @Comment("Allow treasure enchantments to appear")
    private boolean allowTreasure = false;

    @Comment("Require compatibility of enchantments in different slots.\n" +
             "For example, if this is false, you can enchant an armor both for protection and fire protection simultaneously.\n" +
             "Anvils are also affected.")
    private boolean requireCompatibility = true;

    @Comment("Sets cost of enchanting in xp levels\n" +
             "Each level increases cost by step\n" +
             "Anvil's mixing price is also affected")
    private EnchantCostParameters enchantCost = new EnchantCostParameters(
        new EnchantCost(3, 3),
        new EnchantCost(5, 5)
    );

    @Comment("Sets amount of lapis needed for reroll\n" +
             "For each reroll, the cost is either increased or decreased by step")
    private RerollCostParameters rerollCost = new RerollCostParameters(
        new RerollCost(15, 1, 3),
        new RerollCost(25, 3, 5)
    );

    @Comment("Whether a full reroll of enchantment slots can decrease amount of them")
    private boolean fullRerollCanRemoveSlots = true;

    @Comment("Allow mixing items in anvil\n" +
             "On true, vanilla anvil behaviour is applied")
    private boolean allowAnvilItemMixing = true;

    @Comment("Allow applying additional enchantments via books in anvil")
    private boolean allowEnchantingWithBooks = true;

    @Comment("Whether to allow players to use enchanting table.\n" +
             "Creative players can still use it.")
    private boolean allowUsingEnchantingTable = false;
    
    @Comment("Specify gilding merge strategy. Possible values are:\n" +
             "REMOVE - remove gilding from the result\n" +
             "FIRST - keep the gilding from first item\n" +
             "SECOND - keep the gilding from second item\n" +
             "BOTH - keep both gildings, essentially allowing several gilded enchantments on one item")
    private GildingMergeStrategy gildingMergeStrategy = GildingMergeStrategy.FIRST;

    @Comment("Sets cost of gilding")
    private int gildingCost = 8;

    @Comment("Duration of gilding in ticks")
    private int gildingDurationTicks = 33;

    @Comment("Enchantments from this pool would be used in trades.\n" +
             "If this pool is empty, then trades will not be affected.\n" +
             "This list supports the same features as 'list' option")
    private EnchantmentList villagerBookPool = new EnchantmentList("minecraft:unbreaking");

    @Comment("Enchantments from this pool would be used to enchant books in loot tables.\n" +
             "If this pool is empty, then loot tables will not be affected.\n" +
             "This list supports the same features as 'list' option")
    private EnchantmentList treasurePool = new EnchantmentList();

    @Comment("Sets a base chance for second slot to appear when generating enchantment slots for a new item.\n" +
             "Value must be between 0 and 1")
    private float secondSlotBaseChance = 0.5f;

    @Comment("Same thing as above only for third slot")
    private float thirdSlotBaseChance = 0.25f;

    @Comment("Defines how slot chances increases with advancements")
    private Map<Identifier, SlotChances> progressChances = new LinkedHashMap<>(Map.ofEntries(
        Map.entry(Identifier.of("minecraft", "story/cure_zombie_villager"),             new SlotChances(0.011432f, 0.01079f)),
        Map.entry(Identifier.of("minecraft", "adventure/kill_mob_near_sculk_catalyst"), new SlotChances(0.011694f, 0.011256f)),
        Map.entry(Identifier.of("minecraft", "adventure/bullseye"),                     new SlotChances(0.012399f, 0.011303f)),
        Map.entry(Identifier.of("minecraft", "adventure/summon_iron_golem"),            new SlotChances(0.012235f, 0.012248f)),
        Map.entry(Identifier.of("minecraft", "husbandry/froglights"),                   new SlotChances(0.012514f, 0.012777f)),
        Map.entry(Identifier.of("minecraft", "nether/return_to_sender"),                new SlotChances(0.012801f, 0.013328f)),
        Map.entry(Identifier.of("minecraft", "adventure/hero_of_the_village"),          new SlotChances(0.013093f, 0.013904f)),
        Map.entry(Identifier.of("minecraft", "nether/netherite_armor"),                 new SlotChances(0.013392f, 0.014503f)),
        Map.entry(Identifier.of("minecraft", "adventure/totem_of_undying"),             new SlotChances(0.013699f, 0.01513f)),
        Map.entry(Identifier.of("minecraft", "end/dragon_breath"),                      new SlotChances(0.014012f, 0.015782f)),
        Map.entry(Identifier.of("minecraft", "husbandry/bred_all_animals"),             new SlotChances(0.014332f, 0.016464f)),
        Map.entry(Identifier.of("minecraft", "end/respawn_dragon"),                     new SlotChances(0.01466f,  0.017174f)),
        Map.entry(Identifier.of("minecraft", "end/elytra"),                             new SlotChances(0.014995f, 0.017916f)),
        Map.entry(Identifier.of("minecraft", "nether/explore_nether"),                  new SlotChances(0.015338f, 0.018688f)),
        Map.entry(Identifier.of("minecraft", "nether/fast_travel"),                     new SlotChances(0.015689f, 0.019496f)),
        Map.entry(Identifier.of("minecraft", "husbandry/balanced_diet"),                new SlotChances(0.016048f, 0.020336f)),
        Map.entry(Identifier.of("minecraft", "end/levitate"),                           new SlotChances(0.01641f,  0.02121f)),
        Map.entry(Identifier.of("minecraft", "nether/create_beacon"),                   new SlotChances(0.016794f, 0.022135f)),
        Map.entry(Identifier.of("minecraft", "adventure/two_birds_one_arrow"),          new SlotChances(0.017174f, 0.023085f)),
        Map.entry(Identifier.of("minecraft", "husbandry/complete_catalogue"),           new SlotChances(0.01757f,  0.02408f)),
        Map.entry(Identifier.of("minecraft", "nether/create_full_beacon"),              new SlotChances(0.017964f, 0.025123f)),
        Map.entry(Identifier.of("minecraft", "nether/uneasy_alliance"),                 new SlotChances(0.018379f, 0.026205f)),
        Map.entry(Identifier.of("minecraft", "nether/all_potions"),                     new SlotChances(0.018798f, 0.027336f)),
        Map.entry(Identifier.of("minecraft", "adventure/kill_all_mobs"),                new SlotChances(0.019229f, 0.028516f)),
        Map.entry(Identifier.of("minecraft", "adventure/adventuring_time"),             new SlotChances(0.019669f, 0.029746f)),
        Map.entry(Identifier.of("minecraft", "nether/all_effects"),                     new SlotChances(0.020118f, 0.031031f))
    ));
    
    @Comment("List of entries which define what enchantments would unlock after obtaining certain advancements\n" + 
             "'advancements' field supports globs, so you can specify \"minecraft:story/enter_*\" to mark all advancements which start with 'story/enter_'\n" +
             "'enchantments' field supports all features 'list' supports\n" +
             "If entries have overlapping enchantments, they will be unlocked when only one of the entries is done (i.e. all advancements from this entry are obtained)")
    private List<Unlock> unlocks = new ArrayList<>(List.of(new Unlock(
        new AdvancementList("minecraft:story/enter_the_end"),
        new EnchantmentList("#c:powerful")
    )));
}
