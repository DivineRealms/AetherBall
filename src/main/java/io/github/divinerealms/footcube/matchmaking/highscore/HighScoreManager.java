package io.github.divinerealms.footcube.matchmaking.highscore;

import static io.github.divinerealms.footcube.configs.Lang.BEST_ASSISTS;
import static io.github.divinerealms.footcube.configs.Lang.BEST_ENTRY;
import static io.github.divinerealms.footcube.configs.Lang.BEST_GOALS;
import static io.github.divinerealms.footcube.configs.Lang.BEST_HEADER;
import static io.github.divinerealms.footcube.configs.Lang.BEST_OWN_GOALS;
import static io.github.divinerealms.footcube.configs.Lang.BEST_UPDATING;
import static io.github.divinerealms.footcube.configs.Lang.BEST_WINS;
import static io.github.divinerealms.footcube.configs.Lang.BEST_WINSTREAK;
import static io.github.divinerealms.footcube.configs.Lang.NOBODY;
import static io.github.divinerealms.footcube.configs.Lang.SIMPLE_FOOTER;

import io.github.divinerealms.footcube.configs.PlayerData;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.managers.PlayerDataManager;
import io.github.divinerealms.footcube.managers.Utilities;
import io.github.divinerealms.footcube.utils.Logger;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class HighScoreManager {

  private final FCManager fcManager;
  private final Plugin plugin;
  private final Logger logger;
  private final Utilities utilities;
  private final PlayerDataManager playerDataManager;
  private final Object highScoreLock = new Object();
  public double[] bestRatings;
  public int[] mostGoals;
  public int[] mostAssists;
  public int[] mostOwnGoals;
  public int[] mostWins;
  public int[] longestStreak;
  public String[] topSkillNames;
  public String[] topGoalsNames;
  public String[] topAssistsNames;
  public String[] topOwnGoalsNames;
  public String[] topWinsNames;
  public String[] topStreakNames;
  @Getter
  private long lastUpdate;
  @Getter
  private String[] participants;
  @Getter
  private boolean isUpdating;
  @Getter
  private boolean hasInitialData = false;
  private static final int TOP_SCORES_SIZE = 5;

  public HighScoreManager(FCManager fcManager) {
    this.fcManager = fcManager;
    this.plugin = fcManager.getPlugin();
    this.logger = fcManager.getLogger();
    this.utilities = fcManager.getUtilities();
    this.playerDataManager = fcManager.getDataManager();
    initializeArrays();
  }

  private void initializeArrays() {
    bestRatings = new double[TOP_SCORES_SIZE];
    mostGoals = new int[TOP_SCORES_SIZE];
    mostAssists = new int[TOP_SCORES_SIZE];
    mostOwnGoals = new int[TOP_SCORES_SIZE];
    mostWins = new int[TOP_SCORES_SIZE];
    longestStreak = new int[TOP_SCORES_SIZE];

    String nobody = NOBODY.toString();
    topSkillNames = new String[TOP_SCORES_SIZE];
    topGoalsNames = new String[TOP_SCORES_SIZE];
    topAssistsNames = new String[TOP_SCORES_SIZE];
    topOwnGoalsNames = new String[TOP_SCORES_SIZE];
    topWinsNames = new String[TOP_SCORES_SIZE];
    topStreakNames = new String[TOP_SCORES_SIZE];

    for (int i = 0; i < TOP_SCORES_SIZE; i++) {
      topSkillNames[i] = nobody;
      topGoalsNames[i] = nobody;
      topAssistsNames[i] = nobody;
      topOwnGoalsNames[i] = nobody;
      topWinsNames[i] = nobody;
      topStreakNames[i] = nobody;
    }
  }

  public void showHighScores(CommandSender sender) {
    if (isUpdating) {
      logger.send(sender, BEST_UPDATING);
      return;
    }

    if (!hasInitialData) {
      logger.send(sender, BEST_UPDATING);
      return;
    }

    logger.send(sender, BEST_HEADER);
    showTopCategory(sender, topSkillNames, bestRatings);

    logger.send(sender, BEST_GOALS);
    showTopCategory(sender, topGoalsNames, mostGoals);

    logger.send(sender, BEST_ASSISTS);
    showTopCategory(sender, topAssistsNames, mostAssists);

    logger.send(sender, BEST_OWN_GOALS);
    showTopCategory(sender, topOwnGoalsNames, mostOwnGoals);

    logger.send(sender, BEST_WINS);
    showTopCategory(sender, topWinsNames, mostWins);

    logger.send(sender, BEST_WINSTREAK);
    showTopCategory(sender, topStreakNames, longestStreak);

    logger.send(sender, SIMPLE_FOOTER);
  }

  private void showTopCategory(CommandSender sender, String[] names, double[] values) {
    for (int i = 0; i < 5; i++) {
      logger.send(sender, BEST_ENTRY,
          String.valueOf(i + 1),
          names[i],
          String.valueOf(values[i])
      );
    }
  }

  private void showTopCategory(CommandSender sender, String[] names, int[] values) {
    for (int i = 0; i < 5; i++) {
      logger.send(sender, BEST_ENTRY,
          String.valueOf(i + 1),
          names[i],
          String.valueOf(values[i])
      );
    }
  }

  public void startUpdate() {
    File playerFolder = new File(plugin.getDataFolder(), "players");
    File[] files = playerFolder.listFiles((dir, name) -> name.endsWith(".yml"));
    participants = new String[files != null
        ? files.length
        : 0];

    for (int i = 0; i < participants.length; i++) {
      participants[i] = files[i].getName().replace(".yml", "");
    }

    isUpdating = true;
    clearArrays();
    processAllPlayers();
  }

  private void clearArrays() {
    synchronized (highScoreLock) {
      String nobody = NOBODY.toString();

      for (int i = 0; i < 5; i++) {
        bestRatings[i] = 0.0;
        topSkillNames[i] = nobody;
      }

      for (int i = 0; i < 5; i++) {
        mostGoals[i] = 0;
        topGoalsNames[i] = nobody;
      }

      for (int i = 0; i < 5; i++) {
        mostAssists[i] = 0;
        topAssistsNames[i] = nobody;
      }

      for (int i = 0; i < 5; i++) {
        mostOwnGoals[i] = 0;
        topOwnGoalsNames[i] = nobody;
      }

      for (int i = 0; i < 5; i++) {
        mostWins[i] = 0;
        topWinsNames[i] = nobody;
      }

      for (int i = 0; i < 5; i++) {
        longestStreak[i] = 0;
        topStreakNames[i] = nobody;
      }
    }
  }

  public void processAllPlayers() {
    List<CompletableFuture<Void>> nameFutures = new ArrayList<>();

    for (String playerName : participants) {
      PlayerData data = playerDataManager.get(playerName);
      if (data == null) {
        continue;
      }

      int matches = (int) data.get("matches");
      int wins = (int) data.get("wins");
      int ties = (int) data.get("ties");
      int goals = (int) data.get("goals");
      int assists = (int) data.get("assists");
      int ownGoals = (int) data.get("owngoals");
      int bestWinStreak = (int) data.get("bestwinstreak");

      double multiplier = 1 - Math.pow(0.9, matches);
      double goalBonus = matches > 0
          ? (goals == matches
          ? 1
          : Math.min(1, 1 - multiplier * Math.pow(0.2, (double) goals / matches)))
          : 0.5;
      double addition = (matches > 0 && wins + ties > 0)
          ? 8 * (1 / ((100 * matches) / (wins + 0.5 * ties) / 100)) - 4
          : (matches > 0
              ? -4
              : 0);
      double skillLevel = Math.min(5 + goalBonus + addition * multiplier, 10);

      UUID uuid = playerDataManager.getUUID(playerName);
      if (uuid == null) {
        logger.info("&cUUID not found for player &b" + playerName);
        continue;
      }

      String cachedPrefixedName = fcManager.getPrefixedName(uuid);

      if (cachedPrefixedName != null) {
        insertTop5(bestRatings, topSkillNames, skillLevel, cachedPrefixedName);
        insertTop5(mostGoals, topGoalsNames, goals, cachedPrefixedName);
        insertTop5(mostAssists, topAssistsNames, assists, cachedPrefixedName);
        insertTop5(mostOwnGoals, topOwnGoalsNames, ownGoals, cachedPrefixedName);
        insertTop5(mostWins, topWinsNames, wins, cachedPrefixedName);
        insertTop5(longestStreak, topStreakNames, bestWinStreak, cachedPrefixedName);
      } else {
        CompletableFuture<Void> playerFuture = utilities.getPrefixedName(uuid, playerName)
            .thenAccept(prefixedName -> {
              insertTop5(bestRatings, topSkillNames, skillLevel, prefixedName);
              insertTop5(mostGoals, topGoalsNames, goals, prefixedName);
              insertTop5(mostAssists, topAssistsNames, assists, prefixedName);
              insertTop5(mostOwnGoals, topOwnGoalsNames, ownGoals, prefixedName);
              insertTop5(mostWins, topWinsNames, wins, prefixedName);
              insertTop5(longestStreak, topStreakNames, bestWinStreak, prefixedName);
            });

        nameFutures.add(playerFuture);
      }
    }

    CompletableFuture.allOf(nameFutures.toArray(new CompletableFuture[0])).join();

    lastUpdate = System.currentTimeMillis();
    isUpdating = false;
    hasInitialData = true;
  }

  private void insertTop5(double[] array, String[] names, double value, String prefixedName) {
    value = (double) Math.round(value * 100) / 100;
    insertIntoArray(array, names, value, prefixedName);
  }

  private void insertTop5(int[] array, String[] names, int value, String prefixedName) {
    insertIntoArray(array, names, value, prefixedName);
  }

  private void insertIntoArray(double[] array, String[] names, double value, String prefixedName) {
    synchronized (highScoreLock) {
      int existingIndex = -1;
      for (int i = 0; i < 5; i++) {
        if (names[i] != null && names[i].equals(prefixedName)) {
          existingIndex = i;
          break;
        }
      }

      if (existingIndex != -1 && array[existingIndex] >= value) {
        return;
      }

      if (existingIndex != -1) {
        for (int j = existingIndex; j < 4; j++) {
          array[j] = array[j + 1];
          names[j] = names[j + 1];
        }
        array[4] = 0;
        names[4] = NOBODY.toString();
      }

      for (int i = 0; i < 5; i++) {
        if (value > array[i]) {
          for (int j = 4; j > i; j--) {
            array[j] = array[j - 1];
            names[j] = names[j - 1];
          }

          array[i] = value;
          names[i] = prefixedName;
          break;
        }
      }
    }
  }

  private void insertIntoArray(int[] array, String[] names, int value, String prefixedName) {
    synchronized (highScoreLock) {
      int existingIndex = -1;
      for (int i = 0; i < 5; i++) {
        if (names[i] != null && names[i].equals(prefixedName)) {
          existingIndex = i;
          break;
        }
      }

      if (existingIndex != -1 && array[existingIndex] >= value) {
        return;
      }

      if (existingIndex != -1) {
        for (int j = existingIndex; j < 4; j++) {
          array[j] = array[j + 1];
          names[j] = names[j + 1];
        }
        array[4] = 0;
        names[4] = NOBODY.toString();
      }

      for (int i = 0; i < 5; i++) {
        if (value > array[i]) {
          for (int j = 4; j > i; j--) {
            array[j] = array[j - 1];
            names[j] = names[j - 1];
          }

          array[i] = value;
          names[i] = prefixedName;
          break;
        }
      }
    }
  }
}