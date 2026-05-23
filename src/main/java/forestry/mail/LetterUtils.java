package forestry.mail;

import forestry.api.mail.ILetter;
import forestry.core.features.CoreDataComponents;
import forestry.mail.features.MailItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;

public class LetterUtils {
	public static ItemStack createLetterStack(ILetter letter, HolderLookup.Provider registries) {
		CompoundTag compoundNBT = new CompoundTag();
		letter.write(compoundNBT, registries);

		ItemStack letterStack = LetterProperties.createStampedLetterStack(letter);
		setLetterData(letterStack, compoundNBT);

		return letterStack;
	}

	@Nullable
	public static ILetter getLetter(ItemStack itemstack, HolderLookup.Provider registries) {
		if (itemstack.isEmpty()) {
			return null;
		}

		if (!LetterUtils.isLetter(itemstack)) {
			return null;
		}

		CompoundTag tag = getLetterData(itemstack);
		if (tag == null) {
			return null;
		}

		return new Letter(tag, registries);
	}

	public static boolean isLetter(ItemStack itemstack) {
		return MailItems.LETTERS.itemEqual(itemstack);
	}

	@Nullable
	public static CompoundTag getLetterData(ItemStack stack) {
		CustomData data = stack.get(CoreDataComponents.LETTER_DATA);
		return data == null || data.isEmpty() ? null : data.copyTag();
	}

	public static void setLetterData(ItemStack stack, CompoundTag tag) {
		if (tag.isEmpty()) {
			stack.remove(CoreDataComponents.LETTER_DATA);
		} else {
			stack.set(CoreDataComponents.LETTER_DATA, CustomData.of(tag.copy()));
		}
	}
}
