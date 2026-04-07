package dev.zeith.hpvr.net.http;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HttpResponse
{
	private static final DateTimeFormatter HTTP_DATE_FORMAT = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US);
	
	private final OutputStream out;
	private final Map<String, String> headers = new LinkedHashMap<>();
	private int status = 200;
	private String statusText = "OK";
	private byte[] body = new byte[0];
	
	public HttpResponse(OutputStream out)
	{
		this.out = out;
	}
	
	public void setStatus(int code, String text)
	{
		this.status = code;
		this.statusText = text;
	}
	
	public void setHeader(String key, String value)
	{
		headers.put(key, value);
	}
	
	public void setHeaderInt(String key, int value)
	{
		headers.put(key, Integer.toUnsignedString(value));
	}
	
	public void setHeaderDate(String key, Instant value)
	{
		setHeader(key, formatHttpDate(value.toEpochMilli()));
	}
	
	public void setHeaderDateNow(String key)
	{
		setHeaderDate(key, Instant.now());
	}
	
	public void setBody(String body)
	{
		this.body = body.getBytes();
		setHeaderInt("Content-Length", this.body.length);
		setHeader("Content-Type", "text/plain");
	}
	
	public void setJsonBody(Gson serializer, Object body)
	{
		this.body = serializer.toJson(body).getBytes(StandardCharsets.UTF_8);
		setHeaderInt("Content-Length", this.body.length);
		setHeader("Content-Type", "application/json");
	}
	
	public void setJsonBody(JsonElement element)
	{
		this.body = element.toString().getBytes(StandardCharsets.UTF_8);
		setHeaderInt("Content-Length", this.body.length);
		setHeader("Content-Type", "application/json");
	}
	
	public void send()
			throws IOException
	{
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.ISO_8859_1));
		
		writer.printf("HTTP/1.1 %d %s\r\n", status, sanitizeStatusText(statusText));
		
		if(!headers.containsKey("Content-Length"))
			headers.put("Content-Length", String.valueOf(body.length));
		
		if(!headers.containsKey("Content-Type"))
			headers.put("Content-Type", "text/plain");
		
		for(var entry : headers.entrySet())
		{
			String key = sanitizeHeaderName(entry.getKey());
			String value = sanitizeHeaderValue(entry.getValue());
			
			writer.print(key);
			writer.print(": ");
			writer.print(value);
			writer.print("\r\n");
		}
		
		writer.print("\r\n");
		writer.flush();
		
		out.write(body);
		out.flush();
	}
	
	private static String formatHttpDate(long epochMillis)
	{
		return HTTP_DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC));
	}
	
	private static String sanitizeHeaderName(String name)
	{
		if(name == null || name.isEmpty())
			throw new IllegalArgumentException("Header name cannot be empty");
		
		// Disallow control chars and separators (very simplified)
		for(int i = 0; i < name.length(); i++)
		{
			char c = name.charAt(i);
			if(c == ':' || c <= 32 || c >= 127)
				throw new IllegalArgumentException("Invalid header name: " + name);
		}
		
		return name;
	}
	
	private static String sanitizeHeaderValue(String value)
	{
		if(value == null) return "";
		
		// Prevent header injection
		value = value.replace("\r", "").replace("\n", "");
		
		// Trim but preserve internal spaces
		return value.trim();
	}
	
	private static String sanitizeStatusText(String text)
	{
		if(text == null) return "";
		return text.replace("\r", "").replace("\n", "").trim();
	}
}