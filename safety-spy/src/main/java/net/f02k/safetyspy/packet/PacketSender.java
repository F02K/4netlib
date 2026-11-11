package net.f02k.safetyspy.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.waypoint.EmptyWaypointInfo;
import com.github.retrooper.packetevents.protocol.world.waypoint.TrackedWaypoint;
import com.github.retrooper.packetevents.protocol.world.waypoint.WaypointIcon;
import com.github.retrooper.packetevents.util.Either;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWaypoint;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Packets the plugin sends itself.
 */
public final class PacketSender {

    private PacketSender() {
    }

    /** Removes the spy from the viewer's locator bar (1.21.6+). */
    public static void untrackWaypoint(Player viewer, UUID spy) {
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_21_6)) {
            return;
        }
        User user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
        if (user == null) {
            return;
        }
        TrackedWaypoint waypoint = new TrackedWaypoint(
                Either.createLeft(spy),
                new WaypointIcon(WaypointIcon.ICON_STYLE_DEFAULT, null),
                EmptyWaypointInfo.EMPTY);
        user.sendPacket(new WrapperPlayServerWaypoint(WrapperPlayServerWaypoint.Operation.UNTRACK, waypoint));
    }
}
