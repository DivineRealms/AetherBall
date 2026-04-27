package io.github.divinerealms.aetherball.utils;

/**
 * Data class to hold average execution times for various tasks.
 */
public record TaskStats(double physicsAvgMs, double touchCleanupAvgMs, double playerUpdateAvgMs,
                        double particleTrailAvgMs, double matchmakingAvgMs, double queueStatusAvgMs) {

  public double getTotalAverageMs() {
    return physicsAvgMs + touchCleanupAvgMs + playerUpdateAvgMs + particleTrailAvgMs
        + matchmakingAvgMs + queueStatusAvgMs;
  }

  public double getAveragePerTask() {
    return getTotalAverageMs() / 8;
  }
}
