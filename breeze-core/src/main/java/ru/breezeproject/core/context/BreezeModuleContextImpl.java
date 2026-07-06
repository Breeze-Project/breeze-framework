package ru.breezeproject.core.context;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.module.BreezeModuleContext;
import ru.breezeproject.api.service.ServiceRegistry;
import ru.breezeproject.core.command.DynamicCommandRegistrar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BreezeModuleContextImpl implements BreezeModuleContext {

    private final ServiceRegistry serviceRegistry;
    private final EventBus eventBus;
    private final File dataFolder;
    private final DynamicCommandRegistrar commandRegistrar;
    private final String moduleName;
    private final List<Command> registeredCommands = new ArrayList<>();

    public BreezeModuleContextImpl(ServiceRegistry serviceRegistry, EventBus eventBus, File dataFolder,
                                    DynamicCommandRegistrar commandRegistrar, String moduleName) {
        this.serviceRegistry = serviceRegistry;
        this.eventBus = eventBus;
        this.dataFolder = dataFolder;
        this.commandRegistrar = commandRegistrar;
        this.moduleName = moduleName;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
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
    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public void registerCommand(String name, List<String> aliases, String description,
                                 String usage, CommandExecutor executor, TabCompleter tabCompleter) {
        Command command = commandRegistrar.register(moduleName.toLowerCase(), name, aliases, description,
                usage, executor, tabCompleter);
        registeredCommands.add(command);
    }

    public void unregisterAllCommands() {
        registeredCommands.forEach(commandRegistrar::unregister);
        registeredCommands.clear();
    }
}
