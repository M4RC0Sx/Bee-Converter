package io.github.m4rc0sx.beeconverter.menu;

import io.github.m4rc0sx.beeconverter.blockentity.ConverterBlockEntity;
import io.github.m4rc0sx.beeconverter.blockentity.ConverterItemHandler;
import io.github.m4rc0sx.beeconverter.registry.ModBlocks;
import io.github.m4rc0sx.beeconverter.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ConverterMenu extends AbstractContainerMenu {
  // Container data is sent as 16-bit values, so energy values are split into two halves.
  public static final int DATA_ENERGY_LOW = 0;
  public static final int DATA_ENERGY_HIGH = 1;
  public static final int DATA_CAPACITY_LOW = 2;
  public static final int DATA_CAPACITY_HIGH = 3;
  public static final int DATA_PROGRESS = 4;
  public static final int DATA_MAX_PROGRESS = 5;
  public static final int DATA_COUNT = 6;

  // Slot layout, shared with the screen. Upgrades sit in a column right of the main panel, where
  // Productive Bees machines keep theirs.
  public static final int CAGE_X = 44;
  public static final int CAGE_Y = 20;
  public static final int ITEM_X = 44;
  public static final int ITEM_Y = 50;
  public static final int OUTPUT_X = 116;
  public static final int OUTPUT_Y = 35;
  public static final int UPGRADE_X = 178;
  public static final int UPGRADE_Y = 8;

  private static final int MACHINE_SLOTS = 3;
  private static final int UPGRADES_END = MACHINE_SLOTS + ConverterBlockEntity.UPGRADE_SLOTS;
  private static final int PLAYER_END = UPGRADES_END + 36;

  private final ConverterBlockEntity converter;
  private final ContainerLevelAccess access;
  private final ContainerData data;

  /** Client-side constructor: the block entity is the client copy at the synced position. */
  public ConverterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
    this(
        containerId,
        playerInventory,
        clientConverter(playerInventory, buffer.readBlockPos()),
        new SimpleContainerData(DATA_COUNT));
  }

  /** Server-side constructor. */
  public ConverterMenu(int containerId, Inventory playerInventory, ConverterBlockEntity converter) {
    this(containerId, playerInventory, converter, converter.dataAccess);
  }

  private ConverterMenu(
      int containerId,
      Inventory playerInventory,
      ConverterBlockEntity converter,
      ContainerData data) {
    super(ModMenus.CONVERTER.get(), containerId);
    this.converter = converter;
    this.access = ContainerLevelAccess.create(converter.getLevel(), converter.getBlockPos());
    this.data = data;

    ConverterItemHandler inventory = converter.getInventory();
    addSlot(new SlotItemHandler(inventory, ConverterItemHandler.SLOT_CAGE, CAGE_X, CAGE_Y));
    addSlot(new SlotItemHandler(inventory, ConverterItemHandler.SLOT_ITEM, ITEM_X, ITEM_Y));
    addSlot(
        new SlotItemHandler(inventory, ConverterItemHandler.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
          @Override
          public boolean mayPlace(ItemStack stack) {
            return false;
          }
        });

    for (int i = 0; i < ConverterBlockEntity.UPGRADE_SLOTS; i++) {
      addSlot(new SlotItemHandler(converter.getUpgradeHandler(), i, UPGRADE_X, UPGRADE_Y + i * 18));
    }

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
      }
    }
    for (int col = 0; col < 9; col++) {
      addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    addDataSlots(data);
  }

  private static ConverterBlockEntity clientConverter(Inventory playerInventory, BlockPos pos) {
    if (playerInventory.player.level().getBlockEntity(pos) instanceof ConverterBlockEntity found) {
      return found;
    }
    throw new IllegalStateException("No Bee Converter at " + pos);
  }

  public ConverterBlockEntity converter() {
    return converter;
  }

  public int energy() {
    return combine(data.get(DATA_ENERGY_LOW), data.get(DATA_ENERGY_HIGH));
  }

  public int capacity() {
    return combine(data.get(DATA_CAPACITY_LOW), data.get(DATA_CAPACITY_HIGH));
  }

  /**
   * Rebuilds an int from its synced halves. Values arrive on the client as signed shorts, so the
   * low half must be masked or anything with bit 15 set turns negative.
   */
  static int combine(int low, int high) {
    return (low & 0xFFFF) | ((high & 0xFFFF) << 16);
  }

  public int progress() {
    return data.get(DATA_PROGRESS);
  }

  /** Ticks per attempt with the installed upgrades. */
  public int maxProgress() {
    return data.get(DATA_MAX_PROGRESS);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    Slot slot = slots.get(index);
    if (!slot.hasItem()) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = slot.getItem();
    ItemStack original = stack.copy();

    if (index < UPGRADES_END) {
      if (!moveItemStackTo(stack, UPGRADES_END, PLAYER_END, true)) {
        return ItemStack.EMPTY;
      }
    } else if (!moveItemStackTo(stack, MACHINE_SLOTS, UPGRADES_END, false)
        // Upgrades are tried first so they never end up in the conversion item slot.
        && !moveItemStackTo(stack, 0, ConverterItemHandler.SLOT_OUTPUT, false)) {
      return ItemStack.EMPTY;
    }

    if (stack.isEmpty()) {
      slot.setByPlayer(ItemStack.EMPTY);
    } else {
      slot.setChanged();
    }
    if (stack.getCount() == original.getCount()) {
      return ItemStack.EMPTY;
    }
    slot.onTake(player, stack);
    return original;
  }

  @Override
  public boolean stillValid(Player player) {
    return stillValid(access, player, ModBlocks.BEE_CONVERTER.get());
  }
}
