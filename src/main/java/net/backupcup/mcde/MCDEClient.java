package net.backupcup.mcde;

import net.backupcup.mcde.block.ModBlocks;
import net.backupcup.mcde.screen.GildingFoundryScreen;
import net.backupcup.mcde.screen.RollBenchScreen;
import net.backupcup.mcde.screen.RunicTableScreen;
import net.backupcup.mcde.screen.handler.GildingFoundryScreenHandler;
import net.backupcup.mcde.screen.handler.GildingFoundryScreenHandler.GildingPacket;
import net.backupcup.mcde.screen.handler.ModScreenHandlers;
import net.backupcup.mcde.screen.handler.RollBenchScreenHandler;
import net.backupcup.mcde.screen.handler.RollBenchScreenHandler.LockedSlotsPacket;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class MCDEClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.RUNIC_TABLE_SCREEN_HANDLER, RunicTableScreen::new);
        HandledScreens.register(ModScreenHandlers.ROLL_BENCH_SCREEN_HANDLER, RollBenchScreen::new);
        HandledScreens.register(ModScreenHandlers.GILDING_FOUNDRY_SCREEN_HANDLER, GildingFoundryScreen::new);

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUNIC_TABLE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ROLL_BENCH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GILDING_FOUNDRY, RenderLayer.getCutout());

        Registry.register(Registries.DATA_COMPONENT_TYPE, MCDE.id("component"), EnchantmentSlots.COMPONENT_TYPE);

        ClientPlayNetworking.registerGlobalReceiver(Config.PACKET_ID, (config, context) -> {
            MCDE.setConfig(config);
        });

        ClientPlayNetworking.registerGlobalReceiver(LockedSlotsPacket.PACKET_ID, RollBenchScreenHandler::receiveNewLocks);
        ClientPlayNetworking.registerGlobalReceiver(GildingPacket.PACKET_ID, GildingFoundryScreenHandler::receiveNewEnchantment);
    }
}
