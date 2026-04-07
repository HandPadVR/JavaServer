package dev.zeith.hpvr.cfg.actions;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record OSCParameter(
		@SerializedName("Name") String name,
		@SerializedName("Float") List<Float> asFloat,
		@SerializedName("Int") List<Integer> asInt,
		@SerializedName("Bool") List<Boolean> asBool
)
{
	public OSCParameter(String name, List<Float> asFloat, List<Integer> asInt, List<Boolean> asBool)
	{
		this.name = name;
		this.asFloat = asFloat;
		this.asInt = asInt;
		this.asBool = asBool;
		
		// Test out the record state upfront
		toOscArg();
	}
	
	public List<?> toOscArg()
	{
		if(asFloat != null) return asFloat;
		if(asInt != null) return asInt;
		if(asBool != null) return asBool;
		throw new IllegalStateException("OSCParameter(" + name + ") has no value bound");
	}
	
	public static OSCParameter createFloat(String name, float value)
	{
		return new OSCParameter(name, List.of(value), null, null);
	}
	
	public static OSCParameter createInt(String name, int value)
	{
		return new OSCParameter(name, null, List.of(value), null);
	}
	
	public static OSCParameter createBool(String name, boolean value)
	{
		return new OSCParameter(name, null, null, List.of(value));
	}
}