package io.github.divinerealms.aetherball.tasks;

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
      logger.info("&e! &d" + getTaskName() + " &6update already in progress.");
      return;
    }

    long startTime = System.currentTimeMillis();
    highScoreManager.startUpdate();
    long duration = System.currentTimeMillis() - startTime;

    int totalPlayers = highScoreManager.getTotalPlayerFiles();
    logger.info("&a✔ &2Started &d" + getTaskName() + " &2update (&e" + totalPlayers
        + " &2total player files)");

    int processedCount = highScoreManager.getProcessedCount();
    int skippedCount = highScoreManager.getSkippedCount();
    logger.info("&a✔ &d" + getTaskName() + " &2update completed in &e" + duration + "ms &afor &e"
        + processedCount + " players (skipped " + skippedCount + ")");
  }
}
