package io.github.divinerealms.aetherball.listeners;

import static io.github.divinerealms.aetherball.utils.Permissions.PERM_HIT_DEBUG;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.utils.Logger;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class CubeDamageListener implements Listener {

  private final PhysicsData physicsData;
  private final Logger logger;

  public CubeDamageListener(Manager manager) {
    this.physicsData = manager.getPhysicsData();
    this.logger = manager.getLogger();
  }

  /**
   * Cancels any damage event involving tracked cube entities.
   * <p>This ensures that physics-enabled {@link Slime} instances are not damaged
   * by players or environmental sources, preserving gameplay integrity.</p>
   *
   * @param event the {@link EntityDamageEvent} fired when any entity takes damage
   */
  @EventHandler
  public void disableDamage(EntityDamageEvent event) {
    long start = System.nanoTime();
    try {
      // Cancel all damage applied to physics cubes.
      if (event.getEntity() instanceof Slime && physicsData.getCubes()
          .contains((Slime) event.getEntity())) {
        event.setCancelled(true);
      }
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          logger.send(PERM_HIT_DEBUG, "{prefix-admin}&dCubeDamageListener &ftook &e" + ms + "ms");
        }
      }
    }
  }
}
