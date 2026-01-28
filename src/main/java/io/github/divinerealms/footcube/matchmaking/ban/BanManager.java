package io.github.divinerealms.footcube.matchmaking.ban;

import static io.github.divinerealms.footcube.configs.Lang.BAN_REMAINING;

import io.github.divinerealms.footcube.configs.PlayerData;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.managers.Utilities;
import io.github.divinerealms.footcube.utils.Logger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.bukkit.entity.Player;

public class BanManager {

  private final FCManager fcManager;
  private final Logger logger;
  @Getter
  private final Map<UUID, Long> bannedPlayers = new ConcurrentHashMap<>();

  public BanManager(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
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

    PlayerData data = fcManager.getDataManager().get(player);
    bannedPlayers.remove(player.getUniqueId());
    data.set("ban", null);
    fcManager.getDataManager().savePlayerData(player.getName());
    return false;
  }

  public void banPlayer(Player player, long durationMillis) {
    PlayerData data = fcManager.getDataManager().get(player);
    long banUntil = System.currentTimeMillis() + durationMillis;
    bannedPlayers.put(player.getUniqueId(), banUntil);
    data.set("ban", banUntil);
    fcManager.getDataManager().savePlayerData(player.getName());
  }

  public void unbanPlayer(Player player) {
    bannedPlayers.remove(player.getUniqueId());
  }
}