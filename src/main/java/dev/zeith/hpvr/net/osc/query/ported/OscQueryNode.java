package dev.zeith.hpvr.net.osc.query.ported;

import com.google.gson.*;
import com.google.gson.annotations.*;

import java.util.Map;

public class OscQueryNode
{
	// Empty constructor for JSON serialization
	public OscQueryNode() {}
	
	public OscQueryNode(String fullPath)
	{
		this(fullPath, AccessValues.NoValue, null);
	}
	
	public OscQueryNode(String fullPath, AccessValues access, String oscType)
	{
		this.FullPath = fullPath;
		this.Access = access;
		this.OscType = oscType;
	}
	
	@SerializedName("DESCRIPTION")
	@Expose
	public String Description;
	
	@SerializedName("FULL_PATH")
	@Expose
	public String FullPath;
	
	@SerializedName("ACCESS")
	@Expose
	public AccessValues Access;
	
	@SerializedName("CONTENTS")
	@Expose
	public Map<String, OscQueryNode> Contents;
	
	@SerializedName("TYPE")
	@Expose
	public String OscType;
	
	@SerializedName("VALUE")
	@Expose
	public Object[] Value;
	
	// Non-serialized fields
	public String getParentPath()
	{
		int lastSlash = Math.max(1, FullPath.lastIndexOf('/'));
		return FullPath.substring(0, lastSlash);
	}
	
	public String getName()
	{
		int lastSlash = FullPath.lastIndexOf('/');
		return FullPath.substring(lastSlash + 1);
	}
	
	@Override
	public String toString()
	{
		Gson gson = new GsonBuilder()
				.serializeNulls() // or skip nulls with .excludeFieldsWithoutExposeAnnotation()
				.setPrettyPrinting()
				.create();
		return gson.toJson(this);
	}
}