package dev.zeith.hpvr.net.osc.query.ported;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(AccessValues.AccessValuesAdapter.class)
public enum AccessValues
{
	NoValue,
	Read,
	Write,
	ReadWrite;
	
	public static class AccessValuesAdapter
			implements JsonDeserializer<AccessValues>, JsonSerializer<AccessValues>
	{
		@Override
		public AccessValues deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException
		{
			return AccessValues.values()[json.getAsInt()];
		}
		
		@Override
		public JsonElement serialize(AccessValues src, Type typeOfSrc, JsonSerializationContext context)
		{
			return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.ordinal());
		}
	}
}
