package forestry.core.utils;

import com.mojang.authlib.GameProfile;
import forestry.core.network.IStreamable;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.connection.ConnectionType;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public abstract class NBTUtilForestry {
	private static final String GAME_PROFILE_ID = "Id";
	private static final String GAME_PROFILE_NAME = "Name";

	public static CompoundTag writeStreamableToNbt(IStreamable streamable, CompoundTag nbt, HolderLookup.Provider registries) {
		return writeStreamableToNbt(streamable, nbt, (RegistryAccess) registries);
	}

	public static CompoundTag writeStreamableToNbt(IStreamable streamable, CompoundTag nbt, RegistryAccess registries) {
		RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
		streamable.writeData(data);

		byte[] bytes = new byte[data.readableBytes()];
		data.getBytes(0, bytes);
		nbt.putByteArray("dataBytes", bytes);
		return nbt;
	}

	public static void readStreamableFromNbt(IStreamable streamable, CompoundTag nbt, HolderLookup.Provider registries) {
		readStreamableFromNbt(streamable, nbt, (RegistryAccess) registries);
	}

	public static void readStreamableFromNbt(IStreamable streamable, CompoundTag nbt, RegistryAccess registries) {
		if (nbt.contains("dataBytes")) {
			byte[] bytes = nbt.getByteArray("dataBytes");
			RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), registries, ConnectionType.NEOFORGE);
			streamable.readData(data);
		}
	}

	@Nullable
	public static GameProfile readGameProfile(CompoundTag nbt) {
		if (nbt.isEmpty()) {
			return null;
		}

		UUID id = nbt.hasUUID(GAME_PROFILE_ID) ? nbt.getUUID(GAME_PROFILE_ID) : null;
		String name = nbt.contains(GAME_PROFILE_NAME) ? nbt.getString(GAME_PROFILE_NAME) : null;
		if (id == null && (name == null || name.isEmpty())) {
			return null;
		}
		return new GameProfile(id, name);
	}

	public static CompoundTag writeGameProfile(CompoundTag nbt, GameProfile profile) {
		if (profile.getId() != null) {
			nbt.putUUID(GAME_PROFILE_ID, profile.getId());
		}
		if (profile.getName() != null) {
			nbt.putString(GAME_PROFILE_NAME, profile.getName());
		}
		return nbt;
	}

	@Nullable
	public static CompoundTag getItemStackTag(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		return customData == null || customData.isEmpty() ? null : customData.copyTag();
	}

	public static void setItemStackTag(ItemStack stack, CompoundTag tag) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
	}
}
