package ru.breezeproject.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import ru.breezeproject.core.loader.ModuleManager;

public class ModulesCommand implements CommandExecutor {
  public static final String PERMISSION_LIST = "breeze.modules.list";
  public static final String PERMISSION_RELOAD = "breeze.modules.reload";

  private final ModuleManager moduleManager;

  public ModulesCommand(final ModuleManager moduleManager) {
    this.moduleManager = moduleManager;
  }

  @Override
  public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
    if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
      return handleReload(sender, args);
    }
    return listModules(sender);
  }

  private boolean listModules(final CommandSender sender) {
    if (!sender.hasPermission(PERMISSION_LIST)) {
      sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
      return true;
    }

    if (moduleManager.getLoadedModules().isEmpty()) {
      sender.sendMessage(ChatColor.YELLOW + "No modules loaded.");
      return true;
    }

    sender.sendMessage(ChatColor.AQUA + "Loaded BreezeCore modules:");
    moduleManager.getLoadedModules().values()
        .forEach(module -> sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + module.getName()));

    if (sender.hasPermission(PERMISSION_RELOAD)) {
      sender.sendMessage(ChatColor.DARK_GRAY + "Use /breezemodules reload <name> to reload a single module.");
    }
    return true;
  }

  private boolean handleReload(final CommandSender sender, final String[] args) {
    if (!sender.hasPermission(PERMISSION_RELOAD)) {
      sender.sendMessage(ChatColor.RED + "You don't have permission to reload modules.");
      return true;
    }

    if (args.length < 2) {
      sender.sendMessage(ChatColor.RED + "Usage: /breezemodules reload <name>");
      return true;
    }

    final String name = args[1];
    final boolean success = moduleManager.reload(name);

    if (success) {
      sender.sendMessage(ChatColor.GREEN + "Reloaded module '" + name + "'.");
    } else {
      sender.sendMessage(ChatColor.RED + "No loaded module named '" + name
          + "' (names are case-sensitive; check /breezemodules).");
    }
    return true;
  }
}
