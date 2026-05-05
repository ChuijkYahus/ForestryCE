package forestry.core.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fails datagen when the painting_variant directory and the
 * minecraft:placeable painting tag drift apart. The placeable tag is
 * hand-maintained alongside the painting JSONs, so a new variant
 * without a tag entry would silently fail to be placeable in survival.
 */
public final class ForestryPaintingPlaceableValidator implements DataProvider {
	private static final Path RESOURCES_ROOT = Path.of("src", "main", "resources");
	private static final Path PAINTING_DIR = RESOURCES_ROOT.resolve("data/forestry/painting_variant");
	private static final Path PLACEABLE_TAG = RESOURCES_ROOT.resolve("data/minecraft/tags/painting_variant/placeable.json");

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		Set<String> variants = listVariants();
		Set<String> tagged = readPlaceableTag();

		Set<String> missingFromTag = new TreeSet<>(variants);
		missingFromTag.removeAll(tagged);

		Set<String> tagOrphans = new TreeSet<>(tagged);
		tagOrphans.removeAll(variants);

		if (!missingFromTag.isEmpty() || !tagOrphans.isEmpty()) {
			throw new IllegalStateException(String.format(
				"painting_variant / placeable tag drift detected:\n  missing from %s: %s\n  tag entries with no painting_variant JSON: %s",
				PLACEABLE_TAG, missingFromTag, tagOrphans));
		}

		return CompletableFuture.completedFuture(null);
	}

	@Override
	public String getName() {
		return "Forestry Painting placeable tag validator";
	}

	private static Set<String> listVariants() {
		if (!Files.isDirectory(PAINTING_DIR)) {
			return Set.of();
		}
		try (Stream<Path> stream = Files.list(PAINTING_DIR)) {
			return stream
				.filter(p -> p.getFileName().toString().endsWith(".json"))
				.map(p -> {
					String name = p.getFileName().toString();
					return "forestry:" + name.substring(0, name.length() - ".json".length());
				})
				.collect(Collectors.toCollection(TreeSet::new));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Set<String> readPlaceableTag() {
		if (!Files.isRegularFile(PLACEABLE_TAG)) {
			throw new IllegalStateException("Missing placeable tag at " + PLACEABLE_TAG);
		}
		try {
			JsonElement json = JsonParser.parseString(Files.readString(PLACEABLE_TAG));
			JsonObject obj = json.getAsJsonObject();
			Set<String> entries = new TreeSet<>();
			obj.getAsJsonArray("values").forEach(v -> entries.add(v.getAsString()));
			return entries;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
