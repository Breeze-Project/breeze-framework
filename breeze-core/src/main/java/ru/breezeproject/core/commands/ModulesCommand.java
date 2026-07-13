package ru.breezeproject.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import ru.breezeproject.core.loader.ModuleManager;

public class ModulesCommand implements CommandExecutor, TabCompleter {
  public static final String PERMISSION_LIST = "breeze.modules.list";
  public static final String PERMISSION_RELOAD = "breeze.modules.reload";
  public static final String PERMISSION_LOAD = "breeze.modules.load";
  public static final String PERMISSION_ENABLE = "breeze.modules.enable";
  public static final String PERMISSION_DISABLE = "breeze.modules.disable";

  private final ModuleManager moduleManager;

  public ModulesCommand(final ModuleManager moduleManager) {
    this.moduleManager = moduleManager;
  }

  @Override
  public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
    if (args.length == 0) {
      return listModules(sender);
    }
    return switch (args[0].toLowerCase()) {
      case "reload" -> handleReload(sender, args);
      case "load" -> handleLoad(sender, args);
      case "enable" -> handleEnable(sender, args);
      case "disable" -> handleDisable(sender, args);
      default -> listModules(sender);
    };
  }

  @Override
  public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias,
      final String[] args) {
    if (args.length == 1) {
      return filter(args[0], List.of("load", "reload", "enable", "disable"));
    }
    if (args.length == 2) {
      return switch (args[0].toLowerCase()) {
        case "reload", "enable", "disable", "load" -> filter(args[1], knownModuleNames());
        default -> List.of();
      };
    }
    return List.of();
  }

  private boolean listModules(final CommandSender sender) {
    if (!sender.hasPermission(PERMISSION_LIST)) {
      sender.sendMessage(ChatColor.RED + "У вас нет прав на эту команду.");
      return true;
    }

    sender.sendMessage(ChatColor.AQUA + "Модули BreezeCore:");

    if (moduleManager.getLoadedModules().isEmpty()) {
      sender.sendMessage(ChatColor.GRAY + " Загруженные: " + ChatColor.YELLOW + "нет");
    } else {
      sender.sendMessage(ChatColor.GRAY + " Загруженные:");
      moduleManager.getLoadedModules().values()
          .forEach(module -> sender.sendMessage(ChatColor.GREEN + "  - " + ChatColor.WHITE + module.getName()));
    }

    final Set<String> disabled = moduleManager.getDisabledModules();
    if (disabled.isEmpty()) {
      sender.sendMessage(ChatColor.GRAY + " Отключённые: " + ChatColor.YELLOW + "нет");
    } else {
      sender.sendMessage(ChatColor.GRAY + " Отключённые:");
      disabled.forEach(name -> sender.sendMessage(ChatColor.RED + "  - " + ChatColor.WHITE + name));
    }

    if (sender.hasPermission(PERMISSION_LOAD)) {
      sender.sendMessage(ChatColor.DARK_GRAY + "Команды: load, reload, enable, disable");
    }
    return true;
  }

  private boolean handleReload(final CommandSender sender, final String[] args) {
    if (!sender.hasPermission(PERMISSION_RELOAD)) {
      sender.sendMessage(ChatColor.RED + "У вас нет прав на перезагрузку модулей.");
      return true;
    }
    if (args.length < 2) {
      sender.sendMessage(ChatColor.RED + "Использование: /breezemodules reload <имя>");
      return true;
    }

    final String name = args[1];
    if (moduleManager.reload(name)) {
      sender.sendMessage(ChatColor.GREEN + "Модуль '" + name + "' перезагружен.");
    } else {
      sender.sendMessage(ChatColor.RED + "Не удалось перезагрузить модуль '" + name + "'.");
    }
    return true;
  }

  private boolean handleLoad(final CommandSender sender, final String[] args) {
    if (!sender.hasPermission(PERMISSION_LOAD)) {
      sender.sendMessage(ChatColor.RED + "У вас нет прав на загрузку модулей.");
      return true;
    }

    if (args.length < 2) {
      final int loaded = moduleManager.loadNew();
      if (loaded > 0) {
        sender.sendMessage(ChatColor.GREEN + "Загружено новых модулей: " + loaded);
      } else {
        sender.sendMessage(ChatColor.YELLOW + "Новых модулей для загрузки не найдено.");
      }
      return true;
    }

    final String name = args[1];
    if (moduleManager.load(name)) {
      sender.sendMessage(ChatColor.GREEN + "Модуль '" + name + "' загружен.");
    } else {
      sender.sendMessage(ChatColor.RED + "Не удалось загрузить модуль '" + name + "'.");
    }
    return true;
  }

  private boolean handleEnable(final CommandSender sender, final String[] args) {
    if (!sender.hasPermission(PERMISSION_ENABLE)) {
      sender.sendMessage(ChatColor.RED + "У вас нет прав на включение модулей.");
      return true;
    }
    if (args.length < 2) {
      sender.sendMessage(ChatColor.RED + "Использование: /breezemodules enable <имя>");
      return true;
    }

    final String name = args[1];
    if (moduleManager.enable(name)) {
      sender.sendMessage(ChatColor.GREEN + "Модуль '" + name + "' включён.");
    } else {
      sender.sendMessage(ChatColor.RED + "Не удалось включить модуль '" + name + "'.");
    }
    return true;
  }

  private boolean handleDisable(final CommandSender sender, final String[] args) {
    if (!sender.hasPermission(PERMISSION_DISABLE)) {
      sender.sendMessage(ChatColor.RED + "У вас нет прав на отключение модулей.");
      return true;
    }
    if (args.length < 2) {
      sender.sendMessage(ChatColor.RED + "Использование: /breezemodules disable <имя>");
      return true;
    }

    final String name = args[1];
    if (moduleManager.disable(name)) {
      sender.sendMessage(ChatColor.GREEN + "Модуль '" + name + "' отключён.");
    } else {
      sender.sendMessage(ChatColor.RED + "Не удалось отключить модуль '" + name + "'.");
    }
    return true;
  }

  private List<String> knownModuleNames() {
    final List<String> names = new ArrayList<>(moduleManager.getLoadedModules().keySet());
    names.addAll(moduleManager.getDisabledModules());
    return names.stream().distinct().sorted().toList();
  }

  private List<String> filter(final String input, final List<String> options) {
    final String lower = input.toLowerCase();
    return options.stream()
        .filter(option -> option.toLowerCase().startsWith(lower))
        .toList();
  }
}
