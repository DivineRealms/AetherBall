package io.github.divinerealms.aetherball.listeners;

import static io.github.divinerealms.aetherball.physics.PhysicsConstants.CUBE_JUMP_RIGHT_CLICK;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_HIT_DEBUG;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import io.github.divinerealms.aetherball.utils.Logger;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.util.Vector;

public class CubeTapListener implements Listener {

  private final Manager manager;
  private final Logger logger;
  private final PhysicsData data;
  private final PhysicsSystem system;

  public CubeTapListener(Manager manager) {
    this.manager = manager;
    this.logger = manager.getLogger();
    this.data = manager.getPhysicsData();
    this.system = manager.getPhysicsSystem();
  }

  /**
   * Handles right-click interactions with cube entities.
   * <p>When a player right-clicks a tracked {@link Slime}, applies a vertical boost
   * and triggers sound effects to simulate a lighter form of interaction than a kick.</p>
   *
   * @param event the {@link PlayerInteractEntityEvent} fired when a player interacts with an
   *              entity
   */
  @EventHandler
  public void rightClick(PlayerInteractEntityEvent event) {
    long start = System.nanoTime();
    try {
      if (!(event.getRightClicked() instanceof Slime)) {
        return;
      }

      if (!data.getCubes().contains((Slime) event.getRightClicked())) {
        return;
      }

      Slime cube = (Slime) event.getRightClicked();
      Player player = event.getPlayer();
      UUID cubeId = cube.getUniqueId();

      // Prevent AFK or unauthorized players from interacting.
      if (system.notAllowedToInteract(player)) {
        return;
      }

      // Enforce per-cube cooldown.
      Long lastRiseTime = data.getRaised().get(cubeId);
      if (lastRiseTime != null) {
        long elapsed = System.currentTimeMillis() - lastRiseTime;
        if (elapsed < Settings.KICK_COOLDOWN_RISE.asLong()) {
          return; // Cube still on cooldown.
        }
      }

      // Calculate and apply vertical boost.
      Vector previousVelocity = cube.getVelocity().clone();
      double newY = Math.max(previousVelocity.getY(), CUBE_JUMP_RIGHT_CLICK);
      cube.setVelocity(previousVelocity.setY(newY));

      // Mark cube as raised.
      data.getRaised().put(cube.getUniqueId(), System.currentTimeMillis());

      // Record interaction.
      system.recordPlayerAction(player);
      manager.getMatchManager().kick(player);

      // Play feedback sound.
      cube.getWorld().playSound(cube.getLocation(), Sound.SLIME_WALK, 0.5F, 1.0F);
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          logger.send(PERM_HIT_DEBUG, "{prefix-admin}&bCubeTapListener &ftook &e" + ms + "ms");
        }
      }
    }
  }
}