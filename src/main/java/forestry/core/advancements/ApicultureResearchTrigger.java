package forestry.core.advancements;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ApicultureResearchTrigger extends SimpleCriterionTrigger<ApicultureResearchTrigger.TriggerInstance> {


	public static final ResourceLocation ID = new ResourceLocation("forestry", "apiculture_research_trigger");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
		double percentage = GsonHelper.getAsDouble(json, "percent");
		return new TriggerInstance(player, percentage);
	}

	public void trigger(Level level, GameProfile gp, double researchCompletion) {

		if (level.getServer() == null) return;
		ServerLevel serverLevel = level.getServer().getLevel(level.dimension());
		if (serverLevel == null) return;


		Player player = serverLevel.getPlayerByUUID(gp.getId());
		if (player instanceof ServerPlayer serverPlayer) {
			this.trigger(serverPlayer, instance -> instance.check(researchCompletion));
		}
	}

	public static class TriggerInstance extends AbstractCriterionTriggerInstance {

		private final double percentage;

		public TriggerInstance(ContextAwarePredicate player, double p) {
			super(ID, player);
			this.percentage = p;
		}

		public static TriggerInstance checkIfResearchIsGreaterThan(double p) {
			return new TriggerInstance(ContextAwarePredicate.ANY, p);
		}

		public boolean check(double amount) {
			return amount >= percentage;
		}

		@Override
		public JsonObject serializeToJson(SerializationContext context) {
			JsonObject json = super.serializeToJson(context);
			json.addProperty("percent", this.percentage);
			return json;
		}
	}

}
