package dev.zeith.hpvr.net.hpvr;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * This service broadcasts "HPVR_DISCOVERY_REQUEST" to local network, and awaits for HPVR boards to respond with "HPVR_DEVICE_PAIR:identifier"
 * These boards then send "HPVR_DEVICE_EVENT:buttonIndex(0-F):buttonState(0-1)"
 */
public class HPVRDiscovery
{
	// This is the port for any HandPadVR board.
	// Our server's port is determined from a pool of free ports.
	public static final int DEVICE_PORT = 31926;
	
	private static final int BUFFER_SIZE = 1024;
	private static final long BROADCAST_INTERVAL_NS = TimeUnit.SECONDS.toNanos(2L);
	
	private final IDeviceStateCallback callback;
	private final DatagramSocket socket;
	private Thread listener, broadcaster;
	
	public HPVRDiscovery(int receivePort, IDeviceStateCallback callback)
			throws SocketException
	{
		this.callback = callback;
		this.socket = new DatagramSocket(receivePort);
		this.socket.setBroadcast(true);
	}
	
	public void start()
	{
		stop();
		listener = Thread.ofPlatform().start(this::listen);
		broadcaster = Thread.ofPlatform().start(this::broadcast);
	}
	
	public void stop()
	{
		if(listener != null)
		{
			listener.interrupt();
			listener = null;
		}
		if(broadcaster != null)
		{
			broadcaster.interrupt();
			broadcaster = null;
		}
	}
	
	private void listen()
	{
		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
		IO.println("[Net] Searching for HandPadVR devices on LAN...");
		
		while(!socket.isClosed() && !Thread.currentThread().isInterrupted())
		{
			try
			{
				socket.receive(packet);
				String message = new String(packet.getData(), 0, packet.getLength()).trim();
				InetAddress sender = packet.getAddress();
				
				if(message.startsWith("HPVR_DEVICE_PAIR:"))
				{
					String identifier = message.substring("HPVR_DEVICE_PAIR:".length());
					callback.onDevicePaired(sender, identifier);
				} else if(message.startsWith("HPVR_DEVICE_EVENT:"))
				{
					// Format: HPVR_DEVICE_EVENT:<buttonIndex>:<buttonState>
					String[] parts = message.split(":");
					if(parts.length == 3)
					{
						try
						{
							int buttonIndex = Integer.parseInt(parts[1], 16);
							boolean buttonState = parts[2].equals("1");
							callback.onDeviceEvent(sender, buttonIndex, buttonState);
						} catch(NumberFormatException ignored) {}
					}
				} else if(message.equals("HPVR_DEVICE_HP"))
				{
					callback.onDeviceHeartbeat(sender);
				}
			} catch(IOException e)
			{
				if(!socket.isClosed())
					e.printStackTrace();
			}
		}
	}
	
	private void broadcast()
	{
		try
		{
			InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
			String message = "HPVR_DISCOVERY_REQUEST";
			byte[] data = message.getBytes();
			
			DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, DEVICE_PORT);
			
			while(!socket.isClosed())
			{
				socket.send(packet);
				callback.tick();
				LockSupport.parkNanos(BROADCAST_INTERVAL_NS);
			}
		} catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}