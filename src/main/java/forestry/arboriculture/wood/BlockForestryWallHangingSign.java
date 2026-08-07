package forestry.arboriculture.wood;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.wood.IWoodTyped;
import forestry.arboriculture.features.ArboricultureBlocks;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class BlockForestryWallHangingSign extends WallHangingSignBlock implements IWoodTyped {
	private final ForestryWoodType type;

	public BlockForestryWallHangingSign(ForestryWoodType type) {
		super(type.getWoodType(), Properties.of().mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission().strength(1.0F).lootFrom(ArboricultureBlocks.HANGING_SIGN.get(type)::block).ignitedByLava());

		this.type = type;
	}

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.WALL_HANGING_SIGN;
	}

	@Override
	public boolean isFireproof() {
		return false;
	}

	@Override
	public IWoodType getWoodType() {
		return this.type;
	}

	// newBlockEntity / getTicker are inherited from WallHangingSignBlock and use vanilla
	// BlockEntityType.HANGING_SIGN. Forestry registers this block as a valid block for that
	// BE type via BlockEntityTypeAddBlocksEvent in ModuleArboriculture.
}
