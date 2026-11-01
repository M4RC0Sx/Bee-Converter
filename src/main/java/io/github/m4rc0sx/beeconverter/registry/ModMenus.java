package io.github.m4rc0sx.beeconverter.registry;

import io.github.m4rc0sx.beeconverter.BeeConverter;
import io.github.m4rc0sx.beeconverter.menu.ConverterMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, BeeConverter.MOD_ID);

  public static final Supplier<MenuType<ConverterMenu>> CONVERTER =
      MENUS.register("bee_converter", () -> IMenuTypeExtension.create(ConverterMenu::new));

  private ModMenus() {}
}
