package forestry.core.advancements;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import forestry.api.genetics.IBreedingTracker;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.capability.IIndividualHandlerItem;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class DiscoverSpeciesTrigger extends SimpleCriterionTrigger<DiscoverSpeciesTrigger.TriggerInstance> {


	public static final ResourceLocation ID = new ResourceLocation("forestry", "pickup_species_trigger");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
		ResourceLocation required = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
		return new TriggerInstance(player, required);
	}

	public void trigger(Level level, GameProfile gp, ResourceLocation speciesID) {

		if (level.getServer() == null) return;

		ServerLevel serverLevel = level.getServer().getLevel(level.dimension());
		if (serverLevel == null) return;
		Player player = serverLevel.getPlayerByUUID(gp.getId());
		if (player instanceof ServerPlayer serverPlayer) {
			this.trigger(serverPlayer, instance -> instance.check(speciesID));
		}
	}

	public static class TriggerInstance extends AbstractCriterionTriggerInstance {

		private final ResourceLocation req;

		public TriggerInstance(ContextAwarePredicate player, ResourceLocation req) {
			super(ID, player);
			this.req = req;
		}

		public static TriggerInstance checkDiscovered(ResourceLocation id) {
			return new TriggerInstance(ContextAwarePredicate.ANY, id);
		}

		public boolean check(ResourceLocation speciesID) {
			return speciesID.equals(req);
		}

		@Override
		public JsonObject serializeToJson(SerializationContext context) {
			JsonObject json = super.serializeToJson(context);
			json.addProperty("tag", req.toString());
			return json;
		}
	}

}
