package forestry.core.content.tools;

import forestry.api.ForestryTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public enum ToolTier implements Tier {
	SURVIVALIST(200, 7.0F, 2.5F, 10, () -> {
		return Ingredient.of(ForestryTags.Items.INGOTS_BRONZE);
	});

	private final int uses;
	private final float speed;
	private final float damage;
	private final int enchantmentValue;
	private final LazyLoadedValue<Ingredient> repairIngredient;

	ToolTier(int uses, float speed, float damage, int enchantmentValue, Supplier<Ingredient> repairIngredient) {
		this.uses = uses;
		this.speed = speed;
		this.damage = damage;
		this.enchantmentValue = enchantmentValue;
		this.repairIngredient = new LazyLoadedValue<>(repairIngredient);
	}

	@Override
	public int getUses() {
		return this.uses;
	}

	@Override
	public float getSpeed() {
		return this.speed;
	}

	@Override
	public float getAttackDamageBonus() {
		return this.damage;
	}

	@Override
	public TagKey<Block> getIncorrectBlocksForDrops() {
		// 1.21: replaces getLevel() — bronze sits between iron and stone, use iron's incorrect-for-drops tag
		return BlockTags.INCORRECT_FOR_IRON_TOOL;
	}

	@Override
	public int getEnchantmentValue() {
		return this.enchantmentValue;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return this.repairIngredient.get();
	}
}
