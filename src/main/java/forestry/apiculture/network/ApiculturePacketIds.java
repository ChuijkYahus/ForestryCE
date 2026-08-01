package forestry.apiculture.network;

import forestry.apiculture.network.packets.PacketAlvearyChange;
import forestry.apiculture.network.packets.PacketBeeLogicActive;
import forestry.apiculture.network.packets.PacketHabitatBiomePointer;
import forestry.core.network.PacketIdServer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Packet ids owned by the apiculture module. The path strings are the packets' wire identity and
 * must never change.
 */
public class ApiculturePacketIds {
	public static final CustomPacketPayload.Type<PacketBeeLogicActive> BEE_LOGIC_ACTIVE = PacketIdServer.type("bee_logic_active");
	public static final CustomPacketPayload.Type<PacketHabitatBiomePointer> HABITAT_BIOME_POINTER = PacketIdServer.type("habitat_biome_pointer");
	public static final CustomPacketPayload.Type<PacketAlvearyChange> ALVEARY_CONTROLLER_CHANGE = PacketIdServer.type("alveary_controller_change");
}
