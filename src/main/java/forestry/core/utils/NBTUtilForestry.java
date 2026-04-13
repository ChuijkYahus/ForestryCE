package forestry.core.utils;

import forestry.core.network.IStreamable;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

/**
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public abstract class NBTUtilForestry {

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
}
