package io.github.m4rc0sx.beeconverter.process;

import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * Minecraft-free logic of the conversion cycle: attempt timing and the chance roll. It mirrors how
 * Productive Bees converts a bee by hand: one matching recipe is picked at random, the item is
 * spent whether or not the roll succeeds, and the roll is {@code nextFloat() < chance}.
 */
public final class ConversionProcess {
  /** Result of one attempt: which recipe was picked and whether the bee converted. */
  public record Attempt(int recipeIndex, boolean converted) {}

  private int progress;

  public int progress() {
    return progress;
  }

  public void setProgress(int progress) {
    this.progress = Math.max(0, progress);
  }

  public void reset() {
    progress = 0;
  }

  /**
   * Advances the timer by one tick.
   *
   * @return true when the timer reached {@code ticksPerAttempt} and an attempt is due; the timer
   *     restarts in that case
   */
  public boolean advance(int ticksPerAttempt) {
    progress++;
    if (progress >= ticksPerAttempt) {
      progress = 0;
      return true;
    }
    return false;
  }

  /**
   * Ticks one attempt takes with the installed speed upgrades: {@code baseTicks * multiplier ^
   * speedUnits}, never below one tick.
   */
  public static int attemptDuration(int baseTicks, double multiplier, int speedUnits) {
    return Math.max(1, (int) Math.round(baseTicks * Math.pow(multiplier, speedUnits)));
  }

  /** How much shorter an attempt gets with {@code speedUnits} installed, as a whole percentage. */
  public static int timeReductionPercent(double multiplier, int speedUnits) {
    return (int) Math.round((1 - Math.pow(multiplier, speedUnits)) * 100);
  }

  /** FE drawn per tick so that a whole attempt costs at least {@code energyPerAttempt}. */
  public static int energyPerTick(int energyPerAttempt, int attemptDuration) {
    return (energyPerAttempt + attemptDuration - 1) / attemptDuration;
  }

  /**
   * Picks one of the matching recipes at random and rolls its chance.
   *
   * @param chances the chance of every matching recipe, in the same order as the recipe list
   * @throws IllegalArgumentException if there is no matching recipe
   */
  public static Attempt attempt(List<Float> chances, RandomSource random) {
    if (chances.isEmpty()) {
      throw new IllegalArgumentException("At least one matching recipe is required");
    }
    int index = random.nextInt(chances.size());
    return new Attempt(index, random.nextFloat() < chances.get(index));
  }
}
