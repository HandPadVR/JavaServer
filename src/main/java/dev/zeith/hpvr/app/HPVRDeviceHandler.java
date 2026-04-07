package dev.zeith.hpvr.app;

import com.illposed.osc.transport.OSCPortOut;
import dev.zeith.hpvr.cfg.HPVRConfig;
import dev.zeith.hpvr.devices.ActiveDevice;
import dev.zeith.hpvr.net.hpvr.IDeviceStateCallback;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class HPVRDeviceHandler
		implements IDeviceStateCallback
{
	protected final Map<InetAddress, ActiveDevice> devices = new ConcurrentHashMap<>();
	
	protected final Path devicesDir;
	protected final AtomicReference<HPVRConfig> cfg;
	protected final Supplier<OSCPortOut> vrcOut;
	
	@Override
	public void tick()
	{
		Set<InetAddress> dead = new HashSet<>();
		
		var now = Instant.now();
		for(var e : devices.values())
		{
			if(Duration.between(e.heartbeat, now).toMillis() > 5000L)
			{
				IO.println("[Callback] Device '" + e.identifier + "' is dead");
				dead.add(e.address);
			}
		}
		
		devices.keySet().removeAll(dead);
		
		if(!dead.isEmpty()) IO.println("[Callback] New device count: " + devices.size());
	}
	
	@Override
	public void onDevicePaired(InetAddress address, String identifier)
	{
		Path id = devicesDir.resolve(identifier + ".js").normalize();
		if(!id.startsWith(devicesDir))
		{
			IO.println("[WARNING] [Callback] Attempted path escape with device identifier '" + identifier + "' from " + address.getHostAddress());
			return;
		}
		
		try
		{
			devices.put(address, new ActiveDevice(address, identifier, id, vrcOut));
			IO.println("[Callback] Device '" + identifier + "' connected from " + address.getHostAddress());
			IO.println("[Callback] New device count: " + devices.size());
		} catch(Exception e)
		{
			IO.println("[ERROR] [Callback] Failed to load device settings for '" + identifier + "' @ " + address.getHostAddress());
			e.printStackTrace(System.out);
		}
	}
	
	@Override
	public void onDeviceHeartbeat(InetAddress address)
	{
		var dev = devices.get(address);
		if(dev == null) return;
		dev.heartbeat = Instant.now();
	}
	
	@Override
	public void onDeviceEvent(InetAddress address, int buttonIndex, boolean buttonState)
	{
		var adev = devices.get(address);
		if(adev == null) return;
		
		if(cfg.get().logButtonEvents())
			IO.println("[Callback] Event from " + adev.identifier + " - Button " + buttonIndex + " = " + buttonState);
		
		adev.script.onDeviceEvent(buttonIndex, buttonState);
	}
}