package ru.breezeproject.api.command;

public interface ModuleCommandExecutor {
  boolean onCommand(BreezeCommandSender sender, String label, String[] args);
}
