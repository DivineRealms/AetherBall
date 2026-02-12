package io.github.divinerealms.aetherball.tasks;

import static io.github.divinerealms.aetherball.configs.Lang.CLEARED_CUBES;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.broadcast;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.utils.CubeCleaner;

/**
 * Task that periodically cleans up cubes in practice areas. Runs at an interval configured by the
 * CubeCleaner.
 */
public class CubeCleanerTask extends BaseTask {

  private final CubeCleaner cubeCleaner;

  public CubeCleanerTask(Manager manager, long interval) {
    super(manager, "CubeCleaner", interval, false);
    this.cubeCleaner = manager.getCubeCleaner();
  }

  @Override
  protected void kaboom() {
    cubeCleaner.clearCubes();
    if (!cubeCleaner.isEmpty()) {
      String amount = String.valueOf(cubeCleaner.getAmount());
      broadcast(CLEARED_CUBES, amount);
    }
  }

  @Override
  public void start() {
    if (cubeCleaner.noPracticeAreasSet()) {
      logConsole("{prefix_error}CubeCleaner task not started - no practice areas configured!");
      return;
    }

    super.start();
  }
}
