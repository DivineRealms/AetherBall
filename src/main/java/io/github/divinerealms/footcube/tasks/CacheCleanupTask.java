package io.github.divinerealms.footcube.tasks;

import static io.github.divinerealms.footcube.matchmaking.util.MatchUtils.isPlayerOnline;

import io.github.divinerealms.footcube.core.FCManager;
import org.bukkit.entity.Player;

public class CacheCleanupTask extends BaseTask {

  public CacheCleanupTask(FCManager fcManager, long interval) {
    super(fcManager, "CacheCleanup", interval, true);
  }

  @Override
  protected void kaboom() {
    int removed = 0;
    for (Player player : fcManager.getCachedPlayers()) {
      if (!isPlayerOnline(player)) {
        continue;
      }

      fcManager.getCachedPlayers().remove(player);
      removed++;
    }

    if (removed > 0) {
      logger.info("&2Cleaned up &e" + removed + " &2stale player references.");
    }
  }
}
