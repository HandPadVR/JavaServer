package dev.zeith.hpvr.devices;

import com.illposed.osc.transport.OSCPortOut;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ActiveDevice
{
	public final InetAddress address;
	public final String identifier;
	public final ActiveDeviceScript script;
	public Instant heartbeat = Instant.now();
	
	record CycleKey(int buttonIndex, boolean state, int eventIndex) {}
	
	private final Map<CycleKey, Integer> buttonCycleCache = new ConcurrentHashMap<>();
	
	public ActiveDevice(InetAddress address, String identifier, Path deviceScript, Supplier<OSCPortOut> outPort)
			throws IOException, ReflectiveOperationException
	{
		this.address = address;
		this.identifier = identifier;
		this.script = ActiveDeviceScript.create(deviceScript, outPort);
	}
	
	public int nextIndex(int buttonIndex, boolean state, int eventIndex, int size)
	{
		return buttonCycleCache.compute(
				new CycleKey(buttonIndex, state, eventIndex),
				(k, prev) -> prev != null ? (prev + 1) % size : 0
		);
	}
}