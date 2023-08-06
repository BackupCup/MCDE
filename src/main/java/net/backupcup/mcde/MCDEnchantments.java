package net.backupcup.mcde;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Jankson;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.api.SyntaxError;
import net.backupcup.mcde.block.ModBlocks;
import net.backupcup.mcde.block.entity.ModBlockEntities;
import net.backupcup.mcde.screen.handler.ModScreenHandlers;
import net.backupcup.mcde.util.IdentifierGlobbedList;
import net.backupcup.mcde.util.ModTags;
import net.backupcup.mcde.util.Slots;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MCDEnchantments implements ModInitializer {
	public static final String MOD_ID = "mcde";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Identifier SYNC_CONFIG_PACKET = Identifier.of(MOD_ID, "sync_config");
    private static Config config;

    public static Config getConfig() {
        return config;
    }

	public static void setConfig(Config config) {
        MCDEnchantments.config = config;
    }

    @Override
	public void onInitialize() {
		ModBlocks.RegisterModBlocks();

		ModScreenHandlers.registerAllScreenHandlers();
		ModBlockEntities.registerBlockEntities();

        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
            .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of(MOD_ID, "config");
            }
            @Override
            public void reload(ResourceManager manager) {
                config = Config.load();
            }
        });

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            var buf = PacketByteBufs.create();
            config.writeToClient(buf);
            ServerPlayNetworking.send(player, SYNC_CONFIG_PACKET, buf);
            if (Config.lastError != null) {
                player.sendMessage(Text.literal("[MCDEnchantments]: ")
                        .append(Config.lastError).formatted(Formatting.RED));
            }
        });
	}

    public static class Config {
        private static File getConfigFile() {
            return Path.of(
                FabricLoader.getInstance().getConfigDir().toString(),
                MOD_ID,
                "config.json"
            ).toFile();
        }

        private static final Jankson JANKSON = Jankson.builder()
            .registerSerializer(Identifier.class, (id, marshaller) -> marshaller.serialize(id.toString()))
            .registerDeserializer(String.class, Identifier.class, (str, marshaller) -> Identifier.tryParse(str))
            .registerSerializer(IdentifierGlobbedList.class, (list, marshaller) -> list.toJson(marshaller))
            .build();

        private static Text lastError;

        public void save() throws FileNotFoundException {
            getConfigFile().getParentFile().mkdirs();
            try (var outStream = new FileOutputStream(getConfigFile())) {
                outStream.write(JANKSON.toJson(this).toJson(true, true).getBytes());
            } catch (IOException e) {
                LOGGER.error("IO exception while saving config: {}", e.getMessage());
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
                LOGGER.error("Config syntax error. {}.", e.getLineMessage());
                LOGGER.error(e.getMessage());
                LOGGER.warn("Using default configuration.");
                lastError = Text.translatable("message.mcde.error.config.general");
            } catch (IOException e) {
                LOGGER.error("IO exception occured while reading config. Using defaults.");
                LOGGER.error(e.getMessage());
                LOGGER.warn("Using default configuration.");
                lastError = Text.translatable("message.mcde.error.config.general");
            }
            return defaults;
        }

        public static Config readFromServer(PacketByteBuf buf) {
            try {
                return JANKSON.fromJson(buf.readString(), Config.class);
            } catch (SyntaxError e) {
                LOGGER.error("Error while retrieving config from server: {}", e);
            }
            return null;
        }

        public void writeToClient(PacketByteBuf buf) {
            buf.writeString(JANKSON.toJson(this).toJson());
        }

        public enum ListType {
            ALLOW, DENY
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

        public int getRerollCostPerLevel(Identifier id) {
            return isEnchantmentPowerful(id) ? rerollCostPowerful : rerollCost;
        }

        public boolean areVillagersSellOnlyUnbreaking() {
            return villagersSellOnlyUnbreaking;
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

        public Map<Identifier, Map<Slots, Float>> getProgressChances() {
            return progressChances;
        }

        public boolean isTreasureCustom() {
            return customTreasure;
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

        @Comment("Lists all enchantments to be excluded (or included) when using blocks from this mod")
        private IdentifierGlobbedList list = new IdentifierGlobbedList(List.of(
            "minecraft:mending",
            "minecraft:unbreaking"
        ));

        @Comment("Allow cursed enchantments to appear")
        private boolean allowCursed = false;

        @Comment("Generate enchantments only if they are available for random selection.")
        private boolean respectAvailabilityForRandomSelection = true;

        @Comment("Allow treasure enchantments to appear")
        private boolean allowTreasure = true;

        @Comment("Require compatibility of enchantments in different slots.\n" +
        "For example, if this is false, you can enchant an armor both for protection and fire protection simultaneously.")
        private boolean requireCompatibility = true;

        @Comment("Sets cost of enchanting in xp levels per level")
        private int enchantCost = 3;
        @Comment("Sets cost of enchanting in xp levels per level for powerful enchantments")
        private int enchantCostPowerful = 5;

        @Comment("Sets amount of lapis needed for reroll per level")
        private int rerollCost = 3;
        @Comment("Sets amount of lapis needed for reroll per level for powerful enchantments")
        private int rerollCostPowerful = 5;

        @Comment("Sets whether villagers sell enchanted books only with unbreaking\n" +
                 "On false, villagers have vanilla trades")
        private boolean villagersSellOnlyUnbreaking = true;

        @Comment("Allow mixing items in anvil\n" +
                 "On true, vanilla anvil behaviour is applied")
        private boolean allowAnvilItemMixing = false;

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

        @Comment("Whether to use custom pool for treasure enchantments")
        private boolean customTreasure = false;

        @Comment("Enchantments from this pool would be used to enchant books in loot tables")
        private IdentifierGlobbedList treasurePool = new IdentifierGlobbedList(List.of());

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
}
