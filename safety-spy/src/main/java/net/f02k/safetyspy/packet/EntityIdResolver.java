package net.f02k.safetyspy.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;

/**
 * Extracts the "main" entity id of entity related play packets.
 */
final class EntityIdResolver {

    static final int NONE = Integer.MIN_VALUE;

    private EntityIdResolver() {
    }

    static int resolve(PacketSendEvent event, PacketType.Play.Server type) {
        return switch (type) {
            case SPAWN_ENTITY -> new WrapperPlayServerSpawnEntity(event).getEntityId();
            case SPAWN_PLAYER -> new WrapperPlayServerSpawnPlayer(event).getEntityId();
            case SPAWN_LIVING_ENTITY -> new WrapperPlayServerSpawnLivingEntity(event).getEntityId();
            case ENTITY_MOVEMENT -> new WrapperPlayServerEntityMovement(event).getEntityId();
            case ENTITY_RELATIVE_MOVE -> new WrapperPlayServerEntityRelativeMove(event).getEntityId();
            case ENTITY_RELATIVE_MOVE_AND_ROTATION -> new WrapperPlayServerEntityRelativeMoveAndRotation(event).getEntityId();
            case ENTITY_ROTATION -> new WrapperPlayServerEntityRotation(event).getEntityId();
            case ENTITY_HEAD_LOOK -> new WrapperPlayServerEntityHeadLook(event).getEntityId();
            case ENTITY_TELEPORT -> new WrapperPlayServerEntityTeleport(event).getEntityId();
            case ENTITY_POSITION_SYNC -> new WrapperPlayServerEntityPositionSync(event).getId();
            case ENTITY_VELOCITY -> new WrapperPlayServerEntityVelocity(event).getEntityId();
            case ENTITY_METADATA -> new WrapperPlayServerEntityMetadata(event).getEntityId();
            case ENTITY_EQUIPMENT -> new WrapperPlayServerEntityEquipment(event).getEntityId();
            case ENTITY_ANIMATION -> new WrapperPlayServerEntityAnimation(event).getEntityId();
            case SWING_ANIMATION -> new WrapperPlayServerSwingAnimation(event).getEntityId();
            case ENTITY_STATUS -> new WrapperPlayServerEntityStatus(event).getEntityId();
            case ENTITY_EFFECT -> new WrapperPlayServerEntityEffect(event).getEntityId();
            case REMOVE_ENTITY_EFFECT -> new WrapperPlayServerRemoveEntityEffect(event).getEntityId();
            case UPDATE_ATTRIBUTES -> new WrapperPlayServerUpdateAttributes(event).getEntityId();
            case ENTITY_SOUND_EFFECT -> new WrapperPlayServerEntitySoundEffect(event).getEntityId();
            case HURT_ANIMATION -> new WrapperPlayServerHurtAnimation(event).getEntityId();
            case BLOCK_BREAK_ANIMATION -> new WrapperPlayServerBlockBreakAnimation(event).getEntityId();
            case COLLECT_ITEM -> new WrapperPlayServerCollectItem(event).getCollectorEntityId();
            case USE_BED -> new WrapperPlayServerUseBed(event).getEntityId();
            default -> NONE;
        };
    }
}
