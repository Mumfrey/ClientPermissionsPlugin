package net.eq2online.permissions.plugin.providers;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import net.eq2online.permissions.ReplicatedPermissionsContainer;
import net.eq2online.permissions.plugin.ReplicatedPermissionsMappingProvider;

/**
 * Permissions mapping provider which 
 *
 * @author Adam Mummery-Smith
 */
public class PermissionsMappingProviderGeneric implements ReplicatedPermissionsMappingProvider
{
	/**
	 * Parent plugin 
	 */
	private Plugin plugin;
	
	/**
	 * Configuration file for this provider
	 */
	private File configFile;
	
	/**
	 * Configuration as an object
	 */
	private YamlConfiguration config;
	
	/**
	 * List of mods we support
	 */
	private List<String> modList = new ArrayList<String>();
	
	/**
	 * Supported mod versions
	 */
	private Map<String, Float> modVersions = new HashMap<String, Float>();
	
	/* (non-Javadoc)
	 * @see net.eq2online.permissions.ReplicatedPermissionsMappingProvider#initPermissionsMappingProvider(java.io.File)
	 */
	@Override
	public void initPermissionsMappingProvider(Plugin plugin)
	{
		this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "mods.yml");
        this.config = YamlConfiguration.loadConfiguration(this.configFile);

        if (!this.configFile.exists())
        {
        	this.plugin.getLogger().warning("No mods.yml was found in the data directory");
        }

		this.config.addDefault("mods", new ArrayList<String>());
        this.modList = this.config.getStringList("mods");
        
        ConfigurationSection versions = this.config.getConfigurationSection("versions");
        
        if (versions != null)
        {
			for (String key : versions.getKeys(false))
			{
				if (this.modList.contains(key))
				{
					this.modVersions.put(key, (float)this.config.getDouble("versions." + key));
				}
			}
		}
        
		saveConfig();
	}
	
	/* (non-Javadoc)
	 * @see net.eq2online.permissions.ReplicatedPermissionsProvider#providesPermissionsFor(java.lang.String, float)
	 */
	@Override
	public boolean providesPermissionMappingsFor(ReplicatedPermissionsContainer data)
	{
		return this.modList.contains(data.modName);
	}

	/* (non-Javadoc)
	 * @see net.eq2online.permissions.ReplicatedPermissionsProvider#checkVersion(org.bukkit.plugin.Plugin, org.bukkit.entity.Player, net.eq2online.permissions.ReplicatedPermissionsContainer)
	 */
	@Override
	public boolean checkVersion(Plugin plugin, Player player, ReplicatedPermissionsContainer data)
	{
		if (this.modList.contains(data.modName))
		{
			float minVersion = this.getMinModVersion(data.modName);
			return minVersion <= 0.0F || data.modVersion >= minVersion;
		}
		
		return true;
	}

	/* (non-Javadoc)
	 * @see net.eq2online.permissions.ReplicatedPermissionsProvider#getPermissions(org.bukkit.plugin.Plugin, org.bukkit.entity.Player, java.lang.String, float)
	 */
	@Override
	public List<String> getPermissions(Plugin plugin, Player player, ReplicatedPermissionsContainer data)
	{
		List<String> permissionsList = new ArrayList<String>();
		
		if (this.modList.contains(data.modName))
		{
			for (String checkPerm : data.permissions)
			{
				Permission perm = new Permission(checkPerm, PermissionDefault.FALSE);
				
				if (player.isPermissionSet(perm))
				{
					String hasPermission = player.hasPermission(perm) ? "+" : "-";
					permissionsList.add(hasPermission + checkPerm);
				}
			}
		}
		
		return permissionsList;
	}

	/**
	 * Save the configuration
	 */
	protected void saveConfig()
	{
		this.config.set("mods", this.modList);
		this.config.set("versions", null);
		
		for (String modName : this.modList)
		{
			double modVersion = this.getMinModVersion(modName);
			
			if (modVersion > 0.0F)
			{
				this.config.set("versions." + modName, modVersion);
			}
		}
		
        try
		{
			this.config.save(this.configFile);
		}
		catch (IOException ex)
		{
			this.plugin.getLogger().log(Level.WARNING, "Problem saving mods.yml", ex);
		}
	}

	/**
	 * Add a supported mod
	 * 
	 * @param modName
	 * @return
	 */
	public boolean addMod(String modName)
	{
		if (modName == null || modName.length() < 1) return false;
		if (modName.equalsIgnoreCase("all")) return false;
		
		if (!this.modList.contains(modName))
		{
			this.modList.add(modName);
			this.saveConfig();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Remove a supported mod
	 * 
	 * @param modName
	 * @return
	 */
	public boolean removeMod(String modName)
	{
		if (this.modList.contains(modName))
		{
			this.modList.remove(modName);
			this.saveConfig();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Get list of supported mods
	 * 
	 * @return
	 */
	public List<String> getMods()
	{
		return Collections.unmodifiableList(this.modList);
	}
	
	/**
	 * Get the minimum supported version for the specified mod
	 * 
	 * @param modName
	 * @return
	 */
	public Float getMinModVersion(String modName)
	{
		if (!this.modList.contains(modName)) return 0.0F;
		if (!this.modVersions.containsKey(modName)) return 0.0F;
		return this.modVersions.get(modName);
	}
	
	/**
	 * Set the minimum supported version for the specified mod
	 * 
	 * @param modName
	 * @param version
	 * @return
	 */
	public boolean setMinModVersion(String modName, Float version)
	{
		if (!this.modList.contains(modName)) return false;
		if (version < 0.0F) version = Float.valueOf(0.0F);
		
		this.modVersions.put(modName, version);
		this.saveConfig();
		return true;
	}
}
