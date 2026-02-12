package io.github.divinerealms.aetherball.tasks;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.matchmaking.highscore.HighScoreManager;
import org.bukkit.Bukkit;

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
    highScoreManager.startUpdate();
    long duration = System.currentTimeMillis() - startTime;

    int totalPlayers = highScoreManager.getTotalPlayerFiles();
    debugConsole("{prefix_success}Started " + getTaskName() + " update (" + totalPlayers
        + " total player files)");

    int processedCount = highScoreManager.getProcessedCount();
    int skippedCount = highScoreManager.getSkippedCount();
    debugConsole("{prefix_success}" + getTaskName() + " update completed in " + duration + "ms for "
        + processedCount + " players (skipped " + skippedCount + ")");
  }
}
