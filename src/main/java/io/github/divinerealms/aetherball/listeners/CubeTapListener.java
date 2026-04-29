package io.github.divinerealms.aetherball.listeners;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.util.Vector;

import java.util.UUID;

import static io.github.divinerealms.aetherball.physics.PhysicsConstants.CUBE_JUMP_RIGHT_CLICK;

/**
 * Handles right-click interactions with cube entities.
 * <p>
 * Provides a lighter form of cube interaction compared to kicks, applying vertical boosts with
 * cooldown management to prevent spam and maintain gameplay balance.
 * </p>
 */
public class CubeTapListener extends BaseListener {

  private final PhysicsData data;
  private final PhysicsSystem system;
  private final MatchManager matchManager;

  public CubeTapListener(Manager manager) {
    this.data = manager.getPhysicsData();
    this.system = manager.getPhysicsSystem();
    this.matchManager = manager.getMatchManager();
  }

  /**
   * Handles right-click interactions with cube entities.
   * <p>
   * Applies a vertical boost to tracked cubes when right-clicked, with per-cube cooldown tracking
   * to prevent spam. This provides a softer interaction method compared to kicks.
   * </p>
   *
   * @param event the {@link PlayerInteractEntityEvent} fired when a player interacts with an
   *              entity
   */
  @EventHandler
  public void rightClick(PlayerInteractEntityEvent event) {
    monitoredExecution(() -> {
      // Only process Slime entities (cubes).
      if (!(event.getRightClicked() instanceof Slime cube)) {
        return;
      }

      // Only process tracked physics cubes.
      if (!data.getCubes().contains(cube)) {
        return;
      }

      Player player = event.getPlayer();
      UUID cubeId = cube.getUniqueId();

      // Prevent AFK or unauthorized players from interacting.
      if (system.notAllowedToInteract(player)) {
        return;
      }

      // Enforce per-cube cooldown to prevent spam.
      Long lastRiseTime = data.getRaised().get(cubeId);
      if (lastRiseTime != null) {
        long elapsed = System.currentTimeMillis() - lastRiseTime;
        if (elapsed < Settings.KICK_COOLDOWN_RISE.asLong()) {
          return; // Cube still on cooldown.
        }
      }

      // Apply vertical boost while preserving horizontal velocity.
      Vector previousVelocity = cube.getVelocity().clone();
      double newY = Math.max(previousVelocity.getY(), CUBE_JUMP_RIGHT_CLICK);
      cube.setVelocity(previousVelocity.setY(newY));

      // Update cooldown timestamp for this cube.
      data.getRaised().put(cube.getUniqueId(), System.currentTimeMillis());

      // Record player action and notify match manager.
      system.recordPlayerAction(player);
      matchManager.kick(player);

      // Play feedback sound at cube location.
      cube.getWorld().playSound(cube.getLocation(), Sound.SLIME_WALK, 0.5F, 1.0F);
    });
  }
}