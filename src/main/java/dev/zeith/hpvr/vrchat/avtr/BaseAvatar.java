package dev.zeith.hpvr.vrchat.avtr;

import dev.zeith.hpvr.vrchat.AvatarParser;
import lombok.AllArgsConstructor;

import java.util.*;

@AllArgsConstructor
public final class BaseAvatar
{
	public static final BaseAvatar DEF_LOADING = new BaseAvatar("avtr_00000000-0000-0000-0000-000000000000", "Loading...", false, new AvatarParameters(List.of()));
	
	public final String id;
	public final String name;
	public final boolean loaded;
	public final AvatarParameters parameters;
	
	public static BaseAvatar create(AvatarParser.AvatarConfigFile cfg)
	{
		return new BaseAvatar(cfg.id, cfg.name, true, new AvatarParameters(List.of(cfg.parameters)));
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == this) return true;
		if(obj == null || obj.getClass() != this.getClass()) return false;
		var that = (BaseAvatar) obj;
		return Objects.equals(this.id, that.id) &&
				Objects.equals(this.name, that.name) &&
				this.loaded == that.loaded &&
				Objects.equals(this.parameters, that.parameters);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, loaded, parameters);
	}
	
	@Override
	public String toString()
	{
		return "BaseAvatar[" +
				"id=" + id + ", " +
				"name=" + name + ", " +
				"loaded=" + loaded + ", " +
				"parameters=" + parameters + ']';
	}
}