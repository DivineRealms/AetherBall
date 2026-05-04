package io.github.divinerealms.aetherball.matchmaking;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.managers.Utilities;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.github.divinerealms.aetherball.configs.Lang.BAN_REMAINING;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;

public class BanManager {

  private final PlayerDataManager dataManager;

  @Getter private final Map<UUID, Long> bannedPlayers = new ConcurrentHashMap<>();

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
      return true;
    }

    PlayerData data = dataManager.get(player);
    bannedPlayers.remove(player.getUniqueId());
    data.set("ban", null);
    dataManager.savePlayerData(player.getName());
    return false;
  }

  public long getRemainingMillis(Player player) {
    if (!bannedPlayers.containsKey(player.getUniqueId())) {
      return 0L;
    }
    return Math.max(0L, bannedPlayers.get(player.getUniqueId()) - System.currentTimeMillis());
  }

  public boolean checkAndNotify(Player player) {
    if (!isBanned(player)) {
      return false;
    }
    long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(getRemainingMillis(player));
    sendMessage(player, BAN_REMAINING, player.getDisplayName(), Utilities.formatTime(secondsLeft));
    return true;
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
