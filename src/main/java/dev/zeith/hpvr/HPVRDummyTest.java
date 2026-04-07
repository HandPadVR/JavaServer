import dev.zeith.hpvr.net.hpvr.HPVRDiscovery;

import java.net.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

void main()
{
	runDummyBoard("RightController");
}

private static void runDummyBoard(String identifier)
{
	try(DatagramSocket socket = new DatagramSocket(HPVRDiscovery.DEVICE_PORT))
	{
		socket.setSoTimeout(100);
		
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		Random rand = new Random();
		
		IO.println("Dummy HPVR board running: " + identifier);
		
		InetAddress server = null;
		int serverPort = -1;
		long lastHP = System.currentTimeMillis();
		
		while(true)
		{
			// Receive broadcast
			try
			{
				socket.receive(packet);
				String msg = new String(packet.getData(), 0, packet.getLength()).trim();
				InetAddress sender = packet.getAddress();
				
				if("HPVR_DISCOVERY_REQUEST".equals(msg))
				{
					String response = server == null ? "HPVR_DEVICE_PAIR:" + identifier : "HPVR_DEVICE_HP";
					byte[] data = response.getBytes();
					DatagramPacket resp = new DatagramPacket(data, data.length, sender, packet.getPort());
					socket.send(resp);
					if(server == null)
					{
						server = resp.getAddress();
						serverPort = packet.getPort();
						IO.println("Responded to discovery from " + sender.getHostAddress());
					}
					lastHP = System.currentTimeMillis();
				}
			} catch(SocketTimeoutException _)
			{
			}
			
			if(server != null && System.currentTimeMillis() - lastHP > 5000)
			{
				server = null;
				IO.println("Discovery server dead.");
			}
			
			if(server != null)
			{
				// Randomly send a device event
				if(rand.nextDouble() < 0.1)
				{ // 10% chance per loop
					int button = rand.nextInt(3);
					boolean state = rand.nextBoolean();
					String event = "HPVR_DEVICE_EVENT:" + Integer.toHexString(button) + ":" + (state ? "1" : "0");
					byte[] data = event.getBytes();
					socket.send(new DatagramPacket(data, data.length, server, serverPort));
					IO.println("Sent event: " + event);
				}
			}
			
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
		}
	} catch(Exception e)
	{
		e.printStackTrace();
	}
}