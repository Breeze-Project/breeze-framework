package ru.breezeproject.core.command;

import org.bukkit.command.CommandSender;

import ru.breezeproject.api.command.BreezeCommandSender;

public class BukkitCommandSenderAdapter implements BreezeCommandSender {
  private final CommandSender sender;

  public BukkitCommandSenderAdapter(final CommandSender sender) {
    this.sender = sender;
  }

  @Override
  public String getName() {
    return sender.getName();
  }

  @Override
  public void sendMessage(final String message) {
    sender.sendMessage(message);
  }

  @Override
  public boolean hasPermission(final String permission) {
    return sender.hasPermission(permission);
  }
}
