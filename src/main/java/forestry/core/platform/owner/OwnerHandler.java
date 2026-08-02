package forestry.core.platform.owner;

import com.mojang.authlib.GameProfile;
import forestry.api.core.INbtReadable;
import forestry.api.core.INbtWritable;
import forestry.core.platform.network.IStreamable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.UUID;

public class OwnerHandler implements IOwnerHandler, IStreamable, INbtWritable, INbtReadable {
	@Nullable
	private GameProfile owner = null;

	@Override
	@Nullable
	public GameProfile getOwner() {
		return this.owner;
	}

	@Override
	public void setOwner(GameProfile owner) {
		this.owner = owner;
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		if (this.owner == null) {
			data.writeBoolean(false);
		} else {
			data.writeBoolean(true);
			data.writeLong(this.owner.getId().getMostSignificantBits());
			data.writeLong(this.owner.getId().getLeastSignificantBits());
			data.writeUtf(this.owner.getName());
		}
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		if (data.readBoolean()) {
			GameProfile owner = new GameProfile(new UUID(data.readLong(), data.readLong()), data.readUtf());
			setOwner(owner);
		}
	}

	@Override
	public void read(CompoundTag data, HolderLookup.Provider registries) {
		if (data.contains("owner_name")) {
			UUID id = data.hasUUID("owner_uuid") ? data.getUUID("owner_uuid") : null;
			setOwner(new GameProfile(id, data.getString("owner_name")));
		}
	}

	@Override
	public CompoundTag write(CompoundTag data, HolderLookup.Provider registries) {
		if (this.owner != null) {
			if (this.owner.getId() != null) {
				data.putUUID("owner_uuid", this.owner.getId());
			}
			data.putString("owner_name", this.owner.getName());
		}
		return data;
	}
}
