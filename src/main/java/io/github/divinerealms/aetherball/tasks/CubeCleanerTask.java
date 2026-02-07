package io.github.divinerealms.aetherball.tasks;

import static io.github.divinerealms.aetherball.configs.Lang.CLEARED_CUBES;

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
      logger.broadcast(CLEARED_CUBES, String.valueOf(cubeCleaner.getAmount()));
    }
  }

  @Override
  public void start() {
    if (cubeCleaner.noPracticeAreasSet()) {
      logger.info("&e! &dCubeCleaner &etask not started - no practice areas configured!");
      return;
    }

    super.start();
  }
}
