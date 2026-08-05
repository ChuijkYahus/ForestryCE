package forestry.core.tiles;

import forestry.api.core.INbtWritable;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ISpecies;
import forestry.api.genetics.ISpeciesType;
import forestry.api.genetics.capability.IIndividualHandlerItem;
import forestry.core.network.IStreamable;
import forestry.core.utils.NetworkUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EscritoireGameBoard implements INbtWritable, IStreamable {
	private static final RandomSource rand = RandomSource.create();
	private static final int TOKEN_COUNT_MAX = 22;
	private static final int TOKEN_COUNT_MIN = 6;

	public static final String NBT_TOKEN_COUNT = "TokenCount";
	public static final String NBT_GAME_TOKENS = "GameTokens";

	private final List<EscritoireGameToken> gameTokens = new ArrayList<>(TOKEN_COUNT_MAX);
	private int tokenCount;

	public EscritoireGameBoard() {
	}

	public EscritoireGameBoard(CompoundTag nbt) {
		if (!nbt.contains(NBT_GAME_TOKENS, Tag.TAG_LIST)) {
			return;
		}

		ListTag list = nbt.getList(NBT_GAME_TOKENS, Tag.TAG_COMPOUND);
		int size = Math.min(list.size(), TOKEN_COUNT_MAX);

		if (size == 0) {
			return;
		}

		EscritoireGameToken[] tokens = new EscritoireGameToken[size];
		boolean complete = true;

		for (int j = 0; j < size; ++j) {
			CompoundTag tokenNbt = list.getCompound(j);
			int index = tokenNbt.getByte("Slot");
			if (index < 0 || index >= size || tokens[index] != null) {
				index = j;
			}

			EscritoireGameToken token = new EscritoireGameToken(tokenNbt);
			complete &= token.hasSpecies();
			tokens[index] = token;
		}

		for (EscritoireGameToken token : tokens) {
			complete &= token != null;
		}

		if (!complete) {
			return;
		}

		Collections.addAll(this.gameTokens, tokens);
		this.tokenCount = this.gameTokens.size();
	}

	public boolean isEmpty() {
		return this.gameTokens.isEmpty();
	}

	public boolean initialize(ItemStack specimen) {
		IIndividual individual = IIndividualHandlerItem.getIndividual(specimen);

		if (individual != null) {
			ISpeciesType<?, ?> type = individual.getType();

			this.tokenCount = getTokenCount(individual);

			for (int i = 0; i < this.tokenCount / 2; i++) {
				ISpecies<?> randomSpecies = type.getRandomSpecies(rand);
                this.gameTokens.add(new EscritoireGameToken(randomSpecies));
                this.gameTokens.add(new EscritoireGameToken(randomSpecies));
			}
			Collections.shuffle(this.gameTokens);

			return true;
		}
		return false;
	}

	@Nullable
	public EscritoireGameToken getToken(int index) {
		// todo figure out why tokenCount is out of sync with gameTokens
		if (index >= this.tokenCount || index >= this.gameTokens.size()) {
			return null;
		}
		return this.gameTokens.get(index);
	}

	public int getTokenCount() {
		return this.tokenCount;
	}

	public void hideProbedTokens() {
		for (EscritoireGameToken token : this.gameTokens) {
			if (token.isProbed()) {
				token.setProbed(false);
			}
		}
	}

	private List<EscritoireGameToken> getUnrevealedTokens() {
		List<EscritoireGameToken> unrevealed = new ArrayList<>();
		for (EscritoireGameToken token : this.gameTokens) {
			if (!token.isVisible()) {
				unrevealed.add(token);
			}
		}

		return unrevealed;
	}

	@Nullable
	private EscritoireGameToken getSelected() {
		for (EscritoireGameToken token : this.gameTokens) {
			if (token.isSelected()) {
				return token;
			}
		}

		return null;
	}

	private boolean isBoardCleared() {
		for (EscritoireGameToken token : this.gameTokens) {
			if (!token.isMatched()) {
				return false;
			}
		}

		return true;
	}

	public void probe() {
		List<EscritoireGameToken> tokens = getUnrevealedTokens();
		int index = rand.nextInt(tokens.size());

		EscritoireGameToken token = tokens.get(index);
		token.setProbed(true);
	}

	public EscritoireGame.Status choose(EscritoireGameToken token) {
		EscritoireGame.Status status = EscritoireGame.Status.PLAYING;
		if (token.isMatched() || token.isSelected()) {
			return status;
		}

		EscritoireGameToken selected = getSelected();
		if (selected == null) {
			token.setSelected();
			hideProbedTokens();
		} else if (token.matches(selected)) {
			selected.setMatched();
			token.setMatched();
			if (isBoardCleared()) {
				status = EscritoireGame.Status.SUCCESS;
			}
			hideProbedTokens();
		} else {
			token.setFailed();
			selected.setFailed();
			status = EscritoireGame.Status.FAILURE;
		}

		return status;
	}

	public void reset() {
        this.gameTokens.clear();
        this.tokenCount = 0;
	}

	private static int getTokenCount(IIndividual individual) {
		ISpecies<?> species1 = individual.getSpecies();
		ISpecies<?> species2 = individual.getInactiveSpecies();

		int tokenCount = species1.getComplexity() + species2.getComplexity();

		if (tokenCount % 2 != 0) {
			tokenCount = Math.round((float) tokenCount / 2) * 2;
		}

		if (tokenCount > TOKEN_COUNT_MAX) {
			tokenCount = TOKEN_COUNT_MAX;
		} else if (tokenCount < TOKEN_COUNT_MIN) {
			tokenCount = TOKEN_COUNT_MIN;
		}

		return tokenCount;
	}

	@Override
	public CompoundTag write(CompoundTag compoundNBT) {
		if (this.tokenCount > 0) {
			compoundNBT.putInt(NBT_TOKEN_COUNT, this.tokenCount);
			ListTag nbttaglist = new ListTag();

			for (int i = 0; i < this.tokenCount; i++) {
				EscritoireGameToken token = this.gameTokens.get(i);
				if (token == null) {
					continue;
				}

				CompoundTag compoundNBT2 = new CompoundTag();
				compoundNBT2.putByte("Slot", (byte) i);
				token.write(compoundNBT2);
				nbttaglist.add(compoundNBT2);
			}

			compoundNBT.put(NBT_GAME_TOKENS, nbttaglist);
		} else {
			compoundNBT.putInt(NBT_TOKEN_COUNT, 0);
		}
		return compoundNBT;
	}

	@Override
	public void writeData(FriendlyByteBuf data) {
		data.writeVarInt(this.tokenCount);
		NetworkUtil.writeStreamables(data, this.gameTokens);
	}

	@Override
	public void readData(FriendlyByteBuf data) {
		this.tokenCount = data.readVarInt();
		NetworkUtil.readStreamables(data, this.gameTokens, EscritoireGameToken::new);
	}
}
