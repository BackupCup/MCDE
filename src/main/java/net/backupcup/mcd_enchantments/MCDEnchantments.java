package net.backupcup.mcd_enchantments;

import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.Nullable;
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
            if (entity.isPlayer()) {
                entity.sendMessage(Text.literal("[MCDEnchantments]: ")
                        .append(Config.lastError()).formatted(Formatting.RED));
            }
        });
	}


    @ConfigSerializable
    public static class Config {
        public enum ConstraintType {
            ALLOW, DENY
        }

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

        public static @Nullable Text lastError() {
            return lastError;
        }

        @Comment("Determines whether constraints should allow or deny listed enchantments")
        private ConstraintType constraintType = ConstraintType.DENY;

        @Comment("Lists all enchantments that should (not)\nbe included when using blocks from this mod")
        private IdentifierGlobbedList constraintList = new IdentifierGlobbedList(List.of(
            "minecraft:mending",
            "minecraft:unbreaking"
        ));

        public static Config load() {
            var defaults = new Config();
            try {
                if (getConfigFile().toFile().exists()) {
                    lastError = null;
                    return loader.load().get(Config.class);
                }
                var root = loader.load();
                root.set(Config.class, defaults);
                loader.save(root);
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
    }
}
