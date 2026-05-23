package forestry.core.owner;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;

import java.util.Optional;
import java.util.UUID;

public final class GameProfileDataSerializer {
	private GameProfileDataSerializer() {
	}

	private static final StreamCodec<RegistryFriendlyByteBuf, GameProfile> GAME_PROFILE_STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, GameProfile::getId,
			ByteBufCodecs.STRING_UTF8, GameProfile::getName,
			GameProfile::new
	);

	public static final EntityDataSerializer<Optional<GameProfile>> INSTANCE = EntityDataSerializer.forValueType(
			GAME_PROFILE_STREAM_CODEC.apply(ByteBufCodecs::optional)
	);
}
