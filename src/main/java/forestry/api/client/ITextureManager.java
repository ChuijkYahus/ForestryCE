package forestry.api.client;

import forestry.api.ForestryConstants;
import forestry.api.core.TemperatureType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * To use Forestry textures in your own screens, bind the {@link ForestrySprites#TEXTURE_ATLAS} texture.
 */
public interface ITextureManager {
	/**
	 * Get a texture atlas sprite that has been registered by Forestry, for Forestry's Gui Texture Map.
	 */
	TextureAtlasSprite getSprite(ResourceLocation location);

	default TextureAtlasSprite getSprite(TemperatureType temperature) {
		return getSprite(temperature.iconTexture);
	}

	default TextureAtlasSprite getSprite(String path) {
		return getSprite(ForestryConstants.forestry(path));
	}
}
