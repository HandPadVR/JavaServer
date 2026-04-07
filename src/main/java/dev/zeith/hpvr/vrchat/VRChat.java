package dev.zeith.hpvr.vrchat;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class VRChat
{
	private static String vrcOscDirectory;
	
	public static void ensureVRCOSCDirectory()
			throws IOException
	{
		String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if(os.contains("win"))
		{
			String localLow = System.getenv("LOCALAPPDATA") + "Low";
			vrcOscDirectory = localLow + File.separator + "VRChat" + File.separator + "VRChat" + File.separator + "OSC";
		} else
		{
			String home = System.getProperty("user.home");
			
			String[] possiblePaths = new String[] {
					home + "/.steam/steam",
					home + "/.local/share/Steam",
					home + "/.var/app/com.valvesoftware.Steam/.local/share/Steam"
			};
			
			String steamPath = Arrays.stream(possiblePaths).filter(p -> new File(p).exists()).findFirst()
									 .orElseThrow(() -> new IllegalStateException("Steam was not detected!"));
			
			File libraryFoldersFile = new File(steamPath + "/steamapps/libraryfolders.vdf");
			if(!libraryFoldersFile.exists())
			{
				throw new IllegalStateException("Steam libraryfolders.vdf not found!");
			}
			
			String vdfContent = Files.readString(libraryFoldersFile.toPath());
			Map<String, Object> libraries = parseVdfFile(vdfContent);
			
			List<String> vrchatPaths = new ArrayList<>();
			Object libObj = libraries.get("libraryfolders");
			if(libObj instanceof Map<?, ?> libMap)
			{
				for(Object value : libMap.values())
				{
					if(value instanceof Map<?, ?> libraryData)
					{
						Object pathObj = libraryData.get("path");
						Object appsObj = libraryData.get("apps");
						if(appsObj instanceof Map<?, ?> apps && apps.containsKey("438100"))
						{
							vrchatPaths.add(pathObj.toString());
							break;
						}
					}
				}
			}
			
			if(vrchatPaths.isEmpty())
			{
				throw new IllegalStateException("Steam detected but VRChat not found!");
			}
			
			for(String vrchatPath : vrchatPaths)
			{
				String candidate = vrchatPath + "/steamapps/compatdata/438100/pfx/drive_c/users/steamuser/AppData/LocalLow/VRChat/VRChat/OSC";
				if(new File(candidate).exists())
				{
					vrcOscDirectory = candidate;
					break;
				}
			}
		}
	}
	
	public static String getVRCOSCDirectory()
	{
		return vrcOscDirectory;
	}
	
	private static Map<String, Object> parseVdfFile(String content)
	{
		VdfParser parser = new VdfParser(content);
		return parser.parse();
	}
	
	private static class VdfParser
	{
		private final String content;
		private int pos;
		
		public VdfParser(String content)
		{
			this.content = content;
			this.pos = 0;
		}
		
		public Map<String, Object> parse()
		{
			skipWhitespace();
			return parseObject();
		}
		
		private Map<String, Object> parseObject()
		{
			Map<String, Object> result = new LinkedHashMap<>();
			
			while(pos < content.length())
			{
				skipWhitespace();
				if(pos < content.length() && content.charAt(pos) == '}')
				{
					pos++;
					break;
				}
				
				String key = parseString();
				if(key.isEmpty()) break;
				
				skipWhitespace();
				
				Object value;
				if(pos < content.length() && content.charAt(pos) == '{')
				{
					pos++;
					value = parseObject();
				} else
				{
					value = parseString();
				}
				
				result.put(key, value);
			}
			
			return result;
		}
		
		private String parseString()
		{
			skipWhitespace();
			if(pos < content.length() && content.charAt(pos) == '"')
			{
				pos++;
				int start = pos;
				while(pos < content.length() && content.charAt(pos) != '"')
				{
					if(content.charAt(pos) == '\\' && pos + 1 < content.length())
					{
						pos += 2;
					} else
					{
						pos++;
					}
				}
				String result = content.substring(start, pos);
				pos++;
				return result;
			}
			return "";
		}
		
		private void skipWhitespace()
		{
			while(pos < content.length())
			{
				char c = content.charAt(pos);
				if(Character.isWhitespace(c))
				{
					pos++;
				} else if(c == '/' && pos + 1 < content.length() && content.charAt(pos + 1) == '/')
				{
					pos += 2;
					while(pos < content.length() && content.charAt(pos) != '\n') pos++;
				} else
				{
					break;
				}
			}
		}
	}
	
	public static boolean isVrChatRunning()
	{
		String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		String processName = os.contains("win") ? "VRChat.exe" : "VRChat";
		return Arrays.stream(ProcessHandle
				.allProcesses()
				.map(ph -> ph.info().command().orElse(""))
				.toArray(String[]::new)
		).anyMatch(cmd -> cmd.toLowerCase(Locale.ROOT).endsWith(processName.toLowerCase(Locale.ROOT)));
	}
}