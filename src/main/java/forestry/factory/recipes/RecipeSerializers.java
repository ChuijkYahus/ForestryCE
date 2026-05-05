package forestry.factory.recipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RecipeSerializers {
	static <E> void write(FriendlyByteBuf buffer, List<E> list, BiConsumer<FriendlyByteBuf, E> consumer) {
		buffer.writeVarInt(list.size());

		for (E e : list) {
			consumer.accept(buffer, e);
		}
	}

	static <E> List<E> read(FriendlyByteBuf buffer, Function<FriendlyByteBuf, E> reader) {
		int size = buffer.readVarInt();

		ArrayList<E> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(reader.apply(buffer));
		}

		return list;
	}

	public static FluidStack deserializeFluid(JsonObject object) {
		// Recipe JSON parsing predates registry-aware components; use empty registries (no Holder lookups expected).
		CompoundTag tag = (CompoundTag) Dynamic.convert(JsonOps.INSTANCE, NbtOps.INSTANCE, object);
		return FluidStack.parseOptional(RegistryAccess.EMPTY, tag);
	}

	public static JsonObject serializeFluid(FluidStack fluid) {
		return (JsonObject) Dynamic.convert(NbtOps.INSTANCE, JsonOps.INSTANCE, fluid.save(RegistryAccess.EMPTY));
	}

	public static ItemStack item(JsonObject object) {
		CompoundTag tag = (CompoundTag) Dynamic.convert(JsonOps.INSTANCE, NbtOps.INSTANCE, object);
		// Recipe JSON parsing predates registry-aware components; use empty registries (no Holder lookups expected).
		return ItemStack.parse(RegistryAccess.EMPTY, tag).orElse(ItemStack.EMPTY);
	}

	public static JsonObject item(ItemStack stack) {
		return (JsonObject) Dynamic.convert(NbtOps.INSTANCE, JsonOps.INSTANCE, stack.save(RegistryAccess.EMPTY, new CompoundTag()));
	}

	public static Ingredient deserialize(JsonElement resource) {
		if (resource.isJsonArray() && resource.getAsJsonArray().size() == 0) {
			return Ingredient.EMPTY;
		}

		return Ingredient.fromJson(resource);
	}
}
