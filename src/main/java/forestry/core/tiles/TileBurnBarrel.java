package forestry.core.tiles;

import forestry.Forestry;
import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.core.blocks.BlockBurnBarrel;
import forestry.core.features.CoreItems;
import forestry.core.features.CoreTiles;
import forestry.core.gui.ContainerBurnBarrel;
import forestry.core.inventory.IInventoryAdapter;
import forestry.core.inventory.InventoryBurnBarrel;
import forestry.core.network.IStreamableGui;
import forestry.core.utils.SlotUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class TileBurnBarrel extends TileBase implements IStreamableGui {

	private int preheatTime = 0;

	private int currentMaxBurnTime = 0; //How long this should burn for, determined by the last accepted item's burn time.
	private int burnTime = 0; //How long until the next item is burned.

	private int ashProductionTimer = 0; //How many ticks until this produces ash
	public static final int ASH_PRODUCTION_TIME = 800; //How many ticks it takes for ash to be produced. 100 ticks means there is a 1:1 ratio between ash produced and number of items this item could smelt.
	public static final int PARTICLE_TICK_INTERVAL = 5; //How many ticks to emit particles on

	private int errorTime = 0;

	private int soundTimer = 0;

	private Random rng = new Random();


	public TileBurnBarrel(BlockPos pos, BlockState state) {
		super(CoreTiles.BURN_BARREL.tileType(), pos, state);
		setInternalInventory(new InventoryBurnBarrel(this));
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {

		int nextOutputSlotIndex = canOutput();
		boolean hasResources = false;

		BlockState newState = level.getBlockState(pos);
		boolean updateState = false;

		//If there is an item burning already, continue processing it
		if (this.burnTime > 0 && nextOutputSlotIndex > -1) {
			this.ashProductionTimer++;
			this.burnTime--;
			this.preheatTime = 5; //We do this so that it can continue to look for items to burn after burning one, since fuel is also ingredients.

			//If the production time has progressed fuly, make some ash.
			if (this.ashProductionTimer >= ASH_PRODUCTION_TIME){

				ItemStack s = this.getInternalInventory().getItem(nextOutputSlotIndex);
				//Update slots where ash already exists first.
				if (s.is(CoreItems.ASH.item()) && s.getCount() < s.getMaxStackSize())
					this.getInternalInventory().setItem(nextOutputSlotIndex, s.copyWithCount(s.getCount() + 1));
				else
					this.getInternalInventory().setItem(nextOutputSlotIndex, CoreItems.ASH.stack(1));

				this.setChanged();
				newState = newState.setValue(BlockBurnBarrel.HAS_ASH, true);
				//Forestry.LOGGER.info("Updating state to yes ash");
				updateState = true;
				this.ashProductionTimer = 0;
			}

		}
		//If there is not an item processing, find one to process
		else if(this.preheatTime > 0 && nextOutputSlotIndex > -1 ) {

			int nextInputIndex = hasInput();
			if (nextInputIndex > -1) {
				hasResources = true;

				ItemStack s = this.getInternalInventory().getItem(nextInputIndex);
				this.burnTime = ForgeHooks.getBurnTime(s, null);
				this.currentMaxBurnTime = burnTime;
				this.getInternalInventory().setItem(nextInputIndex, s.copyWithCount(s.getCount() - 1));
			}
		}
		//No work can be done
		else {
			if (this.ashProductionTimer > 0) this.ashProductionTimer--;
		}

		//Decrease preheat time even if the barrel is burning.
		if (this.preheatTime > 0) {
			this.preheatTime--;
			if (this.preheatTime <= 0){
				//Forestry.LOGGER.info("Updating state to no fire");
				newState = newState.setValue(BlockBurnBarrel.LIT, false);
				updateState = true;
				//We do this here so that it doesnt immediately revert every update.
				//This makes it maybe more useful in building/decoration, I suppose?
			}
		}

		boolean noSpaceInInventory = nextOutputSlotIndex == -1;
		boolean notLit = this.burnTime <= 0 || this.preheatTime <= 0;
		boolean noResources = !hasResources;
		if (noResources) noResources = (hasInput() == -1); //Seems superfluous but the inventory is not checked if the barrel is not burning, causing potential false positives.

		//This just stops errors flashing up every time a new item is processed. There's probably an easier way.
		if (noSpaceInInventory || notLit || noResources){
			if (this.errorTime < 5) errorTime++;
		}
		else this.errorTime = 0; //I coulda sworn there was something to do this automagically.
		IErrorLogic errorLogic = this.getErrorLogic();
		errorLogic.setCondition(noSpaceInInventory && this.errorTime >= 5, ForestryError.NO_SPACE_INVENTORY);
		errorLogic.setCondition(notLit && this.errorTime >= 5, ForestryError.NOT_LIT);
		errorLogic.setCondition(noResources && this.errorTime >= 5, ForestryError.NO_RESOURCE);


		//Handle Block States
		if (updateState)
			level.setBlock(pos, newState, 3);


		//FX
		if (level instanceof ServerLevel serverLevel) {
			if (serverLevel.getBlockState(pos).getValue(BlockBurnBarrel.LIT)) {
				soundTimer--;

				if (soundTimer <= 0) {
					serverLevel.playSound( null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1, 1);
					soundTimer = 20 + rng.nextInt(20);
				}

				float x = pos.getX() + (0.5f);
				float y = pos.getY() + (0.9f);
				float z = pos.getZ() + (0.5f);

				if (updateOnInterval(PARTICLE_TICK_INTERVAL)) {
					serverLevel.sendParticles(
						ParticleTypes.SMOKE,
						x, y, z,
						1,
						0.15, 0, 0.15,
						0.01f
					);
				}

				/*if (updateOnInterval(PARTICLE_TICK_INTERVAL)) {
					serverLevel.sendParticles(
						ParticleTypes.LAVA,
						x, y, z,
						1,
						0.15, 0, 0.15,
						0.1f
					);
				}*/
			}
		}
	}

	public int hasInput(){
		for (int i = InventoryBurnBarrel.SLOT_INPUT_COUNT-1;
			 i >= InventoryBurnBarrel.SLOT_INPUT_1;
			 i--){
			//loop backwards, consuming the last item first

			ItemStack s = this.getInternalInventory().getItem(i);
			if (!s.isEmpty()) {
				int burnAmount = ForgeHooks.getBurnTime(s, null);
				//Forestry.LOGGER.info("Burnable item in slot " + i + " with burn time of " + burnAmount);
				if (burnAmount > 0) {
					return i;
				}
			}
		}
		return -1;
	}

	public int canOutput(){
		for (int i = InventoryBurnBarrel.SLOT_OUTPUT_1;
			 i < InventoryBurnBarrel.SLOT_OUTPUT_1 + InventoryBurnBarrel.SLOT_OUTPUT_COUNT;
			 i++){

			ItemStack s = this.getInternalInventory().getItem(i);
			if (s.isEmpty()) {
				//Forestry.LOGGER.info("There is nothing in slot " + i);
				return i;
			}
			//You never know what funny shenanigans people have going on.
			else if (s.is(CoreItems.ASH.item()) && s.getCount() < s.getMaxStackSize()){ //Deliberately using our ash and not the tag.
				//Forestry.LOGGER.info("There is something already in slot " + i);
				return i;
			}
		}
		return -1;
	}

	@Override
	public void openGui(ServerPlayer player, InteractionHand hand, BlockPos pos) {

		Level level = this.level;

		//Ignite the barrel, if it needs it.
		if (this.preheatTime <= 0) {
			ItemStack heldItem = player.getItemInHand(hand);
			if (heldItem.is(Items.FIRE_CHARGE)) {
				if (!player.isCreative())
					heldItem = heldItem.copyWithCount(heldItem.getCount() - 1);
				this.preheatTime = 5;
				if (level instanceof ServerLevel serverLevel) {
					serverLevel.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS);
					serverLevel.sendParticles(
						ParticleTypes.SMOKE,
						pos.getX()+0.5f,
						pos.getY()+0.9f,
						pos.getZ()+0.5f,
						3,
						0.25,
						0.25,
						0.25,
						0.01
					);
					level.setBlock(pos, level.getBlockState(pos).setValue(BlockBurnBarrel.LIT, true), 3);
				}
				return;
			}
			else if (heldItem.is(Items.FLINT_AND_STEEL)) {
				heldItem.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
				this.preheatTime = 5;
				if (level instanceof ServerLevel serverLevel) {
					serverLevel.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS);
					serverLevel.sendParticles(
						ParticleTypes.SMOKE,
						pos.getX() + 0.5f,
						pos.getY() + 0.9f,
						pos.getZ() + 0.5f,
						3,
						0.25,
						0.25,
						0.25,
						0.01
					);
					level.setBlock(pos, level.getBlockState(pos).setValue(BlockBurnBarrel.LIT, true), 3);
				}
				return;
			}
		}
		super.openGui(player, hand, pos);
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
		return new ContainerBurnBarrel(i, player.getInventory(), this);
	}

	public int getAshProductionTimer(){
		return this.ashProductionTimer;
	}

	public int getBurnTime(){
		return this.burnTime;
	}

	public int getCurrentMaxBurnTime(){
		return this.currentMaxBurnTime;
	}

	public int getValueScaled(int progress, int maxAmount, int scaleAmount){
		if (maxAmount == 0) {
			return 0;
		}
		return progress * scaleAmount / maxAmount;
	}

	/* LOADING & SAVING */
	@Override
	public void saveAdditional(CompoundTag compoundNBT) {
		super.saveAdditional(compoundNBT);
		compoundNBT.putInt("BurnTime", this.burnTime);
		compoundNBT.putInt("MaxBurnTime", this.currentMaxBurnTime);
		compoundNBT.putInt("AshTime", this.ashProductionTimer);
	}

	@Override
	public void load(CompoundTag compoundNBT) {
		super.load(compoundNBT);
		this.burnTime = compoundNBT.getInt("BurnTime");
		this.currentMaxBurnTime = compoundNBT.getInt("MaxBurnTime");
		this.ashProductionTimer = compoundNBT.getInt("AshTime");
	}

	@Override
	public void writeData(FriendlyByteBuf data) {
		super.writeData(data);
		data.writeVarInt(this.burnTime);
		data.writeVarInt(this.currentMaxBurnTime);
		data.writeVarInt(this.ashProductionTimer);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void readData(FriendlyByteBuf data) {
		super.readData(data);
		this.burnTime = data.readVarInt();
		this.currentMaxBurnTime = data.readVarInt();
		this.ashProductionTimer = data.readVarInt();
	}

	@Override
	public void writeGuiData(FriendlyByteBuf data) {
		data.writeVarInt(this.burnTime);
		data.writeVarInt(this.currentMaxBurnTime);
		data.writeVarInt(this.ashProductionTimer);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void readGuiData(FriendlyByteBuf data) {
		this.burnTime = data.readVarInt();
		this.currentMaxBurnTime = data.readVarInt();
		this.ashProductionTimer = data.readVarInt();
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.forestry.burn_barrel");
	}

}
