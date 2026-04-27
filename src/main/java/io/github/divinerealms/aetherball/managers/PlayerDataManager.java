package io.github.divinerealms.aetherball.managers;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.configs.Settings;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerDataManager {

  private final Manager manager;
  private final Plugin plugin;
  private final ConfigManager configManager;
  private final Map<String, PlayerData> playerCache = new ConcurrentHashMap<>();

  private final FileConfiguration uuidConfig;
  private final Map<String, String> uuidCache = new ConcurrentHashMap<>();
  private final Queue<String> dataQueue = new ConcurrentLinkedQueue<>();
  private final Set<String> dataQueueSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private volatile boolean uuidsChanged = false;
  private volatile boolean saveScheduled = false;

  public PlayerDataManager(Manager manager) {
    this.manager = manager;
    this.plugin = manager.getPlugin();
    this.configManager = manager.getConfigManager();
    configManager.createNewFile("player_uuids.yml", "Cache of player UUIDs");
    this.uuidConfig = configManager.getConfig("player_uuids.yml");

    for (String key : uuidConfig.getKeys(false)) {
      uuidCache.put(key, uuidConfig.getString(key));
    }
  }

  public PlayerData get(Player player) {
    uuidCache.computeIfAbsent(player.getName(), name -> {
      String uuid = player.getUniqueId().toString();
      uuidConfig.set(name, uuid);
      uuidsChanged = true;
      queueAdd(name);
      return uuid;
    });

    return playerCache.computeIfAbsent(player.getName(),
        name -> new PlayerData(name, configManager, this));
  }

  public PlayerData get(String playerName) {
    String uuid = uuidCache.get(playerName);
    if (uuid == null) {
      return null;
    }

    return playerCache.computeIfAbsent(playerName,
        name -> new PlayerData(name, configManager, this));
  }

  public UUID getUUID(String playerName) {
    String uuidString = uuidCache.get(playerName);
    if (uuidString == null) {
      return null;
    }

    return UUID.fromString(uuidString);
  }

  public void queueAdd(String playerName) {
    if (dataQueueSet.add(playerName)) {
      dataQueue.add(playerName);
      scheduleSave();
    }
  }

  public void unload(Player player) {
    queueAdd(player.getName());
    playerCache.remove(player.getName());
  }

  public void addDefaults(PlayerData playerData) {
    if (!playerData.has("wins")) {
      playerData.set("wins", 0);
    }

    if (!playerData.has("matches")) {
      playerData.set("matches", 0);
    }

    if (!playerData.has("losses")) {
      playerData.set("losses", 0);
    }

    if (!playerData.has("ties")) {
      playerData.set("ties", 0);
    }

    if (!playerData.has("goals")) {
      playerData.set("goals", 0);
    }

    if (!playerData.has("assists")) {
      playerData.set("assists", 0);
    }

    if (!playerData.has("owngoals")) {
      playerData.set("owngoals", 0);
    }

    if (!playerData.has("winstreak")) {
      playerData.set("winstreak", 0);
    }

    if (!playerData.has("bestwinstreak")) {
      playerData.set("bestwinstreak", 0);
    }
  }

  public void clearAllStats() {
    for (String playerName : uuidCache.keySet()) {
      PlayerData data = get(playerName);
      if (data == null) {
        continue;
      }

      data.set("wins", 0);
      data.set("matches", 0);
      data.set("losses", 0);
      data.set("ties", 0);
      data.set("goals", 0);
      data.set("assists", 0);
      data.set("owngoals", 0);
      data.set("winstreak", 0);
      data.set("bestwinstreak", 0);
    }
    saveAll();
  }

  public void saveQueue() {
    if (dataQueue.isEmpty() && !uuidsChanged) {
      return;
    }

    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
      int processed = 0;
      int totalSaved = 0;

      while (processed < Settings.PLAYER_DATA_BATCH_SIZE.asInt()) {
        String playerName = dataQueue.poll();
        if (playerName == null) {
          break;
        }

        try {
          savePlayerData(playerName);
          totalSaved++;
        } catch (Exception exception) {
          logConsole("{prefix_error}Failed to save player data for " + playerName,
              exception.getMessage());
        }
        processed++;
      }

      debugConsole("{prefix_success}Auto saved " + totalSaved + " player data file(s) this batch.");

      if (uuidsChanged) {
        uuidsChanged = false;
        try {
          configManager.saveConfig("player_uuids.yml");
          debugConsole("{prefix_success}Saved updated player UUIDs.");
        } catch (Exception exception) {
          logConsole("{prefix_error}Failed to save UUID config", exception.getMessage());
        }
      }

      if (!dataQueue.isEmpty()) {
        debugConsole("{prefix_info}" + dataQueue.size()
            + " player data file(s) remaining in queue, scheduling next batch...");
        scheduleSave();
      }
    });
  }

  public void saveAll() {
    playerCache.values().forEach(PlayerData::save);
    dataQueue.clear();

    if (uuidsChanged) {
      try {
        configManager.saveConfig("player_uuids.yml");
        uuidsChanged = false;
        debugConsole("{prefix_success}Saved all player UUIDs.");
      } catch (Exception exception) {
        logConsole("{prefix_error}Failed to save UUID config.", exception.getMessage());
      }
    }

    debugConsole("{prefix_success}Saved all player data.");
  }

  public void savePlayerData(String playerName) {
    PlayerData data = playerCache.get(playerName);
    if (data != null) {
      data.save();
    }

    dataQueueSet.remove(playerName);
  }

  private void scheduleSave() {
    if (manager.isDisabling()) {
      return;
    }

    if (saveScheduled) {
      return;
    }

    saveScheduled = true;

    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
      saveQueue();
      saveScheduled = false;
    }, Settings.getAutoSaveInterval());
  }
}