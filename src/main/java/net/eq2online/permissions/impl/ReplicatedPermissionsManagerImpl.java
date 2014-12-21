package net.eq2online.permissions.impl;

import java.util.*;

import net.eq2online.permissions.ReplicatedPermissionsContainer;
import net.eq2online.permissions.ReplicatedPermissionsPlugin;

import org.bukkit.entity.Player;

/**
 * Class which manages replicating permissions to clients
 *
 * @author Adam Mummery-Smith
 */
public class ReplicatedPermissionsManagerImpl implements ReplicatedPermissionsManager
{
	/**
	 * Plugin which owns this manager 
	 */
	private ReplicatedPermissionsPlugin parent;
	
	/**
	 * List of permission providers 
	 */
	private List<ReplicatedPermissionsMappingProvider> mappingProviders = new ArrayList<ReplicatedPermissionsMappingProvider>();
	
	/**
	 * @param parent
	 */
	public ReplicatedPermissionsManagerImpl(ReplicatedPermissionsPlugin parent)
	{
		this.parent = parent;
	}
	
	/* (non-Javadoc)
	 * @see net.eq2online.permissions.ReplicatedPermissionsManager#addMappingProvider(net.eq2online.permissions.ReplicatedPermissionsMappingProvider)
	 */
	@Override
	public void addMappingProvider(ReplicatedPermissionsMappingProvider provider)
	{
		if (!this.mappingProviders.contains(provider))
		{
			this.mappingProviders.add(0, provider);
		}
	}

	/* (non-Javadoc)
	 * @see net.eq2online.permissions.ReplicatedPermissionsManager#initAllMappingProviders()
	 */
	@Override
	public void initAllMappingProviders()
	{
		for (ReplicatedPermissionsMappingProvider provider : this.mappingProviders)
		{
			provider.initPermissionsMappingProvider(this.parent.getDataFolder());
		}
	}
	
	/* (non-Javadoc)
	 * @see net.eq2online.permissions.ReplicatedPermissionsManager#checkVersion(org.bukkit.entity.Player, net.eq2online.permissions.ReplicatedPermissionsContainer)
	 */
	@Override
	public boolean checkVersion(Player player, ReplicatedPermissionsContainer data)
	{
		if (data.modName.equals("all")) return true;
		
		for (ReplicatedPermissionsMappingProvider provider : this.mappingProviders)
		{
			if (!provider.checkVersion(this.parent, player, data) && !player.hasPermission(ReplicatedPermissionsPlugin.ADMIN_PERMISSION_NODE)) return false;
		}		
		
		return true;
	}

	/* (non-Javadoc)
	 * @see net.eq2online.permissions.ReplicatedPermissionsManager#replicatePermissions(org.bukkit.entity.Player, net.eq2online.permissions.ReplicatedPermissionsContainer)
	 */
	@Override
	public void replicatePermissions(Player player, ReplicatedPermissionsContainer data)
	{
		Set<String> replicatePermissions = new HashSet<String>();
		boolean havePermissions = false;
		
		for (ReplicatedPermissionsMappingProvider provider : this.mappingProviders)
		{
			if (provider.providesPermissionMappingsFor(data))
			{
				havePermissions = true;
				
				List<String> permissions = provider.getPermissions(this.parent, player, data);
				replicatePermissions.addAll(permissions);
				break;
			}
		}
		
		if (havePermissions)
		{
			ReplicatedPermissionsContainer replContainer = new ReplicatedPermissionsContainer(data.modName, data.modVersion, replicatePermissions);
			
			byte[] replData = replContainer.getBytes();
			player.sendPluginMessage(this.parent, ReplicatedPermissionsContainer.CHANNEL, replData);
		}
	}
}
