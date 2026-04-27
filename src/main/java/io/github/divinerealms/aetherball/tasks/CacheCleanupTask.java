package io.github.divinerealms.aetherball.tasks;

import static io.github.divinerealms.aetherball.utils.MatchUtils.isPlayerOnline;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;

import io.github.divinerealms.aetherball.managers.Manager;
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
      debugConsole("{prefix_info}Cleaned up " + removed + " stale player references.");
    }
  }
}
