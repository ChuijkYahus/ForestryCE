package forestry.mail;

import forestry.api.mail.ILetter;
import forestry.api.mail.IMailAddress;
import forestry.core.utils.NBTUtilForestry;
import forestry.mail.features.MailItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class LetterUtils {
	public static ILetter createLetter(IMailAddress sender, IMailAddress recipient) {
		return new Letter(sender, recipient);
	}

	public static ItemStack createLetterStack(ILetter letter, HolderLookup.Provider registries) {
		CompoundTag compoundNBT = new CompoundTag();
		letter.write(compoundNBT, registries);

		ItemStack letterStack = LetterProperties.createStampedLetterStack(letter);
		NBTUtilForestry.setItemStackTag(letterStack, compoundNBT);

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

		CompoundTag tag = NBTUtilForestry.getItemStackTag(itemstack);
		if (tag == null) {
			return null;
		}

		return new Letter(tag, registries);
	}

	@Nullable
	public static ILetter getLetter(ItemStack itemstack) {
		return getLetter(itemstack, RegistryAccess.EMPTY);
	}

	public static boolean isLetter(ItemStack itemstack) {
		return MailItems.LETTERS.itemEqual(itemstack);
	}
}
