package forestry.mail.postoffice;

import forestry.Forestry;
import forestry.api.mail.*;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.letters.EnumDeliveryState;
import forestry.mail.letters.PostageUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import forestry.mail.letters.LetterUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostOffice extends SavedData implements IPostOffice {
	public static final String SAVE_NAME = "forestry_mail";
	private static final String KEY_COLLECTED = "collected";
	private static final String KEY_LEGACY = "CPS";

	// Cheapest first, matching the ordinal walk this replaced. The id breaks ties so two mods'
	// stamps of equal value keep a stable order
	private static final Comparator<Item> CHEAPEST_FIRST = (left, right) -> {
		int byPostage = Integer.compare(PostageUtil.getPostage(left), PostageUtil.getPostage(right));
		return byPostage != 0 ? byPostage : BuiltInRegistries.ITEM.getKey(left).compareTo(BuiltInRegistries.ITEM.getKey(right));
	};

	private final Object2IntOpenHashMap<Item> collectedStamps = new Object2IntOpenHashMap<>();

	public PostOffice() {
	}

	public PostOffice(CompoundTag tag) {
		if (tag.contains(KEY_COLLECTED, Tag.TAG_COMPOUND)) {
			readCollected(tag.getCompound(KEY_COLLECTED));
		} else {
			readLegacy(tag);
		}
	}

	private void readCollected(CompoundTag collected) {
		for (String key : collected.getAllKeys()) {
			int count = collected.getInt(key);
			if (count <= 0) {
				continue;
			}

			ResourceLocation id = ResourceLocation.tryParse(key);
			Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);

			// Nothing can be handed back for an item that no longer exists, so drop it loudly
			if (item == Items.AIR) {
				Forestry.LOGGER.warn("Post office dropped {} collected stamp(s) of unknown item {}", count, key);
				continue;
			}

			this.collectedStamps.put(item, count);
		}
	}

	// Saves from before the postage data map keyed an array by EnumPostage.ordinal(), so CPS1 through
	// CPS7 were P_1 through P_100. CPS0 was never incremented and CPS8 (P_200) never had a stamp item
	private void readLegacy(CompoundTag tag) {
		EnumStampDefinition[] byLegacyIndex = EnumStampDefinition.VALUES;

		for (int i = 1; i <= byLegacyIndex.length; i++) {
			int count = tag.getInt(KEY_LEGACY + i);
			if (count > 0) {
				this.collectedStamps.put(MailItems.STAMPS.item(byLegacyIndex[i - 1]), count);
			}
		}

		int dropped = tag.getInt(KEY_LEGACY + "0") + tag.getInt(KEY_LEGACY + (byLegacyIndex.length + 1));
		if (dropped > 0) {
			Forestry.LOGGER.warn("Post office dropped {} collected stamp(s) from a denomination that never had an item", dropped);
		}
	}

	@Override
	public CompoundTag save(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		CompoundTag collected = new CompoundTag();

		for (Object2IntMap.Entry<Item> entry : this.collectedStamps.object2IntEntrySet()) {
			if (entry.getIntValue() > 0) {
				collected.putInt(BuiltInRegistries.ITEM.getKey(entry.getKey()).toString(), entry.getIntValue());
			}
		}

		compoundNBT.put(KEY_COLLECTED, collected);
		return compoundNBT;
	}

	// / STAMP MANAGMENT
	@Override
	public ItemStack getAnyStamp(int max) {
		List<Item> order = new ArrayList<>(this.collectedStamps.keySet());
		order.sort(CHEAPEST_FIRST);

		for (Item stamp : order) {
			ItemStack withdrawn = getAnyStamp(stamp, max);
			if (!withdrawn.isEmpty()) {
				return withdrawn;
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack getAnyStamp(Item stamp, int max) {
		int available = this.collectedStamps.getInt(stamp);
		int collected = Math.min(max, available);

		if (collected <= 0) {
			return ItemStack.EMPTY;
		}

		if (collected == available) {
			this.collectedStamps.removeInt(stamp);
		} else {
			this.collectedStamps.put(stamp, available - collected);
		}

		// The stamp collector is the only caller, and it marks nothing dirty of its own
		setDirty();
		return new ItemStack(stamp, collected);
	}

	@Override
	public void collectPostage(NonNullList<ItemStack> stamps) {
		for (ItemStack stamp : stamps) {
			if (stamp == null || !PostageUtil.isStamp(stamp)) {
				continue;
			}

			this.collectedStamps.addTo(stamp.getItem(), stamp.getCount());
		}

		setDirty();
	}

	// / DELIVERY
	@Override
	public IPostalState lodgeLetter(ServerLevel world, ItemStack itemstack, boolean doLodge) {
		ILetter letter = LetterUtils.getLetter(itemstack);
		if (letter == null) {
			return EnumDeliveryState.NOT_MAILABLE;
		}

		if (letter.isProcessed()) {
			return EnumDeliveryState.ALREADY_MAILED;
		}

		if (!letter.isPostPaid()) {
			return EnumDeliveryState.NOT_POSTPAID;
		}

		if (!letter.isMailable()) {
			return EnumDeliveryState.NOT_MAILABLE;
		}

		IPostalState state = EnumDeliveryState.NOT_MAILABLE;
		IMailAddress address = letter.getRecipient();
		if (address != null) {
			IPostalCarrier carrier = address.getCarrier();
			state = carrier.deliverLetter(world, this, address, itemstack, doLodge);
		}

		if (!state.isOk()) {
			return state;
		}

		collectPostage(letter.getPostage());

		setDirty();
		return EnumDeliveryState.OK;

	}

	public static PostOffice getOrCreate(ServerLevel level) {
		SavedData.Factory<PostOffice> factory = new SavedData.Factory<>(PostOffice::new, (tag, registries) -> new PostOffice(tag));
		return level.getDataStorage().computeIfAbsent(factory, PostOffice.SAVE_NAME);
	}
}
