package dev.zeith.hpvr.net.http;

import java.util.*;

public class HttpRequest
{
	public Map<String, String> headers = new HashMap<>();
	public String method;
	public String path;
	public String version;
	public byte[] body;
	
	public String getHeader(String name)
	{
		return headers.get(name.toLowerCase(Locale.ROOT));
	}
	
	@Override
	public String toString()
	{
		return method + " " + path + " " + version;
	}
}