package net.eq2online.permissions.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import net.eq2online.permissions.ReplicatedPermissionsContainer;
import net.eq2online.permissions.plugin.providers.PermissionsMappingProviderGeneric;

/**
 * Main Bukkit plugin class for the client permissions system
 *
 * @author Adam Mummery-Smith
 */
public class ReplicatedPermissionsPlugin extends JavaPlugin implements Listener, PluginMessageListener
{
	/**
	 * Admin permission node needed for changing plugin settings 
	 */
	public static final String ADMIN_PERMISSION_NODE = "clientmods.admin";
	
	/**
	 * Generic provider (permissions bridge)
	 */
	private ReplicatedPermissionsProvider permissionsProvider;

	/**
	 * Permissions bridge manager
	 */
	private ReplicatedPermissionsManager permissionsManager;
	
	/**
	 * Command handler
	 */
	private ReplicatedPermissionsPluginCommandHandler commandHandler;
	
	/**
	 * 
	 */
	private ReplicatedPermissionsMonitor monitor;

	/* (non-Javadoc)
	 * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
	 */
	@Override
	public void onEnable()
	{
		// Command handler
		this.commandHandler = new ReplicatedPermissionsPluginCommandHandler(this);
		
		// Monitor
		this.monitor = new ReplicatedPermissionsMonitor(this);
		
		// Initialise permission manager
		this.permissionsManager = new ReplicatedPermissionsManagerImpl(this);
		this.addDefaultProviders(this.permissionsManager);
		this.permissionsManager.initAllMappingProviders();

		// Register the plugin channels we will use
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, ReplicatedPermissionsContainer.CHANNEL);
		Bukkit.getMessenger().registerIncomingPluginChannel(this, ReplicatedPermissionsContainer.CHANNEL, this);

		// Register the event handlers on this object
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	/**
	 * Add default permissions provider, stub 
	 */
	public void addDefaultProviders(ReplicatedPermissionsManager permissionsManager)
	{
		PermissionsMappingProviderGeneric genericProvider = new PermissionsMappingProviderGeneric();
		this.permissionsProvider = genericProvider;
		permissionsManager.addMappingProvider(genericProvider);
	}

	/* (non-Javadoc)
	 * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (sender != null && "clientperms".equalsIgnoreCase(command.getName()) && args.length > 0)
		{
			return this.commandHandler.onCommand(sender, command, label, args[0], args);
		}
		
		return false;
	}

	/**
	 * Called when the player registers a plugin channel, we check whether it's the replicated permissions channel and initialise
	 * a container for the mod info for the player if it is
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event)
	{
		if (event.getChannel().equals(ReplicatedPermissionsContainer.CHANNEL))
		{
			this.monitor.addPlayer(event.getPlayer());
		}
	}

	/**
	 * Handle player quit event
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		this.monitor.removePlayer(event.getPlayer());
	}
	
	/* (non-Javadoc)
	 * @see org.bukkit.plugin.messaging.PluginMessageListener#onPluginMessageReceived(java.lang.String, org.bukkit.entity.Player, byte[])
	 */
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message)
	{
		if (this.permissionsManager == null) return;
		
		ReplicatedPermissionsContainer query = null;
		
		try
		{
			query = ReplicatedPermissionsContainer.fromBytes(message);
		}
		catch (Exception ex) {}
		
		if (query != null)
		{
			query.sanitise();

			if (!this.permissionsManager.checkVersion(player, query) && !player.hasPermission(ADMIN_PERMISSION_NODE) && !player.isOp())
			{
				this.getLogger().info("Kicking player [" + player.getName() + "] due to outdated mod [" + query.modName + "]. Has version " + query.modVersion);
				player.kickPlayer(String.format("Mod '%s' is out of date", query.modName));
				
				return;
			}
			
			this.monitor.onQuery(player, query);
			this.permissionsManager.replicatePermissions(player, query);
		}
	}

	/**
	 * @return the replicatedPermissionsManager
	 */
	public ReplicatedPermissionsManager getPermissionsManager()
	{
		return this.permissionsManager;
	}
	
	/**
	 * @return the permissions rovider
	 */
	public ReplicatedPermissionsProvider getProvider()
	{
		return this.permissionsProvider;
	}

	/**
	 * @return the monitor
	 */
	public ReplicatedPermissionsMonitor getMonitor()
	{
		return this.monitor;
	}
}
