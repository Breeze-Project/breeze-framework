package ru.breezeproject.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.breezeproject.api.module.BreezeModule;
import ru.breezeproject.core.loader.ModuleLoader;

public class ModulesCommand implements CommandExecutor {

    public static final String PERMISSION_LIST = "breeze.modules.list";
    public static final String PERMISSION_RELOAD = "breeze.modules.reload";

    private final ModuleLoader moduleLoader;

    public ModulesCommand(ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender, args);
        }
        return listModules(sender);
    }

    private boolean listModules(CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_LIST)) {
            sender.sendMessage(ChatColor.RED + "У тебя нету прав сделать это.");
            return true;
        }

        if (moduleLoader.getLoadedModules().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Нету загруженных модулей.");
            return true;
        }

        sender.sendMessage(ChatColor.AQUA + "Загруженные BreezeCore модули:");
        for (BreezeModule module : moduleLoader.getLoadedModules().values()) {
            sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + module.getName());
        }
        if (sender.hasPermission(PERMISSION_RELOAD)) {
            sender.sendMessage(ChatColor.DARK_GRAY + "Используй /breezemodules reload <name> чтобы перезагрузить модуль.");
        }
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_RELOAD)) {
            sender.sendMessage(ChatColor.RED + "У тебя нету прав на это.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Используй: /breezemodules reload <name>");
            return true;
        }

        String name = args[1];
        boolean success = moduleLoader.reload(name);

        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Перезагрузка модуля '" + name + "'.");
        } else {
            sender.sendMessage(ChatColor.RED + "Нет загруженного модуля с именем '" + name
                    + "' (Имена чувствительны к регистру; проверьте /breezemodules).");
        }
        return true;
    }
}
