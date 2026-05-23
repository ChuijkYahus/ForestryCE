package forestry.mail;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.ForestryRegistries;
import forestry.core.utils.NBTUtilForestry;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.IPostalCarrier;
import forestry.core.utils.PlayerUtil;
import forestry.mail.carriers.PostalCarriers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class MailAddress implements IMailAddress {
	public static final Codec<MailAddress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("carrier").forGetter(address -> ForestryRegistries.POSTAL_CARRIER.getKey(address.getCarrier())),
		Codec.STRING.optionalFieldOf("name", "").forGetter(MailAddress::getName),
		UUIDUtil.CODEC.optionalFieldOf("id").forGetter(address -> Optional.ofNullable(address.gameProfile.getId()))
	).apply(instance, MailAddress::fromCodec));

	private static final GameProfile INVALID_GAME_PROFILE = new GameProfile(new UUID(0, 0), "");

	public static final MailAddress INVALID = new MailAddress(null, INVALID_GAME_PROFILE);

	@Nullable
	private final IPostalCarrier carrier;
	private final GameProfile gameProfile; // gameProfile is a fake GameProfile for traders, and real for players

	public MailAddress(GameProfile gameProfile) {

		this.carrier = PostalCarriers.PLAYER.value();
		this.gameProfile = gameProfile;
	}

	public MailAddress(String name) {
		Preconditions.checkNotNull(name, "name must not be null");
		Preconditions.checkArgument(StringUtils.isNotBlank(name), "name must not be blank");

		this.carrier = PostalCarriers.TRADER.value();
		this.gameProfile = INVALID_GAME_PROFILE;
	}

	public MailAddress(CompoundTag nbt) {
		IPostalCarrier carrier = null;
		GameProfile gameProfile = INVALID_GAME_PROFILE;
		if (nbt.contains("carrier")) {
			carrier = ForestryRegistries.POSTAL_CARRIER.get(ResourceLocation.tryParse(nbt.getString("carrier")));
		}

		if (carrier == null) {
			carrier = PostalCarriers.PLAYER.value();
		} else if (nbt.contains("profile")) {
			CompoundTag profileTag = nbt.getCompound("profile");
			gameProfile = NBTUtilForestry.readGameProfile(profileTag);
			if (gameProfile == null) {
				gameProfile = INVALID_GAME_PROFILE;
			}
		}

		this.carrier = carrier;
		this.gameProfile = gameProfile;
	}

	private MailAddress(@Nullable IPostalCarrier carrier, GameProfile gameProfile) {
		this.carrier = carrier;
		this.gameProfile = gameProfile;
	}

	private static MailAddress fromCodec(ResourceLocation carrierId, String name, Optional<UUID> id) {
		IPostalCarrier carrier = ForestryRegistries.POSTAL_CARRIER.get(carrierId);
		if (carrier == null) {
			carrier = PostalCarriers.PLAYER.value();
		}

		GameProfile profile;
		if (StringUtils.isBlank(name) || id.isEmpty()) {
			profile = INVALID_GAME_PROFILE;
		} else {
			profile = new GameProfile(id.get(), name);
		}
		return new MailAddress(carrier, profile);
	}

	public static MailAddress copyOf(IMailAddress address) {
		if (address instanceof MailAddress mailAddress) {
			return mailAddress;
		}

		GameProfile profile = address.getCarrier().equals(PostalCarriers.PLAYER.value())
			? address.getPlayerProfile()
			: new GameProfile(null, address.getName());
		return new MailAddress(address.getCarrier(), profile);
	}

	@Override
	public IPostalCarrier getCarrier() {
		return this.carrier == null ? PostalCarriers.PLAYER.value() : this.carrier;
	}

	@Override
	public String getName() {
		return this.gameProfile.getName();
	}

	@Override
	public boolean isValid() {
		return this.gameProfile.getName() != null && !PlayerUtil.isSameGameProfile(this.gameProfile, INVALID_GAME_PROFILE);
	}

	@Override
	public GameProfile getPlayerProfile() {
		if (!getCarrier().equals(PostalCarriers.PLAYER.value())) {
			return INVALID_GAME_PROFILE;
		}
		return this.gameProfile;
	}

	@Override
	public int hashCode() {
		return this.gameProfile.getName().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MailAddress address)) {
			return false;
		}

		return PlayerUtil.isSameGameProfile(address.gameProfile, this.gameProfile);
	}

	@Override
	public String toString() {
		String name = getName().toLowerCase(Locale.ENGLISH);
		if (getCarrier().equals(PostalCarriers.PLAYER.value())) {
			return getCarrier() + "-" + name + '-' + this.gameProfile.getId();
		} else {
			return getCarrier() + "-" + name;
		}
	}

	@Override
	public CompoundTag write(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		compoundNBT.putString("carrier", ForestryRegistries.POSTAL_CARRIER.getKey(getCarrier()).toString());

		if (this.gameProfile != INVALID_GAME_PROFILE) {
			CompoundTag profileNbt = new CompoundTag();
			NBTUtilForestry.writeGameProfile(profileNbt, this.gameProfile);
			compoundNBT.put("profile", profileNbt);
		}
		return compoundNBT;
	}


}
