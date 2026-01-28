package io.github.divinerealms.footcube.configs;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
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
  AFK_THRESHOLD("Physics.AFK_Threshold", 60),

  // ==================== MATCHMAKING - MATCH TYPES ====================
  MATCH_1V1_ENABLED("Matchmaking.Match_Types.1v1.Enabled", true),
  MATCH_1V1_DURATION("Matchmaking.Match_Types.1v1.Duration", 120),
  MATCH_1V1_MAX_SCORE("Matchmaking.Match_Types.1v1.Max_Score", 5),
  MATCH_1V1_COUNT_STATS("Matchmaking.Match_Types.1v1.Count_Stats", true),

  MATCH_2V2_ENABLED("Matchmaking.Match_Types.2v2.Enabled", true),
  MATCH_2V2_DURATION("Matchmaking.Match_Types.2v2.Duration", 240),
  MATCH_2V2_MAX_SCORE("Matchmaking.Match_Types.2v2.Max_Score", 7),
  MATCH_2V2_COUNT_STATS("Matchmaking.Match_Types.2v2.Count_Stats", true),

  MATCH_3V3_ENABLED("Matchmaking.Match_Types.3v3.Enabled", true),
  MATCH_3V3_DURATION("Matchmaking.Match_Types.3v3.Duration", 300),
  MATCH_3V3_MAX_SCORE("Matchmaking.Match_Types.3v3.Max_Score", 10),
  MATCH_3V3_COUNT_STATS("Matchmaking.Match_Types.3v3.Count_Stats", true),

  MATCH_4V4_ENABLED("Matchmaking.Match_Types.4v4.Enabled", true),
  MATCH_4V4_DURATION("Matchmaking.Match_Types.4v4.Duration", 300),
  MATCH_4V4_MAX_SCORE("Matchmaking.Match_Types.4v4.Max_Score", 12),
  MATCH_4V4_COUNT_STATS("Matchmaking.Match_Types.4v4.Count_Stats", true),

  MATCH_5V5_ENABLED("Matchmaking.Match_Types.5v5.Enabled", true),
  MATCH_5V5_DURATION("Matchmaking.Match_Types.5v5.Duration", 300),
  MATCH_5V5_MAX_SCORE("Matchmaking.Match_Types.5v5.Max_Score", 15),
  MATCH_5V5_COUNT_STATS("Matchmaking.Match_Types.5v5.Count_Stats", true),

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

  // ==================== LOBBY SETTINGS ====================
  LOBBY_WORLD("Lobby.World", "world"),

  // ==================== FEATURE TOGGLES ====================
  FEATURES_SIGNS("Features.Signs_Enabled", true);

  public static FileConfiguration CONFIG;
  private final String path;
  private final Object def;

  Settings(String path, Object def) {
    this.path = path;
    this.def = def;
  }

  public static void setFile(FileConfiguration config) {
    CONFIG = config;
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

  // ==================== HELPER METHODS ====================

  public static long getMatchDuration(int type) {
    switch (type) {
      case 1:
        return MATCH_1V1_DURATION.asLong();
      case 2:
        return MATCH_2V2_DURATION.asLong();
      case 3:
        return MATCH_3V3_DURATION.asLong();
      case 4:
        return MATCH_4V4_DURATION.asLong();
      case 5:
        return MATCH_5V5_DURATION.asLong();
      default:
        return 300L;
    }
  }

  public static int getMaxScore(int type) {
    switch (type) {
      case 1:
        return MATCH_1V1_MAX_SCORE.asInt();
      case 2:
        return MATCH_2V2_MAX_SCORE.asInt();
      case 3:
        return MATCH_3V3_MAX_SCORE.asInt();
      case 4:
        return MATCH_4V4_MAX_SCORE.asInt();
      case 5:
        return MATCH_5V5_MAX_SCORE.asInt();
      default:
        return 10;
    }
  }

  public static boolean shouldCountStats(int type) {
    switch (type) {
      case 1:
        return MATCH_1V1_COUNT_STATS.asBoolean();
      case 2:
        return MATCH_2V2_COUNT_STATS.asBoolean();
      case 3:
        return MATCH_3V3_COUNT_STATS.asBoolean();
      case 4:
        return MATCH_4V4_COUNT_STATS.asBoolean();
      case 5:
        return MATCH_5V5_COUNT_STATS.asBoolean();
      default:
        return true;
    }
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
    if (MATCH_1V1_ENABLED.asBoolean()) {
      types.add(1);
    }
    if (MATCH_2V2_ENABLED.asBoolean()) {
      types.add(2);
    }
    if (MATCH_3V3_ENABLED.asBoolean()) {
      types.add(3);
    }
    if (MATCH_4V4_ENABLED.asBoolean()) {
      types.add(4);
    }
    if (MATCH_5V5_ENABLED.asBoolean()) {
      types.add(5);
    }
    return types;
  }

  public static long getCacheCleanupTicks() {
    return 20L * 60L * CACHE_CLEANUP_INTERVAL.asInt();
  }

  public static long getHighScoreUpdateTicks() {
    return 20L * 60L * HIGHSCORE_UPDATE_INTERVAL.asInt();
  }

  public static long getAFKThreshold() {
    return 20L * 60L * AFK_THRESHOLD.asInt();
  }

  public static long getSpawnCooldown() {
    return 20L * CUBE_SPAWN_COOLDOWN.asInt();
  }

  public static long getTakePlaceAnnouncement() {
    return 20L * TAKE_PLACE_ANNOUNCEMENT.asInt();
  }

  public static long getPracticeAreaCleanupTicks() {
    return 20L * 60L * PRACTICE_AREA_CLEANUP.asInt();
  }
}