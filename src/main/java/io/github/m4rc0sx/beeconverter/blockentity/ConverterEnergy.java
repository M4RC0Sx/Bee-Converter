package io.github.m4rc0sx.beeconverter.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Receive-only FE buffer for the outside world; the machine itself spends energy via {@link
 * #tryConsume}.
 */
final class ConverterEnergy extends EnergyStorage {
  private final Runnable onChanged;

  ConverterEnergy(int capacity, Runnable onChanged) {
    super(capacity, capacity, 0);
    this.onChanged = onChanged;
  }

  boolean tryConsume(int amount) {
    if (energy < amount) {
      return false;
    }
    if (amount > 0) {
      energy -= amount;
      onChanged.run();
    }
    return true;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider registries, Tag tag) {
    super.deserializeNBT(registries, tag);
    // The capacity comes from the config and may have shrunk since the energy was saved.
    energy = Math.min(energy, capacity);
  }

  @Override
  public int receiveEnergy(int toReceive, boolean simulate) {
    int received = super.receiveEnergy(toReceive, simulate);
    if (received > 0 && !simulate) {
      onChanged.run();
    }
    return received;
  }
}
