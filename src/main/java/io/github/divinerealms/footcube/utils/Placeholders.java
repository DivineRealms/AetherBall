package io.github.divinerealms.footcube.utils;

import io.github.divinerealms.footcube.configs.PlayerData;
import io.github.divinerealms.footcube.configs.Settings;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.managers.PlayerDataManager;
import io.github.divinerealms.footcube.matchmaking.Match;
import io.github.divinerealms.footcube.matchmaking.MatchManager;
import io.github.divinerealms.footcube.matchmaking.highscore.HighScoreManager;
import io.github.divinerealms.footcube.matchmaking.player.StatsHelper;
import java.util.Queue;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

public class Placeholders extends PlaceholderExpansion {

  private final PluginDescriptionFile pluginDescriptionFile;
  private final MatchManager matchManager;
  private final HighScoreManager highscoreManager;
  private final PlayerDataManager dataManager;

  public Placeholders(FCManager fcManager) {
    this.pluginDescriptionFile = fcManager.getPlugin().getDescription();
    this.matchManager = fcManager.getMatchManager();
    this.highscoreManager = fcManager.getHighscoreManager();
    this.dataManager = fcManager.getDataManager();
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public boolean canRegister() {
    return true;
  }

  @Override
  public @NotNull String getIdentifier() {
    return "fc";
  }

  @Override
  public @NotNull String getAuthor() {
    return "neonsh";
  }

  @Override
  public @NotNull String getVersion() {
    return pluginDescriptionFile.getVersion();
  }

  @Override
  public String onPlaceholderRequest(Player player, @NotNull String identifier) {
    if (identifier.equals("enabled")) {
      return matchManager.getData().isMatchesEnabled() ? "YES" : "NO";
    }

    if (identifier.equals("active_lobbies_all")) {
      int count = 0;
      for (int type : Settings.getEnabledMatchTypes()) {
        count += matchManager.countActiveLobbies(type);
      }

      return String.valueOf(count);
    }

    if (identifier.equalsIgnoreCase("active_players_all")) {
      int playersInMatches = 0;
      if (matchManager.getData().getMatches() != null) {
        for (Match match : matchManager.getData().getMatches()) {
          if (match == null) {
            continue;
          }

          if (match.getPlayers() == null) {
            continue;
          }

          playersInMatches += match.getPlayers().size();
        }
      }

      int playersInQueues = 0;
      if (matchManager.getData().getPlayerQueues() != null) {
        for (Queue<Player> queue : matchManager.getData().getPlayerQueues().values()) {
          if (queue == null) {
            continue;
          }

          playersInQueues += queue.size();
        }
      }

      return String.valueOf(playersInMatches + playersInQueues);
    }

    if (identifier.startsWith("active_lobbies_")) {
      String type = identifier.replace("active_lobbies_", "");
      return String.valueOf(
          matchManager.countActiveLobbies(Integer.parseInt(type.substring(0, 1))));
    }

    if (identifier.startsWith("players_")) {
      String type = identifier.replace("players_", "");
      return String.valueOf(
          matchManager.countPlayersInMatches(Integer.parseInt(type.substring(0, 1))));
    }

    if (identifier.startsWith("waiting_")) {
      String type = identifier.replace("waiting_", "");
      return String.valueOf(
          matchManager.countWaitingPlayers(Integer.parseInt(type.substring(0, 1))));
    }

    if (identifier.startsWith("listplayers_")) {
      String type = identifier.replace("listplayers_", "");
      return matchManager.listPlayersInMatches(Integer.parseInt(type.substring(0, 1)));
    }

    if (identifier.startsWith("best_")) {
      if (highscoreManager == null || highscoreManager.topSkillNames == null) {
        return "---";
      }

      String[] parts = identifier.split("_");
      if (parts.length != 4) {
        return null;
      }

      String category = parts[1];
      int rank;
      try {
        rank = Integer.parseInt(parts[2]) - 1;
      } catch (NumberFormatException e) {
        return null;
      }

      if (rank < 0 || rank > 4) {
        return null;
      }

      switch (category) {
        case "rating":
          if ("name".equals(parts[3])) {
            return highscoreManager.topSkillNames[rank];
          }
          if ("value".equals(parts[3])) {
            return String.valueOf(highscoreManager.bestRatings[rank]);
          }
          break;
        case "goals":
          if ("name".equals(parts[3])) {
            return highscoreManager.topGoalsNames[rank];
          }
          if ("value".equals(parts[3])) {
            return String.valueOf(highscoreManager.mostGoals[rank]);
          }
          break;
        case "assists":
          if ("name".equals(parts[3])) {
            return highscoreManager.topAssistsNames[rank];
          }
          if ("value".equals(parts[3])) {
            return String.valueOf(highscoreManager.mostAssists[rank]);
          }
          break;
        case "owngoals":
          if ("name".equals(parts[3])) {
            return highscoreManager.topOwnGoalsNames[rank];
          }
          if ("value".equals(parts[3])) {
            return String.valueOf(highscoreManager.mostOwnGoals[rank]);
          }
          break;
        case "wins":
          if ("name".equals(parts[3])) {
            return highscoreManager.topWinsNames[rank];
          }
          if ("value".equals(parts[3])) {
            return String.valueOf(highscoreManager.mostWins[rank]);
          }
          break;
        case "streak":
          if ("name".equals(parts[3])) {
            return highscoreManager.topStreakNames[rank];
          }
          if ("value".equals(parts[3])) {
            return String.valueOf(highscoreManager.longestStreak[rank]);
          }
          break;
      }
      return "---";
    }

    if (identifier.startsWith("stats_") && player != null) {
      String statKey = identifier.replace("stats_", "").toLowerCase();
      PlayerData data = dataManager.get(player);
      if (data == null || !data.has("matches")) {
        return "---";
      }

      StatsHelper stats = new StatsHelper(data);

      switch (statKey) {
        case "matches":
          return String.valueOf(stats.getMatches());
        case "wins":
          return String.valueOf(stats.getWins());
        case "losses":
          return String.valueOf(stats.getLosses());
        case "ties":
          return String.valueOf(stats.getTies());
        case "winspermatch":
          return String.format("%.2f", stats.getWinsPerMatch());
        case "goals":
          return String.valueOf(stats.getGoals());
        case "owngoals":
          return String.valueOf(stats.getOwnGoals());
        case "assists":
          return String.valueOf(stats.getAssists());
        case "goalspermatch":
          return String.format("%.2f", stats.getGoalsPerMatch());
        case "bestwinstreak":
          return String.valueOf(stats.getBestWinStreak());
        case "skill":
          return String.format("%.2f", stats.getSkillLevel());
        case "rank":
          return stats.getRankName();
        default:
          return "---";
      }
    }

    return null;
  }
}
