package net.eq2online.permissions.impl;

import net.eq2online.permissions.ReplicatedPermissionsContainer;

import org.bukkit.entity.Player;

public interface ReplicatedPermissionsManager
{
	/**
	 * Add a mapping provider to the provider set
	 * 
	 * @param provider
	 */
	public abstract void addMappingProvider(ReplicatedPermissionsMappingProvider provider);
	
	/**
	 * Initialise all registered providers
	 */
	public abstract void initAllMappingProviders();
	
	/**
	 * Check the version of the specified permissions container
	 * 
	 * @param player
	 * @param data
	 * @return
	 */
	public abstract boolean checkVersion(Player player, ReplicatedPermissionsContainer data);
	
	/**
	 * If the version check succeeds, replicate the permissions to the client
	 * 
	 * @param player
	 * @param data
	 */
	public abstract void replicatePermissions(Player player, ReplicatedPermissionsContainer data);
	
}