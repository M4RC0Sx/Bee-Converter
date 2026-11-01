package io.github.m4rc0sx.beeconverter.client;

import io.github.m4rc0sx.beeconverter.BeeConverter;
import io.github.m4rc0sx.beeconverter.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BeeConverter.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
  private ClientSetup() {}

  @SubscribeEvent
  public static void registerScreens(RegisterMenuScreensEvent event) {
    event.register(ModMenus.CONVERTER.get(), ConverterScreen::new);
  }
}
