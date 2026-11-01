package io.github.m4rc0sx.beeconverter.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConverterMenuTest {
  /** Simulates the network round trip: container data is written and read as a signed short. */
  private static int overTheWire(int value) {
    return (short) value;
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 32_767, 32_768, 65_535, 65_536, 100_000, 1_000_000})
  void combineRestoresValueAfterShortSync(int value) {
    int low = overTheWire(value & 0xFFFF);
    int high = overTheWire(value >>> 16);

    assertEquals(value, ConverterMenu.combine(low, high));
  }
}
