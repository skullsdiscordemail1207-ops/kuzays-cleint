package meteordevelopment.meteorclient.utils.render.postprocess;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;

public final class PostProcessShaders {
	public static EntityShader CHAMS = new DisabledEntityShader();
	public static EntityShader ENTITY_OUTLINE = new DisabledEntityShader();
	public static PostProcessShader STORAGE_OUTLINE = new DisabledPostProcessShader();
	public static boolean rendering;

	private static boolean loggedInitializationFailure;

	private PostProcessShaders() {
	}

	public static void init() {
		tryInitialize();
	}

	public static void beginRender() {
		tryInitialize();
		CHAMS.beginRender();
		ENTITY_OUTLINE.beginRender();
		STORAGE_OUTLINE.beginRender();
	}

	public static void endRender() {
		CHAMS.endRender();
		ENTITY_OUTLINE.endRender();
	}

	public static void onResized(int width, int height) {
		if (MeteorClient.mc == null) {
			return;
		}

		tryInitialize();
		CHAMS.onResized(width, height);
		ENTITY_OUTLINE.onResized(width, height);
		STORAGE_OUTLINE.onResized(width, height);
	}

	public static boolean isCustom(VertexConsumerProvider provider) {
		return provider != null
			&& ((CHAMS.vertexConsumerProvider != null && provider == CHAMS.vertexConsumerProvider)
				|| (ENTITY_OUTLINE.vertexConsumerProvider != null && provider == ENTITY_OUTLINE.vertexConsumerProvider));
	}

	private static boolean initialized() {
		return !(CHAMS instanceof DisabledEntityShader)
			&& !(ENTITY_OUTLINE instanceof DisabledEntityShader)
			&& !(STORAGE_OUTLINE instanceof DisabledPostProcessShader);
	}

	private static void tryInitialize() {
		if (initialized() || MeteorClient.mc == null || MeteorClient.mc.getWindow() == null) {
			return;
		}

		try {
			EntityShader entityOutline = new EntityOutlineShader();
			PostProcessShader storageOutline = new StorageOutlineShader();
			EntityShader chams = new ChamsShader();

			CHAMS = chams;
			ENTITY_OUTLINE = entityOutline;
			STORAGE_OUTLINE = storageOutline;
			loggedInitializationFailure = false;
		} catch (Throwable t) {
			CHAMS = new DisabledEntityShader();
			ENTITY_OUTLINE = new DisabledEntityShader();
			STORAGE_OUTLINE = new DisabledPostProcessShader();

			if (!loggedInitializationFailure) {
				MeteorClient.LOG.warn("Post-process shaders are unavailable right now. Continuing with safe fallbacks.", t);
				loggedInitializationFailure = true;
			}
		}
	}

	private static final class DisabledEntityShader extends EntityShader {
		@Override
		protected boolean shouldDraw() {
			return false;
		}

		@Override
		public boolean shouldDraw(Entity entity) {
			return false;
		}

		@Override
		protected void setUniforms() {
		}
	}

	private static final class DisabledPostProcessShader extends PostProcessShader {
		@Override
		protected boolean shouldDraw() {
			return false;
		}

		@Override
		public boolean shouldDraw(Entity entity) {
			return false;
		}

		@Override
		protected void setUniforms() {
		}
	}
}
