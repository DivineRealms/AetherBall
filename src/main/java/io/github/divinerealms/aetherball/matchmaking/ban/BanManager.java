package io.github.divinerealms.aetherball.matchmaking.ban;

import static io.github.divinerealms.aetherball.configs.Lang.BAN_REMAINING;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.utils.Logger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.bukkit.entity.Player;

public class BanManager {

  private final Manager manager;
  private final Logger logger;
  @Getter
  private final Map<UUID, Long> bannedPlayers = new ConcurrentHashMap<>();

  public BanManager(Manager manager) {
    this.manager = manager;
    this.logger = manager.getLogger();
  }

  public boolean isBanned(Player player) {
    if (!bannedPlayers.containsKey(player.getUniqueId())) {
      return false;
    }

    long now = System.currentTimeMillis();
    long banUntil = bannedPlayers.get(player.getUniqueId());

    if (now < banUntil) {
      long millisLeft = banUntil - now;
      long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(millisLeft);
      logger.send(player, BAN_REMAINING, player.getDisplayName(),
          Utilities.formatTime(secondsLeft));
      return true;
    }

    PlayerData data = manager.getDataManager().get(player);
    bannedPlayers.remove(player.getUniqueId());
    data.set("ban", null);
    manager.getDataManager().savePlayerData(player.getName());
    return false;
  }

  public void banPlayer(Player player, long durationMillis) {
    PlayerData data = manager.getDataManager().get(player);
    long banUntil = System.currentTimeMillis() + durationMillis;
    bannedPlayers.put(player.getUniqueId(), banUntil);
    data.set("ban", banUntil);
    manager.getDataManager().savePlayerData(player.getName());
  }

  public void unbanPlayer(Player player) {
    bannedPlayers.remove(player.getUniqueId());
  }
}