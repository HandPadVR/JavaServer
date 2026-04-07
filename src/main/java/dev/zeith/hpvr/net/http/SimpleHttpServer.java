package dev.zeith.hpvr.net.http;

import lombok.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;

public class SimpleHttpServer
{
	private static final int MAX_UPLOAD_LIMIT = 10 * 1024 * 1024; // 10MB
	
	private final int initialPort;
	private @Getter OptionalInt activePort = OptionalInt.empty();
	private final IHttpListener listener;
	private volatile boolean running = true;
	
	protected Thread thread;
	
	public SimpleHttpServer(int port, IHttpListener listener)
	{
		this.initialPort = port;
		this.listener = listener;
	}
	
	public synchronized void start()
	{
		stop();
		running = true;
		thread = new Thread(this::run);
		thread.setName("HttpServerMain");
		thread.start();
	}
	
	public synchronized void stop()
	{
		running = false;
		if(thread == null) return;
		thread.interrupt();
		thread = null;
		activePort = OptionalInt.empty();
	}
	
	@SneakyThrows
	protected void run()
	{
		try(ServerSocket server = new ServerSocket(initialPort))
		{
			activePort = OptionalInt.of(server.getLocalPort());
			
			IO.println("[HTTPServer] Started on port " + server.getLocalPort());
			
			Thread.Builder.OfVirtual builder = Thread.ofVirtual().name("HttpServerHandler", 1L);
			while(running)
			{
				Socket socket = server.accept();
				builder.start(() -> handleClient(socket));
			}
		} finally
		{
			activePort = OptionalInt.empty();
		}
	}
	
	private void handleClient(Socket socket)
	{
		try(socket)
		{
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			
			HttpRequest request = parseRequest(in);
			HttpResponse response = new HttpResponse(out);
			
			if(request != null)
			{
				try
				{
					listener.handle(request, response);
				} catch(Exception e)
				{
					// internal server errror
					response.setStatus(502, "Internal Server Error");
					response.send();
					throw new RuntimeException(e);
				}
			} else
			{
				response.setStatus(400, "Bad Request");
				response.setBody("Bad Request");
			}
			
			response.send();
		} catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private HttpRequest parseRequest(InputStream in)
			throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1));
		
		String requestLine = reader.readLine();
		if(requestLine == null || requestLine.isEmpty()) return null;
		
		String[] parts = requestLine.split(" ");
		if(parts.length < 3) return null;
		
		HttpRequest req = new HttpRequest();
		req.method = parts[0];
		req.path = parts[1];
		req.version = parts[2];
		
		// Headers
		String line;
		while((line = reader.readLine()) != null && !line.isEmpty())
		{
			int idx = line.indexOf(':');
			if(idx > 0)
			{
				String key = line.substring(0, idx).trim();
				String value = line.substring(idx + 1).trim();
				req.headers.put(key, value);
			}
		}
		
		// Body (optional)
		int contentLength = 0;
		if(req.headers.containsKey("Content-Length"))
		{
			contentLength = Integer.parseInt(req.headers.get("Content-Length"));
			if(contentLength > MAX_UPLOAD_LIMIT) throw new IOException("Body too large");
		}
		
		byte[] body = new byte[contentLength];
		if(contentLength > 0)
		{
			int read = 0;
			while(read < contentLength)
			{
				int r = in.read(body, read, contentLength - read);
				if(r == -1) break;
				read += r;
			}
		}
		req.body = body;
		
		return req;
	}
}