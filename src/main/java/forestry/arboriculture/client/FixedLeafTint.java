package forestry.arboriculture.client;

import forestry.api.client.arboriculture.ILeafTint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

import javax.annotation.Nullable;
import java.awt.*;

public record FixedLeafTint(int color) implements ILeafTint {
	// TODO use for Azalea and Cherry trees
	public static final FixedLeafTint NONE = new FixedLeafTint(0xffffff);

	public FixedLeafTint(Color color) {
		this(color.getRGB());
	}

	@Override
	public int get(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
		return this.color;
	}
}
