package com.kuzay2023.client.freecam;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import org.lwjgl.glfw.GLFW;

public final class FreecamController {
	private static final float LOOK_MULTIPLIER = 0.15F;

	private static boolean active;
	private static Vec3d position = Vec3d.ZERO;
	private static Vec3d previousPosition = Vec3d.ZERO;
	private static float yaw;
	private static float previousYaw;
	private static float pitch;
	private static float previousPitch;
	private static double speed = 1.0D;
	private static double verticalSpeed = 1.0D;
	private static boolean collision = false;
	private static boolean smoothing = true;
	private static boolean previousSmoothCamera;

	private FreecamController() {
	}

	public static void activate(MinecraftClient client, double nextSpeed, double nextVerticalSpeed, boolean nextCollision, boolean nextSmoothing) {
		if (client == null || client.player == null) {
			return;
		}
		active = true;
		position = client.gameRenderer.getCamera().isReady() ? client.gameRenderer.getCamera().getPos() : client.player.getCameraPosVec(1.0F);
		previousPosition = position;
		yaw = client.player.getYaw();
		previousYaw = yaw;
		pitch = client.player.getPitch();
		previousPitch = pitch;
		speed = nextSpeed;
		verticalSpeed = nextVerticalSpeed;
		collision = nextCollision;
		smoothing = nextSmoothing;
		if (client.options != null) {
			previousSmoothCamera = client.options.smoothCameraEnabled;
			client.options.smoothCameraEnabled = smoothing;
			client.options.setPerspective(Perspective.FIRST_PERSON);
			suppressMovementKeys(client);
		}
		try {
			client.player.input.movementForward = 0.0F;
			client.player.input.movementSideways = 0.0F;
		} catch (Throwable ignored) {
		}
	}

	public static void deactivate(MinecraftClient client) {
		active = false;
		if (client != null && client.options != null) {
			client.options.smoothCameraEnabled = previousSmoothCamera;
			restoreMovementKeys(client);
		}
		position = Vec3d.ZERO;
	}

	public static boolean isActive() {
		return active;
	}

	public static Vec3d getPosition() {
		return position;
	}

	public static Vec3d getRenderPosition(float tickDelta) {
		return previousPosition.lerp(position, MathHelper.clamp(tickDelta, 0.0F, 1.0F));
	}

	public static float getYaw() {
		return yaw;
	}

	public static float getRenderYaw(float tickDelta) {
		return MathHelper.lerp(MathHelper.clamp(tickDelta, 0.0F, 1.0F), previousYaw, yaw);
	}

	public static float getPitch() {
		return pitch;
	}

	public static float getRenderPitch(float tickDelta) {
		return MathHelper.lerp(MathHelper.clamp(tickDelta, 0.0F, 1.0F), previousPitch, pitch);
	}

	public static void updateSettings(double nextSpeed, double nextVerticalSpeed, boolean nextCollision, boolean nextSmoothing) {
		speed = nextSpeed;
		verticalSpeed = nextVerticalSpeed;
		collision = nextCollision;
		smoothing = nextSmoothing;
	}

	public static boolean handleMouseDelta(MinecraftClient client, double deltaX, double deltaY) {
		if (!active || client == null || client.player == null) {
			return false;
		}
		previousYaw = yaw;
		previousPitch = pitch;
		pitch = MathHelper.clamp(pitch + ((float) deltaY * LOOK_MULTIPLIER), -90.0F, 90.0F);
		yaw += (float) deltaX * LOOK_MULTIPLIER;
		return true;
	}

	public static void tick(MinecraftClient client) {
		if (!active || client == null || client.player == null || client.world == null) {
			return;
		}

		if (client.options != null) {
			client.options.smoothCameraEnabled = smoothing;
		}
		previousPosition = position;

		long windowHandle = client.getWindow().getHandle();
		boolean forwardPressed = isPhysicalKeyPressed(client.options.forwardKey, windowHandle);
		boolean backPressed = isPhysicalKeyPressed(client.options.backKey, windowHandle);
		boolean leftPressed = isPhysicalKeyPressed(client.options.leftKey, windowHandle);
		boolean rightPressed = isPhysicalKeyPressed(client.options.rightKey, windowHandle);
		boolean jumpPressed = isPhysicalKeyPressed(client.options.jumpKey, windowHandle);
		boolean sneakPressed = isPhysicalKeyPressed(client.options.sneakKey, windowHandle);
		suppressMovementKeys(client);

		Vec3d forward = Vec3d.fromPolar(0.0F, yaw).normalize();
		Vec3d flatForward = new Vec3d(forward.x, 0.0D, forward.z).normalize();
		Vec3d right = new Vec3d(-flatForward.z, 0.0D, flatForward.x);
		Vec3d movement = Vec3d.ZERO;

		if (forwardPressed) {
			movement = movement.add(flatForward.multiply(speed));
		}
		if (backPressed) {
			movement = movement.subtract(flatForward.multiply(speed));
		}
		if (leftPressed) {
			movement = movement.subtract(right.multiply(speed));
		}
		if (rightPressed) {
			movement = movement.add(right.multiply(speed));
		}
		if (jumpPressed) {
			movement = movement.add(0.0D, verticalSpeed, 0.0D);
		}
		if (sneakPressed) {
			movement = movement.add(0.0D, -verticalSpeed, 0.0D);
		}

		if (movement.lengthSquared() > 0.0D) {
			Vec3d nextPosition = position.add(movement.multiply(0.1D));
			if (collision) {
				HitResult hitResult = client.world.raycast(new RaycastContext(position, nextPosition, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
				if (hitResult.getType() == HitResult.Type.BLOCK) {
					Vec3d diff = nextPosition.subtract(position);
					nextPosition = hitResult.getPos().subtract(diff.normalize().multiply(0.15D));
				}
			}
			position = nextPosition;
		}

		try {
			client.player.input.movementForward = 0.0F;
			client.player.input.movementSideways = 0.0F;
		} catch (Throwable ignored) {
		}
	}

	private static boolean isPhysicalKeyPressed(KeyBinding keyBinding, long windowHandle) {
		if (keyBinding == null) {
			return false;
		}
		InputUtil.Key key = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
		return key != InputUtil.UNKNOWN_KEY && InputUtil.isKeyPressed(windowHandle, key.getCode());
	}

	private static void suppressMovementKeys(MinecraftClient client) {
		client.options.forwardKey.setPressed(false);
		client.options.backKey.setPressed(false);
		client.options.leftKey.setPressed(false);
		client.options.rightKey.setPressed(false);
		client.options.jumpKey.setPressed(false);
		client.options.sneakKey.setPressed(false);
	}

	private static void restoreMovementKeys(MinecraftClient client) {
		long windowHandle = client.getWindow().getHandle();
		client.options.forwardKey.setPressed(isPhysicalKeyPressed(client.options.forwardKey, windowHandle));
		client.options.backKey.setPressed(isPhysicalKeyPressed(client.options.backKey, windowHandle));
		client.options.leftKey.setPressed(isPhysicalKeyPressed(client.options.leftKey, windowHandle));
		client.options.rightKey.setPressed(isPhysicalKeyPressed(client.options.rightKey, windowHandle));
		client.options.jumpKey.setPressed(isPhysicalKeyPressed(client.options.jumpKey, windowHandle));
		client.options.sneakKey.setPressed(isPhysicalKeyPressed(client.options.sneakKey, windowHandle));
	}
}
