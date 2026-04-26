package com.kuzay2023.client.tab;

public record ClientTab(
	String id,
	String label,
	String baseCategory,
	String serverKey,
	boolean vip,
	boolean all
) {
	public boolean isGlobal() {
		return !all && (serverKey == null || serverKey.isBlank());
	}

	public boolean isServerScoped() {
		return !all && serverKey != null && !serverKey.isBlank();
	}

	public boolean isAssignable() {
		return !all;
	}
}
