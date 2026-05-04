package io.github.divinerealms.aetherball.managers;

import io.github.divinerealms.aetherball.AetherBall;
import io.github.divinerealms.aetherball.commands.DynamicCommandRegistry;
import io.github.divinerealms.aetherball.configs.Lang;
import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.matchmaking.*;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchData;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchSystem;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsFormulae;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import io.github.divinerealms.aetherball.utils.*;
import lombok.Getter;
import lombok.Setter;
import me.neznamy.tab.api.TabAPI;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.MatchUtils.isPlayerOnline;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_ADMIN;

@Getter
public class Manager {
  private final AetherBall plugin;
  private final ConfigManager configManager;
  private final DependencyLoader dependencyLoader;
  private final PlayerCache playerCache;
  private final CommandRegistry commandRegistry;
  private final PlayerDataManager dataManager;
  private final Utilities utilities;
  private final ArenaManager arenaManager;
  private final ScoreManager scoreboardManager;
  private final MatchData matchData;
  private final TeamManager teamManager;
  private final DuelManager duelManager;
  private final MatchSystem matchSystem;
  private final BanManager banManager;
  private final HighScoreManager highscoreManager;
  private final MatchManager matchManager;
  private final DisableCommands disableCommands;
  private final BukkitScheduler scheduler;
  private final PhysicsData physicsData;
  private final PhysicsFormulae physicsFormulae;
  private final PhysicsSystem physicsSystem;
  private final CubeCleaner cubeCleaner;
  private final ListenerManager listenerManager;
  private final TaskManager taskManager;
  private final DynamicCommandRegistry dynamicCommandRegistry;

  @Setter private boolean enabling;
  @Setter private boolean disabling;

  public Manager(AetherBall plugin) throws IllegalStateException {
    this.plugin = plugin;
    this.configManager = new ConfigManager(plugin, "");
    this.dataManager = new PlayerDataManager(this);

    setupConfigs();
    sendBanner();

    this.dependencyLoader = new DependencyLoader(plugin);
    this.utilities = new Utilities(this);
    this.playerCache = new PlayerCache(this);

    this.arenaManager = new ArenaManager(this);
    this.scoreboardManager = new ScoreManager(this);
    this.matchData = new MatchData();
    this.teamManager = new TeamManager(this);
    this.duelManager = new DuelManager(this);
    this.matchSystem = new MatchSystem(this);
    this.banManager = new BanManager(this);
    this.highscoreManager = new HighScoreManager(this);
    this.matchManager = new MatchManager(this);
    this.disableCommands = new DisableCommands(this);
    this.scheduler = plugin.getServer().getScheduler();
    this.physicsData = new PhysicsData();
    this.physicsFormulae = new PhysicsFormulae();
    this.physicsSystem = new PhysicsSystem(this);
    this.cubeCleaner = new CubeCleaner(this);
    this.listenerManager = new ListenerManager(this);
    this.taskManager = new TaskManager(this);
    this.dynamicCommandRegistry = new DynamicCommandRegistry(this);
    this.commandRegistry = new CommandRegistry(this);

    new Placeholders(this).register();
  }

  public Economy getEconomy() {
    return dependencyLoader.getEconomy();
  }

  public LuckPerms getLuckPerms() {
    return dependencyLoader.getLuckPerms();
  }

  public TabAPI getTabAPI() {
    return dependencyLoader.getTabAPI();
  }

  public Set<Player> getCachedPlayers() {
    return playerCache.getCachedPlayers();
  }

  public PlayerSettings getPlayerSettings(Player player) {
    return playerCache.getSettings(player);
  }

  public String getPrefixedName(UUID uuid) {
    return playerCache.getPrefixedName(uuid);
  }

  public void cachePrefixedName(Player player) {
    playerCache.cachePrefixedName(player);
  }

  public void preloadSettings(Player player, PlayerData playerData) {
    playerCache.preloadSettings(player, playerData);
  }

  public void reload() {
    playerCache.initialize();
    if (!enabling) {
      configManager.reloadAllConfigs();
    }

    enabling = false;
    setupConfigs();
    playerCache.reloadDefaults();
    matchManager.forceLeaveAllPlayers();
    matchSystem.initializeMatchTypes();
    physicsSystem.removeCubes();
    arenaManager.reloadArenas();
    commandRegistry.register();
    highscoreManager.initializeArrays();
    listenerManager.unregisterAll();
    taskManager.restart();
    listenerManager.registerAll();

    reloadOnlinePlayers();
  }

  private void reloadOnlinePlayers() {
    List<UUID> uuids =
        playerCache.getCachedPlayers().stream()
            .filter(MatchUtils::isPlayerOnline)
            .map(Player::getUniqueId)
            .toList();

    scheduler.runTaskAsynchronously(
        plugin,
        () ->
            uuids.forEach(
                uuid -> {
                  Player player = plugin.getServer().getPlayer(uuid);
                  if (!isPlayerOnline(player)) {
                    return;
                  }

                  PlayerData playerData = dataManager.get(player);
                  if (playerData != null) {
                    playerCache.preloadSettings(player, playerData);
                  }
                }));
  }

  public void reloadTabAPI() {
    scheduler.runTask(
        plugin,
        () -> {
          dependencyLoader.reloadTab();
          if (scoreboardManager != null) {
            scoreboardManager.refreshTabAPI();
          }
          if (matchManager != null) {
            matchManager.recreateScoreboards();
          }
        });
  }

  public void saveAll() {
    configManager.saveAll();
    dataManager.saveAll();
  }

  public void cleanup() {
    long start = System.nanoTime();
    try {
      dynamicCommandRegistry.unregisterAllMatchTypeCommands();
      commandRegistry.unregister();
      listenerManager.unregisterAll();
      physicsData.cleanup();
      matchData.cleanup();
      playerCache.clear();
    } catch (Exception exception) {
      logConsole("{prefix_error}Error in cleanup", exception.getMessage());
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          sendMessage(
              PERM_ADMIN,
              "{prefix_debug}&dCleanup &ftook &e"
                  + ms
                  + "ms &f(threshold: "
                  + Settings.DEBUG_THRESHOLD.asLong()
                  + "ms)");
        }
      }
    }
  }

  public void unloadPlayer(Player player) {
    dataManager.unload(player);
    physicsSystem.removePlayer(player);
    getCachedPlayers().remove(player);
    playerCache.removePlayer(player.getUniqueId());
    duelManager.cleanupPlayerRequests(player);
    teamManager.handlePlayerDisconnect(player);
    matchManager.handlePlayerDisconnect(player);
  }

  private void setupConfigs() {
    configManager.createNewFile("config.yml", "AetherBall Main Configuration");
    FileConfiguration lang = configManager.getConfig("messages.yml");
    FileConfiguration settings = configManager.getConfig("settings.yml");
    Lang.setFile(lang);
    Settings.setFile(settings);
    for (Lang value : Lang.values()) {
      if (!lang.isSet(value.getPath())) {
        lang.set(value.getPath(), value.getDef());
      }
    }
    lang.options().copyDefaults(true);
    configManager.saveConfig("messages.yml");
  }

  public void sendBanner() {
    StringJoiner joiner = new StringJoiner(", ");
    List<String> authors = plugin.getDescription().getAuthors();
    for (String author : authors) {
      joiner.add(author);
    }

    String[] banner =
        new String[] {
          "&2┏┓┏┓" + "&8 -+-------------------------------------------+-",
          "&2┣ ┃ "
              + "&7  Created by &b"
              + joiner
              + "&7, version &f"
              + plugin.getDescription().getVersion(),
          "&2┻ ┗┛" + "&8 -+-------------------------------------------+-"
        };

    for (String line : banner) {
      logConsole(line);
    }
  }
}
