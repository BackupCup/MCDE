package net.backupcup.mcd_enchantments;

import java.nio.file.Path;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ParsingException;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.util.NamingSchemes;

import com.typesafe.config.ConfigException;

import net.backupcup.mcd_enchantments.block.ModBlocks;
import net.backupcup.mcd_enchantments.block.entity.ModBlockEntities;
import net.backupcup.mcd_enchantments.screen.ModScreenHandlers;
import net.backupcup.mcd_enchantments.util.IdentifierGlobbedList;
import net.backupcup.mcd_enchantments.util.IdentifierGlobbedListSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

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

        @Comment("Determines whether constraints should allow or deny listed enchantments")
        private ConstraintType constraintType = ConstraintType.DENY;

        @Comment("Lists all enchantments that should (not) be included when using blocks from this mod")
        private IdentifierGlobbedList constraintList = new IdentifierGlobbedList(List.of(
            "minecraft:mending",
            "minecraft:unbreaking"
        ));

        public static Config load() {
            try {
                if (getConfigFile().toFile().exists()) {
                    return loader.load().get(Config.class);
                }
                var root = loader.load();
                var defaults = new Config();
                root.set(Config.class, defaults);
                loader.save(root);
                return defaults;
            } catch (ConfigurateException e) {
                if (e instanceof ParsingException pe) {
                    var parse = (ConfigException.Parse)pe.getCause();
                    // Parse exception message is prefixed by origin description + ": "
                    // So I get rid of it
                    var origin = parse.origin().description();
                    LOGGER.error(
                            "Config syntax error at line {}: {}",
                            pe.line(),
                            parse.getMessage().substring(origin.length() + 2)
                            );
                    throw new RuntimeException(pe);
                }
                LOGGER.error("Error while loading config: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
