package ru.breezeproject.api.event.chat;

import java.util.UUID;

import ru.breezeproject.api.event.BreezeEvent;

public final class PlayerChatMessageEvent extends BreezeEvent {
  private final UUID playerId;
  private final String playerName;
  private final String rawMessage;

  public PlayerChatMessageEvent(final UUID playerId, final String playerName, final String rawMessage) {
    super(true);
    this.playerId = playerId;
    this.playerName = playerName;
    this.rawMessage = rawMessage;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public String getPlayerName() {
    return playerName;
  }

  public String getRawMessage() {
    return rawMessage;
  }
}
