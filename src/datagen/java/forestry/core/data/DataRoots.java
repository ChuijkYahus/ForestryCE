package forestry.core.data;

import java.nio.file.Path;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * The generated resource root of each jar. Core's root is the data run's output folder and the other
 * three are its siblings, so every root is derived from the run rather than assumed relative to a
 * working directory.
 *
 * <p>Core's root stays the output folder because {@code HashCache} deletes every file under that
 * folder which no provider wrote. The ownership manifests sit beside it and would not survive a run
 * from the parent. They are deleted in the last task of this work, and the output folder moves up to
 * the parent then.
 */
public final class DataRoots {
	public static final String CORE = "resources";
	public static final String FARMS = "resources_farms";
	public static final String MAIL = "resources_mail";
	public static final String BUTTERFLIES = "resources_butterflies";

	private DataRoots() {
	}

	/**
	 * @param event     The gather event the run's output folder is read from
	 * @param directory The root directory name, one of the constants above
	 * @return The pack output every provider belonging to that jar writes to
	 */
	public static PackOutput of(GatherDataEvent event, String directory) {
		Path root = event.getGenerator().getPackOutput().getOutputFolder().getParent();
		return new PackOutput(root.resolve(directory));
	}
}
