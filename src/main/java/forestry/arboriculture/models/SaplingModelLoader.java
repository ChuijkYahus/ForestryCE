package forestry.arboriculture.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public class SaplingModelLoader implements IGeometryLoader<SaplingModel> {
	@Override
	public SaplingModel read(JsonObject modelContents, JsonDeserializationContext context) throws JsonParseException {
		return new SaplingModel();
	}
}
