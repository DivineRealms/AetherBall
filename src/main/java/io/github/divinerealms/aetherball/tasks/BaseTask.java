package io.github.divinerealms.aetherball.tasks;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_ADMIN;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;

import java.util.ArrayDeque;
import java.util.Queue;

import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Base class for all scheduled tasks with built-in performance monitoring.
 * <p>
 * Tracks execution times and provides automatic debug warnings when tasks exceed their threshold.
 * Maintains a rolling window of the last 20 execution times for performance analysis.
 * </p>
 */
public abstract class BaseTask implements Runnable {

  protected final Manager manager;
  protected final Plugin plugin;
  @Getter
  private final String taskName;
  private final long interval;
  private final boolean async;
  private final Queue<Long> recentExecutionTimes = new ArrayDeque<>(20);
  private final long debugThreshold;
  private BukkitTask task;
  @Getter
  private boolean running = false;
  @Getter
  private long totalExecutions = 0;
  private long recentExecutionTimeSum = 0;

  protected BaseTask(Manager manager, String taskName, long interval, boolean async) {
    this(manager, taskName, interval, async, getDefaultThreshold(interval));
  }

  protected BaseTask(Manager manager, String taskName, long interval, boolean async,
                     long customThreshold) {
    this.manager = manager;
    this.plugin = manager.getPlugin();
    this.taskName = taskName;
    this.interval = interval;
    this.async = async;
    this.debugThreshold = customThreshold;
  }

  private static long getDefaultThreshold(long interval) {
    if (interval <= 2) {
      return 5;
    }

    if (interval <= 40) {
      return 20;
    }

    return 50;
  }

  public void start() {
    if (running) {
      debugConsole("{prefix_warn}" + taskName + " task is already running");
      return;
    }

    if (async) {
      task = plugin.getServer().getScheduler()
          .runTaskTimerAsynchronously(plugin, this, interval, interval);
    } else {
      task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    running = true;
    debugConsole("{prefix_success}Started " + taskName + " task (type: " + (async ? "a" : "")
        + "sync, frequency: " + interval + " ticks)");
  }

  public void stop() {
    if (!running) {
      return;
    }

    if (task != null) {
      task.cancel();
      task = null;
    }

    running = false;
  }

  @Override
  public final void run() {
    long start = System.nanoTime();
    try {
      kaboom();
    } catch (Exception exception) {
      logConsole("{prefix_error}Error in " + taskName + " task", exception.getMessage());
    } finally {
      long durationNanos = System.nanoTime() - start;
      recordExecution(durationNanos);
      if (Settings.DEBUG_MODE.asBoolean()) {
        long durationMillis = durationNanos / 1_000_000;
        if (durationMillis > debugThreshold) {
          sendMessage(PERM_ADMIN,
              "{prefix_debug}&d" + taskName + " &ftook &e" + durationMillis + "ms &f(threshold: "
                  + debugThreshold
                  + "ms)");
        }
      }
    }
  }

  protected abstract void kaboom();

  /**
   * Records a task execution and maintains a rolling window of the last 20 execution times.
   *
   * @param duration the execution duration in nanoseconds
   */
  private void recordExecution(long duration) {
    totalExecutions++;

    // Add new duration to the rolling sum.
    recentExecutionTimeSum += duration;
    recentExecutionTimes.offer(duration);

    // Keep only the last 20 execution times
    if (recentExecutionTimes.size() > 20) {
      long removed = recentExecutionTimes.poll();
      recentExecutionTimeSum -= removed;
    }
  }

  /**
   * Gets the average execution time in milliseconds based on the last 20 runs.
   *
   * @return average execution time in milliseconds, or 0.0 if no executions recorded
   */
  public double getAverageExecutionTime() {
    if (recentExecutionTimes.isEmpty()) {
      return 0.0;
    }

    return recentExecutionTimeSum / (double) recentExecutionTimes.size() / 1_000_000.0;
  }

  public void resetStats() {
    totalExecutions = 0;
    recentExecutionTimes.clear();
    recentExecutionTimeSum = 0;
  }
}
