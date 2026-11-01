package io.github.m4rc0sx.beeconverter.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BeeConverterConfig {
  public static final ModConfigSpec SPEC;

  public static final ModConfigSpec.IntValue ENERGY_CAPACITY;
  public static final ModConfigSpec.IntValue ENERGY_PER_ATTEMPT;
  public static final ModConfigSpec.IntValue TICKS_PER_ATTEMPT;
  public static final ModConfigSpec.DoubleValue UPGRADE_SPEED_MULTIPLIER;

  static {
    ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

    ENERGY_CAPACITY =
        builder
            .comment("Internal FE buffer of the Bee Converter.")
            .defineInRange("energyCapacity", 10_000, 1, 1_000_000);
    ENERGY_PER_ATTEMPT =
        builder
            .comment(
                "FE spent on one conversion attempt. It is spread over the attempt's ticks, so",
                "speed upgrades make the machine draw more FE per tick but not more per attempt.")
            .defineInRange("energyPerAttempt", 2_000, 0, 1_000_000);
    TICKS_PER_ATTEMPT =
        builder
            .comment(
                "Ticks one conversion attempt takes without upgrades (each attempt consumes one item).")
            // Capped at Short.MAX_VALUE because menu data is synced as shorts.
            .defineInRange("ticksPerAttempt", 400, 1, Short.MAX_VALUE);
    UPGRADE_SPEED_MULTIPLIER =
        builder
            .comment(
                "Attempt time is multiplied by this once per speed unit. A Speed upgrade is one unit",
                "and a Speedier upgrade two, so four Speedier upgrades apply it eight times.")
            .defineInRange("upgradeSpeedMultiplier", 0.6, 0.05, 1.0);

    SPEC = builder.build();
  }

  private BeeConverterConfig() {}
}
