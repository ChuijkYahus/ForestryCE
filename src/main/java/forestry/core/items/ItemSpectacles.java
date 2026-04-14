package forestry.core.items;

import forestry.api.ForestryCapabilities;
import forestry.api.ForestryConstants;
import forestry.api.core.ISpectacleVision;
import forestry.arboriculture.capabilities.SpectacleVision;
import forestry.core.config.Constants;
import forestry.core.utils.ItemTooltipUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.*;
import java.util.List;

public class ItemSpectacles extends ArmorItem {
	public static final String TEXTURE_NATURALIST_ARMOR_PRIMARY = ForestryConstants.MOD_ID + ":" + Constants.TEXTURE_PATH_ITEM + "/naturalist_armor_1.png";
	public static final ISpectacleVision SPECTACLE_VISION = SpectacleVision.INSTANCE;

	public ItemSpectacles() {
		super(ArmorMaterials.LEATHER, Type.HELMET, (new Item.Properties()).durability(100));
	}

	@Override
	public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		return ResourceLocation.parse(TEXTURE_NATURALIST_ARMOR_PRIMARY);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag advanced) {
		ItemTooltipUtil.addInformation(stack, tooltip);
	}
}
