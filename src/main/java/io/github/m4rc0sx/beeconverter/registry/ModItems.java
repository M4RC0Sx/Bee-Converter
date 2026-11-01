package io.github.m4rc0sx.beeconverter.registry;

import io.github.m4rc0sx.beeconverter.BeeConverter;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
  public static final DeferredRegister.Items ITEMS =
      DeferredRegister.createItems(BeeConverter.MOD_ID);

  public static final DeferredItem<BlockItem> BEE_CONVERTER =
      ITEMS.registerSimpleBlockItem("bee_converter", ModBlocks.BEE_CONVERTER);

  private ModItems() {}
}
