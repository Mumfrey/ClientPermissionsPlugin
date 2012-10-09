package net.eq2online.permissions;

import java.util.logging.Logger;

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

import net.eq2online.permissions.providers.PermissionsMappingProviderGeneric;

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
	 * Logger
	 */
	private static Logger logger = Logger.getLogger("ClientPermissions");
	
	/**
	 * Generic provider (permissions bridge)
	 */
	private PermissionsMappingProviderGeneric genericProvider;

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
		logger.info("[ClientPermissions] Client mod permissions plugin is starting");
		
		// Command handler
		this.commandHandler = new ReplicatedPermissionsPluginCommandHandler(this);
		
		// Monitor
		this.monitor = new ReplicatedPermissionsMonitor(this.getDataFolder());
		
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
		permissionsManager.addMappingProvider(this.genericProvider = new PermissionsMappingProviderGeneric());
	}

	/* (non-Javadoc)
	 * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (sender != null && command.getName().equalsIgnoreCase("clientperms") && args.length > 0)
		{
			return commandHandler.onCommand(sender, command, label, args[0], args);
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
			monitor.addPlayer(event.getPlayer());
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
		monitor.removePlayer(event.getPlayer());
	}

	/* (non-Javadoc)
	 * @see org.bukkit.plugin.messaging.PluginMessageListener#onPluginMessageReceived(java.lang.String, org.bukkit.entity.Player, byte[])
	 */
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message)
	{
		if (permissionsManager == null) return;
		
		ReplicatedPermissionsContainer query = null;
		
		try
		{
			query = ReplicatedPermissionsContainer.fromBytes(message);
			query.sanitise();
		}
		catch (Exception ex) {}
		
		if (query != null)
		{
			if (!permissionsManager.checkVersion(player, query) && !player.hasPermission(ADMIN_PERMISSION_NODE) && !player.isOp())
			{
				logger.info("[ClientPermissions] Kicking player [" + player.getName() + "] due to outdated mod [" + query.modName + "]. Has version " + query.modVersion);
				player.kickPlayer(String.format("Mod '%s' is out of date", query.modName));
				
				return;
			}
			
			monitor.onQuery(player, query);
			permissionsManager.replicatePermissions(player, query);
		}
	}

	/**
	 * @return the replicatedPermissionsManager
	 */
	public ReplicatedPermissionsManager getPermissionsManager()
	{
		return permissionsManager;
	}
	
	/**
	 * @return the genericProvider
	 */
	public PermissionsMappingProviderGeneric getGenericProvider()
	{
		return genericProvider;
	}

	/**
	 * @return the monitor
	 */
	public ReplicatedPermissionsMonitor getMonitor()
	{
		return monitor;
	}
}
