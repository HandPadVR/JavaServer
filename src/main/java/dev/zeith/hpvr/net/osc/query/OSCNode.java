package dev.zeith.hpvr.net.osc.query;

import com.google.gson.annotations.SerializedName;

public record OSCNode(
		@SerializedName("FULL_PATH") String path,
		@SerializedName("TYPE") String type,
		@SerializedName("VALUE") Object value
)
{
}