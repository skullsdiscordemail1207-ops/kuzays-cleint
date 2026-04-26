package com.kuzay2023.client.freelook;

import java.util.UUID;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class FreeLookController {
	private static final double ENTITY_TRACE_RANGE = 64.0;
	private static final float LOOK_MULTIPLIER = 0.15F;

	private static boolean active;
	private static boolean initialized;
	private static float cameraYaw;
	private static float cameraPitch;
	private static int targetedEntityId = -1;
	private static UUID targetedPlayerUuid;

	private FreeLookController() {
	}

	public static void activate(MinecraftClient client) {
		if (client == null || client.player == null) {
			active = false;
			initialized = false;
			clearTarget();
			return;
		}

		active = true;
		if (client.options != null) {
			client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
		}
		syncFromPlayer(client.player);
	}

	public static void deactivate(MinecraftClient client) {
		active = false;
		initialized = false;
		clearTarget();
		if (client != null && client.player != null) {
			syncFromPlayer(client.player);
		}
		if (client != null && client.options != null) {
			client.options.setPerspective(Perspective.FIRST_PERSON);
		}
	}

	public static boolean isActive() {
		return active;
	}

	public static float getCameraYaw() {
		return cameraYaw;
	}

	public static float getCameraPitch() {
		return cameraPitch;
	}

	public static int getTargetedEntityId() {
		return targetedEntityId;
	}

	public static UUID getTargetedPlayerUuid() {
		return targetedPlayerUuid;
	}

	public static boolean handleMouseDelta(ClientPlayerEntity player, double deltaX, double deltaY) {
		if (!active || player == null) {
			return false;
		}

		if (!initialized) {
			syncFromPlayer(player);
		}

		cameraPitch = MathHelper.clamp(cameraPitch + ((float) deltaY * LOOK_MULTIPLIER), -90.0F, 90.0F);
		cameraYaw += (float) deltaX * LOOK_MULTIPLIER;
		return true;
	}

	public static void updateTargetedPlayer(MinecraftClient client) {
		if (!active || client == null || client.player == null || client.world == null) {
			clearTarget();
			return;
		}

		Vec3d start = client.gameRenderer.getCamera().isReady()
			? client.gameRenderer.getCamera().getPos()
			: client.player.getCameraPosVec(1.0F);
		Vec3d look = Vec3d.fromPolar(cameraPitch, cameraYaw);
		Vec3d end = start.add(look.multiply(ENTITY_TRACE_RANGE));

		HitResult blockHit = client.world.raycast(new RaycastContext(
			start,
			end,
			RaycastContext.ShapeType.COLLIDER,
			RaycastContext.FluidHandling.NONE,
			client.player
		));

		double maxDistanceSquared = ENTITY_TRACE_RANGE * ENTITY_TRACE_RANGE;
		if (blockHit.getType() != HitResult.Type.MISS) {
			maxDistanceSquared = blockHit.getPos().squaredDistanceTo(start);
		}

		Box searchBox = new Box(start, end).expand(1.5);
		PlayerEntity bestTarget = null;
		double bestDistanceSquared = maxDistanceSquared;

		for (Entity entity : client.world.getOtherEntities(client.player, searchBox, candidate -> candidate instanceof PlayerEntity player && player.isAlive())) {
			if (!(entity instanceof PlayerEntity player)) {
				continue;
			}

			Box targetBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
			Vec3d hitPos = targetBox.contains(start)
				? start
				: targetBox.raycast(start, end).orElse(null);
			if (hitPos == null) {
				continue;
			}

			double distanceSquared = start.squaredDistanceTo(hitPos);
			if (distanceSquared <= bestDistanceSquared) {
				bestTarget = player;
				bestDistanceSquared = distanceSquared;
			}
		}

		if (bestTarget == null) {
			clearTarget();
			return;
		}

		targetedEntityId = bestTarget.getId();
		targetedPlayerUuid = bestTarget.getUuid();
	}

	private static void syncFromPlayer(ClientPlayerEntity player) {
		cameraYaw = player.getYaw();
		cameraPitch = player.getPitch();
		initialized = true;
	}

	private static void clearTarget() {
		targetedEntityId = -1;
		targetedPlayerUuid = null;
	}
}
