package dev.zeith.hpvr;

import dev.zeith.hpvr.app.HPVRDeviceHandler;
import dev.zeith.hpvr.cfg.HPVRConfig;
import dev.zeith.hpvr.net.hpvr.HPVRDiscovery;
import dev.zeith.hpvr.net.osc.query.OSCQueryService;
import dev.zeith.hpvr.vrchat.VRChat;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;

public class Main
{
	public static final String NAME = "HandPadVR";
	public static HPVRDiscovery discovery;
	public static OSCQueryService oscQuery;
	
	@SneakyThrows
	static void main()
	{
		IO.println("[Main] Version " + System.getProperty("jpackage.app-version"));
		
		VRChat.ensureVRCOSCDirectory();
		
		IO.println("[Main] VRChat directory: " + VRChat.getVRCOSCDirectory());
		
		var appdataFile = new File(new File(System.getenv("APPDATA"), "Zeith.DEV"), "HandPadVR");
		var appdata = appdataFile.toPath();
		appdataFile.mkdirs();
		
		IO.println("[Main] Application data at " + appdataFile.getAbsolutePath());
		
		LogFile.setup(appdata.resolve("Logs"));
		
		Path devicesDir = appdata.resolve("Devices");
		Files.createDirectories(devicesDir);
		
		Path cfgFile = appdata.resolve("Config.json");
		
		AtomicReference<HPVRConfig> cfg = new AtomicReference<>(HPVRConfig.load(cfgFile));
		
		IO.println("[Main] Starting OSCQuery");
		oscQuery = new OSCQueryService();
		oscQuery.start();
		
		IO.println("[Main] Starting HPVR Discovery");
		discovery = new HPVRDiscovery(0, new HPVRDeviceHandler(devicesDir, cfg, () -> oscQuery.getOscPortOut()));
		discovery.start();
		
		IO.println("[Main] Ready.");
	}
}