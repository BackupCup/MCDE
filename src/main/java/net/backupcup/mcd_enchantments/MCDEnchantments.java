package net.backupcup.mcd_enchantments;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ParsingException;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.util.NamingSchemes;

import net.backupcup.mcd_enchantments.block.ModBlocks;
import net.backupcup.mcd_enchantments.block.entity.ModBlockEntities;
import net.backupcup.mcd_enchantments.screen.ModScreenHandlers;
import net.backupcup.mcd_enchantments.util.IdentifierGlobbedList;
import net.backupcup.mcd_enchantments.util.IdentifierGlobbedListSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class MCDEnchantments implements ModInitializer {
	public static final String MOD_ID = "mcd_enchantments";
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
        config = Config.load();

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
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

        public boolean isEnchantmentAllowed(Identifier id) {
            return switch (listKind) {
                case ALLOW -> list.contains(id);
                case DENY -> !list.contains(id);
            };
        }

        public boolean areCursedEnchantmentsAllowed() {
            return allowCursed;
        }

        public boolean isEnchantmentPowerful(Identifier id) {
            return powerful.contains(id);
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

        @Comment("All enchantments from this list is considered 'powerful'.\n" + 
                 "Generally, it means increased cost for enchanting and rerolling.")
        private IdentifierGlobbedList powerful = new IdentifierGlobbedList(Map.of("minecraft", List.of(
            "protection",
            "sharpness",
            "sweeping",
            "riptide",
            "channeling",
            "infinity",
            "fortune",
            "silk_touch",
            "multishot"
        ))); 

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

        @Comment("Sets cost of gilding")
        private int gildingCost = 8;

        @Comment("Each n-th tick (where n is this setting) would increment progress of gilding.\n" +
                 "The process consists of 33 steps (frames). So, overall process would take n * 33 ticks.")
        private int ticksPerGildingProcessStep = 1;
    }
}
