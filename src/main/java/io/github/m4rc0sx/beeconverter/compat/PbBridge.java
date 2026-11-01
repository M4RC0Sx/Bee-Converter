package io.github.m4rc0sx.beeconverter.compat;

import cy.jdkdigital.productivebees.common.entity.bee.ConfigurableBee;
import cy.jdkdigital.productivebees.common.item.BeeCage;
import cy.jdkdigital.productivebees.common.recipe.BeeConversionRecipe;
import cy.jdkdigital.productivebees.init.ModRecipeTypes;
import cy.jdkdigital.productivebees.util.BeeHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The only class that touches Productive Bees internals. Productive Bees has no public API, so a
 * change in its code should only ever require changes here.
 */
public final class PbBridge {
  private PbBridge() {}

  /** Whether the stack is a Productive Bees cage that currently holds a bee. */
  public static boolean isFilledCage(ItemStack stack) {
    return BeeCage.isFilled(stack);
  }

  /** Whether the stack is any Productive Bees cage, filled or empty. */
  public static boolean isCage(ItemStack stack) {
    return stack.getItem() instanceof BeeCage;
  }

  /** All {@code bee_conversion} recipes that match the bee in the cage and the given item. */
  public static List<BeeConversionRecipe> findRecipes(
      ServerLevel level, ItemStack cage, ItemStack item) {
    String beeType = BeeCage.getBeeType(cage);
    if (beeType == null || item.isEmpty()) {
      return List.of();
    }
    var input =
        new BeeHelper.IdentifierInventory(
            beeType, BuiltInRegistries.ITEM.getKey(item.getItem()).toString());

    List<BeeConversionRecipe> matches = new ArrayList<>();
    for (RecipeHolder<BeeConversionRecipe> holder :
        level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.BEE_CONVERSION_TYPE.get())) {
      if (holder.value().matches(input, level)) {
        matches.add(holder.value());
      }
    }
    return matches;
  }

  /**
   * Builds the cage that results from converting the bee in {@code cage} with {@code recipe}.
   * Follows {@code BeeHelper.itemInteract}: configurable bees get their type and default
   * attributes, and the new bee inherits genes and age from the old one.
   *
   * @return a single filled cage of the same kind as the input, or empty if the result is not a bee
   */
  public static ItemStack convert(ServerLevel level, ItemStack cage, BeeConversionRecipe recipe) {
    var result = recipe.result.get();
    if (result == null || result.getBeeEntity() == null) {
      return ItemStack.EMPTY;
    }
    Entity created = result.getBeeEntity().create(level);
    if (!(created instanceof Bee newBee)) {
      return ItemStack.EMPTY;
    }
    if (newBee instanceof ConfigurableBee configurable) {
      configurable.setBeeType(result.getBeeType().toString());
      configurable.setDefaultAttributes();
    }

    Bee oldBee = BeeCage.getEntityFromStack(cage, level, true);
    if (oldBee != null) {
      BeeHelper.setOffspringAttributes(newBee, oldBee, oldBee);
      newBee.setAge(oldBee.getAge());
    }

    ItemStack converted = cage.copyWithCount(1);
    BeeCage.captureEntity(newBee, converted);
    return converted;
  }
}
