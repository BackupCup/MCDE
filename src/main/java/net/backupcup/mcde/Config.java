package net.backupcup.mcde;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Jankson;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.api.SyntaxError;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.IdentifierGlobbedList;
import net.backupcup.mcde.util.ModTags;
import net.backupcup.mcde.util.Slots;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

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
        .registerSerializer(IdentifierGlobbedList.class, (list, marshaller) -> list.toJson(marshaller))
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
                return JANKSON.fromJson(json, Config.class);
            }
            defaults.save();
            lastError = null;
            return defaults;
        } catch (SyntaxError e) {
            MCDEnchantments.LOGGER.error("Config syntax error. {}.", e.getLineMessage());
            MCDEnchantments.LOGGER.error(e.getMessage());
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
            return JANKSON.fromJson(buf.readString(), Config.class);
        } catch (SyntaxError e) {
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

    public static record RerollCost(int startCost, int endCost, int step) {
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

    public static record RerollCostParameters(RerollCost normal, RerollCost powerful) {
        public void updateCost(EnchantmentSlots slots) {
            slots.setNextRerollCost(normal.getNextCost(slots.getNextRerollCost()));
            slots.setNextRerollCostPowerful(powerful.getNextCost(slots.getNextRerollCostPowerful()));
        }
    }

    public boolean isEnchantmentAllowed(Enchantment enchantment) {
        return isEnchantmentAllowed(Registry.ENCHANTMENT.getId(enchantment));
    }

    public boolean isEnchantmentAllowed(Identifier id) {
        return switch (listKind) {
            case ALLOW -> list.contains(id);
            case DENY -> !list.contains(id);
        };
    }

    public boolean areCursedAllowed() {
        return allowCursed;
    }

    public boolean isEnchantmentPowerful(Identifier id) {
        return ModTags.isIn(id, ModTags.Enchantments.POWERFUL);
    }

    public int getEnchantCostPerLevel(Identifier id) {
        return isEnchantmentPowerful(id) ? enchantCostPowerful : enchantCost;
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

    public int getGildingCost() {
        return gildingCost;
    }

    public int getTicksPerGildingProcessStep() {
        return ticksPerGildingProcessStep;
    }

    public boolean isUsingEnchantingTableAllowed() {
        return allowUsingEnchantingTable;
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

    public Map<Identifier, Map<Slots, Float>> getProgressChances() {
        return progressChances;
    }

    public List<Enchantment> getVillagerBookPool() {
        return Registry.ENCHANTMENT.stream().filter(villagerBookPool::contains).toList();
    }

    public boolean isInCustomTreasurePool(Identifier id) {
        return treasurePool.contains(id);
    }

    public boolean isInCustomTreasurePool(Enchantment enchantment) {
        return treasurePool.contains(enchantment);
    }

    public List<Enchantment> getCustomTreasurePool() {
        return Registry.ENCHANTMENT.stream().filter(treasurePool::contains).toList();
    }

    @Comment("Has two possible values:\n" +
             "ALLOW - Only allow enchantments specified in 'list' to appear\n" +
             "DENY - Make enchantments specified in 'list' to never appear")
    private ListType listKind = ListType.DENY;

    @Comment("Lists all enchantments to be excluded (or included) when using tables from MCDE\n" + 
             "You can specify tags (#c:powerful), whole namespaces (mcdw:*), also you can specify all tags from a namespace (#mcdw:*)\n" +
             "The format can also be different, you can specify this list in 'nested' format like so:\n" +
             "\"list\" = {\n" + 
             "  \"minecraft\": [\"unbreaking\", \"mending\"],\n" +
             "  \"mcda\": [\"chilling\", \"burning\"],\n" +
             "  \"c\": [\"#powerful\"], // tags\n" +
             "  \"mcdw\": [\"*\"], // whole namespace\n" + 
             "  \"namespace\": [\"#*\"] // all tags from a namespace\n" +
             "}")
    private IdentifierGlobbedList list = new IdentifierGlobbedList(List.of(
        "minecraft:mending",
        "minecraft:unbreaking"
    ));

    @Comment("Allow cursed enchantments to appear")
    private boolean allowCursed = false;

    @Comment("Generate enchantments only if they are available for random selection.")
    private boolean respectAvailabilityForRandomSelection = true;

    @Comment("Allow treasure enchantments to appear")
    private boolean allowTreasure = false;

    @Comment("Require compatibility of enchantments in different slots.\n" +
             "For example, if this is false, you can enchant an armor both for protection and fire protection simultaneously.\n" +
             "Anvils are also affected.")
    private boolean requireCompatibility = true;

    @Comment("Sets cost of enchanting in xp levels per level\n" +
             "Anvil's mixing price relies on this and also affected")
    private int enchantCost = 3;
    @Comment("Sets cost of enchanting in xp levels per level for powerful enchantments\n" +
             "Enchantment is considered powerful whenever it is listed in #c:powerful tag\n" + 
             "Anvil's mixing price relies on this and also affected")
    private int enchantCostPowerful = 5;

    @Comment("Sets amount of lapis needed for reroll\n" +
             "For each reroll, the cost is either increased or decreased by step")
    private RerollCostParameters rerollCost = new RerollCostParameters(
        new RerollCost(30, 3, 3),
        new RerollCost(50, 5, 5)
    );

    @Comment("Allow mixing items in anvil\n" +
             "On true, vanilla anvil behaviour is applied")
    private boolean allowAnvilItemMixing = true;

    @Comment("Allow applying additional enchantments via books in anvil")
    private boolean allowEnchantingWithBooks = true;

    @Comment("Whether to allow players to use enchanting table.\n" +
             "Creative players can still use it.")
    private boolean allowUsingEnchantingTable = false;

    @Comment("Sets cost of gilding")
    private int gildingCost = 8;

    @Comment("Each n-th tick (where n is this setting) would increment progress of gilding.\n" +
             "The process consists of 33 steps (frames). So, overall process would take n * 33 ticks.")
    private int ticksPerGildingProcessStep = 1;

    @Comment("Enchantments from this pool would be used in trades.\n" +
             "If this pool is empty, then trades will not be affected.\n" +
             "This list supports the same features as 'list' option")
    private IdentifierGlobbedList villagerBookPool = new IdentifierGlobbedList(List.of("minecraft:unbreaking"));

    @Comment("Enchantments from this pool would be used to enchant books in loot tables.\n" +
             "If this pool is empty, then loot tables will not be affected.\n" +
             "This list supports the same features as 'list' option")
    private IdentifierGlobbedList treasurePool = new IdentifierGlobbedList(List.of());

    @Comment("Sets a base chance for second slot to appear when generating enchantment slots for a new item.\n" +
             "Value must be between 0 and 1")
    private float secondSlotBaseChance = 0.5f;

    @Comment("Same thing as above only for third slot")
    private float thirdSlotBaseChance = 0.25f;

    @Comment("Defines how slot chances increases with game progression")
    private Map<Identifier, Map<Slots, Float>> progressChances = Map.ofEntries(
        Map.entry(Identifier.of("minecraft", "story/cure_zombie_villager"),             Map.of(Slots.SECOND, 0.011432f, Slots.THIRD, 0.01079f)),
        Map.entry(Identifier.of("minecraft", "adventure/kill_mob_near_sculk_catalyst"), Map.of(Slots.SECOND, 0.011694f, Slots.THIRD, 0.011256f)),
        Map.entry(Identifier.of("minecraft", "adventure/bullseye"),                     Map.of(Slots.SECOND, 0.012399f, Slots.THIRD, 0.011303f)),
        Map.entry(Identifier.of("minecraft", "adventure/summon_iron_golem"),            Map.of(Slots.SECOND, 0.012235f, Slots.THIRD, 0.012248f)),
        Map.entry(Identifier.of("minecraft", "husbandry/froglights"),                   Map.of(Slots.SECOND, 0.012514f, Slots.THIRD, 0.012777f)),
        Map.entry(Identifier.of("minecraft", "nether/return_to_sender"),                Map.of(Slots.SECOND, 0.012801f, Slots.THIRD, 0.013328f)),
        Map.entry(Identifier.of("minecraft", "adventure/hero_of_the_village"),          Map.of(Slots.SECOND, 0.013093f, Slots.THIRD, 0.013904f)),
        Map.entry(Identifier.of("minecraft", "nether/netherite_armor"),                 Map.of(Slots.SECOND, 0.013392f, Slots.THIRD, 0.014503f)),
        Map.entry(Identifier.of("minecraft", "adventure/totem_of_undying"),             Map.of(Slots.SECOND, 0.013699f, Slots.THIRD, 0.01513f)),
        Map.entry(Identifier.of("minecraft", "end/dragon_breath"),                      Map.of(Slots.SECOND, 0.014012f, Slots.THIRD, 0.015782f)),
        Map.entry(Identifier.of("minecraft", "husbandry/bred_all_animals"),             Map.of(Slots.SECOND, 0.014332f, Slots.THIRD, 0.016464f)),
        Map.entry(Identifier.of("minecraft", "end/respawn_dragon"),                     Map.of(Slots.SECOND, 0.01466f,  Slots.THIRD, 0.017174f)),
        Map.entry(Identifier.of("minecraft", "end/elytra"),                             Map.of(Slots.SECOND, 0.014995f, Slots.THIRD, 0.017916f)),
        Map.entry(Identifier.of("minecraft", "nether/explore_nether"),                  Map.of(Slots.SECOND, 0.015338f, Slots.THIRD, 0.018688f)),
        Map.entry(Identifier.of("minecraft", "nether/fast_travel"),                     Map.of(Slots.SECOND, 0.015689f, Slots.THIRD, 0.019496f)),
        Map.entry(Identifier.of("minecraft", "husbandry/balanced_diet"),                Map.of(Slots.SECOND, 0.016048f, Slots.THIRD, 0.020336f)),
        Map.entry(Identifier.of("minecraft", "end/levitate"),                           Map.of(Slots.SECOND, 0.01641f,  Slots.THIRD, 0.02121f)),
        Map.entry(Identifier.of("minecraft", "nether/create_beacon"),                   Map.of(Slots.SECOND, 0.016794f, Slots.THIRD, 0.022135f)),
        Map.entry(Identifier.of("minecraft", "adventure/two_birds_one_arrow"),          Map.of(Slots.SECOND, 0.017174f, Slots.THIRD, 0.023085f)),
        Map.entry(Identifier.of("minecraft", "husbandry/complete_catalogue"),           Map.of(Slots.SECOND, 0.01757f,  Slots.THIRD, 0.02408f)),
        Map.entry(Identifier.of("minecraft", "nether/create_full_beacon"),              Map.of(Slots.SECOND, 0.017964f, Slots.THIRD, 0.025123f)),
        Map.entry(Identifier.of("minecraft", "nether/uneasy_alliance"),                 Map.of(Slots.SECOND, 0.018379f, Slots.THIRD, 0.026205f)),
        Map.entry(Identifier.of("minecraft", "nether/all_potions"),                     Map.of(Slots.SECOND, 0.018798f, Slots.THIRD, 0.027336f)),
        Map.entry(Identifier.of("minecraft", "adventure/kill_all_mobs"),                Map.of(Slots.SECOND, 0.019229f, Slots.THIRD, 0.028516f)),
        Map.entry(Identifier.of("minecraft", "adventure/adventuring_time"),             Map.of(Slots.SECOND, 0.019669f, Slots.THIRD, 0.029746f)),
        Map.entry(Identifier.of("minecraft", "nether/all_effects"),                     Map.of(Slots.SECOND, 0.020118f, Slots.THIRD, 0.031031f))
    );
}
