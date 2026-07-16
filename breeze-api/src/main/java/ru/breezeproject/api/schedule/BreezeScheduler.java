package ru.breezeproject.api.schedule;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface BreezeScheduler {
  void runGlobal(Runnable task);

  BreezeTask runGlobalLater(Runnable task, long delayTicks);

  BreezeTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks);

  void runAsync(Runnable task);

  BreezeTask runAsyncLater(Runnable task, long delayTicks);

  void runAtEntity(Entity entity, Runnable task);

  BreezeTask runAtEntityLater(Entity entity, Runnable task, long delayTicks);

  BreezeTask runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks);

  void runAtLocation(Location location, Runnable task);

  BreezeTask runAtLocationLater(Location location, Runnable task, long delayTicks);

  BreezeTask runAtLocationTimer(Location location, Runnable task, long delayTicks, long periodTicks);
}
