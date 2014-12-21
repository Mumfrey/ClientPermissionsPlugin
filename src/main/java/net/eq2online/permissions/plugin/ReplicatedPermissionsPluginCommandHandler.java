package net.eq2online.permissions.plugin;

import java.util.*;
import java.util.Map.Entry;

import net.eq2online.permissions.ReplicatedPermissionsContainer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReplicatedPermissionsPluginCommandHandler
{
	/**
	 * Plugin which owns this manager 
	 */
	private ReplicatedPermissionsPlugin parent;

	public ReplicatedPermissionsPluginCommandHandler(ReplicatedPermissionsPlugin plugin)
	{
		this.parent = plugin;
	}
	
	/**
	 * Called when a plugin command is received 
	 * 
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param cmd First arg which is the verb 
     * @param args Passed command arguments
     * @return true if a valid command, otherwise false
	 */
	public boolean onCommand(CommandSender sender, Command command, String label, String cmd, String[] args)
	{
		if (cmd.equalsIgnoreCase("query"))
		{
			return onCommandQuery(sender, command, label, cmd, args);
		}
		
		if (cmd.equalsIgnoreCase("refresh"))
		{
			return onCommandRefresh(sender, command, label, cmd, args);
		}
		
		if (cmd.equalsIgnoreCase("listmods") || cmd.equalsIgnoreCase("list"))
		{
			return onCommandListMods(sender, command, label, cmd, args);
		}
		
		if (cmd.equalsIgnoreCase("addmod") || cmd.equalsIgnoreCase("add"))
		{
			return onCommandAddMod(sender, command, label, cmd, args);
		}
		
		if (cmd.equalsIgnoreCase("removemod") || cmd.equalsIgnoreCase("remove"))
		{
			return onCommandRemoveMod(sender, command, label, cmd, args);
		}
		
		if (cmd.equalsIgnoreCase("modversion") || cmd.equalsIgnoreCase("version") || cmd.equalsIgnoreCase("ver"))
		{
			return onCommandModVersion(sender, command, label, cmd, args);
		}
		
		return false;
	}
	
	private boolean onCommandQuery(CommandSender sender, Command command, String label, String cmd, String[] args)
	{
		if (sender.hasPermission(ReplicatedPermissionsPlugin.ADMIN_PERMISSION_NODE))
		{
			String queryPlayerName = getQueryPlayerName(sender, args, 1);
			
			if (!queryPlayerName.equals("CONSOLE"))
			{
				Hashtable<String, ReplicatedPermissionsContainer> playerMods = null;
	
				for (Entry<String, Hashtable<String, ReplicatedPermissionsContainer>> player : this.parent.getMonitor().getPlayerModInfo().entrySet())
				{
					if (player.getKey().equalsIgnoreCase(queryPlayerName))
					{
						playerMods = player.getValue();
						queryPlayerName = player.getKey();
						break;
					}
				}
				
				String reply = "";
				
				if (playerMods != null)
				{
					boolean first = true;
					
					for (Entry<String, ReplicatedPermissionsContainer> modInfo : playerMods.entrySet())
					{
						if (!modInfo.getKey().equals("all"))
						{
							String modName = String.format("%S%s %.2f", modInfo.getKey().substring(0, 1), modInfo.getKey().substring(1), modInfo.getValue().modVersion);
							
							if (!first) reply += ", "; first = false;
							reply += ChatColor.AQUA + modName + ChatColor.RESET;
						}
					}
				}
				else
				{
					reply = ChatColor.YELLOW + "not registered";
				}
	
				sender.sendMessage(ChatColor.GREEN + "Player: " + ChatColor.AQUA + queryPlayerName);
				sender.sendMessage(ChatColor.GREEN + "Mods: " + reply);
			}
			else
			{
				sender.sendMessage(String.format(ChatColor.GREEN + "/%s %s " + ChatColor.AQUA + "<player>", label, cmd));
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "No permission");
		}
		
		return true;
	}

	private boolean onCommandRefresh(CommandSender sender, Command command, String label, String cmd, String[] args)
	{
		String queryPlayerName = getQueryPlayerName(sender, args, 1);
		
		if (!queryPlayerName.equals("CONSOLE"))
		{
			Hashtable<String, ReplicatedPermissionsContainer> playerMods = null;

			for (Entry<String, Hashtable<String, ReplicatedPermissionsContainer>> player : this.parent.getMonitor().getPlayerModInfo().entrySet())
			{
				if (player.getKey().equalsIgnoreCase(queryPlayerName))
				{
					playerMods = player.getValue();
					queryPlayerName = player.getKey();
					break;
				}
			}
			
			Player player = Bukkit.getPlayer(queryPlayerName);
			
			if (player != null && playerMods != null && (sender.hasPermission(ReplicatedPermissionsPlugin.ADMIN_PERMISSION_NODE) || sender.getName().equals(queryPlayerName)))
			{
				for (Map.Entry<String, ReplicatedPermissionsContainer> modEntry : playerMods.entrySet())
				{
					this.parent.getPermissionsManager().replicatePermissions(player, modEntry.getValue());
				}
				
				sender.sendMessage(ChatColor.GREEN + "Refreshed mod permissions for player: " + ChatColor.AQUA + queryPlayerName);
			}
		}
		else
		{
			sender.sendMessage(String.format(ChatColor.GREEN + "/%s %s " + ChatColor.AQUA + "<player>", label, cmd));
		}
		
		return true;
	}

	private boolean onCommandListMods(CommandSender sender, Command command, String label, String cmd, String[] args)
	{
		if (sender.hasPermission(ReplicatedPermissionsPlugin.ADMIN_PERMISSION_NODE))
		{
			List<String> supportedModList = this.parent.getProvider().getMods();
			
			boolean first = true;
			String reply = ChatColor.GREEN + "Supported mods: ";
			
			for (String supportedMod : supportedModList)
			{
				Float minVersion = this.parent.getProvider().getMinModVersion(supportedMod);
				if (minVersion > 0.0F) supportedMod += "(" + minVersion + ")";
				
				if (!first) reply += ", "; first = false;
				reply += ChatColor.AQUA + supportedMod + ChatColor.RESET;
			}
			
			sender.sendMessage(reply);
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "No permission");
		}
		
		return true;
	}

	private boolean onCommandAddMod(CommandSender sender, Command command, String label, String cmd, String[] args)
	{
		if (sender.hasPermission(ReplicatedPermissionsPlugin.ADMIN_PERMISSION_NODE))
		{
			if (args.length > 1)
			{
				String modName = args[1].toLowerCase();
				
				if (this.parent.getProvider().addMod(modName))
				{
					sender.sendMessage(ChatColor.GREEN + "Added supported mod " + ChatColor.AQUA + modName + ChatColor.GREEN + " successfully");
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "Failed to add supported mod " + ChatColor.AQUA + modName + ChatColor.RED + ". Maybe the mod is already registered?");
				}

				onCommandListMods(sender, command, label, cmd, args);
			}
			else
			{
				sender.sendMessage(String.format(ChatColor.GREEN + "/%s %s " + ChatColor.AQUA + "<modname>", label, cmd));
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "No permission");
		}
		
		return true;
	}

	private boolean onCommandRemoveMod(CommandSender sender, Command command, String label, String cmd, String[] args)
	{
		if (sender.hasPermission(ReplicatedPermissionsPlugin.ADMIN_PERMISSION_NODE))
		{
			if (args.length > 1)
			{
				String modName = args[1].toLowerCase();
				
				if (this.parent.getProvider().removeMod(modName))
				{
					sender.sendMessage(ChatColor.GREEN + "Removed supported mod " + ChatColor.AQUA + modName + ChatColor.GREEN + " successfully");
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "Failed to remove supported mod " + ChatColor.AQUA + modName + ChatColor.RED + ". Maybe the mod is not registered?");
				}
				
				onCommandListMods(sender, command, label, cmd, args);
			}
			else
			{
				sender.sendMessage(String.format(ChatColor.GREEN + "/%s %s " + ChatColor.AQUA + "<modname>", label, cmd));
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "No permission");
		}
		
		return true;
	}
	
	private boolean onCommandModVersion(CommandSender sender, Command command, String label, String cmd, String[] args)
	{
		if (sender.hasPermission(ReplicatedPermissionsPlugin.ADMIN_PERMISSION_NODE))
		{
			if (args.length > 1)
			{
				String modName = args[1].toLowerCase();
				
				if (args.length > 2)
				{
					float modVersion = -1;
					try
					{
						modVersion = Float.parseFloat(args[2]);
					} catch (NumberFormatException ex) {}
					
					if (modVersion > -1)
					{
						if (this.parent.getProvider().setMinModVersion(modName, modVersion))
						{
							sender.sendMessage(ChatColor.GREEN + "Updated mod " + ChatColor.AQUA + modName + ChatColor.GREEN + " successfully");
						}
						else
						{
							sender.sendMessage(ChatColor.RED + "Failed to update mod " + ChatColor.AQUA + modName + ChatColor.RED + ". Maybe the mod is not registered?");
						}
					}
				}
					
				float minModVersion = this.parent.getProvider().getMinModVersion(modName);
				String strMinModVersion = (minModVersion > 0.0F) ? String.valueOf(minModVersion) : "none"; 
				sender.sendMessage(ChatColor.GREEN + "Mod: " + ChatColor.AQUA + modName + ChatColor.GREEN + " requires minimum version " + ChatColor.AQUA + strMinModVersion);
			}
			else
			{
				sender.sendMessage(String.format(ChatColor.GREEN + "/%s %s " + ChatColor.AQUA + "<modname> {version}", label, cmd));
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "No permission");
		}
		
		return true;
	}
	
	/**
	 * @param sender
	 * @param args
	 * @param argPos
	 * @return
	 */
	public String getQueryPlayerName(CommandSender sender, String[] args, int argPos)
	{
		return (args.length > argPos) ? args[argPos] : sender.getName();
	}
}
