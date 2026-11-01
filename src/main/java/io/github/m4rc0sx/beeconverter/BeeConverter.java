package io.github.m4rc0sx.beeconverter;

import io.github.m4rc0sx.beeconverter.compat.UpgradeTooltips;
import io.github.m4rc0sx.beeconverter.config.BeeConverterConfig;
import io.github.m4rc0sx.beeconverter.registry.ModBlockEntities;
import io.github.m4rc0sx.beeconverter.registry.ModBlocks;
import io.github.m4rc0sx.beeconverter.registry.ModCreativeTabs;
import io.github.m4rc0sx.beeconverter.registry.ModItems;
import io.github.m4rc0sx.beeconverter.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BeeConverter.MOD_ID)
public final class BeeConverter {
  public static final String MOD_ID = "beeconverter";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  public BeeConverter(IEventBus modEventBus, ModContainer container) {
    ModBlocks.BLOCKS.register(modEventBus);
    ModItems.ITEMS.register(modEventBus);
    ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
    ModMenus.MENUS.register(modEventBus);
    ModCreativeTabs.TABS.register(modEventBus);

    modEventBus.addListener(BeeConverter::registerCapabilities);
    // ProductiveLib posts its upgrade tooltip event on the game bus, not the mod bus.
    NeoForge.EVENT_BUS.addListener(UpgradeTooltips::addValidBlock);
    container.registerConfig(ModConfig.Type.COMMON, BeeConverterConfig.SPEC);
  }

  private static void registerCapabilities(RegisterCapabilitiesEvent event) {
    event.registerBlockEntity(
        Capabilities.ItemHandler.BLOCK,
        ModBlockEntities.BEE_CONVERTER.get(),
        (converter, side) -> converter.getExternalInventory());
    event.registerBlockEntity(
        Capabilities.EnergyStorage.BLOCK,
        ModBlockEntities.BEE_CONVERTER.get(),
        (converter, side) -> converter.getEnergy());
  }
}
