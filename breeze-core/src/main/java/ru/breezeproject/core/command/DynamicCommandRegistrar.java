package ru.breezeproject.core.command;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class DynamicCommandRegistrar {
  private static class DelegatingCommand extends Command {

    private final CommandExecutor executor;
    private final TabCompleter tabCompleter;

    DelegatingCommand(final String name, final String description, final String usage, final List<String> aliases,
        final CommandExecutor executor, final TabCompleter tabCompleter) {
      super(name, description == null ? "" : description, usage == null ? "/" + name : usage, aliases);
      this.executor = executor;
      this.tabCompleter = tabCompleter;
    }

    @Override
    public boolean execute(final CommandSender sender, final String label, final String[] args) {
      return executor.onCommand(sender, this, label, args);
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String label, final String[] args) {
      if (tabCompleter != null) {
        final List<String> result = tabCompleter.onTabComplete(sender, this, label, args);
        if (result != null) {
          return result;
        }
      }
      return super.tabComplete(sender, label, args);
    }
  }

  private final CommandMap commandMap;
  private final Logger logger;

  public DynamicCommandRegistrar(final Logger logger) throws ReflectiveOperationException {
    this.logger = logger;
    final Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
    field.setAccessible(true);
    this.commandMap = (CommandMap) field.get(Bukkit.getServer());
  }

  public Command register(final String fallbackPrefix, final String name, final List<String> aliases,
      final String description, final String usage,
      final CommandExecutor executor, final TabCompleter tabCompleter) {
    final DelegatingCommand command = new DelegatingCommand(name, description, usage, aliases, executor, tabCompleter);
    commandMap.register(fallbackPrefix, command);
    return command;
  }

  public void unregister(final Command command) {
    command.unregister(commandMap);
    purgeKnownCommands(command);
  }

  public void syncCommands() {
    try {
      final Method method = Bukkit.getServer().getClass().getMethod("syncCommands");
      method.invoke(Bukkit.getServer());
    } catch (final ReflectiveOperationException ignored) {
    }
  }

  private void purgeKnownCommands(final Command command) {
    final Map<String, Command> knownCommands = getKnownCommands();
    if (knownCommands == null) {
      return;
    }

    final Set<String> keysToRemove = new HashSet<>();
    final String lowerName = command.getName().toLowerCase(Locale.ROOT);

    for (final Map.Entry<String, Command> entry : knownCommands.entrySet()) {
      if (entry.getValue() == command) {
        keysToRemove.add(entry.getKey());
        continue;
      }

      final String key = entry.getKey().toLowerCase(Locale.ROOT);
      if (matchesCommandKey(key, lowerName, command.getAliases())) {
        keysToRemove.add(entry.getKey());
      }
    }

    for (final String key : keysToRemove) {
      knownCommands.remove(key);
    }
  }

  private boolean matchesCommandKey(final String key, final String lowerName, final List<String> aliases) {
    if (key.equals(lowerName) || key.endsWith(":" + lowerName)) {
      return true;
    }
    for (final String alias : aliases) {
      final String lowerAlias = alias.toLowerCase(Locale.ROOT);
      if (key.equals(lowerAlias) || key.endsWith(":" + lowerAlias)) {
        return true;
      }
    }
    return false;
  }

  private Map<String, Command> getKnownCommands() {
    try {
      return commandMap.getKnownCommands();
    } catch (final Exception ignored) {
      try {
        final Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
        knownCommandsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
        return knownCommands;
      } catch (final Exception e) {
        logger.warning("Could not access knownCommands: " + e.getMessage());
        return null;
      }
    }
  }
}
