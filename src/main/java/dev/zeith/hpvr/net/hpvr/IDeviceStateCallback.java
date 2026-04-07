package dev.zeith.hpvr.net.hpvr;

import java.net.InetAddress;

public interface IDeviceStateCallback
{
	void tick();
	
	void onDevicePaired(InetAddress address, String identifier);
	
	void onDeviceHeartbeat(InetAddress identifier);
	
	void onDeviceEvent(InetAddress identifier, int buttonState, boolean active);
}