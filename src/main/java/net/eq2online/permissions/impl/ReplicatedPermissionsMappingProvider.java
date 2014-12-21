package net.eq2online.permissions.impl;

import java.io.File;
import java.util.List;

import net.eq2online.permissions.ReplicatedPermissionsContainer;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * In the replicated permissions model, client permissions are mapped to bukkit
 * permissions by permissions mapping providers. This interface defines classes
 * which are able to map replicated client permissions to bukkit permissions. The
 * idea here is that if a client mod requires special behaviour in terms of
 * mapping requested permissions to real permissions, it can be implemented as a
 * permissions provider.
 * 
 * The default implementation {@link PermissionsMappingProviderGeneric} is used
 * to provide a 1-to-1 mapping of permissions, and is configurable to support any
 * client mod. If no special behaviour is required, the generic provider can be
 * used and can be easily configured by server admins to support any mod required.
 * 
 * @author Adam Mummery-Smith
 */
public interface ReplicatedPermissionsMappingProvider
{
	/**
	 * Initialise this provider, the provider can load settings from the supplied
	 * location.
	 * 
	 * @param dataFolder Location to load settings and other ancilliary files from
	 */
	public void initPermissionsMappingProvider(File dataFolder);

	/**
	 * Checks whether this provider can provide permissions mapping for the mod
	 * described by the supplied data structure.
	 * 
	 * @param data Client query
	 * @return True if this provider supports mappings for the specified mod
	 */
	public boolean providesPermissionMappingsFor(ReplicatedPermissionsContainer data);
	
	/**
	 * This function returns false if the specified mod is supported but does not
	 * meet the required version threshold. The plugin will kick the player with an
	 * invalid message.
	 * 
	 * This function should return true if the mod is not supported by this mod,
	 * only returning false if the player should be kicked.
	 * 
	 * @param plugin Plugin sending the query
	 * @param player Player sending the query
	 * @param data Mod query data
	 * @return False if the mod is out of date, otherwise return true.
	 */
	public boolean checkVersion(Plugin plugin, Player player, ReplicatedPermissionsContainer data);
	
	/**
	 * If the call to providesPermissionMappingsFor() returns true, the manager
	 * will call this function to allow the provider to provide permissions
	 * mappings. The function should return a list of permissions prefixed with
	 * + to indicate a permission is granted or - to indicate that it is denied.
	 * 
	 * @param plugin Plugin sending the query
	 * @param player Player sending the query
	 * @param data Mod query data
	 * @return List of permissions to send back to the client
	 */
	public List<String> getPermissions(Plugin plugin, Player player, ReplicatedPermissionsContainer data);
}
