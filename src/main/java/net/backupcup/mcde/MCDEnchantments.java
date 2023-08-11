package net.backupcup.mcde;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.backupcup.mcde.block.entity.ModBlockEntities;
import net.backupcup.mcde.screen.handler.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

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
}
