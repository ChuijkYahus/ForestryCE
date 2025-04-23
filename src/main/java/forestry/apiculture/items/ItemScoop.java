/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.apiculture.items;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import forestry.api.ForestryTags;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.core.items.ItemForestry;
import forestry.core.utils.SpeciesUtil;

public class ItemScoop extends ItemForestry {
	public ItemScoop() {
		super(new Item.Properties().durability(10));
	}

	@Override
	public float getDestroySpeed(ItemStack itemstack, BlockState state) {
		if (state.is(ForestryTags.Blocks.MINEABLE_SCOOP)) {
			return 2.0F;
		} else {
			return 1.0F;
		}
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity entity, LivingEntity player) {
		stack.hurtAndBreak(2, player, (living) -> living.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level world, BlockState blockState, BlockPos pos, LivingEntity player) {
		if (!world.isClientSide && blockState.getDestroySpeed(world, pos) != 0.0F) {
			stack.hurtAndBreak(1, player, (living) -> living.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}

		return true;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
		if(interactionTarget instanceof Bee){
			ItemEntity bee=new ItemEntity(interactionTarget.level(), interactionTarget.getX(), interactionTarget.getY(), interactionTarget.getZ(), SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.VANILLA, BeeLifeStage.DRONE));
			interactionTarget.level().addFreshEntity(bee);
			interactionTarget.level().playSound(null,interactionTarget.blockPosition(), SoundEvents.BEE_HURT, SoundSource.PLAYERS,1F,1F);
			interactionTarget.setRemoved(Entity.RemovalReason.DISCARDED);
			stack.hurtAndBreak(1, player, (living) -> living.broadcastBreakEvent(usedHand));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}
