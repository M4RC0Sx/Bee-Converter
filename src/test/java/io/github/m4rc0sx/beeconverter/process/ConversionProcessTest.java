package io.github.m4rc0sx.beeconverter.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class ConversionProcessTest {
  @Test
  void attemptIsDueEveryTicksPerAttempt() {
    ConversionProcess process = new ConversionProcess();

    for (int tick = 1; tick < 5; tick++) {
      assertFalse(process.advance(5), "tick " + tick);
    }
    assertTrue(process.advance(5));
    assertEquals(0, process.progress());
  }

  @Test
  void resetClearsProgress() {
    ConversionProcess process = new ConversionProcess();
    process.advance(10);
    process.advance(10);

    process.reset();

    assertEquals(0, process.progress());
  }

  @Test
  void loweringTicksPerAttemptMidCycleTriggersImmediately() {
    ConversionProcess process = new ConversionProcess();
    process.setProgress(50);

    assertTrue(process.advance(10));
  }

  @Test
  void chanceOfOneAlwaysConverts() {
    RandomSource random = RandomSource.create(1L);
    for (int i = 0; i < 1000; i++) {
      assertTrue(ConversionProcess.attempt(List.of(1.0F), random).converted());
    }
  }

  @Test
  void chanceOfZeroNeverConverts() {
    RandomSource random = RandomSource.create(1L);
    for (int i = 0; i < 1000; i++) {
      assertFalse(ConversionProcess.attempt(List.of(0.0F), random).converted());
    }
  }

  @Test
  void conversionRateFollowsChance() {
    RandomSource random = RandomSource.create(42L);
    int trials = 20_000;
    int converted = 0;
    for (int i = 0; i < trials; i++) {
      if (ConversionProcess.attempt(List.of(0.25F), random).converted()) {
        converted++;
      }
    }

    assertEquals(0.25, (double) converted / trials, 0.02);
  }

  @Test
  void picksAmongAllMatchingRecipes() {
    RandomSource random = RandomSource.create(7L);
    boolean[] seen = new boolean[3];
    for (int i = 0; i < 200; i++) {
      seen[ConversionProcess.attempt(List.of(0.5F, 0.5F, 0.5F), random).recipeIndex()] = true;
    }

    assertTrue(seen[0] && seen[1] && seen[2]);
  }

  @Test
  void durationWithoutUpgradesIsTheBase() {
    assertEquals(400, ConversionProcess.attemptDuration(400, 0.6, 0));
  }

  @Test
  void eachSpeedUnitAppliesTheMultiplier() {
    assertEquals(240, ConversionProcess.attemptDuration(400, 0.6, 1));
    assertEquals(52, ConversionProcess.attemptDuration(400, 0.6, 4));
  }

  @Test
  void fourSpeedierUpgradesAreNearlyInstant() {
    // Four Speedier upgrades are eight speed units.
    assertEquals(7, ConversionProcess.attemptDuration(400, 0.6, 8));
  }

  @Test
  void durationNeverDropsBelowOneTick() {
    assertEquals(1, ConversionProcess.attemptDuration(1, 0.05, 8));
  }

  @Test
  void timeReductionMatchesTheUpgradeUnits() {
    // One Speed upgrade is one unit, one Speedier upgrade two.
    assertEquals(40, ConversionProcess.timeReductionPercent(0.6, 1));
    assertEquals(64, ConversionProcess.timeReductionPercent(0.6, 2));
    assertEquals(0, ConversionProcess.timeReductionPercent(1.0, 2));
  }

  @Test
  void energyPerTickCoversTheWholeAttempt() {
    assertEquals(5, ConversionProcess.energyPerTick(2_000, 400));
    // 2000 / 7 does not divide evenly: round up so the attempt is never cheaper than configured.
    assertEquals(286, ConversionProcess.energyPerTick(2_000, 7));
    assertEquals(0, ConversionProcess.energyPerTick(0, 7));
  }

  @Test
  void noMatchingRecipeIsRejected() {
    RandomSource random = RandomSource.create(1L);

    assertThrows(
        IllegalArgumentException.class, () -> ConversionProcess.attempt(List.of(), random));
  }
}
