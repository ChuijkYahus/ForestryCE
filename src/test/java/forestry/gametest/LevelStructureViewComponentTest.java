package forestry.gametest;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.multiblock.IAlvearyComponent;
import forestry.api.multiblock.IFarmComponent;
import forestry.api.multiblock.IMultiblockController;
import forestry.api.multiblock.IMultiblockLogicAlveary;
import forestry.api.multiblock.IMultiblockLogicFarm;
import forestry.apiculture.multiblock.AlvearyPattern;
import forestry.core.multiblock.LevelStructureView;
import forestry.farming.multiblock.FarmPattern;

/**
 * Covers the addon-component fallback in {@link LevelStructureView#typeIdFor}: a block entity implementing
 * {@link IAlvearyComponent} / {@link IFarmComponent} maps to a prefixed type id that is never a reserved
 * {@code *_plain} / {@code *_gearbox} role, while a BE implementing neither stays a non-component.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class LevelStructureViewComponentTest {
	private static final BlockState ANY_STATE = Blocks.FURNACE.defaultBlockState();

	@GameTest(template = "empty")
	public static void addonAlvearyComponentIsRecognised(GameTestHelper helper) {
		String id = LevelStructureView.typeIdFor(new FakeAlvearyPart());
		helper.assertTrue(id != null && id.startsWith(AlvearyPattern.PREFIX),
				"an addon IAlvearyComponent must map to an alveary_ type id, got " + id);
		helper.assertTrue(!AlvearyPattern.PLAIN.equals(id),
				"an addon component must not usurp the reserved plain interior/top role");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void addonFarmComponentIsRecognised(GameTestHelper helper) {
		String id = LevelStructureView.typeIdFor(new FakeFarmPart());
		helper.assertTrue(id != null && id.startsWith(FarmPattern.PREFIX),
				"an addon IFarmComponent must map to a farm_ type id, got " + id);
		helper.assertTrue(!FarmPattern.PLAIN.equals(id) && !FarmPattern.GEARBOX.equals(id),
				"an addon component must not usurp a reserved farm role (plain/gearbox)");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void nonComponentBlockEntityMapsToNull(GameTestHelper helper) {
		helper.assertTrue(LevelStructureView.typeIdFor(new FakeInert()) == null,
				"a BE implementing no multiblock-part interface must not be treated as a component");
		helper.succeed();
	}

	// --- fakes: real BlockEntities whose type (furnace) is absent from the member map, so they exercise the fallback ---

	private static class FakeInert extends BlockEntity {
		FakeInert() {
			super(BlockEntityType.FURNACE, BlockPos.ZERO, ANY_STATE);
		}
	}

	private static class FakeAlvearyPart extends BlockEntity implements IAlvearyComponent<IMultiblockLogicAlveary> {
		FakeAlvearyPart() {
			super(BlockEntityType.FURNACE, BlockPos.ZERO, ANY_STATE);
		}

		@Override
		public BlockPos getCoordinates() {
			return getBlockPos();
		}

		@Nullable
		@Override
		public GameProfile getOwner() {
			return null;
		}

		@Override
		public IMultiblockLogicAlveary getMultiblockLogic() {
			return null;
		}

		@Override
		public void onMachineAssembled(IMultiblockController controller, BlockPos min, BlockPos max) {
		}

		@Override
		public void onMachineBroken() {
		}
	}

	private static class FakeFarmPart extends BlockEntity implements IFarmComponent<IMultiblockLogicFarm> {
		FakeFarmPart() {
			super(BlockEntityType.FURNACE, BlockPos.ZERO, ANY_STATE);
		}

		@Override
		public BlockPos getCoordinates() {
			return getBlockPos();
		}

		@Nullable
		@Override
		public GameProfile getOwner() {
			return null;
		}

		@Override
		public IMultiblockLogicFarm getMultiblockLogic() {
			return null;
		}

		@Override
		public void onMachineAssembled(IMultiblockController controller, BlockPos min, BlockPos max) {
		}

		@Override
		public void onMachineBroken() {
		}
	}
}
