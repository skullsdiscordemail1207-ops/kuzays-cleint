package com.kuzay2023.client.profile;

import java.util.ArrayList;
import java.util.List;

public class ProfileState {
	public List<ProfileEntry> profiles = new ArrayList<>();
	public boolean autoLoadEnabled;
	public String autoLoadProfileName = "";
}
