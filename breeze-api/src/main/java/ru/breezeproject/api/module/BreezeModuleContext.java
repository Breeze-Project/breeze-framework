package ru.breezeproject.api.module;

import java.nio.file.Path;
import java.util.List;

import ru.breezeproject.api.command.ModuleCommandExecutor;
import ru.breezeproject.api.command.ModuleTabCompleter;
import ru.breezeproject.api.event.BreezeListener;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.service.ServiceRegistry;

public interface BreezeModuleContext {
  ServiceRegistry getServiceRegistry();

  EventBus getEventBus();

  Path getDataFolder();

  void registerCommand(String name, List<String> aliases, String description,
      String usage, ModuleCommandExecutor executor, ModuleTabCompleter tabCompleter);

  void registerListener(BreezeListener listener);
}
