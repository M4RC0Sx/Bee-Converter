package io.github.m4rc0sx.beeconverter.blockentity;

import cy.jdkdigital.productivelib.common.item.AbstractUpgradeItem;
import io.github.m4rc0sx.beeconverter.compat.PbBridge;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Three slots: filled input cage, conversion item, converted output cage. */
public class ConverterItemHandler extends ItemStackHandler {
  public static final int SLOT_CAGE = 0;
  public static final int SLOT_ITEM = 1;
  public static final int SLOT_OUTPUT = 2;

  private final Runnable onChanged;

  public ConverterItemHandler(Runnable onChanged) {
    super(3);
    this.onChanged = onChanged;
  }

  @Override
  public boolean isItemValid(int slot, ItemStack stack) {
    return switch (slot) {
      case SLOT_CAGE -> PbBridge.isFilledCage(stack);
      // Cages and upgrades never act as a conversion item; rejecting them stops shift-clicks
      // landing here.
      case SLOT_ITEM ->
          !PbBridge.isCage(stack) && !(stack.getItem() instanceof AbstractUpgradeItem);
      default -> false;
    };
  }

  @Override
  public int getSlotLimit(int slot) {
    return slot == SLOT_ITEM ? super.getSlotLimit(slot) : 1;
  }

  @Override
  protected void onContentsChanged(int slot) {
    onChanged.run();
  }
}
