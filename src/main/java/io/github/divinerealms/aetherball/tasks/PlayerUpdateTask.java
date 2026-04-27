package io.github.divinerealms.aetherball.tasks;

import static io.github.divinerealms.aetherball.utils.MatchUtils.isPlayerOnline;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.CHARGE_BASE_VALUE;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.CHARGE_RECOVERY_RATE;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.EXP_UPDATE_INTERVAL_TICKS;

import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.physics.PhysicsData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Handles player-specific updates like charge recovery. Can be run at a lower frequency (e.g., 2-5
 * ticks) to save CPU.
 */
public class PlayerUpdateTask extends BaseTask {

  private final PhysicsData data;

  private final Set<UUID> playersToRemove = new HashSet<>();

  public PlayerUpdateTask(Manager manager) {
    super(manager, "PlayerUpdate", EXP_UPDATE_INTERVAL_TICKS, false);
    this.data = manager.getPhysicsData();
  }

  @Override
  protected void kaboom() {
    Map<UUID, Double> charges = data.getCharges();
    if (charges.isEmpty()) {
      return;
    }

    playersToRemove.clear();
    Set<Player> onlinePlayers = manager.getCachedPlayers();

    Map<UUID, Player> onlinePlayerMap = new HashMap<>(onlinePlayers.size());
    for (Player player : onlinePlayers) {
      if (isPlayerOnline(player)) {
        onlinePlayerMap.put(player.getUniqueId(), player);
      }
    }

    for (Map.Entry<UUID, Double> entry : charges.entrySet()) {
      UUID uuid = entry.getKey();
      Player player = onlinePlayerMap.get(uuid);
      if (!isPlayerOnline(player)) {
        playersToRemove.add(uuid);
        continue;
      }

      double currentCharge = entry.getValue();
      double recoveredCharge =
          CHARGE_BASE_VALUE - (CHARGE_BASE_VALUE - currentCharge) * CHARGE_RECOVERY_RATE;
      entry.setValue(recoveredCharge);

      player.setExp((float) recoveredCharge);
    }

    if (!playersToRemove.isEmpty()) {
      for (UUID uuid : playersToRemove) {
        charges.remove(uuid);
      }
    }
  }
}