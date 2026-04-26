package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReport.class)
public abstract class CrashReportMixin {
	@Inject(method = "addDetails", at = @At("TAIL"))
	private void onAddDetails(StringBuilder sb, CallbackInfo info) {
		sb.append("\n\n-- ").append(MeteorClient.NAME).append(" --\n\n");
		sb.append("Version:").append(MeteorClient.VERSION).append('\n');

		if (!MeteorClient.BUILD_NUMBER.isEmpty()) {
			sb.append("Build:").append(MeteorClient.BUILD_NUMBER).append('\n');
		}

		Modules modules = Modules.get();
		if (modules != null) {
			boolean modulesActive = false;

			for (Category category : Modules.loopCategories()) {
				boolean categoryActive = false;

				for (Module module : modules.getGroup(category)) {
					if (module == null || !module.isActive()) {
						continue;
					}

					if (!modulesActive) {
						modulesActive = true;
						sb.append("\n[[ Active Modules ]]\n");
					}

					if (!categoryActive) {
						categoryActive = true;
						sb.append("\n[").append(category).append("]:\n");
					}

					sb.append(module.name).append('\n');
				}
			}
		}

		Hud hud = Hud.get();
		if (hud == null || !hud.active) {
			return;
		}

		boolean hudActive = false;
		for (HudElement element : hud) {
			if (element == null || !element.isActive()) {
				continue;
			}

			if (!hudActive) {
				hudActive = true;
				sb.append("\n[[ Active Hud Elements ]]\n");
			}

			if (element instanceof TextHud textHud) {
				sb.append("Text\n{").append(textHud.text.get()).append("}\n");
				if (textHud.shown.get() != TextHud.Shown.Always) {
					sb.append('(').append(textHud.shown.get()).append(textHud.condition.get()).append(")\n");
				}
			} else {
				sb.append(element.info.name).append('\n');
			}
		}
	}
}
