package forestry.arboriculture.network;

import forestry.core.network.PacketIdServer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Packet ids owned by the arboriculture module. The path strings are the packets' wire identity and
 * must never change.
 */
public class ArboriculturePacketIds {
	public static final CustomPacketPayload.Type<PacketRipeningUpdate> RIPENING_UPDATE = PacketIdServer.type("ripening_update");
}
