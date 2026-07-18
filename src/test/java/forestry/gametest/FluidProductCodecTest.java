package forestry.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.mojang.serialization.JsonOps;

import forestry.api.ForestryConstants;
import forestry.api.core.FluidProduct;

@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class FluidProductCodecTest {
	@GameTest(template = "empty")
	public static void referenceImplProducesStack(GameTestHelper helper) {
		FluidProduct product = FluidProduct.of(Fluids.WATER, 1000);
		FluidStack stack = product.createFluidStack();
		FluidStack random = product.createRandomFluidStack(helper.getLevel().random);
		if (stack.getFluid() != Fluids.WATER || stack.getAmount() != 1000) {
			helper.fail("createFluidStack did not return the wrapped fluid/amount: " + stack);
			return;
		}
		if (random.getFluid() != Fluids.WATER || random.getAmount() != 1000) {
			helper.fail("createRandomFluidStack did not return the wrapped fluid/amount: " + random);
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void referenceImplRoundTrips(GameTestHelper helper) {
		FluidProduct original = FluidProduct.of(Fluids.LAVA, 500);

		// Stream round-trip
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		FluidProduct.STREAM_CODEC.encode(buf, original);
		FluidProduct streamDecoded = FluidProduct.STREAM_CODEC.decode(buf);
		if (streamDecoded.stack().getFluid() != Fluids.LAVA || streamDecoded.stack().getAmount() != 500) {
			helper.fail("Stream round-trip lost fluid/amount: " + streamDecoded.stack());
			return;
		}

		// JSON round-trip (fluid holder codec needs a registry-backed ops)
		RegistryOps<com.google.gson.JsonElement> jsonOps = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		var json = FluidProduct.MAP_CODEC.codec().encodeStart(jsonOps, original).getOrThrow();
		FluidProduct jsonDecoded = FluidProduct.MAP_CODEC.codec().parse(jsonOps, json).getOrThrow();
		if (jsonDecoded.stack().getFluid() != Fluids.LAVA || jsonDecoded.stack().getAmount() != 500) {
			helper.fail("JSON round-trip lost fluid/amount: " + json);
			return;
		}
		helper.succeed();
	}
}
