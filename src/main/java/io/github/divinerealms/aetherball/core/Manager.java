package io.github.divinerealms.aetherball.core;

import static io.github.divinerealms.aetherball.configs.Lang.HELP_USAGE;
import static io.github.divinerealms.aetherball.configs.Lang.INGAME_ONLY;
import static io.github.divinerealms.aetherball.configs.Lang.NO_PERM;
import static io.github.divinerealms.aetherball.configs.Lang.NO_PERM_PARAMETERS;
import static io.github.divinerealms.aetherball.configs.Lang.PLAYER_NOT_FOUND;
import static io.github.divinerealms.aetherball.configs.Lang.UNKNOWN_COMMAND;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.isPlayerOnline;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_ADMIN;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.PaperCommandManager;
import io.github.divinerealms.aetherball.AetherBall;
import io.github.divinerealms.aetherball.commands.CubeCommands;
import io.github.divinerealms.aetherball.commands.DynamicCommandRegistry;
import io.github.divinerealms.aetherball.commands.MainCommand;
import io.github.divinerealms.aetherball.commands.MatchMan;
import io.github.divinerealms.aetherball.commands.admin.ArenaCommands;
import io.github.divinerealms.aetherball.commands.admin.BanCommands;
import io.github.divinerealms.aetherball.commands.admin.BaseAdmin;
import io.github.divinerealms.aetherball.commands.admin.DebugCommands;
import io.github.divinerealms.aetherball.commands.admin.PlayerCommands;
import io.github.divinerealms.aetherball.commands.admin.SystemCommands;
import io.github.divinerealms.aetherball.commands.player.BuildCommand;
import io.github.divinerealms.aetherball.commands.player.GameCommands;
import io.github.divinerealms.aetherball.commands.player.MatchesCommand;
import io.github.divinerealms.aetherball.commands.player.SettingsCommands;
import io.github.divinerealms.aetherball.commands.player.TeamCommands;
import io.github.divinerealms.aetherball.configs.Lang;
import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.ConfigManager;
import io.github.divinerealms.aetherball.managers.ListenerManager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.managers.TaskManager;
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.matchmaking.arena.ArenaManager;
import io.github.divinerealms.aetherball.matchmaking.ban.BanManager;
import io.github.divinerealms.aetherball.matchmaking.highscore.HighScoreManager;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchData;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchSystem;
import io.github.divinerealms.aetherball.matchmaking.scoreboard.ScoreManager;
import io.github.divinerealms.aetherball.matchmaking.team.TeamManager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsFormulae;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import io.github.divinerealms.aetherball.utils.CubeCleaner;
import io.github.divinerealms.aetherball.utils.DisableCommands;
import io.github.divinerealms.aetherball.utils.Placeholders;
import io.github.divinerealms.aetherball.utils.PlayerSettings;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import me.neznamy.tab.api.TabAPI;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitScheduler;

@Getter
public class Manager {

  private static final String CONFIG_SOUNDS_KICK_BASE = "sounds.kick";
  private static final String CONFIG_SOUNDS_GOAL_BASE = "sounds.goal";
  private static final String CONFIG_PARTICLES_BASE = "particles";

  @Getter
  private static Manager instance;
  private final AetherBall plugin;
  private final Utilities utilities;
  private final ConfigManager configManager;
  private final PlayerDataManager dataManager;
  private final ArenaManager arenaManager;
  private final ScoreManager scoreboardManager;
  private final MatchData matchData;
  private final TeamManager teamManager;
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
  @Getter
  private final DynamicCommandRegistry dynamicCommandRegistry;
  private final Set<Player> cachedPlayers = ConcurrentHashMap.newKeySet();
  private final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();
  private final Map<UUID, String> cachedPrefixedNames = new ConcurrentHashMap<>();
  private PaperCommandManager commandManager;

  private Economy economy;
  private LuckPerms luckPerms;
  private TabAPI tabAPI;

  @Setter
  private boolean enabling;
  @Setter
  private boolean disabling;

  public Manager(AetherBall plugin) throws IllegalStateException {
    instance = this;

    this.plugin = plugin;
    this.configManager = new ConfigManager(plugin, "");
    this.sendBanner();

    this.dataManager = new PlayerDataManager(this);

    this.setupConfigs();
    this.setupDependencies();

    this.utilities = new Utilities(this);

    this.arenaManager = new ArenaManager(this);
    this.scoreboardManager = new ScoreManager(this);
    this.matchData = new MatchData();
    this.teamManager = new TeamManager(this);
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

    new Placeholders(this).register();
  }

  public void reload() {
    initializeCachedPlayers();
    if (!enabling) {
      configManager.reloadAllConfigs();
    }

    enabling = false;
    setupConfigs();
    matchManager.forceLeaveAllPlayers();
    matchSystem.initializeMatchTypes();
    physicsSystem.removeCubes();
    arenaManager.reloadArenas();
    registerCommands();

    highscoreManager.initializeArrays();
    listenerManager.unregisterAll();
    taskManager.restart();
    listenerManager.registerAll();

    List<UUID> onlinePlayers = new ArrayList<>(cachedPlayers.size());
    for (Player player : cachedPlayers) {
      if (!isPlayerOnline(player)) {
        continue;
      }

      onlinePlayers.add(player.getUniqueId());
    }

    scheduler.runTaskAsynchronously(plugin, () -> onlinePlayers.forEach(uuid -> {
      Player asyncPlayer = plugin.getServer().getPlayer(uuid);
      if (!isPlayerOnline(asyncPlayer)) {
        return;
      }

      PlayerData playerData = dataManager.get(asyncPlayer);
      if (playerData != null) {
        preloadSettings(asyncPlayer, playerData);
      }
    }));
  }

  private void initializeCachedPlayers() {
    Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
    cachedPlayers.clear();
    cachedPlayers.addAll(onlinePlayers);
  }

  public void registerCommands() {
    if (commandManager != null) {
      try {
        commandManager.unregisterCommands();
        clearBukkitCommandMap();
        debugConsole("{prefix_success}Unregistered old ACF commands.");
      } catch (Exception exception) {
        logConsole("{prefix_error}Failed to unregister old commands", exception.getMessage());
      }
    }

    commandManager = new PaperCommandManager(plugin);
    configureACF();
    registerCustomCompletions();
    registerPlayerCommands();
    registerAdminCommands();
    dynamicCommandRegistry.reloadCommands();

    debugConsole("{prefix_success}Registered commands via ACF successfully.");
  }

  private void configureACF() {
    commandManager.getLocales()
        .addMessage(Locale.ENGLISH, MessageKeys.PERMISSION_DENIED, NO_PERM.toString());
    commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.PERMISSION_DENIED_PARAMETER,
        NO_PERM_PARAMETERS.toString());
    commandManager.getLocales()
        .addMessage(Locale.ENGLISH, MessageKeys.INVALID_SYNTAX, HELP_USAGE.toString());
    commandManager.getLocales()
        .addMessage(Locale.ENGLISH, MessageKeys.COULD_NOT_FIND_PLAYER, PLAYER_NOT_FOUND.toString());
    commandManager.getLocales()
        .addMessage(Locale.ENGLISH, MessageKeys.NOT_ALLOWED_ON_CONSOLE, INGAME_ONLY.toString());
    commandManager.getLocales()
        .addMessage(Locale.ENGLISH, MessageKeys.UNKNOWN_COMMAND, UNKNOWN_COMMAND.toString());
  }

  private void registerCustomCompletions() {
    CommandCompletions<BukkitCommandCompletionContext> completions = commandManager.getCommandCompletions();
    completions.registerStaticCompletion("particles", PlayerSettings.getAllowedParticles());
    completions.registerStaticCompletion("colors", PlayerSettings.getAllowedColorNames());
    completions.registerCompletion("matchtypes",
        context -> Settings.getEnabledMatchTypes().stream()
            .map(Settings::getMatchTypeName)
            .collect(Collectors.toList())
    );
    completions.registerCompletion("allmatchtypes",
        context -> Settings.getAllMatchTypeConfigs().keySet().stream()
            .map(Settings::getMatchTypeName)
            .collect(Collectors.toList())
    );
  }

  private void registerPlayerCommands() {
    commandManager.registerCommand(new MainCommand(this));
    commandManager.registerCommand(new GameCommands(this));
    commandManager.registerCommand(new TeamCommands(this));
    commandManager.registerCommand(new CubeCommands(this));
    commandManager.registerCommand(new SettingsCommands(this));
    commandManager.registerCommand(new BuildCommand(this));
    commandManager.registerCommand(new MatchesCommand(this));
  }

  private void registerAdminCommands() {
    commandManager.registerCommand(new BaseAdmin(this));
    commandManager.registerCommand(new SystemCommands(this));
    commandManager.registerCommand(new BanCommands(this));
    commandManager.registerCommand(new ArenaCommands(this));
    commandManager.registerCommand(new PlayerCommands(this));
    commandManager.registerCommand(new MatchMan(this));
    commandManager.registerCommand(new DebugCommands(this));
  }

  private void setupConfigs() {
    configManager.createNewFile("config.yml", "AetherBall Main Configuration");

    FileConfiguration lang = configManager.getConfig("messages.yml");
    FileConfiguration settings = configManager.getConfig("settings.yml");
    Lang.setFile(lang);
    Settings.setFile(settings);

    for (Lang value : Lang.values()) {
      setDefaultIfMissing(lang, value.getPath(), value.getDef());
    }

    lang.options().copyDefaults(true);
    configManager.saveConfig("messages.yml");
  }

  private void setupDependencies() throws IllegalStateException {
    RegisteredServiceProvider<LuckPerms> luckPermsRsp = plugin.getServer().getServicesManager()
        .getRegistration(LuckPerms.class);
    this.luckPerms = luckPermsRsp == null ? null : luckPermsRsp.getProvider();
    if (luckPerms == null) {
      throw new IllegalStateException("LuckPerms not found!");
    }

    RegisteredServiceProvider<Economy> economyRsp = plugin.getServer().getServicesManager()
        .getRegistration(Economy.class);
    this.economy = economyRsp == null ? null : economyRsp.getProvider();
    if (economy == null) {
      throw new IllegalStateException("Vault not found!");
    }

    if (plugin.getServer().getPluginManager().isPluginEnabled("TAB")) {
      this.tabAPI = TabAPI.getInstance();
    } else {
      this.tabAPI = null;
      logConsole("{prefix_warn}TAB plugin not found. Scoreboard features will be disabled.");
    }

    debugConsole("{prefix_success}Hooked into LuckPerms, TAB and Vault successfully!");
  }

  public void reloadTabAPI() {
    if (plugin.getServer().getPluginManager().isPluginEnabled("TAB")) {
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        this.tabAPI = TabAPI.getInstance();
        debugConsole("{prefix_success}Re-hooked into TAB successfully!");
        if (scoreboardManager != null) {
          scoreboardManager.refreshTabAPI();
        }

        if (matchManager != null) {
          matchManager.recreateScoreboards();
        }
      });
    } else {
      this.tabAPI = null;
      logConsole("{prefix_warn}TAB plugin not found. Scoreboard features will be disabled.");
    }
  }

  private void setDefaultIfMissing(FileConfiguration file, String path, Object value) {
    if (!file.isSet(path)) {
      file.set(path, value);
    }
  }

  public PlayerSettings getPlayerSettings(Player player) {
    return playerSettings.get(player.getUniqueId());
  }

  public void preloadSettings(Player player, PlayerData playerData) {
    PlayerSettings settings = getPlayerSettings(player);
    if (settings == null) {
      settings = new PlayerSettings();
      playerSettings.put(player.getUniqueId(), settings);
    }

    if (playerData.has(CONFIG_PARTICLES_BASE + ".effect")) {
      String effect = (String) playerData.get(CONFIG_PARTICLES_BASE + ".effect");
      try {
        EnumParticle particle = EnumParticle.valueOf(effect.split(":")[0]);
        settings.setParticle(particle);

        if (particle == EnumParticle.REDSTONE && effect.contains(":")) {
          String colorName = effect.split(":")[1];
          try {
            settings.setCustomRedstoneColor(colorName);
          } catch (IllegalArgumentException ignored) {
          }
        }
      } catch (IllegalArgumentException exception) {
        logConsole(
            "{prefix_warn}Invalid particle effect found for player " + player.getName() + ": "
                + effect, exception.getMessage());
      }
    }

    if (playerData.has(CONFIG_SOUNDS_KICK_BASE + ".enabled")) {
      settings.setKickSoundEnabled((Boolean) playerData.get(CONFIG_SOUNDS_KICK_BASE + ".enabled"));
    }
    if (playerData.has(CONFIG_SOUNDS_KICK_BASE + ".sound")) {
      settings.setKickSound(
          Sound.valueOf((String) playerData.get(CONFIG_SOUNDS_KICK_BASE + ".sound")));
    }
    if (playerData.has(CONFIG_SOUNDS_GOAL_BASE + ".enabled")) {
      settings.setGoalSoundEnabled((Boolean) playerData.get(CONFIG_SOUNDS_GOAL_BASE + ".enabled"));
    }
    if (playerData.has(CONFIG_SOUNDS_GOAL_BASE + ".sound")) {
      settings.setGoalSound(
          Sound.valueOf((String) playerData.get(CONFIG_SOUNDS_GOAL_BASE + ".sound")));
    }
    if (playerData.has(CONFIG_PARTICLES_BASE + ".enabled")) {
      settings.setParticlesEnabled((Boolean) playerData.get(CONFIG_PARTICLES_BASE + ".enabled"));
    }
    if (playerData.has(CONFIG_PARTICLES_BASE + ".always-show")) {
      settings.setAlwaysShowParticles(
          (Boolean) playerData.get(CONFIG_PARTICLES_BASE + ".always-show"));
    }
    if (playerData.has("ban")) {
      banManager.getBannedPlayers()
          .put(player.getUniqueId(), (Long) playerData.get("ban"));
    }

    String goalCelebration = "default";
    if (playerData.has("goalcelebration")) {
      goalCelebration = (String) playerData.get("goalcelebration");
    }
    settings.setGoalMessage(goalCelebration);
  }

  public void saveAll() {
    configManager.saveAll();
    dataManager.saveAll();
  }

  public void sendBanner() {
    StringJoiner joiner = new StringJoiner(", ");
    List<String> authors = plugin.getDescription().getAuthors();
    for (String author : authors) {
      joiner.add(author);
    }

    String[] banner = new String[]{
        "&2┏┓┏┓" + "&8 -+-------------------------------------------+-",
        "&2┣ ┃ " + "&7  Created by &b" + joiner + "&7, version &f" + plugin.getDescription()
            .getVersion(),
        "&2┻ ┗┛" + "&8 -+-------------------------------------------+-"
    };

    for (String line : banner) {
      logConsole(line);
    }
  }

  public String getPrefixedName(UUID uuid) {
    return cachedPrefixedNames.get(uuid);
  }

  public void cachePrefixedName(Player player) {
    UUID uuid = player.getUniqueId();
    String playerName = player.getName();

    utilities.getPrefixedName(uuid, playerName)
        .thenAccept(prefixedName -> cachedPrefixedNames.put(uuid, prefixedName));

    scheduler.runTaskLaterAsynchronously(plugin, () -> cachedPrefixedNames.remove(uuid),
        Settings.getPrefixExpiry());
  }

  private void clearBukkitCommandMap() {
    try {
      Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
      field.setAccessible(true);
      CommandMap commandMap = (CommandMap) field.get(Bukkit.getServer());

      Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
      knownCommandsField.setAccessible(true);
      //noinspection unchecked
      Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(
          commandMap);

      knownCommands.entrySet().removeIf(entry -> {
        Command cmd = entry.getValue();
        if (cmd instanceof PluginIdentifiableCommand) {
          return ((PluginIdentifiableCommand) cmd).getPlugin().equals(plugin);
        }
        return false;
      });
    } catch (Exception exception) {
      logConsole("{prefix_warn}Failed to clear command map", exception.getMessage());
    }
  }

  public void cleanup() {
    long start = System.nanoTime();
    try {
      if (dynamicCommandRegistry != null) {
        dynamicCommandRegistry.unregisterAllMatchTypeCommands();
      }
      if (commandManager != null) {
        try {
          commandManager.unregisterCommands();
          clearBukkitCommandMap();
          commandManager = null;
          debugConsole("{prefix_success}Unregistered ACF commands during cleanup.");
        } catch (Exception exception) {
          logConsole("{prefix_error}Failed to unregister commands during cleanup",
              exception.getMessage());
        }
      }
      if (listenerManager != null) {
        listenerManager.unregisterAll();
      }
      if (physicsData != null) {
        physicsData.cleanup();
      }
      playerSettings.clear();
      cachedPrefixedNames.clear();
      cachedPlayers.clear();
      instance = null;
    } catch (Exception exception) {
      logConsole("{prefix_error}Error in cleanup", exception.getMessage());
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          sendMessage(PERM_ADMIN, "{prefix_debug}&dCleanup &ftook &e" + ms + "ms &f(threshold: "
              + Settings.DEBUG_THRESHOLD.asLong() + "ms)");
        }
      }
    }
  }
}
