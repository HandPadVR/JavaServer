package dev.zeith.hpvr.devices;

import com.illposed.osc.*;
import com.illposed.osc.transport.OSCPortOut;
import dev.zeith.hpvr.Main;
import dev.zeith.hpvr.vrchat.avtr.*;
import lombok.*;
import org.mozilla.javascript.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;

public class ActiveDeviceScript
		extends TopLevel
{
	final HPVR eventBus;
	
	@SneakyThrows
	public ActiveDeviceScript(String source, Context ctx, Supplier<OSCPortOut> outPort)
	{
		ctx.initStandardObjects(this, false);
		
		ScriptableObject.defineClass(this, VRChat.class);
		
		VRChat vrc = (VRChat) ctx.newObject(this, "VRChat");
		putConst("VRC", this, vrc);
		
		eventBus = new HPVR();
		putConst("HPVR", this, eventBus);
		
		Console c = new Console();
		c.source = source;
		putConst("console", this, c);
		
		putConst("OSC", this, new OSC(outPort, c));
	}
	
	public void onDeviceEvent(int buttonIndex, boolean buttonState)
	{
		for(Consumer<Boolean> handler : eventBus.callbacks.getOrDefault(buttonIndex, List.of()))
		{
			handler.accept(buttonState);
		}
	}
	
	public static ActiveDeviceScript create(Path source, Supplier<OSCPortOut> outPort)
			throws IOException
	{
		if(!Files.isRegularFile(source)) try(var in = ActiveDeviceScript.class.getResourceAsStream("/example.js"))
		{
			Files.copy(in, source);
		} catch(FileAlreadyExistsException e) {}
		
		try(Context ctx = Context.enter())
		{
			ctx.setLanguageVersion(Context.VERSION_ECMASCRIPT);
			
			ActiveDeviceScript scope = new ActiveDeviceScript(source.getFileName().toString(), ctx, outPort);
			
			Script compiled = ctx.compileString(Files.readString(source), source.getFileName().toString(), 1, null);
			
			compiled.exec(ctx, scope, scope);
			
			return scope;
		}
	}
	
	public static class Console
	{
		String source;
		
		public void log(Object... args)
		{
			IO.println("[" + source + "] " + Arrays.stream(args).map(ActiveDeviceScript::unwrap).map(Objects::toString).collect(Collectors.joining(" ")));
		}
		
		public void info(Object... args)
		{
			IO.println("[" + source + "] " + Arrays.stream(args).map(ActiveDeviceScript::unwrap).map(Objects::toString).collect(Collectors.joining(" ")));
		}
		
		public void warn(Object... args)
		{
			IO.println("[WARNING] [" + source + "] " + Arrays.stream(args).map(ActiveDeviceScript::unwrap).map(Objects::toString).collect(Collectors.joining(" ")));
		}
		
		public void error(Object... args)
		{
			IO.println("[ERROR] [" + source + "] " + Arrays.stream(args).map(ActiveDeviceScript::unwrap).map(Objects::toString).collect(Collectors.joining(" ")));
		}
	}
	
	public static Object unwrap(Object o)
	{
		if(o instanceof EcmaError ee && "JavaException".equals(ee.getName()))
			;
		return o instanceof Wrapper njo ? njo.unwrap() : o;
	}
	
	public static class HPVR
	{
		private final Map<Integer, List<Consumer<Boolean>>> callbacks = new ConcurrentHashMap<>();
		
		public HPVR onButton(int button, Consumer<Boolean> callback)
		{
			callbacks.computeIfAbsent(button, k -> new ArrayList<>()).add(callback);
			return this;
		}
	}
	
	public static class VRChat
			extends ScriptableObject
	{
		@Override
		public String getClassName()
		{
			return "VRChat";
		}
		
		public Object jsGet_avatar()
		{
			var cur = Main.oscQuery.getCurrentAvatar();
			return cur != null ? cur : BaseAvatar.DEF_LOADING;
		}
	}
	
	@RequiredArgsConstructor
	public static class OSC
	{
		private final Supplier<OSCPortOut> outPort;
		private final Console console;
		
		public final String paramPrefix = "/avatar/parameters/";
		
		public String avtrParam(String param)
		{
			return paramPrefix + param;
		}
		
		public Object start()
		{
			return new OscBuilder(outPort, console);
		}
	}
	
	@RequiredArgsConstructor
	public static class OscBuilder
	{
		private final Supplier<OSCPortOut> outPort;
		private final Console console;
		
		private final List<OSCPacket> packets = new ArrayList<>();
		
		public float intScale = 255F;
		
		public OscBuilder addBool(String addr, boolean value)
		{
			packets.add(new OSCMessage(addr, List.of(value)));
			return this;
		}
		
		public OscBuilder addInt(String addr, int value)
		{
			packets.add(new OSCMessage(addr, List.of(value)));
			return this;
		}
		
		public OscBuilder addFloat(String addr, float value)
		{
			packets.add(new OSCMessage(addr, List.of(value)));
			return this;
		}
		
		public OscBuilder addBool(ParameterIO addr, boolean value)
		{
			if(!addr.canUpdate())
			{
				console.warn("Tried writing to read-only parameter: " + addr.name);
				return this;
			}
			String a = addr.input.address();
			return switch(addr.input.type())
			{
				case Bool -> addBool(a, value);
				case Int -> addInt(a, value ? 1 : 0);
				case Float -> addFloat(a, value ? 1F : 0F);
			};
		}
		
		public OscBuilder addInt(ParameterIO addr, int value)
		{
			if(!addr.canUpdate())
			{
				console.warn("Tried writing to read-only parameter: " + addr.name);
				return this;
			}
			String a = addr.input.address();
			return switch(addr.input.type())
			{
				case Bool -> addBool(a, value > 0);
				case Int -> addInt(a, value);
				case Float -> addFloat(a, value / intScale);
			};
		}
		
		public OscBuilder addFloat(ParameterIO addr, float value)
		{
			if(!addr.canUpdate())
			{
				console.warn("Tried writing to read-only parameter: " + addr.name);
				return this;
			}
			String a = addr.input.address();
			return switch(addr.input.type())
			{
				case Bool -> addBool(a, value > 0F);
				case Int -> addInt(a, Math.round(value * intScale));
				case Float -> addFloat(a, value);
			};
		}
		
		public void send()
				throws IOException, OSCSerializeException
		{
			if(packets.isEmpty()) return;
			var port = outPort.get();
			if(port == null)
			{
				console.warn("Attempted to send OSC data before VRChat OSC port was established.");
				return;
			}
			port.send(new OSCBundle(packets));
		}
	}
}