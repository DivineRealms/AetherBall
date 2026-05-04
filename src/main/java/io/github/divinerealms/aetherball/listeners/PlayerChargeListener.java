package io.github.divinerealms.aetherball.listeners;

import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/**
 * Manages charged kick mechanics through player sneaking.
 *
 * <p>Tracks sneak state to initialize and reset charge buildup, allowing players to charge more
 * powerful kicks by holding sneak before attacking cubes.
 */
public class PlayerChargeListener extends BaseListener {

  private final PhysicsData data;
  private final PhysicsSystem system;

  public PlayerChargeListener(Manager manager) {
    this.data = manager.getPhysicsData();
    this.system = manager.getPhysicsSystem();
  }

  /**
   * Manages charge state when players toggle sneaking.
   *
   * <p>Starting to sneak initializes a charge value that builds over time for more powerful kicks.
   * Releasing sneak resets the experience bar and removes the charge state.
   *
   * @param event the {@link PlayerToggleSneakEvent} triggered when sneaking state changes
   */
  @EventHandler
  public void playerChargeCalculator(PlayerToggleSneakEvent event) {
    monitoredExecution(
        () -> {
          Player player = event.getPlayer();

          // Prevent AFK or unauthorized players from charging.
          if (system.notAllowedToInteract(player)) {
            return;
          }

          UUID playerId = player.getUniqueId();

          if (event.isSneaking()) {
            // Initialize charge state when sneaking begins.
            data.getCharges().put(playerId, 0D);
            system.recordPlayerAction(player);
          } else {
            // Clear experience bar and remove charge state when sneak is released.
            player.setExp(0);
            data.getCharges().remove(playerId);
          }
        });
  }
}
