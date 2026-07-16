package ru.breezeproject.api.schedule;

public interface BreezeTask {
  void cancel();

  boolean isCancelled();
}
