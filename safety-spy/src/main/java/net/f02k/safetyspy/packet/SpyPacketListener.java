package net.f02k.safetyspy.packet;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.waypoint.TrackedWaypoint;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAttachEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDamageEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWaypoint;
import net.f02k.safetyspy.config.SpyConfig;
import net.f02k.safetyspy.spy.VisibilityRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Filters every outgoing packet that would reveal a spy to a viewer who is
 * not allowed to see them. Runs on netty threads.
 */
public final class SpyPacketListener implements PacketListener {

    private final VisibilityRegistry registry;
    private final ServerListRewriter serverList;

    public SpyPacketListener(VisibilityRegistry registry) {
        this.registry = registry;
        this.serverList = new ServerListRewriter(registry);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled() || !registry.hasSpies()) {
            return;
        }
        if (event.getPacketType() == PacketType.Status.Server.RESPONSE) {
            if (registry.config().hideServerList) {
                serverList.rewrite(event);
            }
            return;
        }
        if (!(event.getPacketType() instanceof PacketType.Play.Server type)) {
            return;
        }
        UUID viewer = event.getUser().getUUID();
        if (viewer == null) {
            return;
        }
        SpyConfig config = registry.config();

        switch (type) {
            case PLAYER_INFO_UPDATE -> handleInfoUpdate(event, viewer, config);
            case PLAYER_INFO -> handleLegacyInfo(event, viewer, config);
            case TAB_COMPLETE -> {
                if (config.hideTabComplete) {
                    handleTabComplete(event, viewer);
                }
            }
            case WAYPOINT -> {
                if (config.hideLocatorBar) {
                    handleWaypoint(event, viewer);
                }
            }
            case SET_PASSENGERS -> {
                if (config.hideEntity) {
                    handlePassengers(event, viewer);
                }
            }
            case ATTACH_ENTITY -> {
                if (config.hideEntity) {
                    WrapperPlayServerAttachEntity wrapper = new WrapperPlayServerAttachEntity(event);
                    if (hidden(viewer, wrapper.getAttachedId()) || hidden(viewer, wrapper.getHoldingId())) {
                        event.setCancelled(true);
                    }
                }
            }
            case DAMAGE_EVENT -> {
                if (config.hideEntity) {
                    handleDamage(event, viewer);
                }
            }
            default -> {
                if (config.hideEntity) {
                    int entityId = EntityIdResolver.resolve(event, type);
                    if (entityId != EntityIdResolver.NONE && hidden(viewer, entityId)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private boolean hidden(UUID viewer, int entityId) {
        return registry.isEntityHiddenFrom(viewer, entityId);
    }

    // ----------------------------------------------------------- tab list

    private void handleInfoUpdate(PacketSendEvent event, UUID viewer, SpyConfig config) {
        boolean remove = config.removeProfile();
        boolean unlist = config.unlistProfile();
        if (!remove && !unlist) {
            return;
        }
        WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);
        boolean listedAction = wrapper.getActions().contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED);
        List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> entries = wrapper.getEntries();
        List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> kept = new ArrayList<>(entries.size());
        boolean changed = false;

        for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry : entries) {
            if (!registry.isHiddenFrom(viewer, entry.getProfileId())) {
                kept.add(entry);
                continue;
            }
            if (remove) {
                changed = true;
                continue;
            }
            // Unlist only. ADD_PLAYER without UPDATE_LISTED is unlisted by default.
            if (listedAction && entry.isListed()) {
                entry.setListed(false);
                changed = true;
            }
            kept.add(entry);
        }

        if (!changed) {
            return;
        }
        if (kept.isEmpty()) {
            event.setCancelled(true);
            return;
        }
        wrapper.setEntries(kept);
        event.markForReEncode(true);
    }

    /** Pre 1.19.3 player info packet. */
    private void handleLegacyInfo(PacketSendEvent event, UUID viewer, SpyConfig config) {
        if (!config.removeProfile()) {
            return;
        }
        WrapperPlayServerPlayerInfo wrapper = new WrapperPlayServerPlayerInfo(event);
        if (wrapper.getAction() == WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER) {
            return;
        }
        List<WrapperPlayServerPlayerInfo.PlayerData> entries = wrapper.getPlayerDataList();
        List<WrapperPlayServerPlayerInfo.PlayerData> kept = new ArrayList<>(entries.size());
        for (WrapperPlayServerPlayerInfo.PlayerData data : entries) {
            if (data.getUserProfile() == null
                    || !registry.isHiddenFrom(viewer, data.getUserProfile().getUUID())) {
                kept.add(data);
            }
        }
        if (kept.size() == entries.size()) {
            return;
        }
        if (kept.isEmpty()) {
            event.setCancelled(true);
            return;
        }
        wrapper.setPlayerDataList(kept);
        event.markForReEncode(true);
    }

    // ------------------------------------------------------- tab complete

    private void handleTabComplete(PacketSendEvent event, UUID viewer) {
        Set<String> names = registry.hiddenNamesFrom(viewer);
        if (names.isEmpty()) {
            return;
        }
        WrapperPlayServerTabComplete wrapper = new WrapperPlayServerTabComplete(event);
        List<WrapperPlayServerTabComplete.CommandMatch> matches = wrapper.getCommandMatches();
        List<WrapperPlayServerTabComplete.CommandMatch> kept = new ArrayList<>(matches.size());
        for (WrapperPlayServerTabComplete.CommandMatch match : matches) {
            if (!names.contains(match.getText().toLowerCase(Locale.ROOT))) {
                kept.add(match);
            }
        }
        if (kept.size() != matches.size()) {
            wrapper.setCommandMatches(kept);
            event.markForReEncode(true);
        }
    }

    // ------------------------------------------------------- locator bar

    private void handleWaypoint(PacketSendEvent event, UUID viewer) {
        WrapperPlayServerWaypoint wrapper = new WrapperPlayServerWaypoint(event);
        if (wrapper.getOperation() == WrapperPlayServerWaypoint.Operation.UNTRACK) {
            return;
        }
        TrackedWaypoint waypoint = wrapper.getWaypoint();
        if (waypoint != null && waypoint.getIdentifier().isLeft()
                && registry.isHiddenFrom(viewer, waypoint.getIdentifier().getLeft())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------ entities

    private void handlePassengers(PacketSendEvent event, UUID viewer) {
        WrapperPlayServerSetPassengers wrapper = new WrapperPlayServerSetPassengers(event);
        if (hidden(viewer, wrapper.getEntityId())) {
            event.setCancelled(true);
            return;
        }
        int[] passengers = wrapper.getPassengers();
        int[] kept = new int[passengers.length];
        int size = 0;
        for (int passenger : passengers) {
            if (!hidden(viewer, passenger)) {
                kept[size++] = passenger;
            }
        }
        if (size != passengers.length) {
            wrapper.setPassengers(java.util.Arrays.copyOf(kept, size));
            event.markForReEncode(true);
        }
    }

    private void handleDamage(PacketSendEvent event, UUID viewer) {
        WrapperPlayServerDamageEvent wrapper = new WrapperPlayServerDamageEvent(event);
        if (hidden(viewer, wrapper.getEntityId())) {
            event.setCancelled(true);
            return;
        }
        // Source ids are encoded as id + 1, 0 means "no source".
        boolean changed = false;
        if (wrapper.getSourceCauseId() > 0 && hidden(viewer, wrapper.getSourceCauseId() - 1)) {
            wrapper.setSourceCauseId(0);
            changed = true;
        }
        if (wrapper.getSourceDirectId() > 0 && hidden(viewer, wrapper.getSourceDirectId() - 1)) {
            wrapper.setSourceDirectId(0);
            changed = true;
        }
        if (changed) {
            event.markForReEncode(true);
        }
    }
}
