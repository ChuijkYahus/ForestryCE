package forestry.core.platform.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IStreamable {
	/**
	 * Called on the serverside to sync additional information about this block to the client.
	 *
	 * @param buffer The stream of data about this object to send to the client.
	 */
	void writeData(RegistryFriendlyByteBuf buffer);

	default void writeData(FriendlyByteBuf buffer) {
		if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
			writeData(registryBuffer);
		} else {
			throw new IllegalArgumentException("Forestry stream sync requires RegistryFriendlyByteBuf");
		}
	}

	/**
	 * Called on the clientside to receive data from the server.
	 *
	 * @param buffer The stream of data about this object sent by the server.
	 */
	void readData(RegistryFriendlyByteBuf buffer);

	default void readData(FriendlyByteBuf buffer) {
		if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
			readData(registryBuffer);
		} else {
			throw new IllegalArgumentException("Forestry stream sync requires RegistryFriendlyByteBuf");
		}
	}
}
