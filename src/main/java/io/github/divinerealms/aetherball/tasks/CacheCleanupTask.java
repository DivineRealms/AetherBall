package io.github.divinerealms.aetherball.tasks;

import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.isPlayerOnline;

import io.github.divinerealms.aetherball.core.Manager;
import org.bukkit.entity.Player;

public class CacheCleanupTask extends BaseTask {

  public CacheCleanupTask(Manager manager, long interval) {
    super(manager, "CacheCleanup", interval, true);
  }

  @Override
  protected void kaboom() {
    int removed = 0;
    for (Player player : manager.getCachedPlayers()) {
      if (isPlayerOnline(player)) {
        continue;
      }

      manager.getCachedPlayers().remove(player);
      removed++;
    }

    if (removed > 0) {
      logger.info("&2Cleaned up &e" + removed + " &2stale player references.");
    }
  }
}
