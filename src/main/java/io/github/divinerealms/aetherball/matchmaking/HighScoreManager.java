package io.github.divinerealms.aetherball.matchmaking;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.matchmaking.player.StatsHelper;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.github.divinerealms.aetherball.configs.Lang.*;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;

public class HighScoreManager {

  private final Manager manager;
  private final Plugin plugin;
  private final Utilities utilities;
  private final PlayerDataManager playerDataManager;
  private final Object highScoreLock = new Object();

  private double[] bestRatings;
  private int[] mostGoals;
  private int[] mostAssists;
  private int[] mostOwnGoals;
  private int[] mostWins;
  private int[] longestStreak;
  private String[] topSkillNames;
  private String[] topGoalsNames;
  private String[] topAssistsNames;
  private String[] topOwnGoalsNames;
  private String[] topWinsNames;
  private String[] topStreakNames;

  @Getter private long lastUpdate;
  @Getter private String[] participants;
  @Getter private boolean isUpdating;
  @Getter private boolean hasInitialData = false;
  @Getter private int totalPlayerFiles = 0;
  @Getter private int skippedCount = 0;
  @Getter private int processedCount = 0;
  @Getter private int topScoresSize;

  public HighScoreManager(Manager manager) {
    this.manager = manager;
    this.plugin = manager.getPlugin();
    this.utilities = manager.getUtilities();
    this.playerDataManager = manager.getDataManager();
  }

  public String getTopName(HighScoreCategory category, int rank) {
    if (!hasInitialData || rank < 0 || rank >= topScoresSize) {
      return "---";
    }

    return switch (category) {
      case SKILL -> topSkillNames[rank];
      case GOALS -> topGoalsNames[rank];
      case ASSISTS -> topAssistsNames[rank];
      case OWN_GOALS -> topOwnGoalsNames[rank];
      case WINS -> topWinsNames[rank];
      case STREAK -> topStreakNames[rank];
    };
  }

  public String getTopValue(HighScoreCategory category, int rank) {
    if (!hasInitialData || rank < 0 || rank >= topScoresSize) {
      return "---";
    }

    return switch (category) {
      case SKILL -> String.valueOf(bestRatings[rank]);
      case GOALS -> String.valueOf(mostGoals[rank]);
      case ASSISTS -> String.valueOf(mostAssists[rank]);
      case OWN_GOALS -> String.valueOf(mostOwnGoals[rank]);
      case WINS -> String.valueOf(mostWins[rank]);
      case STREAK -> String.valueOf(longestStreak[rank]);
    };
  }

  public void initializeArrays() {
    topScoresSize = Settings.HIGHSCORE_TOP_PLAYERS.asInt();

    bestRatings = new double[topScoresSize];
    mostGoals = new int[topScoresSize];
    mostAssists = new int[topScoresSize];
    mostOwnGoals = new int[topScoresSize];
    mostWins = new int[topScoresSize];
    longestStreak = new int[topScoresSize];

    String nobody = NOBODY.toString();
    topSkillNames = new String[topScoresSize];
    topGoalsNames = new String[topScoresSize];
    topAssistsNames = new String[topScoresSize];
    topOwnGoalsNames = new String[topScoresSize];
    topWinsNames = new String[topScoresSize];
    topStreakNames = new String[topScoresSize];

    for (int i = 0; i < topScoresSize; i++) {
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
      sendMessage(sender, BEST_UPDATING);
      return;
    }

    if (!hasInitialData) {
      sendMessage(sender, BEST_UPDATING);
      return;
    }

    sendMessage(sender, BEST_HEADER);
    showTopCategory(sender, topSkillNames, bestRatings);

    sendMessage(sender, BEST_GOALS);
    showTopCategory(sender, topGoalsNames, mostGoals);

    sendMessage(sender, BEST_ASSISTS);
    showTopCategory(sender, topAssistsNames, mostAssists);

    sendMessage(sender, BEST_OWN_GOALS);
    showTopCategory(sender, topOwnGoalsNames, mostOwnGoals);

    sendMessage(sender, BEST_WINS);
    showTopCategory(sender, topWinsNames, mostWins);

    sendMessage(sender, BEST_WINSTREAK);
    showTopCategory(sender, topStreakNames, longestStreak);

    sendMessage(sender, SIMPLE_FOOTER);
  }

  private void showTopCategory(CommandSender sender, String[] names, double[] values) {
    for (int i = 0; i < topScoresSize; i++) {
      sendMessage(sender, BEST_ENTRY, String.valueOf(i + 1), names[i], String.valueOf(values[i]));
    }
  }

  private void showTopCategory(CommandSender sender, String[] names, int[] values) {
    for (int i = 0; i < topScoresSize; i++) {
      sendMessage(sender, BEST_ENTRY, String.valueOf(i + 1), names[i], String.valueOf(values[i]));
    }
  }

  public void startUpdate(Runnable onComplete) {
    File playerFolder = new File(plugin.getDataFolder(), "players");
    File[] files = playerFolder.listFiles((dir, name) -> name.endsWith(".yml"));
    totalPlayerFiles = files != null ? files.length : 0;
    participants = new String[files != null ? files.length : 0];

    for (int i = 0; i < participants.length; i++) {
      participants[i] = files[i].getName().replace(".yml", "");
    }

    isUpdating = true;
    clearArrays();
    processAllPlayers(onComplete);
  }

  private void clearArrays() {
    synchronized (highScoreLock) {
      String nobody = NOBODY.toString();
      skippedCount = 0;
      processedCount = 0;

      for (int i = 0; i < topScoresSize; i++) {
        bestRatings[i] = 0.0;
        topSkillNames[i] = nobody;

        mostGoals[i] = 0;
        topGoalsNames[i] = nobody;

        mostAssists[i] = 0;
        topAssistsNames[i] = nobody;

        mostOwnGoals[i] = 0;
        topOwnGoalsNames[i] = nobody;

        mostWins[i] = 0;
        topWinsNames[i] = nobody;

        longestStreak[i] = 0;
        topStreakNames[i] = nobody;
      }
    }
  }

  public void processAllPlayers(Runnable onComplete) {
    List<CompletableFuture<Void>> nameFutures = new ArrayList<>();

    for (String playerName : participants) {
      PlayerData data = playerDataManager.get(playerName);
      if (data == null || data.getInt("matches") == 0) {
        skippedCount++;
        continue;
      }

      processedCount++;
      StatsHelper stats = new StatsHelper(data);

      UUID uuid = playerDataManager.getUUID(playerName);
      if (uuid == null) {
        logConsole("{prefix_error}UUID not found for player " + playerName);
        continue;
      }

      String cachedPrefixedName = manager.getPrefixedName(uuid);
      if (cachedPrefixedName != null) {
        insertAllCategories(stats, cachedPrefixedName);
      } else {
        nameFutures.add(
            utilities
                .getPrefixedName(uuid, playerName)
                .thenAccept(prefixedName -> insertAllCategories(stats, prefixedName)));
      }
    }

    CompletableFuture.allOf(nameFutures.toArray(new CompletableFuture[0]))
        .thenRun(
            () -> {
              lastUpdate = System.currentTimeMillis();
              isUpdating = false;
              hasInitialData = true;
              if (onComplete != null) {
                onComplete.run();
              }
            });
  }

  private void insertAllCategories(StatsHelper statsHelper, String prefixedName) {
    insertTop5(bestRatings, topSkillNames, statsHelper.getSkillLevel(), prefixedName);
    insertTop5(mostGoals, topGoalsNames, statsHelper.getGoals(), prefixedName);
    insertTop5(mostAssists, topAssistsNames, statsHelper.getAssists(), prefixedName);
    insertTop5(mostOwnGoals, topOwnGoalsNames, statsHelper.getOwnGoals(), prefixedName);
    insertTop5(mostWins, topWinsNames, statsHelper.getWins(), prefixedName);
    insertTop5(longestStreak, topStreakNames, statsHelper.getBestWinStreak(), prefixedName);
  }

  private void insertTop5(double[] array, String[] names, double value, String prefixedName) {
    insertIntoArray(array, names, Math.round(value * 100) / 100.0, prefixedName);
  }

  private void insertTop5(int[] array, String[] names, int value, String prefixedName) {
    insertIntoArray(array, names, value, prefixedName);
  }

  private void insertIntoArray(double[] array, String[] names, double value, String prefixedName) {
    synchronized (highScoreLock) {
      int existingIndex = findExisting(names, prefixedName);

      if (existingIndex != -1 && array[existingIndex] >= value) {
        return;
      }

      if (existingIndex != -1) {
        shiftLeft(array, names, existingIndex);
      }

      insertSorted(array, names, value, prefixedName);
    }
  }

  private void insertIntoArray(int[] array, String[] names, int value, String prefixedName) {
    synchronized (highScoreLock) {
      int existingIndex = findExisting(names, prefixedName);

      if (existingIndex != -1 && array[existingIndex] >= value) {
        return;
      }

      if (existingIndex != -1) {
        shiftLeft(array, names, existingIndex);
      }

      insertSorted(array, names, value, prefixedName);
    }
  }

  private int findExisting(String[] names, String prefixedName) {
    for (int i = 0; i < topScoresSize; i++) {
      if (names[i] != null && names[i].equals(prefixedName)) {
        return i;
      }
    }
    return -1;
  }

  private void shiftLeft(double[] array, String[] names, int from) {
    for (int j = from; j < topScoresSize - 1; j++) {
      array[j] = array[j + 1];
      names[j] = names[j + 1];
    }
    array[topScoresSize - 1] = 0;
    names[topScoresSize - 1] = NOBODY.toString();
  }

  private void shiftLeft(int[] array, String[] names, int from) {
    for (int j = from; j < topScoresSize - 1; j++) {
      array[j] = array[j + 1];
      names[j] = names[j + 1];
    }
    array[topScoresSize - 1] = 0;
    names[topScoresSize - 1] = NOBODY.toString();
  }

  private void insertSorted(double[] array, String[] names, double value, String prefixedName) {
    for (int i = 0; i < topScoresSize; i++) {
      if (value > array[i]) {
        for (int j = topScoresSize - 1; j > i; j--) {
          array[j] = array[j - 1];
          names[j] = names[j - 1];
        }
        array[i] = value;
        names[i] = prefixedName;
        break;
      }
    }
  }

  private void insertSorted(int[] array, String[] names, int value, String prefixedName) {
    for (int i = 0; i < topScoresSize; i++) {
      if (value > array[i]) {
        for (int j = topScoresSize - 1; j > i; j--) {
          array[j] = array[j - 1];
          names[j] = names[j - 1];
        }
        array[i] = value;
        names[i] = prefixedName;
        break;
      }
    }
  }

  public enum HighScoreCategory {
    SKILL,
    GOALS,
    ASSISTS,
    OWN_GOALS,
    WINS,
    STREAK
  }
}
