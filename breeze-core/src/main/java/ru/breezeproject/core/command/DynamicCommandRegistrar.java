package ru.breezeproject.core.command;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
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

    try {
      final Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
      knownCommandsField.setAccessible(true);
      @SuppressWarnings("unchecked")
	final
      Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
      knownCommands.values().removeIf(c -> c == command);
    } catch (final Exception e) {
      logger.warning(
          "Could not fully unregister command '" + command.getName() + "' from knownCommands: " + e.getMessage());
    }
  }
}
