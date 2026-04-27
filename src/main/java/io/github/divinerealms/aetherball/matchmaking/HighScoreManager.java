package io.github.divinerealms.aetherball.matchmaking;

import static io.github.divinerealms.aetherball.configs.Lang.BEST_ASSISTS;
import static io.github.divinerealms.aetherball.configs.Lang.BEST_ENTRY;
import static io.github.divinerealms.aetherball.configs.Lang.BEST_GOALS;
import static io.github.divinerealms.aetherball.configs.Lang.BEST_HEADER;
import static io.github.divinerealms.aetherball.configs.Lang.BEST_OWN_GOALS;
import static io.github.divinerealms.aetherball.configs.Lang.BEST_UPDATING;
import static io.github.divinerealms.aetherball.configs.Lang.BEST_WINS;
import static io.github.divinerealms.aetherball.configs.Lang.BEST_WINSTREAK;
import static io.github.divinerealms.aetherball.configs.Lang.NOBODY;
import static io.github.divinerealms.aetherball.configs.Lang.SIMPLE_FOOTER;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.matchmaking.player.StatsHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class HighScoreManager {

  private final Manager manager;
  private final Plugin plugin;
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
  @Getter
  private int totalPlayerFiles = 0;
  @Getter
  private int skippedCount = 0;
  @Getter
  private int processedCount = 0;
  @Getter
  private int topScoresSize;

  public HighScoreManager(Manager manager) {
    this.manager = manager;
    this.plugin = manager.getPlugin();
    this.utilities = manager.getUtilities();
    this.playerDataManager = manager.getDataManager();
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
      sendMessage(sender, BEST_ENTRY,
          String.valueOf(i + 1),
          names[i],
          String.valueOf(values[i])
      );
    }
  }

  private void showTopCategory(CommandSender sender, String[] names, int[] values) {
    for (int i = 0; i < topScoresSize; i++) {
      sendMessage(sender, BEST_ENTRY,
          String.valueOf(i + 1),
          names[i],
          String.valueOf(values[i])
      );
    }
  }

  public void startUpdate() {
    File playerFolder = new File(plugin.getDataFolder(), "players");
    File[] files = playerFolder.listFiles((dir, name) -> name.endsWith(".yml"));
    totalPlayerFiles = files != null ? files.length : 0;
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
      skippedCount = 0;
      processedCount = 0;

      for (int i = 0; i < topScoresSize; i++) {
        bestRatings[i] = 0.0;
        topSkillNames[i] = nobody;
      }

      for (int i = 0; i < topScoresSize; i++) {
        mostGoals[i] = 0;
        topGoalsNames[i] = nobody;
      }

      for (int i = 0; i < topScoresSize; i++) {
        mostAssists[i] = 0;
        topAssistsNames[i] = nobody;
      }

      for (int i = 0; i < topScoresSize; i++) {
        mostOwnGoals[i] = 0;
        topOwnGoalsNames[i] = nobody;
      }

      for (int i = 0; i < topScoresSize; i++) {
        mostWins[i] = 0;
        topWinsNames[i] = nobody;
      }

      for (int i = 0; i < topScoresSize; i++) {
        longestStreak[i] = 0;
        topStreakNames[i] = nobody;
      }
    }
  }

  public void processAllPlayers() {
    List<CompletableFuture<Void>> nameFutures = new ArrayList<>();

    for (String playerName : participants) {
      PlayerData data = playerDataManager.get(playerName);
      if (data == null || (int) data.get("matches") == 0) {
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
        insertTop5(bestRatings, topSkillNames, stats.getSkillLevel(), cachedPrefixedName);
        insertTop5(mostGoals, topGoalsNames, stats.getGoals(), cachedPrefixedName);
        insertTop5(mostAssists, topAssistsNames, stats.getAssists(), cachedPrefixedName);
        insertTop5(mostOwnGoals, topOwnGoalsNames, stats.getOwnGoals(), cachedPrefixedName);
        insertTop5(mostWins, topWinsNames, stats.getWins(), cachedPrefixedName);
        insertTop5(longestStreak, topStreakNames, stats.getBestWinStreak(), cachedPrefixedName);
      } else {
        CompletableFuture<Void> playerFuture = utilities.getPrefixedName(uuid, playerName)
            .thenAccept(prefixedName -> {
              insertTop5(bestRatings, topSkillNames, stats.getSkillLevel(), prefixedName);
              insertTop5(mostGoals, topGoalsNames, stats.getGoals(), prefixedName);
              insertTop5(mostAssists, topAssistsNames, stats.getAssists(), prefixedName);
              insertTop5(mostOwnGoals, topOwnGoalsNames, stats.getOwnGoals(), prefixedName);
              insertTop5(mostWins, topWinsNames, stats.getWins(), prefixedName);
              insertTop5(longestStreak, topStreakNames, stats.getBestWinStreak(), prefixedName);
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
      for (int i = 0; i < topScoresSize; i++) {
        if (names[i] != null && names[i].equals(prefixedName)) {
          existingIndex = i;
          break;
        }
      }

      if (existingIndex != -1 && array[existingIndex] >= value) {
        return;
      }

      if (existingIndex != -1) {
        for (int j = existingIndex; j < topScoresSize - 1; j++) {
          array[j] = array[j + 1];
          names[j] = names[j + 1];
        }
        array[topScoresSize - 1] = 0;
        names[topScoresSize - 1] = NOBODY.toString();
      }

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
  }

  private void insertIntoArray(int[] array, String[] names, int value, String prefixedName) {
    synchronized (highScoreLock) {
      int existingIndex = -1;
      for (int i = 0; i < topScoresSize; i++) {
        if (names[i] != null && names[i].equals(prefixedName)) {
          existingIndex = i;
          break;
        }
      }

      if (existingIndex != -1 && array[existingIndex] >= value) {
        return;
      }

      if (existingIndex != -1) {
        for (int j = existingIndex; j < topScoresSize - 1; j++) {
          array[j] = array[j + 1];
          names[j] = names[j + 1];
        }
        array[topScoresSize - 1] = 0;
        names[topScoresSize - 1] = NOBODY.toString();
      }

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
  }
}