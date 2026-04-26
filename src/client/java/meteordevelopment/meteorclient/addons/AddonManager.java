package meteordevelopment.meteorclient.addons;

import java.util.ArrayList;
import java.util.List;

import meteordevelopment.meteorclient.MeteorClient;

public final class AddonManager {
	public static final List<MeteorAddon> ADDONS = new ArrayList<>();

	private AddonManager() {
	}

	public static void init() {
		ADDONS.clear();
		MeteorClient.ADDON = new EmbeddedAddon();
		ADDONS.add(MeteorClient.ADDON);
	}

	private static final class EmbeddedAddon extends MeteorAddon {
		private EmbeddedAddon() {
			name = "kuzays-secrets";
			authors = new String[] { "kuzay2023" };
		}

		@Override
		public void onInitialize() {
		}

		@Override
		public String getPackage() {
			return "com.kuzay2023.client";
		}
	}
}
