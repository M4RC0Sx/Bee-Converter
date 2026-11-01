package io.github.m4rc0sx.beeconverter.blockentity;

import cy.jdkdigital.productivebees.common.recipe.BeeConversionRecipe;
import cy.jdkdigital.productivelib.common.block.entity.IUpgradeableBlockEntity;
import cy.jdkdigital.productivelib.common.block.entity.InventoryHandlerHelper;
import cy.jdkdigital.productivelib.registry.LibItems;
import io.github.m4rc0sx.beeconverter.BeeConverter;
import io.github.m4rc0sx.beeconverter.compat.PbBridge;
import io.github.m4rc0sx.beeconverter.config.BeeConverterConfig;
import io.github.m4rc0sx.beeconverter.menu.ConverterMenu;
import io.github.m4rc0sx.beeconverter.process.ConversionProcess;
import io.github.m4rc0sx.beeconverter.registry.ModBlockEntities;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ConverterBlockEntity extends BlockEntity
    implements MenuProvider, IUpgradeableBlockEntity {
  public static final int UPGRADE_SLOTS = 4;

  private final ConverterItemHandler inventory = new ConverterItemHandler(this::onInventoryChanged);
  private final ConverterEnergy energy =
      new ConverterEnergy(BeeConverterConfig.ENERGY_CAPACITY.get(), this::setChanged);
  // ProductiveLib's handler: one upgrade per slot, filtered to the speed upgrades. Implementing
  // IUpgradeableBlockEntity also enables installing them with sneak + right click.
  private final InventoryHandlerHelper.UpgradeHandler upgrades =
      new InventoryHandlerHelper.UpgradeHandler(
          UPGRADE_SLOTS, this, List.of(LibItems.UPGRADE_TIME.get(), LibItems.UPGRADE_TIME_2.get()));
  private final ConversionProcess process = new ConversionProcess();
  private final IItemHandler externalInventory = new ExternalItemHandler(inventory);

  // Recipe lookup walks every bee_conversion recipe, so it is cached until the inputs change or
  // recipes reload.
  private boolean recipesDirty = true;
  @Nullable private RecipeManager cachedRecipeManager;
  private List<BeeConversionRecipe> cachedRecipes = List.of();

  public final ContainerData dataAccess =
      new ContainerData() {
        @Override
        public int get(int index) {
          return switch (index) {
            case ConverterMenu.DATA_ENERGY_LOW -> energy.getEnergyStored() & 0xFFFF;
            case ConverterMenu.DATA_ENERGY_HIGH -> energy.getEnergyStored() >>> 16;
            case ConverterMenu.DATA_CAPACITY_LOW -> energy.getMaxEnergyStored() & 0xFFFF;
            case ConverterMenu.DATA_CAPACITY_HIGH -> energy.getMaxEnergyStored() >>> 16;
            case ConverterMenu.DATA_PROGRESS -> process.progress();
            case ConverterMenu.DATA_MAX_PROGRESS -> attemptDuration();
            default -> 0;
          };
        }

        @Override
        public void set(int index, int value) {
          // Read-only on the server; the client copy lives in the menu.
        }

        @Override
        public int getCount() {
          return ConverterMenu.DATA_COUNT;
        }
      };

  public ConverterBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.BEE_CONVERTER.get(), pos, state);
  }

  public ConverterItemHandler getInventory() {
    return inventory;
  }

  /**
   * Handler for hoppers and pipes: no insertion into the output, extraction only from the output.
   */
  public IItemHandler getExternalInventory() {
    return externalInventory;
  }

  public IEnergyStorage getEnergy() {
    return energy;
  }

  @Override
  public IItemHandlerModifiable getUpgradeHandler() {
    return upgrades;
  }

  /** Upgrades the machine accepts, as ProductiveLib reports them (addons may extend the list). */
  public List<Item> validUpgrades() {
    return upgrades.getValidUpgrades();
  }

  /** Installed speed units: one per Speed upgrade, two per Speedier upgrade. */
  public int speedUnits() {
    return getUpgradeCount(LibItems.UPGRADE_TIME.get())
        + 2 * getUpgradeCount(LibItems.UPGRADE_TIME_2.get());
  }

  /** Ticks the current attempt takes with the installed upgrades. */
  public int attemptDuration() {
    return ConversionProcess.attemptDuration(
        BeeConverterConfig.TICKS_PER_ATTEMPT.get(),
        BeeConverterConfig.UPGRADE_SPEED_MULTIPLIER.get(),
        speedUnits());
  }

  public static void serverTick(
      Level level, BlockPos pos, BlockState state, ConverterBlockEntity converter) {
    if (level instanceof ServerLevel serverLevel) {
      converter.tick(serverLevel);
    }
  }

  private void tick(ServerLevel level) {
    ItemStack cage = inventory.getStackInSlot(ConverterItemHandler.SLOT_CAGE);
    ItemStack item = inventory.getStackInSlot(ConverterItemHandler.SLOT_ITEM);

    if (cage.isEmpty() || item.isEmpty()) {
      resetProgress();
      return;
    }

    List<BeeConversionRecipe> recipes = matchingRecipes(level, cage, item);
    if (recipes.isEmpty()) {
      resetProgress();
      return;
    }

    // A full output only pauses the machine; progress is kept.
    if (!inventory.getStackInSlot(ConverterItemHandler.SLOT_OUTPUT).isEmpty()) {
      return;
    }

    int duration = attemptDuration();
    int energyPerTick =
        ConversionProcess.energyPerTick(BeeConverterConfig.ENERGY_PER_ATTEMPT.get(), duration);
    if (!energy.tryConsume(energyPerTick)) {
      return;
    }

    if (process.advance(duration)) {
      runAttempt(level, cage, recipes);
    }
    setChanged();
  }

  private void runAttempt(ServerLevel level, ItemStack cage, List<BeeConversionRecipe> recipes) {
    var attempt =
        ConversionProcess.attempt(recipes.stream().map(r -> r.chance).toList(), level.random);

    // The item is spent even when the roll fails, as when converting by hand.
    inventory.extractItem(ConverterItemHandler.SLOT_ITEM, 1, false);

    if (attempt.converted()) {
      ItemStack converted = PbBridge.convert(level, cage, recipes.get(attempt.recipeIndex()));
      if (converted.isEmpty()) {
        BeeConverter.LOGGER.warn(
            "Bee conversion at {} produced no bee; the recipe result may be broken", worldPosition);
      } else {
        inventory.setStackInSlot(ConverterItemHandler.SLOT_CAGE, ItemStack.EMPTY);
        inventory.setStackInSlot(ConverterItemHandler.SLOT_OUTPUT, converted);
      }
    }
  }

  private List<BeeConversionRecipe> matchingRecipes(
      ServerLevel level, ItemStack cage, ItemStack item) {
    RecipeManager recipeManager = level.getRecipeManager();
    if (recipesDirty || recipeManager != cachedRecipeManager) {
      cachedRecipes = PbBridge.findRecipes(level, cage, item);
      cachedRecipeManager = recipeManager;
      recipesDirty = false;
    }
    return cachedRecipes;
  }

  private void resetProgress() {
    if (process.progress() != 0) {
      process.reset();
      setChanged();
    }
  }

  private void onInventoryChanged() {
    recipesDirty = true;
    setChanged();
  }

  /** Drops the contents and installed upgrades when the block is broken. */
  public void dropContents(Level level, BlockPos pos) {
    for (IItemHandler handler : List.<IItemHandler>of(inventory, upgrades)) {
      for (int slot = 0; slot < handler.getSlots(); slot++) {
        Containers.dropItemStack(
            level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(slot));
      }
    }
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.put("inventory", inventory.serializeNBT(registries));
    tag.put("upgrades", upgrades.serializeNBT(registries));
    tag.put("energy", energy.serializeNBT(registries));
    tag.putInt("progress", process.progress());
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    upgrades.deserializeNBT(registries, tag.getCompound("upgrades"));
    if (tag.contains("energy")) {
      energy.deserializeNBT(registries, tag.get("energy"));
    }
    process.setProgress(tag.getInt("progress"));
    recipesDirty = true;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("block.beeconverter.bee_converter");
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(
      int containerId, Inventory playerInventory, Player player) {
    return new ConverterMenu(containerId, playerInventory, this);
  }

  private record ExternalItemHandler(ConverterItemHandler delegate) implements IItemHandler {
    @Override
    public int getSlots() {
      return delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
      return delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      return slot == ConverterItemHandler.SLOT_OUTPUT
          ? stack
          : delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return slot == ConverterItemHandler.SLOT_OUTPUT
          ? delegate.extractItem(slot, amount, simulate)
          : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
      return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
      return slot != ConverterItemHandler.SLOT_OUTPUT && delegate.isItemValid(slot, stack);
    }
  }
}
