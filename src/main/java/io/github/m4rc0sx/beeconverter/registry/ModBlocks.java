package io.github.m4rc0sx.beeconverter.registry;

import io.github.m4rc0sx.beeconverter.BeeConverter;
import io.github.m4rc0sx.beeconverter.block.ConverterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
  public static final DeferredRegister.Blocks BLOCKS =
      DeferredRegister.createBlocks(BeeConverter.MOD_ID);

  public static final DeferredBlock<ConverterBlock> BEE_CONVERTER =
      BLOCKS.registerBlock(
          "bee_converter",
          ConverterBlock::new,
          BlockBehaviour.Properties.of()
              .mapColor(MapColor.COLOR_YELLOW)
              .strength(3.0F, 6.0F)
              .sound(SoundType.METAL)
              .requiresCorrectToolForDrops());

  private ModBlocks() {}
}
