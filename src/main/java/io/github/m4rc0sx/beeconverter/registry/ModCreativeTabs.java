package io.github.m4rc0sx.beeconverter.registry;

import io.github.m4rc0sx.beeconverter.BeeConverter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
  public static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BeeConverter.MOD_ID);

  public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
      TABS.register(
          "main",
          () ->
              CreativeModeTab.builder()
                  .title(Component.translatable("itemGroup.beeconverter"))
                  .icon(() -> ModItems.BEE_CONVERTER.get().getDefaultInstance())
                  .displayItems((parameters, output) -> output.accept(ModItems.BEE_CONVERTER.get()))
                  .build());

  private ModCreativeTabs() {}
}
