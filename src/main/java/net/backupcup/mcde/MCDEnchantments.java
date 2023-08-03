package net.backupcup.mcde;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ParsingException;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingSchemes;

import net.backupcup.mcde.block.ModBlocks;
import net.backupcup.mcde.block.entity.ModBlockEntities;
import net.backupcup.mcde.screen.handler.ModScreenHandlers;
import net.backupcup.mcde.util.IdentifierGlobbedList;
import net.backupcup.mcde.util.IdentifierGlobbedListSerializer;
import net.backupcup.mcde.util.ModTags;
import net.backupcup.mcde.util.Slots;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MCDEnchantments implements ModInitializer {
	public static final String MOD_ID = "mcde";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static Config config;
	public static Config getConfig() {
        return config;
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
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((entity, joined) -> {
            if (entity.isPlayer() && Config.lastError != null) {
                entity.sendMessage(Text.literal("[MCDEnchantments]: ")
                        .append(Config.lastError).formatted(Formatting.RED));
            }
        });
	}

    @ConfigSerializable
    public static class Config {
        private static final HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
            .path(getConfigFile())
            .defaultOptions(opts -> 
                    opts.serializers(builder -> {
                        builder.register(IdentifierGlobbedList.class, IdentifierGlobbedListSerializer.INSTANCE);
                        builder.register(Identifier.class, new TypeSerializer<Identifier>() {
                            @Override
                            public Identifier deserialize(Type type, ConfigurationNode node)
                                    throws SerializationException {
                                    return Identifier.tryParse(node.getString());
                            }

                            @Override
                            public void serialize(Type type, @Nullable Identifier obj, ConfigurationNode node)
                                    throws SerializationException {
                                    if (obj == null) {
                                        return;
                                    }
                                    node.set(obj.toString());
                            }
                        });
                        builder.registerAnnotatedObjects(ObjectMapper.factoryBuilder()
                                .defaultNamingScheme(NamingSchemes.SNAKE_CASE).build());
                    }))
            .build();

        private static Path getConfigFile() {
            return Path.of(
                    FabricLoader.getInstance().getConfigDir().toString(),
                    MOD_ID,
                    "config.conf"
                    );
        }

        private static Text lastError;

        public static Config load() {
            var defaults = new Config();
            try {
                if (getConfigFile().toFile().exists()) {
                    lastError = null;
                    return loader.load().get(Config.class);
                }
                loader.save(loader.load().set(Config.class, defaults));
                lastError = null;
                return defaults;
            } catch (ConfigurateException e) {
                if (e instanceof ParsingException pe) {
                    var parse = pe.getCause();
                    // Parse exception message is prefixed by origin description + ": "
                    // So I get rid of it
                    var origin = pe.context();
                    LOGGER.error(
                            "Config syntax error at line {}: {}. Using defaults.",
                            pe.line(),
                            parse.getMessage().substring(origin.length() + 2)
                            );
                    lastError = Text.translatable("message.mcde.error.config.parsing", pe.line());
                    return defaults;
                }
                LOGGER.error("Error while loading config: {}. Using defaults.", e.getMessage());
                lastError = Text.translatable("message.mcde.error.config.general");
                return defaults;
            }
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

        @Comment("Whether to allow players to use enchanting table.\n" +
                 "Creative players can still use it.")
        private boolean allowUsingEnchantingTable = false;

        @Comment("Sets cost of gilding")
        private int gildingCost = 8;

        @Comment("Each n-th tick (where n is this setting) would increment progress of gilding.\n" +
                 "The process consists of 33 steps (frames). So, overall process would take n * 33 ticks.")
        private int ticksPerGildingProcessStep = 1;

        @Comment("Defines how slot chances increases with game progression")
        private Map<Identifier, Map<Slots, Float>> progressChances = Map.of(
            Identifier.of("minecraft", "story/smelt_iron"), Map.of(Slots.SECOND, 1.1f, Slots.THIRD, 1.04f),
            Identifier.tryParse("minecraft:story/enter_the_end"), Map.of(Slots.SECOND, 1.5f, Slots.THIRD, 1.3f)
        );
    }
}
