package ru.breezeproject.core.schedule;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import ru.breezeproject.api.schedule.BreezeTask;

final class FoliaBreezeTask implements BreezeTask {
  private final ScheduledTask delegate;

  FoliaBreezeTask(final ScheduledTask delegate) {
    this.delegate = delegate;
  }

  @Override
  public void cancel() {
    delegate.cancel();
  }

  @Override
  public boolean isCancelled() {
    return delegate.isCancelled();
  }
}
