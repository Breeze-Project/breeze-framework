package ru.breezeproject.api.event;

public abstract class BreezeEvent {
  private boolean cancelled;
  private final boolean cancellable;

  protected BreezeEvent() {
    this(false);
  }

  protected BreezeEvent(final boolean cancellable) {
    this.cancellable = cancellable;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(final boolean cancelled) {
    if (!cancellable) {
      throw new UnsupportedOperationException(getClass().getName() + " is not cancellable");
    }
    this.cancelled = cancelled;
  }
}
