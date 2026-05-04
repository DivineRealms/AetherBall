package io.github.divinerealms.aetherball.listeners;

import static io.github.divinerealms.aetherball.physics.PhysicsConstants.PLAYER_HEIGHT_SCALE;

import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Tracks player movement for physics calculations.
 *
 * <p>Records player velocity data used to calculate kick power and impact force when interacting
 * with cubes. Filters out players in AFK or restricted states.
 */
public class PlayerMovementListener extends BaseListener {

  private final PhysicsData data;
  private final PhysicsSystem system;

  public PlayerMovementListener(Manager manager) {
    this.data = manager.getPhysicsData();
    this.system = manager.getPhysicsSystem();
  }

  /**
   * Updates player speed tracking based on movement.
   *
   * <p>Calculates instantaneous velocity from position delta and stores it for physics
   * calculations. Movement is scaled vertically to account for player height differences.
   *
   * @param event the {@link PlayerMoveEvent} triggered on any player movement
   */
  @EventHandler
  public void playerMove(PlayerMoveEvent event) {
    monitoredExecution(
        () -> {
          Location to = event.getTo(), from = event.getFrom();

          // Skip if player hasn't actually moved.
          if (to.getX() == from.getX() && to.getY() == from.getY() && to.getZ() == from.getZ()) {
            return;
          }

          Player player = event.getPlayer();

          // Don't track movement for AFK or unauthorized players.
          if (system.notAllowedToInteract(player)) {
            return;
          }

          UUID playerId = player.getUniqueId();

          // Calculate 3D movement speed with vertical scaling.
          double dx = Math.abs(to.getX() - from.getX());
          double dy = Math.abs(to.getY() - from.getY());
          double dz = Math.abs(to.getZ() - from.getZ());
          double scaledDy = dy / PLAYER_HEIGHT_SCALE;
          double speed = Math.sqrt(dx * dx + scaledDy * scaledDy + dz * dz);

          // Update tracked speed and mark player as active.
          system.recordPlayerAction(player);
          data.getSpeed().put(playerId, speed);
        });
  }
}
