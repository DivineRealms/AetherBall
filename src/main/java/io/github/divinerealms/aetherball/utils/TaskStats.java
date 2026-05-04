package io.github.divinerealms.aetherball.utils;

/** Data class to hold average execution times for various tasks. */
public record TaskStats(
    double physicsAvgMs,
    double touchCleanupAvgMs,
    double playerUpdateAvgMs,
    double particleTrailAvgMs,
    double matchmakingAvgMs,
    double queueStatusAvgMs,
    double cubeCleanerAvgMs,
    double cacheCleanupAvgMs,
    double highScoresAvgMs) {

  public double getTotalAverageMs() {
    return physicsAvgMs
        + touchCleanupAvgMs
        + playerUpdateAvgMs
        + particleTrailAvgMs
        + matchmakingAvgMs
        + queueStatusAvgMs
        + cubeCleanerAvgMs
        + cacheCleanupAvgMs
        + highScoresAvgMs;
  }

  public double getAveragePerTask() {
    return getTotalAverageMs() / 9;
  }
}
