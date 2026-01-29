package io.github.divinerealms.footcube.configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

@SuppressWarnings("unused")
@Getter
public enum Settings {
  // ==================== GENERAL SETTINGS ====================
  DEBUG_MODE("General.Debug_Mode", false),
  DEBUG_THRESHOLD("General.Debug_Threshold", 50),

  // ==================== PHYSICS ====================
  MAX_KICK_POWER("Physics.Max_Kick_Power", 5),
  SOFT_CAP("Physics.Soft_Cap", 0.9),
  REMOVAL_DELAY("Physics.Removal_Delay", 2),
  KICK_COOLDOWN_REGULAR("Physics.Kick_Cooldowns.Regular", 150),
  KICK_COOLDOWN_CHARGED("Physics.Kick_Cooldowns.Charged", 500),
  KICK_COOLDOWN_RISE("Physics.Kick_Cooldowns.Rise", 1000),
  KICK_BASE_POWER_REGULAR("Physics.Kick_Base_Power.Regular", 0.65),
  KICK_BASE_POWER_CHARGED("Physics.Kick_Base_Power.Charged", 0.375),
  KICK_VERTICAL_BOOST("Physics.Kick_Vertical_Boost", 0.3),
  AFK_THRESHOLD("Physics.AFK_Threshold", 2),

  // ==================== MATCHMAKING - TIMING ====================
  STARTING_COUNTDOWN("Matchmaking.Timing.Starting_Countdown", 15),
  TAKE_PLACE_ANNOUNCEMENT("Matchmaking.Timing.Take_Place_Announcement", 20),

  // ==================== MATCHMAKING - ECONOMY ====================
  ECONOMY_GOAL("Matchmaking.Economy.Goal", 50),
  ECONOMY_ASSIST("Matchmaking.Economy.Assist", 25),
  ECONOMY_VICTORY("Matchmaking.Economy.Victory", 100),
  ECONOMY_TIE("Matchmaking.Economy.Tie", 50),
  ECONOMY_HAT_TRICK("Matchmaking.Economy.Hat_Trick", 150),
  ECONOMY_WIN_STREAK("Matchmaking.Economy.Win_Streak", 100),
  ECONOMY_CLEAN_SHEET("Matchmaking.Economy.Clean_Sheet", 75),

  // ==================== CUBE SPAWN SETTINGS ====================
  CUBE_MAX_PER_AREA("Cube.Max_Per_Area", 10),
  CUBE_SPAWN_COOLDOWN("Cube.Spawn_Cooldown", 3),

  // ==================== PLAYER SETTINGS ====================
  PLAYER_BUILD_MODE("Player_Defaults.Build_Mode", false),
  PLAYER_PARTICLES("Player_Defaults.Particles", true),
  PLAYER_KICK_SOUND("Player_Defaults.Kick_Sound", true),
  PLAYER_GOAL_SOUND("Player_Defaults.Goal_Sound", true),
  PLAYER_GOAL_CELEBRATION("Player_Defaults.Goal_Celebration", "default"),

  // ==================== BAN SETTINGS ====================
  BAN_DEFAULT_DURATION("Moderation.Default_Duration", 30),
  BAN_RAGEQUIT_DURATION("Moderation.Rage_Quit_Duration", 30),
  BAN_RAGEQUIT_PENALTY("Moderation.Rage_Quit_Penalty", 200),

  // ==================== HIGHSCORE SETTINGS ====================
  HIGHSCORE_UPDATE_INTERVAL("HighScores.Update_Interval", 10),
  HIGHSCORE_TOP_PLAYERS("HighScores.Top_Players_Count", 5),

  // ==================== CACHE SETTINGS ====================
  CACHE_CLEANUP_INTERVAL("Cache.Cleanup_Interval", 5),
  CACHE_PREFIX_EXPIRY("Cache.Prefix_Expiry", 30),

  // ==================== PLAYER DATA SETTINGS ====================
  PLAYER_DATA_AUTO_SAVE("Player_Data.Auto_Save_Interval", 10),
  PLAYER_DATA_BATCH_SIZE("Player_Data.Batch_Size", 20),

  // ==================== TEAM SETTINGS ====================
  TEAM_INVITE_EXPIRY("Teams.Invite_Expiry", 2),

  // ==================== PRACTICE AREA SETTINGS ====================
  PRACTICE_AREA_RADIUS("Practice_Areas.Radius", 100),
  PRACTICE_AREA_CLEANUP("Practice_Areas.Cleanup_Interval", 5),

  // ==================== FEATURE TOGGLES ====================
  FEATURES_SIGNS("Features.Signs_Enabled", true);

  public static FileConfiguration CONFIG;
  private final String path;
  private final Object def;

  static final Map<Integer, MatchTypeConfig> MATCH_TYPE_CACHE = new HashMap<>();

  Settings(String path, Object def) {
    this.path = path;
    this.def = def;
  }

  public static void setFile(FileConfiguration config) {
    CONFIG = config;
    loadMatchTypes();
  }

  public static FileConfiguration getConfig() {
    return CONFIG;
  }

  public Object getDefault() {
    return def;
  }

  // ==================== TYPE-SAFE GETTERS ====================

  public boolean asBoolean() {
    return Boolean.parseBoolean(String.valueOf(CONFIG.get(path, def)));
  }

  public int asInt() {
    return Integer.parseInt(String.valueOf(CONFIG.get(path, def)));
  }

  public long asLong() {
    return Long.parseLong(String.valueOf(CONFIG.get(path, def)));
  }

  public double asDouble() {
    return Double.parseDouble(String.valueOf(CONFIG.get(path, def)));
  }

  public float asFloat() {
    return Float.parseFloat(String.valueOf(CONFIG.get(path, def)));
  }

  @Override
  public String toString() {
    return CONFIG.getString(this.path, (String) this.def);
  }

  // ==================== DYNAMIC MATCH TYPE SYSTEM ====================

  @Getter
  public static class MatchTypeConfig {

    private final int type;
    private final boolean enabled;
    private final long duration;
    private final int maxScore;
    private final boolean countStats;

    public MatchTypeConfig(int type, boolean enabled, long duration, int maxScore,
        boolean countStats) {
      this.type = type;
      this.enabled = enabled;
      this.duration = duration;
      this.maxScore = maxScore;
      this.countStats = countStats;
    }
  }

  private static void loadMatchTypes() {
    MATCH_TYPE_CACHE.clear();

    if (CONFIG == null) {
      return;
    }

    ConfigurationSection matchTypesSection = CONFIG.getConfigurationSection(
        "Matchmaking.Match_Types");
    if (matchTypesSection == null) {
      return;
    }

    for (String key : matchTypesSection.getKeys(false)) {
      try {
        int type = Integer.parseInt(key.split("v")[0]);

        String basePath = "Matchmaking.Match_Types." + key + ".";
        boolean enabled = CONFIG.getBoolean(basePath + "Enabled", true);
        long duration = CONFIG.getLong(basePath + "Duration", 300);
        int maxScore = CONFIG.getInt(basePath + "Max_Score", 10);
        boolean countStats = CONFIG.getBoolean(basePath + "Count_Stats", true);

        MATCH_TYPE_CACHE.put(type,
            new MatchTypeConfig(type, enabled, duration, maxScore, countStats));
      } catch (Exception ignored) {
      }
    }
  }

  public static void reloadMatchTypes() {
    loadMatchTypes();
  }

  public static MatchTypeConfig getMatchTypeConfig(int type) {
    return MATCH_TYPE_CACHE.get(type);
  }

  public static Map<Integer, MatchTypeConfig> getAllMatchTypeConfigs() {
    return new HashMap<>(MATCH_TYPE_CACHE);
  }

  // ==================== HELPER METHODS ====================

  public static long getMatchDuration(int type) {
    MatchTypeConfig config = MATCH_TYPE_CACHE.get(type);
    return config != null ? config.getDuration() : 300L;
  }

  public static int getMaxScore(int type) {
    MatchTypeConfig config = MATCH_TYPE_CACHE.get(type);
    return config != null ? config.getMaxScore() : 10;
  }

  public static boolean shouldCountStats(int type) {
    MatchTypeConfig config = MATCH_TYPE_CACHE.get(type);
    return config != null && config.isCountStats();
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean isMatchTypeEnabled(int type) {
    return getEnabledMatchTypes().contains(type);
  }

  public static String getMatchTypeName(int type) {
    return type + "v" + type;
  }

  public static List<Integer> getEnabledMatchTypes() {
    List<Integer> types = new ArrayList<>();
    for (Map.Entry<Integer, MatchTypeConfig> entry : MATCH_TYPE_CACHE.entrySet()) {
      if (entry.getValue().isEnabled()) {
        types.add(entry.getKey());
      }
    }
    return types;
  }

  public static long getCacheCleanupInterval() {
    return TimeUnit.MINUTES.toSeconds(CACHE_CLEANUP_INTERVAL.asInt()) * 20L;
  }

  public static long getHighScoreUpdateInterval() {
    return TimeUnit.MINUTES.toSeconds(HIGHSCORE_UPDATE_INTERVAL.asInt()) * 20L;
  }

  public static long getAFKThreshold() {
    return TimeUnit.MINUTES.toSeconds(AFK_THRESHOLD.asLong()) * 20L;
  }

  public static long getSpawnCooldown() {
    return TimeUnit.SECONDS.toMillis(CUBE_SPAWN_COOLDOWN.asLong());
  }

  public static long getTakePlaceAnnouncementInterval() {
    return TimeUnit.SECONDS.toSeconds(TAKE_PLACE_ANNOUNCEMENT.asLong()) * 20L;
  }

  public static long getPracticeAreaCleanupInterval() {
    return TimeUnit.MINUTES.toSeconds(PRACTICE_AREA_CLEANUP.asLong()) * 20L;
  }

  public static long getTeamExpiry() {
    return TimeUnit.MINUTES.toSeconds(TEAM_INVITE_EXPIRY.asLong()) * 20L;
  }

  public static long getPrefixExpiry() {
    return TimeUnit.MINUTES.toSeconds(CACHE_PREFIX_EXPIRY.asLong()) * 20L;
  }

  public static long getAutoSaveInterval() {
    return TimeUnit.MINUTES.toSeconds(PLAYER_DATA_AUTO_SAVE.asLong()) * 20L;
  }

  public static long getRageQuitBanDuration() {
    return TimeUnit.MINUTES.toMillis(BAN_RAGEQUIT_DURATION.asLong());
  }

  public static long getDefaultBanDuration() {
    return TimeUnit.MINUTES.toMillis(BAN_DEFAULT_DURATION.asLong());
  }
}