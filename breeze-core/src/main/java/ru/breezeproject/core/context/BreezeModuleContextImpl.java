package ru.breezeproject.core.context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import ru.breezeproject.api.command.ModuleCommandExecutor;
import ru.breezeproject.api.command.ModuleTabCompleter;
import ru.breezeproject.api.event.BreezeEvent;
import ru.breezeproject.api.event.BreezeListener;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.event.EventBus.Subscription;
import ru.breezeproject.api.event.EventPriority;
import ru.breezeproject.api.module.BreezeModuleContext;
import ru.breezeproject.api.service.ServiceRegistry;
import ru.breezeproject.core.command.BukkitCommandSenderAdapter;
import ru.breezeproject.core.command.DynamicCommandRegistrar;

public class BreezeModuleContextImpl implements BreezeModuleContext {
  private final ServiceRegistry serviceRegistry;
  private final EventBus delegateBus;
  private final EventBus trackingBus;
  private final Path dataFolder;
  private final DynamicCommandRegistrar commandRegistrar;
  private final JavaPlugin ownerPlugin;
  private final String moduleName;

  private final List<Command> registeredCommands = new ArrayList<>();
  private final List<Listener> registeredListeners = new ArrayList<>();
  private final List<Subscription> subscriptions = new ArrayList<>();

  public BreezeModuleContextImpl(final ServiceRegistry serviceRegistry,
      final EventBus eventBus,
      final Path dataFolder,
      final DynamicCommandRegistrar commandRegistrar,
      final JavaPlugin ownerPlugin,
      final String moduleName) {
    this.serviceRegistry = serviceRegistry;
    this.delegateBus = eventBus;
    this.trackingBus = new TrackingEventBus(eventBus, subscriptions);
    this.dataFolder = dataFolder;
    this.commandRegistrar = commandRegistrar;
    this.ownerPlugin = ownerPlugin;
    this.moduleName = moduleName;

    try {
      if (!Files.exists(dataFolder)) {
        Files.createDirectories(dataFolder);
      }
    } catch (final Exception e) {
      throw new IllegalStateException("Could not create module data directory", e);
    }
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @Override
  public EventBus getEventBus() {
    return trackingBus;
  }

  @Override
  public Path getDataFolder() {
    return dataFolder;
  }

  @Override
  public void registerCommand(final String name, final List<String> aliases, final String description, final String usage,
      final ModuleCommandExecutor executor, final ModuleTabCompleter tabCompleter) {
    final CommandExecutor bukkitExecutor = (sender, command, label, args) -> executor
        .onCommand(new BukkitCommandSenderAdapter(sender), label, args);
    final TabCompleter bukkitTabCompleter = tabCompleter == null ? null
        : (sender, command, label, args) -> tabCompleter.onTabComplete(new BukkitCommandSenderAdapter(sender), args);

    final Command command = commandRegistrar.register(
        moduleName.toLowerCase(), name, aliases, description, usage,
        bukkitExecutor, bukkitTabCompleter);
    registeredCommands.add(command);
  }

  @Override
  public void registerListener(final BreezeListener listener) {
    if (!(listener instanceof final Listener bukkitListener)) {
      throw new IllegalArgumentException("Listener must implement org.bukkit.event.Listener");
    }
    Bukkit.getPluginManager().registerEvents(bukkitListener, ownerPlugin);
    registeredListeners.add(bukkitListener);
  }

  public void cleanup() {
    subscriptions.forEach(delegateBus::unsubscribe);
    subscriptions.clear();
    registeredCommands.forEach(commandRegistrar::unregister);
    registeredCommands.clear();
    registeredListeners.forEach(HandlerList::unregisterAll);
    registeredListeners.clear();
  }

  private static final class TrackingEventBus implements EventBus {
    private final EventBus delegate;
    private final List<Subscription> sink;

    TrackingEventBus(final EventBus delegate, final List<Subscription> sink) {
      this.delegate = delegate;
      this.sink = sink;
    }

    @Override
    public <T extends BreezeEvent> Subscription subscribe(final Class<T> eventType, final Consumer<T> handler) {
      final Subscription sub = delegate.subscribe(eventType, handler);
      sink.add(sub);
      return sub;
    }

    @Override
    public <T extends BreezeEvent> Subscription subscribe(final Class<T> eventType, final EventPriority priority,
        final Consumer<T> handler) {
      final Subscription sub = delegate.subscribe(eventType, priority, handler);
      sink.add(sub);
      return sub;
    }

    @Override
    public void unsubscribe(final Subscription subscription) {
      delegate.unsubscribe(subscription);
      sink.remove(subscription);
    }

    @Override
    public <T extends BreezeEvent> void publish(final T event) {
      delegate.publish(event);
    }
  }
}
