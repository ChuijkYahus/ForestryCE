package forestry.core.items;

import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemBlockTesr<B extends Block> extends ItemBlockForestry<B> {
	// Tracks every instance so that CoreClientHandler can register a single
	// IClientItemExtensions (returning the BEWLR) for them at RegisterClientExtensionsEvent
	// time. Replaces the per-instance Item#initializeClient override removed in 1.21.
	private static final List<ItemBlockTesr<?>> INSTANCES = new ArrayList<>();

	public ItemBlockTesr(B block, Properties builder) {
		super(block, builder);
		INSTANCES.add(this);
	}

	public ItemBlockTesr(B block) {
		this(block, new Properties());
	}

	public static List<ItemBlockTesr<?>> getInstances() {
		return Collections.unmodifiableList(INSTANCES);
	}
}
