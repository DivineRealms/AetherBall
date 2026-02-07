package io.github.divinerealms.aetherball.listeners;

import static io.github.divinerealms.aetherball.utils.Permissions.PERM_HIT_DEBUG;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import io.github.divinerealms.aetherball.utils.Logger;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerChargeListener implements Listener {

  private final Logger logger;
  private final PhysicsData data;
  private final PhysicsSystem system;

  public PlayerChargeListener(Manager manager) {
    this.logger = manager.getLogger();
    this.data = manager.getPhysicsData();
    this.system = manager.getPhysicsSystem();
  }

  /**
   * Calculates and manages charge buildup when players toggle sneaking. When sneaking starts,
   * initializes a charge state; when sneaking stops, resets experience and removes the charge.
   *
   * @param event the {@link PlayerToggleSneakEvent} triggered when sneaking state changes
   */
  @EventHandler
  public void playerChargeCalculator(PlayerToggleSneakEvent event) {
    long start = System.nanoTime();
    try {
      Player player = event.getPlayer();
      if (system.notAllowedToInteract(player)) {
        return;
      }

      UUID playerId = player.getUniqueId();

      if (event.isSneaking()) {
        // Begin charging.
        data.getCharges().put(playerId, 0D);
        system.recordPlayerAction(player);
      } else {
        // Reset when released.
        player.setExp(0);
        data.getCharges().remove(playerId);
      }
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          logger.send(PERM_HIT_DEBUG, "{prefix-admin}&dPlayerChargeListener &ftook &e" + ms + "ms");
        }
      }
    }
  }
}