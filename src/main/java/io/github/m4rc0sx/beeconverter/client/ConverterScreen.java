package io.github.m4rc0sx.beeconverter.client;

import io.github.m4rc0sx.beeconverter.BeeConverter;
import io.github.m4rc0sx.beeconverter.menu.ConverterMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ConverterScreen extends AbstractContainerScreen<ConverterMenu> {
  private static final ResourceLocation TEXTURE =
      ResourceLocation.fromNamespaceAndPath(BeeConverter.MOD_ID, "textures/gui/bee_converter.png");

  // Positions in the GUI and in the texture; they must match the texture layout.
  private static final int PROGRESS_X = 75;
  private static final int PROGRESS_Y = 39;
  private static final int PROGRESS_U = 204;
  private static final int PROGRESS_V = 0;
  private static final int PROGRESS_WIDTH = 28;
  private static final int PROGRESS_HEIGHT = 8;

  private static final int ENERGY_X = 12;
  private static final int ENERGY_Y = 18;
  private static final int ENERGY_U = 204;
  private static final int ENERGY_V = 20;
  private static final int ENERGY_WIDTH = 10;
  private static final int ENERGY_HEIGHT = 52;

  public ConverterScreen(ConverterMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    // Main panel plus the upgrade column on the right.
    imageWidth = 202;
    imageHeight = 166;
    inventoryLabelY = imageHeight - 94;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    super.render(graphics, mouseX, mouseY, partialTick);
    renderTooltip(graphics, mouseX, mouseY);

    List<Component> tooltip = machineTooltip(mouseX, mouseY);
    if (!tooltip.isEmpty()) {
      graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }
  }

  private List<Component> machineTooltip(int mouseX, int mouseY) {
    if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, mouseX, mouseY)) {
      return List.of(
          Component.translatable("gui.beeconverter.energy", menu.energy(), menu.capacity()));
    }
    if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT, mouseX, mouseY)) {
      int max = menu.maxProgress();
      int percent = max > 0 ? 100 * menu.progress() / max : 0;
      String seconds = String.format(Locale.ROOT, "%.2f", max / 20.0);
      return List.of(
          Component.translatable("gui.beeconverter.progress", percent),
          Component.translatable("gui.beeconverter.attempt_time", seconds)
              .withStyle(ChatFormatting.GRAY));
    }
    if (hoveredSlot instanceof SlotItemHandler slot
        && slot.getItemHandler() == menu.converter().getUpgradeHandler()
        && !slot.hasItem()) {
      List<Component> lines = new ArrayList<>();
      lines.add(Component.translatable("gui.beeconverter.valid_upgrades"));
      // Same style as ProductiveLib's own upgradeable screens: white header, gold upgrade names.
      for (Item upgrade : menu.converter().validUpgrades()) {
        lines.add(
            Component.translatable(upgrade.getDescriptionId()).withStyle(ChatFormatting.GOLD));
      }
      return lines;
    }
    return List.of();
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

    // Honey progress bar fills from the left.
    int max = menu.maxProgress();
    if (max > 0 && menu.progress() > 0) {
      int filled = Math.min(PROGRESS_WIDTH, PROGRESS_WIDTH * menu.progress() / max + 1);
      graphics.blit(
          TEXTURE,
          leftPos + PROGRESS_X,
          topPos + PROGRESS_Y,
          PROGRESS_U,
          PROGRESS_V,
          filled,
          PROGRESS_HEIGHT);
    }

    // Energy bar fills from the bottom.
    if (menu.capacity() > 0) {
      int filled = (int) ((long) ENERGY_HEIGHT * menu.energy() / menu.capacity());
      int empty = ENERGY_HEIGHT - filled;
      graphics.blit(
          TEXTURE,
          leftPos + ENERGY_X,
          topPos + ENERGY_Y + empty,
          ENERGY_U,
          ENERGY_V + empty,
          ENERGY_WIDTH,
          filled);
    }
  }
}
