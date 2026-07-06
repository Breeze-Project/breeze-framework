package ru.breezeproject.core.context;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import ru.breezeproject.api.command.ModuleCommandExecutor;
import ru.breezeproject.api.command.ModuleTabCompleter;
import ru.breezeproject.api.event.BreezeListener;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.module.BreezeModuleContext;
import ru.breezeproject.api.service.ServiceRegistry;
import ru.breezeproject.core.command.BukkitCommandSenderAdapter;
import ru.breezeproject.core.command.DynamicCommandRegistrar;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BreezeModuleContextImpl implements BreezeModuleContext {
  private final ServiceRegistry serviceRegistry;
  private final EventBus eventBus;
  private final Path dataFolder;
  private final DynamicCommandRegistrar commandRegistrar;
  private final Plugin ownerPlugin;
  private final String moduleName;
  private final List<Command> registeredCommands = new ArrayList<>();
  private final List<Listener> registeredListeners = new ArrayList<>();

  public BreezeModuleContextImpl(ServiceRegistry serviceRegistry, EventBus eventBus, Path dataFolder,
      DynamicCommandRegistrar commandRegistrar, Plugin ownerPlugin, String moduleName) {
    this.serviceRegistry = serviceRegistry;
    this.eventBus = eventBus;
    this.dataFolder = dataFolder;
    this.commandRegistrar = commandRegistrar;
    this.ownerPlugin = ownerPlugin;
    this.moduleName = moduleName;
    try {
      if (!Files.exists(dataFolder)) {
        Files.createDirectories(dataFolder);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Could not create module data directory", e);
    }
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @Override
  public EventBus getEventBus() {
    return eventBus;
  }

  @Override
  public Path getDataFolder() {
    return dataFolder;
  }

  @Override
  public void registerCommand(String name, List<String> aliases, String description, String usage,
      ModuleCommandExecutor executor, ModuleTabCompleter tabCompleter) {
    CommandExecutor bukkitExecutor = (sender, command, label, args) -> executor
        .onCommand(new BukkitCommandSenderAdapter(sender), label, args);
    TabCompleter bukkitTabCompleter = tabCompleter == null ? null
        : (sender, command, label, args) -> tabCompleter.onTabComplete(new BukkitCommandSenderAdapter(sender), args);

    Command command = commandRegistrar.register(moduleName.toLowerCase(), name, aliases, description, usage, bukkitExecutor,
        bukkitTabCompleter);
    registeredCommands.add(command);
  }

  @Override
  public void registerListener(BreezeListener listener) {
    if (!(listener instanceof Listener bukkitListener)) {
      throw new IllegalArgumentException("Listener must implement org.bukkit.event.Listener");
    }
    Bukkit.getPluginManager().registerEvents(bukkitListener, ownerPlugin);
    registeredListeners.add(bukkitListener);
  }

  public void unregisterAllCommands() {
    registeredCommands.forEach(commandRegistrar::unregister);
    registeredCommands.clear();
  }

  public void unregisterAllListeners() {
    registeredListeners.forEach(HandlerList::unregisterAll);
    registeredListeners.clear();
  }
}
