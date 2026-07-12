package ru.breezeproject.api.command;

public interface BreezeCommandSender {
  String getName();

  void sendMessage(String message);

  boolean hasPermission(String permission);

  Object getHandle();
}
