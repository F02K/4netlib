package net.f02k.safetyspy.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.f02k.safetyspy.spy.VisibilityRegistry;

import java.util.UUID;

/**
 * Removes spies from the player count and player sample of the server list ping.
 */
final class ServerListRewriter {

    private final VisibilityRegistry registry;

    ServerListRewriter(VisibilityRegistry registry) {
        this.registry = registry;
    }

    void rewrite(PacketSendEvent event) {
        WrapperStatusServerResponse wrapper = new WrapperStatusServerResponse(event);
        JsonObject root = wrapper.getComponent();
        if (root == null || !root.has("players") || !root.get("players").isJsonObject()) {
            return;
        }
        JsonObject players = root.getAsJsonObject("players");

        if (players.has("online")) {
            int online = players.get("online").getAsInt();
            players.addProperty("online", Math.max(0, online - registry.onlineSpyCount()));
        }

        if (players.has("sample") && players.get("sample").isJsonArray()) {
            JsonArray kept = new JsonArray();
            for (JsonElement element : players.getAsJsonArray("sample")) {
                if (!isSpyEntry(element)) {
                    kept.add(element);
                }
            }
            players.add("sample", kept);
        }

        wrapper.setComponent(root);
        event.markForReEncode(true);
    }

    private boolean isSpyEntry(JsonElement element) {
        if (!element.isJsonObject()) {
            return false;
        }
        JsonObject entry = element.getAsJsonObject();
        if (entry.has("id")) {
            try {
                if (registry.isSpy(UUID.fromString(entry.get("id").getAsString()))) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // anonymous / fake entry
            }
        }
        return entry.has("name") && registry.spyByName(entry.get("name").getAsString()) != null;
    }
}
