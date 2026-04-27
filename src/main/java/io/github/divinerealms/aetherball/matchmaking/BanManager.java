package io.github.divinerealms.aetherball.matchmaking;

import static io.github.divinerealms.aetherball.configs.Lang.BAN_REMAINING;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.managers.Utilities;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import org.bukkit.entity.Player;

public class BanManager {

  private final PlayerDataManager dataManager;

  @Getter
  private final Map<UUID, Long> bannedPlayers = new ConcurrentHashMap<>();

  public BanManager(Manager manager) {
    this.dataManager = manager.getDataManager();
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
      sendMessage(player, BAN_REMAINING, player.getDisplayName(),
          Utilities.formatTime(secondsLeft));
      return true;
    }

    PlayerData data = dataManager.get(player);
    bannedPlayers.remove(player.getUniqueId());
    data.set("ban", null);
    dataManager.savePlayerData(player.getName());
    return false;
  }

  public void banPlayer(Player player, long durationMillis) {
    PlayerData data = dataManager.get(player);
    long banUntil = System.currentTimeMillis() + durationMillis;
    bannedPlayers.put(player.getUniqueId(), banUntil);
    data.set("ban", banUntil);
    dataManager.savePlayerData(player.getName());
  }

  public void unbanPlayer(Player player) {
    bannedPlayers.remove(player.getUniqueId());
  }
}