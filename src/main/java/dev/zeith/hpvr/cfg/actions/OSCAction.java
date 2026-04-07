package dev.zeith.hpvr.cfg.actions;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record OSCAction(
		@SerializedName("Button") int button,
		@SerializedName("OnPress") List<OSCParameter> onPress,
		@SerializedName("OnRelease") List<OSCParameter> onRelease
)
{
	public static OSCAction createDefaultAction(String identifier, int button)
	{
		return new OSCAction(button,
				List.of(
						OSCParameter.createBool("/avatar/parameters/HPVR/" + identifier + "/B" + button, true),
						OSCParameter.createFloat("/avatar/parameters/HPVR/" + identifier + "/F" + button, 0.5F)
				),
				List.of(
						OSCParameter.createBool("/avatar/parameters/HPVR/" + identifier + "/B" + button, false),
						OSCParameter.createInt("/avatar/parameters/HPVR/" + identifier + "/I" + button, 0)
				)
		);
	}
}