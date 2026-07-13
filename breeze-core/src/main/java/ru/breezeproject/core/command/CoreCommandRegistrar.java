package ru.breezeproject.core.command;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import ru.breezeproject.core.commands.ModulesCommand;
import ru.breezeproject.core.loader.ModuleManager;

public class CoreCommandRegistrar {
  private final JavaPlugin plugin;
  private final ModuleManager moduleManager;

  public CoreCommandRegistrar(final JavaPlugin plugin, final ModuleManager moduleManager) {
    this.plugin = plugin;
    this.moduleManager = moduleManager;
  }

  public void registerAll() {
    registerModulesCommand();
  }

  private void registerModulesCommand() {
    final PluginCommand cmd = plugin.getCommand("breezemodules");
    if (cmd == null) {
      throw new IllegalStateException("Command 'breezemodules' not defined in plugin.yml");
    }
    final ModulesCommand modulesCommand = new ModulesCommand(moduleManager);
    cmd.setExecutor(modulesCommand);
    cmd.setTabCompleter(modulesCommand);
  }
}
