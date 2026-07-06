package ru.breezeproject.api.command;

import java.util.List;

public interface ModuleTabCompleter {
  List<String> onTabComplete(BreezeCommandSender sender, String[] args);
}
