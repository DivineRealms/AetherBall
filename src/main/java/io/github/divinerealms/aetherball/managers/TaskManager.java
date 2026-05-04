package io.github.divinerealms.aetherball.managers;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.tasks.*;
import io.github.divinerealms.aetherball.utils.TaskStats;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

/**
 * Centralized manager for all scheduled tasks. Handles initialization, lifecycle, and cleanup of
 * physics tasks.
 */
@Getter
public class TaskManager {

  private final List<BaseTask> tasks;

  // Physics Tasks
  private final PhysicsTask physicsTask;
  private final TouchCleanupTask touchCleanupTask;
  private final PlayerUpdateTask playerUpdateTask;
  private final ParticleTrailTask particleTrailTask;

  // General Tasks
  private final CubeCleanerTask cubeCleanerTask;
  private final MatchmakingTask matchmakingTask;
  private final CacheCleanupTask cacheCleanupTask;
  private final QueueStatusTask queueStatusTask;
  private final HighScoresTask highScoresTask;

  public TaskManager(Manager manager) {
    this.tasks = new ArrayList<>();

    // Initialize physics tasks.
    this.physicsTask = new PhysicsTask(manager);
    this.touchCleanupTask = new TouchCleanupTask(manager);
    this.playerUpdateTask = new PlayerUpdateTask(manager);
    this.particleTrailTask = new ParticleTrailTask(manager);

    // Initialize general tasks.
    this.cubeCleanerTask = new CubeCleanerTask(manager, Settings.getPracticeAreaCleanupInterval());
    this.matchmakingTask = new MatchmakingTask(manager);
    this.cacheCleanupTask = new CacheCleanupTask(manager, Settings.getCacheCleanupInterval());
    this.queueStatusTask = new QueueStatusTask(manager);
    this.highScoresTask = new HighScoresTask(manager, Settings.getHighScoreUpdateInterval());

    tasks.add(physicsTask);
    tasks.add(touchCleanupTask);
    tasks.add(playerUpdateTask);
    tasks.add(particleTrailTask);
    tasks.add(cubeCleanerTask);
    tasks.add(matchmakingTask);
    tasks.add(cacheCleanupTask);
    tasks.add(queueStatusTask);
    tasks.add(highScoresTask);
  }

  public void startAll() {
    int started = 0;
    for (BaseTask task : tasks) {
      try {
        task.start();
        started++;
      } catch (Exception exception) {
        logConsole(
            "{prefix_error}Failed to start "
                + task.getTaskName()
                + " task: "
                + exception.getMessage());
      }
    }
    debugConsole(
        "{prefix_success}Started " + started + "/" + tasks.size() + " plugin tasks successfully!");
  }

  public void stopAll() {
    int stopped = 0;
    for (int i = tasks.size() - 1; i >= 0; i--) {
      BaseTask task = tasks.get(i);
      if (task.isRunning()) {
        try {
          task.stop();
          stopped++;
        } catch (Exception exception) {
          logConsole(
              "{prefix_error}Error stopping "
                  + task.getTaskName()
                  + " task: "
                  + exception.getMessage());
        }
      }
    }
    if (stopped > 0) {
      debugConsole("{prefix_success}Stopped " + stopped + " plugin tasks.");
    }
  }

  public void restart() {
    stopAll();
    startAll();
  }

  public TaskStats getStats() {
    return new TaskStats(
        physicsTask.getAverageExecutionTime(),
        touchCleanupTask.getAverageExecutionTime(),
        playerUpdateTask.getAverageExecutionTime(),
        particleTrailTask.getAverageExecutionTime(),
        matchmakingTask.getAverageExecutionTime(),
        queueStatusTask.getAverageExecutionTime(),
        cubeCleanerTask.getAverageExecutionTime(),
        cacheCleanupTask.getAverageExecutionTime(),
        highScoresTask.getAverageExecutionTime());
  }

  public void resetAllStats() {
    for (BaseTask task : tasks) {
      task.resetStats();
    }
    debugConsole("{prefix_success}Reset statistics for all tasks.");
  }

  public int getTaskCount() {
    return tasks.size();
  }

  public int getRunningTaskCount() {
    int count = 0;

    for (BaseTask task : tasks) {
      if (task.isRunning()) {
        count++;
      }
    }

    return count;
  }
}
