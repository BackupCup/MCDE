package net.backupcup.mcd_enchantments;

import net.backupcup.mcd_enchantments.block.ModBlocks;
import net.backupcup.mcd_enchantments.block.entity.ModBlockEntities;
import net.backupcup.mcd_enchantments.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCDEnchantments implements ModInitializer {
	public static final String MOD_ID = "mcd_enchantments";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.RegisterModBlocks();

		ModScreenHandlers.registerAllScreenHandlers();

		ModBlockEntities.registerBlockEntities();
	}
}
