package forestry.core.advancements;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * This class is used to determine if the player's inventory changes and now they have a specific genetic specimen.
 * I was gonna say "this is used for advancements" but then I remembered where this file is located and that was redundant to mention.
 */
public class InventorySpecimenChangeTrigger extends InventoryChangeTrigger {

	static final ResourceLocation ID = new ResourceLocation("forestry", "specimen_specific_inventory_changed");

	public TriggerInstance createInstance(JsonObject json, ContextAwarePredicate predicate, DeserializationContext deserializationContext) {
		JsonObject jsonobject = GsonHelper.getAsJsonObject(json, "slots", new JsonObject());
		MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(jsonobject.get("occupied"));
		MinMaxBounds.Ints minmaxbounds$ints1 = MinMaxBounds.Ints.fromJson(jsonobject.get("full"));
		MinMaxBounds.Ints minmaxbounds$ints2 = MinMaxBounds.Ints.fromJson(jsonobject.get("empty"));
		ItemPredicate[] aitempredicate = ItemPredicate.fromJsonArray(json.get("items"));
		ResourceLocation required = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
		return new TriggerInstance(predicate, minmaxbounds$ints, minmaxbounds$ints1, minmaxbounds$ints2, aitempredicate, required);
	}

	@Override
	public void trigger(ServerPlayer player, Inventory inventory, ItemStack stack) {
		int fullSlots = 0;
		int emptySlots = 0;
		int occupiedSlots = 0;

		for(int i = 0; i < inventory.getContainerSize(); ++i) {
			ItemStack itemstack = inventory.getItem(i);
			if (itemstack.isEmpty()) {
				++emptySlots;
			} else {
				++occupiedSlots;
				if (itemstack.getCount() >= itemstack.getMaxStackSize()) {
					++fullSlots;
				}
			}
		}

		this.trigger(player, inventory, stack, fullSlots, emptySlots, occupiedSlots);
	}

	private void trigger(ServerPlayer player, Inventory inventory, ItemStack stack, int full, int empty, int occupied) {
		this.trigger(player, (p_43166_) -> {
			return p_43166_.matches(inventory, stack, full, empty, occupied);
		});
	}

	public static class TriggerInstance extends AbstractCriterionTriggerInstance {
		private final MinMaxBounds.Ints slotsOccupied;
		private final MinMaxBounds.Ints slotsFull;
		private final MinMaxBounds.Ints slotsEmpty;
		private final ItemPredicate[] predicates;
		private final ResourceLocation req;

		public TriggerInstance(ContextAwarePredicate player, MinMaxBounds.Ints slotsOccupied, MinMaxBounds.Ints slotsFull, MinMaxBounds.Ints slotsEmpty, ItemPredicate[] predicates, ResourceLocation req) {
			super(InventorySpecimenChangeTrigger.ID, player);
			this.slotsOccupied = slotsOccupied;
			this.slotsFull = slotsFull;
			this.slotsEmpty = slotsEmpty;
			this.predicates = predicates;
			this.req = req;
		}

		/*public static InventorySpecimenChangeTrigger.TriggerInstance hasItems(ItemPredicate... items) {
			return new InventorySpecimenChangeTrigger.TriggerInstance(ContextAwarePredicate.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, items);
		}

		public static InventorySpecimenChangeTrigger.TriggerInstance hasItems(ItemLike... items) {
			ItemPredicate[] aitempredicate = new ItemPredicate[items.length];

			for(int i = 0; i < items.length; ++i) {
				aitempredicate[i] = new ItemPredicate((TagKey)null, ImmutableSet.of(items[i].asItem()), MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, EnchantmentPredicate.NONE, EnchantmentPredicate.NONE, (Potion)null, NbtPredicate.ANY);
			}

			return hasItems(aitempredicate);
		}*/

		public JsonObject serializeToJson(SerializationContext conditions) {
			JsonObject jsonobject = super.serializeToJson(conditions);
			jsonobject.addProperty("tag", req.toString());
			return jsonobject;
		}

		public boolean matches(Inventory inventory, ItemStack stack, int full, int empty, int occupied) {
			if (!this.slotsFull.matches(full)) {
				return false;
			} else if (!this.slotsEmpty.matches(empty)) {
				return false;
			} else if (!this.slotsOccupied.matches(occupied)) {
				return false;
			} else {
				int i = this.predicates.length;
				if (i == 0) {
					return true;
				} else if (i != 1) {
					List<ItemPredicate> list = new ObjectArrayList(this.predicates);
					int j = inventory.getContainerSize();

					for(int k = 0; k < j; ++k) {
						if (list.isEmpty()) {
							return true;
						}

						ItemStack itemstack = inventory.getItem(k);
						if (!itemstack.isEmpty()) {
							list.removeIf((pred) -> pred.matches(itemstack));
						}
					}

					return list.isEmpty();
				} else {
					return !stack.isEmpty() && this.predicates[0].matches(stack);
				}
			}
		}
	}

}
