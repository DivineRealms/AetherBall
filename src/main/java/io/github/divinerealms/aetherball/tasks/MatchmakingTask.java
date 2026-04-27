package io.github.divinerealms.aetherball.tasks;

import static io.github.divinerealms.aetherball.physics.PhysicsConstants.MATCH_TASK_INTERVAL_TICKS;

import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;

/**
 * Task that handles match updates and matchmaking logic. Runs synchronously at configured interval
 * (default: 1 tick).
 */
public class MatchmakingTask extends BaseTask {

  private final MatchManager matchManager;

  public MatchmakingTask(Manager manager) {
    super(manager, "Matchmaking", MATCH_TASK_INTERVAL_TICKS, false);
    this.matchManager = manager.getMatchManager();
  }

  @Override
  protected void kaboom() {
    matchManager.update();
  }
}
