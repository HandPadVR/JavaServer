package dev.zeith.hpvr.vrchat;

import com.google.gson.*;
import dev.zeith.hpvr.vrchat.avtr.ParameterIO;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AvatarParser
{
	private static final Gson gson = new Gson();
	
	public static CompletableFuture<AvatarConfigFile> parseAvatar(String newId)
	{
		if(newId == null || newId.isEmpty())
		{
			return CompletableFuture.completedFuture(null);
		}
		
		return CompletableFuture.supplyAsync(() ->
		{
			AvatarConfigFile avatarConfig = null;
			
			File vrcOscDir = new File(VRChat.getVRCOSCDirectory());
			if(!vrcOscDir.exists() || !vrcOscDir.isDirectory())
			{
				IO.println("[ERROR] VRC OSC Directory not found: " + vrcOscDir.getAbsolutePath());
				return null;
			}
			
			File[] userFolders = vrcOscDir.listFiles(File::isDirectory);
			if(userFolders == null) return null;
			
			outer:
			for(File userFolder : userFolders)
			{
				File avatarsDir = new File(userFolder, "Avatars");
				if(!avatarsDir.exists() || !avatarsDir.isDirectory()) continue;
				
				File[] avatarFiles = avatarsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".json"));
				if(avatarFiles == null) continue;
				
				for(File avatarFile : avatarFiles)
				{
					try
					{
						String configText = Files.readString(avatarFile.toPath());
						AvatarConfigFile tempConfig = gson.fromJson(configText, AvatarConfigFile.class);
						if(tempConfig == null || !newId.equals(tempConfig.id)) continue;
						
						avatarConfig = tempConfig;
						break outer;
					} catch(JsonSyntaxException ex)
					{
						IO.println("[WARNING] Malformed JSON file detected: " + avatarFile.getAbsolutePath() + ". Renaming to .bak. Error: " + ex.getMessage());
						try
						{
							File backupFile = new File(avatarFile.getParent(), avatarFile.getName().replaceAll("\\.json$", ".bak"));
							if(!avatarFile.renameTo(backupFile))
							{
								IO.println("[ERROR] Failed to rename malformed JSON file " + avatarFile.getAbsolutePath());
							}
						} catch(Exception moveEx)
						{
							IO.println("[ERROR] Error renaming file " + avatarFile.getAbsolutePath() + ": " + moveEx.getMessage());
						}
					} catch(IOException ioEx)
					{
						IO.println("[ERROR] Error reading file " + avatarFile.getAbsolutePath() + ": " + ioEx.getMessage());
					}
				}
			}
			
			if(avatarConfig == null)
			{
				IO.println("[ERROR] Avatar config file for " + newId + " not found");
				return null;
			}
			
			return avatarConfig;
		});
	}
	
	public static final class AvatarConfigFile
	{
		public final String id;
		public final String name;
		public final long hash;
		public final ParameterIO[] parameters;
		
		public AvatarConfigFile(String id, String name, long hash, ParameterIO[] parameters)
		{
			this.id = id;
			this.name = name;
			this.hash = hash;
			this.parameters = parameters;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if(obj == this) return true;
			if(obj == null || obj.getClass() != this.getClass()) return false;
			var that = (AvatarConfigFile) obj;
			return Objects.equals(this.id, that.id) &&
					Objects.equals(this.name, that.name) &&
					this.hash == that.hash &&
					Objects.equals(this.parameters, that.parameters);
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(id, name, hash, Arrays.hashCode(parameters));
		}
		
		@Override
		public String toString()
		{
			return "AvatarConfigFile[" +
					"id=" + id + ", " +
					"name=" + name + ", " +
					"hash=" + hash + ", " +
					"parameters=" + Arrays.toString(parameters) + ']';
		}
	}
}