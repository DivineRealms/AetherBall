package io.github.divinerealms.aetherball.listeners;

import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Prevents damage to physics-enabled cube entities.
 * <p>
 * This listener extends {@link BaseListener} to automatically gain performance monitoring
 * capabilities. All event handlers are wrapped with timing logic that reports slow executions when
 * debug mode is enabled.
 * </p>
 */
public class CubeDamageListener extends BaseListener {

  private final PhysicsData physicsData;

  public CubeDamageListener(Manager manager) {
    this.physicsData = manager.getPhysicsData();
  }

  /**
   * Cancels any damage event involving tracked cube entities.
   * <p>
   * This ensures that physics-enabled {@link Slime} instances are not damaged by players or
   * environmental sources, preserving gameplay integrity.
   * </p>
   *
   * @param event the {@link EntityDamageEvent} fired when any entity takes damage
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void disableDamage(EntityDamageEvent event) {
    monitoredExecution(() -> {
      // Cancel all damage applied to physics cubes.
      if (event.getEntity() instanceof Slime cube
          && physicsData.getCubes().contains(cube)) {
        event.setCancelled(true);
      }
    });
  }
}
