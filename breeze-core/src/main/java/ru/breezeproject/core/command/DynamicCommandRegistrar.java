package ru.breezeproject.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DynamicCommandRegistrar {

  private final CommandMap commandMap;
  private final Logger logger;

  public DynamicCommandRegistrar(Logger logger) throws ReflectiveOperationException {
    this.logger = logger;
    Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
    field.setAccessible(true);
    this.commandMap = (CommandMap) field.get(Bukkit.getServer());
  }

  public Command register(String fallbackPrefix, String name, List<String> aliases,
      String description, String usage,
      CommandExecutor executor, TabCompleter tabCompleter) {
    DelegatingCommand command = new DelegatingCommand(name, description, usage, aliases, executor, tabCompleter);
    commandMap.register(fallbackPrefix, command);
    return command;
  }

  public void unregister(Command command) {
    command.unregister(commandMap);

    try {
      Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
      knownCommandsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
      knownCommands.values().removeIf(c -> c == command);
    } catch (Exception e) {
      throw new IllegalStateException("Could not fully unregister command `" + command.getName() + "'", e);
    }
  }

  private static class DelegatingCommand extends Command {

    private final CommandExecutor executor;
    private final TabCompleter tabCompleter;

    DelegatingCommand(String name, String description, String usage, List<String> aliases,
        CommandExecutor executor, TabCompleter tabCompleter) {
      super(name, description == null ? "" : description, usage == null ? "/" + name : usage, aliases);
      this.executor = executor;
      this.tabCompleter = tabCompleter;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
      return executor.onCommand(sender, this, label, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, String[] args) {
      if (tabCompleter != null) {
        List<String> result = tabCompleter.onTabComplete(sender, this, label, args);
        if (result != null) {
          return result;
        }
      }
      return super.tabComplete(sender, label, args);
    }
  }
}
