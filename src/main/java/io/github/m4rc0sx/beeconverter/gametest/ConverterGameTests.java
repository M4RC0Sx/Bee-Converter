package io.github.m4rc0sx.beeconverter.gametest;

import cy.jdkdigital.productivebees.common.item.BeeCage;
import cy.jdkdigital.productivebees.init.ModItems;
import cy.jdkdigital.productivelib.event.UpgradeTooltipEvent;
import cy.jdkdigital.productivelib.registry.LibItems;
import io.github.m4rc0sx.beeconverter.BeeConverter;
import io.github.m4rc0sx.beeconverter.blockentity.ConverterBlockEntity;
import io.github.m4rc0sx.beeconverter.blockentity.ConverterItemHandler;
import io.github.m4rc0sx.beeconverter.compat.UpgradeTooltips;
import io.github.m4rc0sx.beeconverter.config.BeeConverterConfig;
import io.github.m4rc0sx.beeconverter.process.ConversionProcess;
import io.github.m4rc0sx.beeconverter.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * End-to-end tests of the machine against the real Productive Bees recipes. The default recipe
 * {@code minecraft:bee + minecraft:hopper -> productivebees:collector_bee} has a chance of 1, so
 * the outcome is deterministic.
 */
@GameTestHolder(BeeConverter.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConverterGameTests {
  private static final String TEMPLATE = "empty";
  private static final BlockPos POS = new BlockPos(2, 1, 2);
  private static final int FULL_ENERGY = 10_000;
  // Longer than one unupgraded attempt (400 ticks by default) plus margin.
  private static final int SLOW_TIMEOUT = 600;

  private ConverterGameTests() {}

  @GameTest(template = TEMPLATE, timeoutTicks = SLOW_TIMEOUT)
  public static void convertsBeeIntoOutput(GameTestHelper helper) {
    ConverterBlockEntity converter = place(helper);
    loadHopperConversion(helper, converter, 3);

    int duration = converter.attemptDuration();
    int expectedEnergy =
        ConversionProcess.energyPerTick(BeeConverterConfig.ENERGY_PER_ATTEMPT.get(), duration)
            * duration;

    helper.succeedWhen(
        () -> {
          ItemStack output = slot(converter, ConverterItemHandler.SLOT_OUTPUT);
          helper.assertTrue(BeeCage.isFilled(output), "output should hold a filled cage");
          helper.assertTrue(
              "productivebees:collector_bee".equals(BeeCage.getBeeType(output)),
              "output bee should be a collector bee but was " + BeeCage.getBeeType(output));
          helper.assertTrue(
              slot(converter, ConverterItemHandler.SLOT_CAGE).isEmpty(),
              "input cage should be consumed");
          helper.assertTrue(
              slot(converter, ConverterItemHandler.SLOT_ITEM).getCount() == 2,
              "exactly one hopper should be spent");
          int spent = FULL_ENERGY - converter.getEnergy().getEnergyStored();
          helper.assertTrue(
              spent == expectedEnergy,
              "energy spent should be " + expectedEnergy + " but was " + spent);
        });
  }

  @GameTest(template = TEMPLATE, timeoutTicks = 40)
  public static void fourSpeedierUpgradesConvertAlmostInstantly(GameTestHelper helper) {
    ConverterBlockEntity converter = place(helper);
    for (int i = 0; i < ConverterBlockEntity.UPGRADE_SLOTS; i++) {
      converter.getUpgradeHandler().setStackInSlot(i, new ItemStack(LibItems.UPGRADE_TIME_2.get()));
    }
    helper.assertTrue(converter.speedUnits() == 8, "four Speedier upgrades are eight speed units");
    loadHopperConversion(helper, converter, 1);

    // Without upgrades this takes 400 ticks; the 40 tick timeout fails the test if they are
    // ignored.
    helper.succeedWhen(
        () ->
            helper.assertTrue(
                BeeCage.isFilled(slot(converter, ConverterItemHandler.SLOT_OUTPUT)),
                "the bee should be converted within a few ticks"));
  }

  @GameTest(template = TEMPLATE)
  public static void acceptsOnlySpeedUpgradesOnePerSlot(GameTestHelper helper) {
    ConverterBlockEntity converter = place(helper);
    var upgrades = converter.getUpgradeHandler();

    helper.assertTrue(
        !upgrades.insertItem(0, new ItemStack(LibItems.UPGRADE_PRODUCTIVITY.get()), true).isEmpty(),
        "a productivity upgrade must be rejected");
    helper.assertTrue(
        upgrades.insertItem(0, new ItemStack(LibItems.UPGRADE_TIME.get()), true).isEmpty(),
        "a Speed upgrade should be accepted");
    helper.assertTrue(
        upgrades.insertItem(0, new ItemStack(LibItems.UPGRADE_TIME_2.get(), 3), true).getCount()
            == 2,
        "each slot should hold a single upgrade");
    helper.assertTrue(
        converter
                .getInventory()
                .insertItem(
                    ConverterItemHandler.SLOT_ITEM,
                    new ItemStack(LibItems.UPGRADE_TIME.get()),
                    true)
                .getCount()
            == 1,
        "an upgrade must not be accepted as the conversion item");
    helper.succeed();
  }

  @GameTest(template = TEMPLATE, timeoutTicks = SLOW_TIMEOUT)
  public static void doesNothingWithoutEnergy(GameTestHelper helper) {
    ConverterBlockEntity converter = place(helper);
    converter.getInventory().setStackInSlot(ConverterItemHandler.SLOT_CAGE, beeCage(helper));
    converter
        .getInventory()
        .setStackInSlot(ConverterItemHandler.SLOT_ITEM, new ItemStack(Items.HOPPER, 3));

    helper.runAfterDelay(
        converter.attemptDuration() + 20L,
        () -> {
          helper.assertTrue(
              slot(converter, ConverterItemHandler.SLOT_ITEM).getCount() == 3,
              "no item should be spent without energy");
          helper.assertTrue(
              slot(converter, ConverterItemHandler.SLOT_OUTPUT).isEmpty(),
              "nothing should be produced without energy");
          helper.succeed();
        });
  }

  @GameTest(template = TEMPLATE, timeoutTicks = SLOW_TIMEOUT)
  public static void ignoresItemWithoutRecipe(GameTestHelper helper) {
    ConverterBlockEntity converter = place(helper);
    converter.getInventory().setStackInSlot(ConverterItemHandler.SLOT_CAGE, beeCage(helper));
    converter
        .getInventory()
        .setStackInSlot(ConverterItemHandler.SLOT_ITEM, new ItemStack(Items.DIRT, 5));
    converter.getEnergy().receiveEnergy(FULL_ENERGY, false);

    helper.runAfterDelay(
        converter.attemptDuration() + 20L,
        () -> {
          helper.assertTrue(
              slot(converter, ConverterItemHandler.SLOT_ITEM).getCount() == 5,
              "an item with no recipe must not be spent");
          helper.assertTrue(
              converter.getEnergy().getEnergyStored() == FULL_ENERGY,
              "no energy should be spent without a matching recipe");
          helper.succeed();
        });
  }

  @GameTest(template = TEMPLATE, timeoutTicks = SLOW_TIMEOUT)
  public static void pausesWhenOutputIsOccupied(GameTestHelper helper) {
    ConverterBlockEntity converter = place(helper);
    converter.getInventory().setStackInSlot(ConverterItemHandler.SLOT_OUTPUT, beeCage(helper));
    loadHopperConversion(helper, converter, 3);

    helper.runAfterDelay(
        converter.attemptDuration() + 20L,
        () -> {
          helper.assertTrue(
              slot(converter, ConverterItemHandler.SLOT_ITEM).getCount() == 3,
              "no item should be spent while the output is occupied");
          helper.assertTrue(
              converter.getEnergy().getEnergyStored() == FULL_ENERGY,
              "no energy should be spent while the output is occupied");
          helper.succeed();
        });
  }

  @GameTest(template = TEMPLATE)
  public static void externalHandlerOnlyExtractsOutput(GameTestHelper helper) {
    ConverterBlockEntity converter = place(helper);
    var external = converter.getExternalInventory();

    helper.assertTrue(
        !external.insertItem(ConverterItemHandler.SLOT_OUTPUT, beeCage(helper), true).isEmpty(),
        "output slot must reject insertion");
    helper.assertTrue(
        external.insertItem(ConverterItemHandler.SLOT_CAGE, beeCage(helper), true).isEmpty(),
        "a filled cage should be accepted in the cage slot");
    helper.assertTrue(
        !external
            .insertItem(
                ConverterItemHandler.SLOT_CAGE, new ItemStack(ModItems.BEE_CAGE.get()), true)
            .isEmpty(),
        "an empty cage must be rejected");
    helper.assertTrue(
        !external.insertItem(ConverterItemHandler.SLOT_ITEM, beeCage(helper), true).isEmpty(),
        "a cage must not be accepted as the conversion item");

    converter
        .getInventory()
        .setStackInSlot(ConverterItemHandler.SLOT_ITEM, new ItemStack(Items.HOPPER));
    helper.assertTrue(
        external.extractItem(ConverterItemHandler.SLOT_ITEM, 1, true).isEmpty(),
        "inputs must not be extractable from outside");

    converter.getInventory().setStackInSlot(ConverterItemHandler.SLOT_OUTPUT, beeCage(helper));
    helper.assertTrue(
        !external.extractItem(ConverterItemHandler.SLOT_OUTPUT, 1, true).isEmpty(),
        "the output must be extractable");
    helper.succeed();
  }

  @GameTest(template = TEMPLATE)
  public static void survivesSaveAndLoad(GameTestHelper helper) {
    ConverterBlockEntity original = place(helper);
    original.getInventory().setStackInSlot(ConverterItemHandler.SLOT_CAGE, beeCage(helper));
    original
        .getInventory()
        .setStackInSlot(ConverterItemHandler.SLOT_ITEM, new ItemStack(Items.HOPPER, 7));
    original.getUpgradeHandler().setStackInSlot(2, new ItemStack(LibItems.UPGRADE_TIME.get()));
    original.getEnergy().receiveEnergy(1234, false);

    var registries = helper.getLevel().registryAccess();
    var saved = original.saveWithFullMetadata(registries);
    var restored =
        (ConverterBlockEntity)
            BlockEntity.loadStatic(
                helper.absolutePos(POS),
                ModBlocks.BEE_CONVERTER.get().defaultBlockState(),
                saved,
                registries);

    helper.assertTrue(restored != null, "block entity should load back");
    helper.assertTrue(
        restored.getEnergy().getEnergyStored() == 1234, "energy should persist across save/load");
    helper.assertTrue(
        slot(restored, ConverterItemHandler.SLOT_ITEM).getCount() == 7,
        "item count should persist across save/load");
    helper.assertTrue(
        BeeCage.isFilled(slot(restored, ConverterItemHandler.SLOT_CAGE)),
        "cage contents should persist across save/load");
    helper.assertTrue(restored.speedUnits() == 1, "upgrades should persist across save/load");
    helper.succeed();
  }

  @GameTest(template = TEMPLATE)
  public static void speedUpgradeTooltipsListTheConverter(GameTestHelper helper) {
    helper.assertTrue(
        tooltipKeys(LibItems.UPGRADE_TIME.get()).contains(UpgradeTooltips.SPEED_KEY),
        "the Speed upgrade tooltip should list the Bee Converter");
    helper.assertTrue(
        tooltipKeys(LibItems.UPGRADE_TIME_2.get()).contains(UpgradeTooltips.SPEEDIER_KEY),
        "the Speedier upgrade tooltip should list the Bee Converter");
    helper.assertTrue(
        tooltipKeys(LibItems.UPGRADE_PRODUCTIVITY.get()).stream()
            .noneMatch(key -> key.startsWith(BeeConverter.MOD_ID)),
        "upgrades the machine does not accept must not list it");
    helper.succeed();
  }

  /** Posts the event ProductiveLib fires when it builds an upgrade tooltip. */
  private static List<String> tooltipKeys(Item upgrade) {
    var event =
        new UpgradeTooltipEvent(
            new ItemStack(upgrade), Item.TooltipContext.EMPTY, new ArrayList<>(), null);
    NeoForge.EVENT_BUS.post(event);
    return event.getValidBlocks().stream()
        .map(UpgradeTooltipEvent.TooltipComponent::translationKey)
        .toList();
  }

  private static ConverterBlockEntity place(GameTestHelper helper) {
    helper.setBlock(POS, ModBlocks.BEE_CONVERTER.get());
    return (ConverterBlockEntity) helper.getBlockEntity(POS);
  }

  /** Loads a vanilla bee, {@code hoppers} hoppers and a full energy buffer. */
  private static void loadHopperConversion(
      GameTestHelper helper, ConverterBlockEntity converter, int hoppers) {
    converter.getInventory().setStackInSlot(ConverterItemHandler.SLOT_CAGE, beeCage(helper));
    converter
        .getInventory()
        .setStackInSlot(ConverterItemHandler.SLOT_ITEM, new ItemStack(Items.HOPPER, hoppers));
    converter.getEnergy().receiveEnergy(FULL_ENERGY, false);
  }

  private static ItemStack slot(ConverterBlockEntity converter, int slot) {
    return converter.getInventory().getStackInSlot(slot);
  }

  /** A bee cage holding a vanilla bee, as produced by Productive Bees when capturing one. */
  private static ItemStack beeCage(GameTestHelper helper) {
    Bee bee = EntityType.BEE.create(helper.getLevel());
    ItemStack cage = new ItemStack(ModItems.BEE_CAGE.get());
    BeeCage.captureEntity(bee, cage);
    return cage;
  }
}
