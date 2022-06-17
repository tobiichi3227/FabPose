package net.fill1890.fabsit.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

import static net.fill1890.fabsit.mixin.PlayerEntityAccessor.getLEFT_SHOULDER_ENTITY;
import static net.fill1890.fabsit.mixin.PlayerEntityAccessor.getRIGHT_SHOULDER_ENTITY;

/**
 * The PoseManagerEntity provides an interface to a variety of posing actions, currently:
 *  entity.Pose.SITTING
 *  entity.Pose.LAYING
 *
 * The manager spawns in invisible armour stand for the player to ride to sit
 * If needed, the player will then be made invisible and an NPC spawned to pose instead
 */
public class PoseManagerEntity extends ArmorStandEntity {
    private boolean used = false;
    private final Pose pose;
    private PosingEntity poser;

    public PoseManagerEntity(World world, Vec3d pos, float yaw, Pose pose, ServerPlayerEntity player) {
        // create a new armour stand at the appropriate height
        // TODO: no magic numbers
        super(world, pos.x, pos.y - 1.6, pos.z);

        this.setInvisible(true);
        this.setInvulnerable(true);
        this.setCustomName(Text.of("FABSEAT"));
        this.setNoGravity(true);
        this.setYaw(yaw);

        // if the pose is more complex than sitting, create a posing npc
        if(pose == Pose.LAYING) {
            // copy player game profile with a random uuid
            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), player.getEntityName());
            gameProfile.getProperties().putAll(player.getGameProfile().getProperties());

            this.poser = new LayingEntity(player, gameProfile);
        }

        this.pose = pose;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);

        // if the pose is npc-based, hide the player when initiated
        if(this.pose == Pose.LAYING) {
            passenger.setInvisible(true);

            // update shoulder entities
            // parrots and such, I think
            this.poser.getDataTracker().set(getLEFT_SHOULDER_ENTITY(), passenger.getDataTracker().get(getLEFT_SHOULDER_ENTITY()));
            this.poser.getDataTracker().set(getRIGHT_SHOULDER_ENTITY(), passenger.getDataTracker().get(getRIGHT_SHOULDER_ENTITY()));
            passenger.getDataTracker().set(getLEFT_SHOULDER_ENTITY(), new NbtCompound());
            passenger.getDataTracker().set(getRIGHT_SHOULDER_ENTITY(), new NbtCompound());
        }

        used = true;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);

        // if the pose was npc-based, show the player again when exited
        if(this.pose == Pose.LAYING) {
            passenger.setInvisible(false);

            // replace shoulder entities
            passenger.getDataTracker().set(getLEFT_SHOULDER_ENTITY(), this.poser.getDataTracker().get(getLEFT_SHOULDER_ENTITY()));
            passenger.getDataTracker().set(getRIGHT_SHOULDER_ENTITY(), this.poser.getDataTracker().get(getRIGHT_SHOULDER_ENTITY()));
            this.poser.getDataTracker().set(getLEFT_SHOULDER_ENTITY(), new NbtCompound());
            this.poser.getDataTracker().set(getRIGHT_SHOULDER_ENTITY(), new NbtCompound());
        }
    }

    public void animate(int id) {
        if(this.pose == Pose.LAYING) {
            poser.animate(id);
        }
    }

    @Override
    public boolean canMoveVoluntarily() {
        return false;
    }

    @Override
    public boolean collides() {
        return false;
    }

    @Override
    public void kill() {
        // if the pose was npc-based, remove the npc
        if(poser != null) {
            poser.destroy();
        }

        super.kill();
    }

    @Override
    public void tick() {
        // kill when the player stops posing
        if(used && getPassengerList().size() < 1) { this.kill(); }

        // rotate the armour stand with the player so the player's legs line up
        ServerPlayerEntity player = (ServerPlayerEntity) this.getFirstPassenger();
        if(player != null) {
            this.setYaw(player.getHeadYaw());
        }

        // stop the player sitting if the block below is broken
        BlockState sittingBlock = getEntityWorld().getBlockState(new BlockPos(getPos()).up());
        if(sittingBlock.isAir()) { kill(); }

        // if pose is npc-based, update players with npc info
        if(this.pose == Pose.LAYING) {
            poser.sendUpdates();
        }

        super.tick();
    }
}