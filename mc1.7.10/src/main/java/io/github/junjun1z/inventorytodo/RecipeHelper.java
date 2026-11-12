package io.github.junjun1z.inventorytodo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/** Looks up crafting table recipes (vanilla and every mod that registers normal crafting recipes). */
final class RecipeHelper {

    private RecipeHelper() {}

    /** One ingredient of a recipe: the item and how many are needed per craft */
    static final class Ingredient {

        final ItemStack stack;
        int count;

        Ingredient(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }

    /** Result of {@link #find}: ingredients for one craft and how many items one craft makes */
    static final class Recipe {

        final List<Ingredient> ingredients = new ArrayList<>();
        int outputCount = 1;
    }

    /** @return the first crafting recipe that makes the item, or null */
    static Recipe find(ItemStack target) {
        List<?> recipes;
        try {
            recipes = CraftingManager.getInstance()
                .getRecipeList();
        } catch (Throwable t) {
            return null;
        }
        for (Object o : recipes) {
            if (!(o instanceof IRecipe)) continue;
            try {
                IRecipe r = (IRecipe) o;
                ItemStack out = r.getRecipeOutput();
                if (!makes(out, target)) continue;
                List<Object> inputs = inputsOf(r);
                if (inputs == null) continue;
                Recipe result = new Recipe();
                result.outputCount = Math.max(1, out.stackSize);
                for (Object in : inputs) {
                    ItemStack s = firstStack(in);
                    if (s == null || s.getItem() == null) continue;
                    add(result.ingredients, s);
                }
                if (!result.ingredients.isEmpty()) return result;
            } catch (Throwable ignored) {
                // a broken recipe from some mod: try the next one
            }
        }
        return null;
    }

    private static boolean makes(ItemStack out, ItemStack target) {
        if (out == null || out.getItem() != target.getItem()) return false;
        if (out.getItemDamage() != target.getItemDamage() && out.getItemDamage() != 32767
            && !target.getItem()
                .isDamageable())
            return false;
        return !target.hasTagCompound() || ItemStack.areItemStackTagsEqual(out, target);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> inputsOf(IRecipe r) {
        List<Object> list = new ArrayList<>();
        if (r instanceof ShapedRecipes) {
            for (ItemStack s : ((ShapedRecipes) r).recipeItems) list.add(s);
        } else if (r instanceof ShapelessRecipes) {
            list.addAll(((ShapelessRecipes) r).recipeItems);
        } else if (r instanceof ShapedOreRecipe) {
            for (Object o : ((ShapedOreRecipe) r).getInput()) list.add(o);
        } else if (r instanceof ShapelessOreRecipe) {
            list.addAll(((ShapelessOreRecipe) r).getInput());
        } else {
            return null;
        }
        return list;
    }

    /** An ItemStack, or the first entry of an ore dictionary list */
    private static ItemStack firstStack(Object in) {
        if (in instanceof ItemStack) return (ItemStack) in;
        if (in instanceof List) {
            for (Object o : (List<?>) in) if (o instanceof ItemStack) return (ItemStack) o;
        }
        return null;
    }

    private static void add(List<Ingredient> list, ItemStack s) {
        for (Ingredient i : list) {
            if (i.stack.getItem() == s.getItem() && i.stack.getItemDamage() == s.getItemDamage()
                && ItemStack.areItemStackTagsEqual(i.stack, s)) {
                i.count++;
                return;
            }
        }
        ItemStack copy = s.copy();
        copy.stackSize = 1;
        list.add(new Ingredient(copy, 1));
    }
}
