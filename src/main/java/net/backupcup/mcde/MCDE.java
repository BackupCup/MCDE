package net.backupcup.mcde;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.backupcup.mcde.block.entity.ModBlockEntities;
import net.backupcup.mcde.screen.handler.ModScreenHandlers;
import net.backupcup.mcde.screen.handler.GildingFoundryScreenHandler.GildingPacket;
import net.backupcup.mcde.screen.handler.RollBenchScreenHandler.LockedSlotsPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class MCDE implements ModInitializer {
	public static final String MOD_ID = "mcde";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static Config config;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static Config getConfig() {
        return config;
    }

	public static void setConfig(Config config) {
        MCDE.config = config;
    }

    @Override
	public void onInitialize() {
		ModScreenHandlers.registerAllScreenHandlers();
		ModBlockEntities.registerBlockEntities();

        PayloadTypeRegistry.playS2C().register(Config.PACKET_ID, Config.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(GildingPacket.PACKET_ID, GildingPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(LockedSlotsPacket.PACKET_ID, LockedSlotsPacket.PACKET_CODEC);

        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
            .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return id("config");
            }
            @Override
            public void reload(ResourceManager manager) {
                config = Config.load();
            }
        });

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            ServerPlayNetworking.send(player, config);
            if (Config.lastError != null) {
                player.sendMessage(Text.literal("[MCDEnchantments]: ")
                        .append(Config.lastError).formatted(Formatting.RED));
            }
        });
	}
}
