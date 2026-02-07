package io.github.divinerealms.aetherball.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data class to hold average execution times for various tasks.
 */
@Getter
@AllArgsConstructor
public class TaskStats {

  private final double physicsAvgMs, touchCleanupAvgMs, playerUpdateAvgMs, particleTrailAvgMs, matchmakingAvgMs, queueStatusAvgMs;

  public double getTotalAverageMs() {
    return physicsAvgMs + touchCleanupAvgMs + playerUpdateAvgMs + particleTrailAvgMs
        + matchmakingAvgMs + queueStatusAvgMs;
  }

  public double getAveragePerTask() {
    return getTotalAverageMs() / 8;
  }
}
