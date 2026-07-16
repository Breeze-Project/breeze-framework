package ru.breezeproject.core.schedule;

import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import ru.breezeproject.api.schedule.BreezeScheduler;
import ru.breezeproject.api.schedule.BreezeTask;

public final class FoliaBreezeScheduler implements BreezeScheduler {
  private final Plugin plugin;

  public FoliaBreezeScheduler(final Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void runGlobal(final Runnable task) {
    plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
  }

  @Override
  public BreezeTask runGlobalLater(final Runnable task, final long delayTicks) {
    final ScheduledTask scheduledTask = plugin.getServer().getGlobalRegionScheduler()
        .runDelayed(plugin, scheduledTask1 -> task.run(), Math.max(1L, delayTicks));
    return new FoliaBreezeTask(scheduledTask);
  }

  @Override
  public BreezeTask runGlobalTimer(final Runnable task, final long delayTicks, final long periodTicks) {
    final ScheduledTask scheduledTask = plugin.getServer().getGlobalRegionScheduler()
        .runAtFixedRate(plugin, t -> task.run(), Math.max(1L, delayTicks), Math.max(1L, periodTicks));
    return new FoliaBreezeTask(scheduledTask);
  }

  @Override
  public void runAsync(final Runnable task) {
    plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
  }

  @Override
  public BreezeTask runAsyncLater(final Runnable task, final long delayTicks) {
    final ScheduledTask scheduledTask = plugin.getServer().getAsyncScheduler().runDelayed(
        plugin, scheduledTask1 -> task.run(), ticksToMillis(Math.max(1L, delayTicks)), TimeUnit.MILLISECONDS);
    return new FoliaBreezeTask(scheduledTask);
  }

  @Override
  public void runAtEntity(final Entity entity, final Runnable task) {
    entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
  }

  @Override
  public BreezeTask runAtEntityLater(final Entity entity, final Runnable task, final long delayTicks) {
    final ScheduledTask scheduledTask = entity.getScheduler()
        .runDelayed(plugin, scheduledTask1 -> task.run(), null, Math.max(1L, delayTicks));
    return new FoliaBreezeTask(scheduledTask);
  }

  @Override
  public BreezeTask runAtEntityTimer(final Entity entity, final Runnable task, final long delayTicks,
      final long periodTicks) {
    final ScheduledTask scheduledTask = entity.getScheduler()
        .runAtFixedRate(plugin, t -> task.run(), null, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
    return new FoliaBreezeTask(scheduledTask);
  }

  @Override
  public void runAtLocation(final Location location, final Runnable task) {
    plugin.getServer().getRegionScheduler().run(plugin, location, scheduledTask -> task.run());
  }

  @Override
  public BreezeTask runAtLocationLater(final Location location, final Runnable task, final long delayTicks) {
    final ScheduledTask scheduledTask = plugin.getServer().getRegionScheduler()
        .runDelayed(plugin, location, scheduledTask1 -> task.run(), Math.max(1L, delayTicks));
    return new FoliaBreezeTask(scheduledTask);
  }

  @Override
  public BreezeTask runAtLocationTimer(final Location location, final Runnable task, final long delayTicks,
      final long periodTicks) {
    final ScheduledTask scheduledTask = plugin.getServer().getRegionScheduler()
        .runAtFixedRate(plugin, location, t -> task.run(), Math.max(1L, delayTicks), Math.max(1L, periodTicks));
    return new FoliaBreezeTask(scheduledTask);
  }

  private static long ticksToMillis(final long ticks) {
    return ticks * 50L;
  }
}
