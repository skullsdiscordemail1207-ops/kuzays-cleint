package com.kuzay2023.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.BlockEspModule;
import com.kuzay2023.client.module.impl.EspModule;
import com.kuzay2023.client.module.impl.InvisEspModule;
import com.kuzay2023.client.module.impl.StorageEspModule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class EspRenderManager {
	private static final Map<Integer, Boolean> MANAGED_GLOW_STATES = new HashMap<>();

	private EspRenderManager() {
	}

	public static void initialize() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(EspRenderManager::render);
		ClientTickEvents.END_CLIENT_TICK.register(EspRenderManager::tickGlowState);
	}

	private static void render(WorldRenderContext context) {
		KuzayClientContext clientContext = KuzayClientModClient.getContext();
		if (clientContext == null || context.world() == null || context.camera() == null || context.matrixStack() == null || context.consumers() == null) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity self = client.player;
		if (self == null) return;

		Vec3d cameraPos = context.camera().getPos();

		Module storageBase = clientContext.moduleManager().getModule("storage_esp");
		if (storageBase instanceof StorageEspModule storageEspModule && storageEspModule.isEnabled()) {
			renderBlocksDirect(context, cameraPos, self.getBlockPos(), storageEspModule.getRenderRadius(), blockState -> storageEspModule.shouldRender(blockState), blockState -> ColorChoice.fromName(storageEspModule.getColorName(blockState)));
		}

		Module blockEspBase = clientContext.moduleManager().getModule("block_esp");
		if (blockEspBase instanceof BlockEspModule blockEspModule && blockEspModule.isEnabled()) {
			renderBlocksDirect(context, cameraPos, self.getBlockPos(), blockEspModule.getRenderRadius(), blockState -> blockEspModule.shouldRender(blockState), blockState -> ColorChoice.fromName(blockEspModule.getColorName(blockState)));
		}
	}

	public static boolean shouldOutlineEntity(Entity entity) {
		if (entity == null) {
			return false;
		}

		KuzayClientContext clientContext = KuzayClientModClient.getContext();
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity self = client == null ? null : client.player;
		if (clientContext == null || self == null || entity == self) {
			return false;
		}

		Module espBase = clientContext.moduleManager().getModule("esp");
		if (espBase instanceof EspModule espModule && espModule.isEnabled() && espModule.shouldRender(entity, self)) {
			return true;
		}

		Module invisBase = clientContext.moduleManager().getModule("invis_esp");
		return invisBase instanceof InvisEspModule invisEspModule
			&& invisEspModule.isEnabled()
			&& invisEspModule.shouldRender(entity, self);
	}

	private static void tickGlowState(MinecraftClient client) {
		KuzayClientContext clientContext = KuzayClientModClient.getContext();
		if (clientContext == null || client.world == null || client.player == null) {
			clearManagedGlows(client);
			return;
		}

		PlayerEntity self = client.player;
		Module espBase = clientContext.moduleManager().getModule("esp");
		Module invisBase = clientContext.moduleManager().getModule("invis_esp");
		EspModule espModule = espBase instanceof EspModule module && espBase.isEnabled() ? module : null;
		InvisEspModule invisEspModule = invisBase instanceof InvisEspModule module && invisBase.isEnabled() ? module : null;

		if (espModule == null && invisEspModule == null) {
			clearManagedGlows(client);
			return;
		}

		Set<Integer> desired = new HashSet<>();
		for (Entity entity : client.world.getEntities()) {
			if (entity == null || entity == self) continue;

			boolean shouldGlow = false;
			if (espModule != null && espModule.shouldRender(entity, self)) {
				shouldGlow = true;
			}
			if (invisEspModule != null && invisEspModule.shouldRender(entity, self)) {
				shouldGlow = true;
			}

			if (!shouldGlow) continue;
			desired.add(entity.getId());
			if (!MANAGED_GLOW_STATES.containsKey(entity.getId())) {
				MANAGED_GLOW_STATES.put(entity.getId(), entity.isGlowing());
			}
			entity.setGlowing(true);
		}

		Set<Integer> stale = new HashSet<>(MANAGED_GLOW_STATES.keySet());
		stale.removeAll(desired);
		for (Integer entityId : stale) {
			Entity entity = client.world.getEntityById(entityId);
			Boolean original = MANAGED_GLOW_STATES.remove(entityId);
			if (entity != null && original != null) {
				entity.setGlowing(original);
			}
		}
	}

	private static void clearManagedGlows(MinecraftClient client) {
		if (client.world != null) {
			for (Map.Entry<Integer, Boolean> entry : MANAGED_GLOW_STATES.entrySet()) {
				Entity entity = client.world.getEntityById(entry.getKey());
				if (entity != null) {
					entity.setGlowing(entry.getValue());
				}
			}
		}
		MANAGED_GLOW_STATES.clear();
	}

	private static void drawBox(WorldRenderContext context, BufferBuilder buffer, Box box, Vec3d cameraPos, ColorChoice color) {
		Box shifted = box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z).expand(0.0025);
		VertexRendering.drawBox(context.matrixStack(), buffer, shifted, color.red() / 255.0F, color.green() / 255.0F, color.blue() / 255.0F, color.alpha() / 255.0F);
	}

	private static void renderBlocksDirect(WorldRenderContext context, Vec3d cameraPos, BlockPos center, int radius, BlockPredicate predicate, BlockColorProvider colorProvider) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
		boolean drewAny = false;

		BlockPos min = center.add(-radius, -radius, -radius);
		BlockPos max = center.add(radius, radius, radius);
		for (BlockPos pos : BlockPos.iterate(min, max)) {
			var state = context.world().getBlockState(pos);
			if (!predicate.shouldRender(state)) {
				continue;
			}
			ColorChoice color = colorProvider.colorFor(state);
			drawBox(context, buffer, new Box(pos), cameraPos, color);
			drewAny = true;
		}

		if (!drewAny) {
			return;
		}

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
		RenderSystem.lineWidth(1.5F);
		BufferRenderer.drawWithGlobalProgram(buffer.end());
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private record ColorChoice(int red, int green, int blue, int alpha) {
		private static ColorChoice fromName(String name) {
			return switch (name) {
				case "Green" -> new ColorChoice(46, 194, 126, 255);
				case "Blue" -> new ColorChoice(64, 156, 255, 255);
				case "Yellow" -> new ColorChoice(240, 197, 74, 255);
				case "Cyan" -> new ColorChoice(59, 214, 255, 255);
				case "Orange" -> new ColorChoice(255, 153, 51, 255);
				case "Pink" -> new ColorChoice(255, 105, 180, 255);
				case "White" -> new ColorChoice(255, 255, 255, 255);
				case "Purple" -> new ColorChoice(163, 73, 255, 255);
				default -> new ColorChoice(227, 75, 75, 255);
			};
		}
	}

	@FunctionalInterface
	private interface BlockPredicate {
		boolean shouldRender(net.minecraft.block.BlockState state);
	}

	@FunctionalInterface
	private interface BlockColorProvider {
		ColorChoice colorFor(net.minecraft.block.BlockState state);
	}
}
