package forestry.core.items;

import forestry.api.core.IItemSubtype;
import net.minecraft.world.item.Item;

import java.util.Locale;

public class ItemFruit extends ItemForestryFood {

	public enum EnumFruit implements IItemSubtype {
		CHERRY,
		WALNUT,
		CHESTNUT,
		LEMON,
		PLUM,
		DATES,
		PAPAYA;

		private final String name;

		EnumFruit() {
			this.name = name().toLowerCase(Locale.ENGLISH);
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	private final EnumFruit type;

	public ItemFruit(EnumFruit type) {
		super(1, 0.2f, (new Item.Properties()));
		this.type = type;
	}

	public EnumFruit getType() {
		return this.type;
	}

	@Override
	public boolean canBeDepleted() {
		return false;
	}
}
