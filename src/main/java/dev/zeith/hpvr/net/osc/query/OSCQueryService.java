package dev.zeith.hpvr.net.osc.query;

import com.google.gson.*;
import com.illposed.osc.*;
import com.illposed.osc.messageselector.OSCPatternAddressMessageSelector;
import com.illposed.osc.transport.*;
import dev.zeith.hpvr.Main;
import dev.zeith.hpvr.net.http.*;
import dev.zeith.hpvr.net.osc.query.ported.*;
import dev.zeith.hpvr.vrchat.AvatarParser;
import dev.zeith.hpvr.vrchat.avtr.BaseAvatar;
import lombok.*;

import javax.jmdns.*;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

public class OSCQueryService
{
	protected final SimpleHttpServer httpServer;
	
	protected final HttpClient httpClient_1_1 = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
	protected OSCPortIn oscPortIn;
	protected @Getter OSCPortOut oscPortOut;
	protected JmDNS jmDNS;
	protected ServiceInfo vrchatService;
	protected int httpServerPort, oscRecvPort;
	protected String appName;
	protected ScheduledExecutorService scheduler;
	
	public OSCQueryService()
	{
		this.httpServer = new SimpleHttpServer(54961, this::handleHttp);
	}
	
	public static boolean isVrchat(ServiceEvent event)
	{
		return event.getName().startsWith("VRChat-Client-");
	}
	
	public void start()
			throws IOException
	{
		httpServer.start();
		
		DatagramSocket dts = new DatagramSocket(0);
		oscRecvPort = dts.getLocalPort();
		dts.close();
		
		appName = Main.NAME + "-" + Namegen.getRandomChars(6);
		
		oscPortIn = new OSCPortInBuilder()
				.setLocalPort(oscRecvPort)
				.setNetworkProtocol(NetworkProtocol.UDP)
				.addPacketListener(new OSCPacketDispatcher())
				.addMessageListener(new OSCPatternAddressMessageSelector("/*"), this::handleOsc)
				.build();
		
		oscPortIn.startListening();
		
		// Wait until port becomes available
		while(httpServer.getActivePort().isEmpty()) LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10L));
		httpServerPort = httpServer.getActivePort().orElseThrow();
		
		jmDNS = JmDNS.create(InetAddress.getLocalHost());
		
		jmDNS.addServiceListener("_oscjson._tcp.local.", new JmDNSServiceListenerAdapter()
				{
					@Override
					public void serviceRemoved(ServiceEvent event)
					{
						if(isVrchat(event) && vrchatService != null && event.getName().equals(vrchatService.getName()))
						{
							onVRCDisconnected();
							vrchatService = null;
						}
					}
					
					@Override
					public void serviceResolved(ServiceEvent event)
					{
						if(isVrchat(event) && vrchatService == null)
						{
							vrchatService = event.getInfo();
							try
							{
								onVRCConnected();
							} catch(Exception e)
							{
								IO.println("Failed to connect to VRChat server: " + e.getMessage());
								e.printStackTrace(System.out);
							}
						}
					}
				}
		);
	}
	
	@SneakyThrows
	public void stop()
	{
		httpServer.stop();
		
		onVRCDisconnected();
		jmDNS.close();
		jmDNS = null;
		
		oscPortOut.close();
		oscPortOut = null;
		
		oscPortIn.stopListening();
		oscPortIn = null;
	}
	
	@SneakyThrows
	protected void onVRCConnected()
	{
		onVRCDisconnected();
		
		IO.println("Discovered VRChat: " + vrchatService.getName());
		
		scheduler = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());
		
		jmDNS.registerService(ServiceInfo.create(
				"_oscjson._tcp.local.",
				appName,
				httpServerPort,
				"OSCQuery service"
		));
		
		jmDNS.registerService(ServiceInfo.create(
				"_osc._udp.local.",
				appName,
				oscRecvPort,
				"OSCQuery service"
		));
		
		String ip = vrchatService.getHostAddresses()[0];
		var resp = httpClient_1_1.send(
				java.net.http.HttpRequest.newBuilder(URI.create("http://" + ip + ":" + vrchatService.getPort() + "?HOST_INFO")).build(),
				java.net.http.HttpResponse.BodyHandlers.ofString()
		);
		
		JsonObject obj = gson.fromJson(resp.body(), JsonObject.class);
		
		String address = obj.get("OSC_IP").getAsString();
		oscPortOut = new OSCPortOutBuilder()
				.setRemoteSocketAddress(new InetSocketAddress(address, obj.get("OSC_PORT").getAsInt()))
				.setNetworkProtocol(NetworkProtocol.valueOf(obj.get("OSC_TRANSPORT").getAsString()))
				.build();
		
		scheduler.scheduleWithFixedDelay(this::checkAvatar, 0L, 1L, TimeUnit.SECONDS);
	}
	
	protected void onVRCDisconnected()
	{
		jmDNS.unregisterAllServices();
		if(scheduler != null)
		{
			scheduler.shutdown();
			scheduler = null;
		}
	}
	
	private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
	protected @Getter BaseAvatar currentAvatar;
	
	protected void checkAvatar()
	{
		var vrc = vrchatService;
		if(vrc == null) return;
		try
		{
			String ip = vrchatService.getHostAddresses()[0];
			var resp = httpClient_1_1.send(
					java.net.http.HttpRequest.newBuilder(URI.create("http://" + ip + ":" + vrc.getPort() + "/avatar/change")).build(),
					java.net.http.HttpResponse.BodyHandlers.ofString()
			);
			
			String avtr = gson.fromJson(resp.body(), JsonObject.class).getAsJsonArray("VALUE").get(0).getAsString();
			if(currentAvatar == null || !Objects.equals(avtr, currentAvatar.id))
			{
				currentAvatar = AvatarParser.parseAvatar(avtr).thenApply(BaseAvatar::create).join();
				IO.println("Update avatar: " + currentAvatar.name);
			}
		} catch(Exception e)
		{
			IO.println("[ERROR] [OSC] Failed to query avatar");
		}
	}
	
	public void handleHttp(HttpRequest request, HttpResponse response)
	{
		response.setHeader("Pragma", "no-cache");
		
		var resp = new JsonObject();
		
		if(request.path.contains("HOST_INFO"))
		{
			resp.addProperty("NAME", appName);
			resp.addProperty("OSC_IP", "127.0.0.1");
			resp.addProperty("OSC_PORT", oscRecvPort);
			resp.addProperty("OSC_TRANSPORT", "UDP");
			resp.add("EXTENSIONS", new JsonObject());
		} else
		{
			if(!"/".equals(request.path))
				return;
			
			var rootNode = new OscQueryRoot();
			rootNode.addNode(new OscQueryNode("/avatar/change", AccessValues.Write, "s"));
			response.setJsonBody(gson, rootNode);
			return;
		}
		
		System.out.println(gson.toJson(request));
		response.setJsonBody(gson, resp);
	}
	
	public void handleOsc(OSCMessageEvent event)
	{
		System.out.println(event);
		OSCMessage msg = event.getMessage();
		switch(msg.getAddress())
		{
			case "/avatar/change" ->
			{
			
			}
			default ->
			{
			
			}
		}
	}
}