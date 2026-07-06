package ru.breezeproject.core.command;

import org.bukkit.command.CommandSender;

import ru.breezeproject.api.command.BreezeCommandSender;

public class BukkitCommandSenderAdapter implements BreezeCommandSender {
  private final CommandSender sender;

  public BukkitCommandSenderAdapter(CommandSender sender) {
    this.sender = sender;
  }

  @Override
  public String getName() {
    return sender.getName();
  }

  @Override
  public void sendMessage(String message) {
    sender.sendMessage(message);
  }

  @Override
  public boolean hasPermission(String permission) {
    return sender.hasPermission(permission);
  }
}
