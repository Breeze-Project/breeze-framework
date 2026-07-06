package ru.breezeproject.api.module;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.service.ServiceRegistry;

import java.util.List;

public interface BreezeModuleContext {

    ServiceRegistry getServiceRegistry();

    EventBus getEventBus();

    java.io.File getDataFolder();

    void registerCommand(String name, List<String> aliases, String description,
                          String usage, CommandExecutor executor, TabCompleter tabCompleter);

    void registerListener(Listener listener);
}
