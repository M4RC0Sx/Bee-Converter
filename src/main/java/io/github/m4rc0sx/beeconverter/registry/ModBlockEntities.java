package io.github.m4rc0sx.beeconverter.registry;

import io.github.m4rc0sx.beeconverter.BeeConverter;
import io.github.m4rc0sx.beeconverter.blockentity.ConverterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BeeConverter.MOD_ID);

  // The block must only be resolved inside the supplier: registries are not bound yet when this
  // class initializes.
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConverterBlockEntity>>
      BEE_CONVERTER =
          BLOCK_ENTITIES.register(
              "bee_converter",
              () ->
                  BlockEntityType.Builder.of(
                          ConverterBlockEntity::new, ModBlocks.BEE_CONVERTER.get())
                      .build(null));

  private ModBlockEntities() {}
}
