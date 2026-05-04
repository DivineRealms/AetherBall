package io.github.divinerealms.aetherball.tasks;

import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.matchmaking.HighScoreManager;
import org.bukkit.Bukkit;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

public class HighScoresTask extends BaseTask {

  private final HighScoreManager highScoreManager;

  public HighScoresTask(Manager manager, long interval) {
    super(manager, "HighScores", interval, true);
    this.highScoreManager = manager.getHighscoreManager();
  }

  @Override
  protected void kaboom() {
    if (highScoreManager.isUpdating()) {
      return;
    }

    startUpdateCycle();
  }

  @Override
  public void start() {
    super.start();
    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::startUpdateCycle, 20 * 2);
  }

  public void startUpdateCycle() {
    if (highScoreManager.isUpdating()) {
      logConsole("{prefix_warn}" + getTaskName() + " update already in progress.");
      return;
    }

    long startTime = System.currentTimeMillis();
    int totalPlayers = highScoreManager.getTotalPlayerFiles();
    debugConsole(
        "{prefix_success}Started "
            + getTaskName()
            + " update ("
            + totalPlayers
            + " total player files)");

    highScoreManager.startUpdate(
        () -> {
          long duration = System.currentTimeMillis() - startTime;
          int processedCount = highScoreManager.getProcessedCount();
          int skippedCount = highScoreManager.getSkippedCount();
          debugConsole(
              "{prefix_success}"
                  + getTaskName()
                  + " update completed in "
                  + duration
                  + "ms for "
                  + processedCount
                  + " players (skipped "
                  + skippedCount
                  + ")");
        });
  }
}
