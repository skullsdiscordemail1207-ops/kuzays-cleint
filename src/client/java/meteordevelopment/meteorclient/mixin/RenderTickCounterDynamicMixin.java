package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(RenderTickCounter.Dynamic.class)
public abstract class RenderTickCounterDynamicMixin {
	@Shadow
	private float lastFrameDuration;

	@Inject(method = "beginRenderTick(J)I", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;prevTimeMillis:J", opcode = 181))
	private void onBeingRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
		Modules modules = Modules.get();
		if (modules == null) {
			return;
		}

		Timer timer = modules.get(Timer.class);
		if (timer == null) {
			return;
		}

		lastFrameDuration *= (float) timer.getMultiplier();
	}
}
