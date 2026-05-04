package io.github.divinerealms.aetherball.listeners;

import io.github.divinerealms.aetherball.configs.Settings;
import org.bukkit.event.Listener;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_HIT_DEBUG;

/**
 * Base class for all event listeners that provides automatic performance monitoring.
 *
 * <p>Subclasses can use the {@link #monitoredExecution(String, Runnable)} method to wrap their
 * event handler logic with performance timing. If debug mode is enabled and the execution time
 * exceeds the configured threshold, a debug message will be broadcast to players with the
 * appropriate permission.
 *
 * <p>This design centralizes the performance monitoring logic that would otherwise need to be
 * duplicated across every event handler, making the codebase more maintainable and ensuring
 * consistent performance tracking across all listeners.
 */
public abstract class BaseListener implements Listener {

  /**
   * Executes the given logic while monitoring its performance.
   *
   * <p>This method wraps the provided runnable in a try-finally block that measures execution time.
   * If debug mode is enabled in the settings and the execution time exceeds the configured
   * threshold, a debug message is sent to all players with the debug permission.
   *
   * <p>The timing is performed using {@link System#nanoTime()} for high precision, and the result
   * is converted to milliseconds for human-readable output.
   *
   * @param listenerName the name of the listener class for debug message identification
   * @param logic the actual event handling logic to execute
   */
  protected void monitoredExecution(String listenerName, Runnable logic) {
    long start = System.nanoTime();
    try {
      // Execute the actual event handler logic
      logic.run();
    } finally {
      // Only perform the performance check if debug mode is enabled
      // This avoids unnecessary calculations in production environments
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;

        // Only broadcast if the execution exceeded the configured threshold
        // This prevents spam from fast-executing handlers
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          sendMessage(PERM_HIT_DEBUG, "{prefix_debug}&d" + listenerName + " &ftook &e" + ms + "ms");
        }
      }
    }
  }

  /**
   * Convenience overload that automatically determines the listener name from the class.
   *
   * @param logic the actual event handling logic to execute
   */
  protected void monitoredExecution(Runnable logic) {
    monitoredExecution(this.getClass().getSimpleName(), logic);
  }
}
