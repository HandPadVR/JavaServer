package dev.zeith.hpvr.cfg;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

@With
public record HPVRConfig(
		@SerializedName("Port To VRC OSC") int port2vrcOsc,
		@SerializedName("Log Button Events") boolean logButtonEvents
)
{
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();
	
	public static final HPVRConfig DEFAULT = new HPVRConfig(9000, false);
	
	public static HPVRConfig load(Path file)
			throws IOException
	{
		HPVRConfig loaded = DEFAULT;
		
		if(!Files.isRegularFile(file))
		{
			loaded.save(file);
			return loaded;
		}
		
		try(var reader = Files.newBufferedReader(file))
		{
			return GSON.fromJson(reader, HPVRConfig.class);
		}
	}
	
	public void save(Path file)
			throws IOException
	{
		Files.writeString(file, GSON.toJson(this, HPVRConfig.class));
	}
	
	@SneakyThrows
	public void saveSneaky(Path file)
	{
		save(file);
	}
}