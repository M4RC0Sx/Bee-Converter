package io.github.m4rc0sx.beeconverter.compat;

import cy.jdkdigital.productivelib.event.UpgradeTooltipEvent;
import cy.jdkdigital.productivelib.registry.LibItems;
import io.github.m4rc0sx.beeconverter.config.BeeConverterConfig;
import io.github.m4rc0sx.beeconverter.process.ConversionProcess;
import io.github.m4rc0sx.beeconverter.registry.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * Lists the Bee Converter under "Valid blocks" in the tooltip of the speed upgrades. ProductiveLib
 * builds that list from this event; every mod adds its own machines.
 */
public final class UpgradeTooltips {
  public static final String SPEED_KEY =
      "beeconverter.information.upgrade.upgrade_time.bee_converter";
  public static final String SPEEDIER_KEY =
      "beeconverter.information.upgrade.upgrade_time_2.bee_converter";

  private UpgradeTooltips() {}

  public static void addValidBlock(UpgradeTooltipEvent event) {
    // With entities set, the event is filling an entity filter tooltip instead.
    if (event.getEntities() != null) {
      return;
    }
    Item upgrade = event.getStack().getItem();
    double multiplier = BeeConverterConfig.UPGRADE_SPEED_MULTIPLIER.get();
    Component name = Component.translatable(ModBlocks.BEE_CONVERTER.get().getDescriptionId());

    if (upgrade == LibItems.UPGRADE_TIME.get()) {
      event.addValidBlock(name, SPEED_KEY, ConversionProcess.timeReductionPercent(multiplier, 1));
    } else if (upgrade == LibItems.UPGRADE_TIME_2.get()) {
      event.addValidBlock(
          name, SPEEDIER_KEY, ConversionProcess.timeReductionPercent(multiplier, 2));
    }
  }
}
