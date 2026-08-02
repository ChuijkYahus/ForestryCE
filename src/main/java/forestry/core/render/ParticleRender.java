package forestry.core.render;

import forestry.core.config.ForestryConfig;
import forestry.core.entities.ParticleIgnition;
import forestry.core.entities.ParticleSmoke;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;


@OnlyIn(Dist.CLIENT)
public class ParticleRender {
	private static final DustParticleOptions HONEY_DUST = new DustParticleOptions(new Vector3f(0.9F, 0.75F, 0.0F), 1.0F);

	public static boolean shouldSpawnParticle(Level world) {
		if (!ForestryConfig.CLIENT.showParticles.get()) {
			return false;
		}

		Minecraft mc = Minecraft.getInstance();
		ParticleStatus particleSetting = mc.options.particles().get();

		if (particleSetting == ParticleStatus.MINIMAL) { // minimal
			return world.random.nextInt(10) == 0;
		} else if (particleSetting == ParticleStatus.DECREASED) { // decreased
			return world.random.nextInt(3) != 0;
		} else { // all
			return true;
		}
	}


	public static void addEntityHoneyDustFX(Level world, double x, double y, double z) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		world.addParticle(HONEY_DUST, x, y, z, 0, 0, 0);
		//		effectRenderer.addEffect(new ParticleHoneydust(world, x, y, z, 0, 0, 0));
	}

	public static void addEntityExplodeFX(Level world, double x, double y, double z) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		ParticleEngine effectRenderer = Minecraft.getInstance().particleEngine;
		//TODO particle data
		Particle Particle = effectRenderer.createParticle(DustParticleOptions.REDSTONE, x, y, z, 0, 0, 0);
		effectRenderer.add(Particle);
	}


	public static void addEntityIgnitionFX(ClientLevel world, double x, double y, double z) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		ParticleEngine effectRenderer = Minecraft.getInstance().particleEngine;
		effectRenderer.add(new ParticleIgnition(world, x, y, z));
	}

	public static void addEntitySmokeFX(Level world, double x, double y, double z) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		ParticleEngine effectRenderer = Minecraft.getInstance().particleEngine;
		effectRenderer.add(new ParticleSmoke((ClientLevel) world, x, y, z));
	}

	public static void addEntityPotionFX(Level world, double x, double y, double z, int color) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		float red = (color >> 16 & 255) / 255.0F;
		float green = (color >> 8 & 255) / 255.0F;
		float blue = (color & 255) / 255.0F;

		ParticleEngine effectRenderer = Minecraft.getInstance().particleEngine;
		//TODO - maybe EFFECT?
		//TODO particle data
		Particle particle = effectRenderer.createParticle(DustParticleOptions.REDSTONE, x, y, z, 0, 0, 0);
		if (particle != null) {
			particle.setColor(red, green, blue);
			effectRenderer.add(particle);
		}
	}

	public static void addPortalFx(Level world, BlockPos pos, RandomSource rand) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		int j = rand.nextInt(2) * 2 - 1;
		int k = rand.nextInt(2) * 2 - 1;
		double xPos = (double) pos.getX() + 0.5D + 0.25D * (double) j;
		double yPos = (float) pos.getY() + rand.nextFloat();
		double zPos = (double) pos.getZ() + 0.5D + 0.25D * (double) k;
		double xSpeed = rand.nextFloat() * (float) j;
		double ySpeed = ((double) rand.nextFloat() - 0.5D) * 0.125D;
		double zSpeed = rand.nextFloat() * (float) k;
		ParticleEngine effectRenderer = Minecraft.getInstance().particleEngine;
		//TODO particle data
		Particle particle = effectRenderer.createParticle(DustParticleOptions.REDSTONE, xPos, yPos, zPos, xSpeed, ySpeed, zSpeed);
		if (particle != null) {
			effectRenderer.add(particle);
		}
	}
}
