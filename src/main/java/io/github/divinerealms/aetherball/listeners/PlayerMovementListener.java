package io.github.divinerealms.aetherball.listeners;

import static io.github.divinerealms.aetherball.physics.PhysicsConstants.PLAYER_HEIGHT_SCALE;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_HIT_DEBUG;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import io.github.divinerealms.aetherball.utils.Logger;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMovementListener implements Listener {

  private final PhysicsData data;
  private final PhysicsSystem system;
  private final Logger logger;

  public PlayerMovementListener(Manager manager) {
    this.data = manager.getPhysicsData();
    this.system = manager.getPhysicsSystem();
    this.logger = manager.getLogger();
  }

  /**
   * Updates player speed tracking within the physics system based on movement events.
   * <p>Called whenever a player moves, this method calculates instantaneous player
   * velocity and records it for use in subsequent physics calculations (e.g. impact power).</p>
   *
   * @param event the {@link PlayerMoveEvent} triggered on any player movement
   */
  @EventHandler
  public void playerMove(PlayerMoveEvent event) {
    long start = System.nanoTime();
    try {
      Location to = event.getTo(), from = event.getFrom();
      // Skip if no movement occurred.
      if (to.getX() == from.getX() && to.getY() == from.getY() && to.getZ() == from.getZ()) {
        return;
      }

      Player player = event.getPlayer();
      if (system.notAllowedToInteract(player)) {
        return;
      }

      UUID playerId = player.getUniqueId();

      // Compute normalized velocity components.
      double dx = Math.abs(to.getX() - from.getX());
      double dy = Math.abs(to.getY() - from.getY());
      double dz = Math.abs(to.getZ() - from.getZ());
      double scaledDy = dy / PLAYER_HEIGHT_SCALE;
      double speed = Math.sqrt(dx * dx + scaledDy * scaledDy + dz * dz);

      // Record recent motion and mark player as active.
      system.recordPlayerAction(player);
      data.getSpeed().put(playerId, speed);
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          logger.send(PERM_HIT_DEBUG,
              "{prefix-admin}&dPlayerMovementListener &ftook &e" + ms + "ms");
        }
      }
    }
  }
}
