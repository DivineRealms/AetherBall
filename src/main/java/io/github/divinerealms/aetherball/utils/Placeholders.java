package io.github.divinerealms.aetherball.utils;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.matchmaking.HighScoreManager;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.matchmaking.player.StatsHelper;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;

public class Placeholders extends PlaceholderExpansion {

  private final PluginDescriptionFile pluginDescriptionFile;
  private final MatchManager matchManager;
  private final HighScoreManager highscoreManager;
  private final PlayerDataManager dataManager;

  public Placeholders(Manager manager) {
    this.pluginDescriptionFile = manager.getPlugin().getDescription();
    this.matchManager = manager.getMatchManager();
    this.highscoreManager = manager.getHighscoreManager();
    this.dataManager = manager.getDataManager();
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
      return String.valueOf(matchManager.countActiveLobbies(parseTypeFromSuffix(identifier, "active_lobbies_")));
    }

    if (identifier.startsWith("players_")) {
      return String.valueOf(matchManager.countPlayersInMatches(parseTypeFromSuffix(identifier, "players_")));
    }

    if (identifier.startsWith("waiting_")) {
      return String.valueOf(matchManager.countWaitingPlayers(parseTypeFromSuffix(identifier, "waiting_")));
    }

    if (identifier.startsWith("listplayers_")) {
      return matchManager.listPlayersInMatches(parseTypeFromSuffix(identifier, "listplayers_"));
    }

    if (identifier.startsWith("best_")) {
      return resolveBestPlaceholder(identifier);
    }

    if (identifier.startsWith("stats_") && player != null) {
      return resolveStatPlaceholder(player, identifier);
    }

    return null;
  }

  private String resolveBestPlaceholder(String identifier) {
    if (highscoreManager == null || !highscoreManager.isHasInitialData()) {
      return "---";
    }

    String[] parts = identifier.split("_");
    if (parts.length != 4) {
      return null;
    }

    int rank;
    try {
      rank = Integer.parseInt(parts[2]) - 1; // config is 1-indexed
    } catch (NumberFormatException e) {
      return null;
    }

    HighScoreManager.HighScoreCategory category = switch (parts[1]) {
      case "rating" -> HighScoreManager.HighScoreCategory.SKILL;
      case "goals" -> HighScoreManager.HighScoreCategory.GOALS;
      case "assists" -> HighScoreManager.HighScoreCategory.ASSISTS;
      case "owngoals" -> HighScoreManager.HighScoreCategory.OWN_GOALS;
      case "wins" -> HighScoreManager.HighScoreCategory.WINS;
      case "streak" -> HighScoreManager.HighScoreCategory.STREAK;
      default -> null;
    };

    if (category == null) return "---";

    return switch (parts[3]) {
      case "name" -> highscoreManager.getTopName(category, rank);
      case "value" -> highscoreManager.getTopValue(category, rank);
      default -> "---";
    };
  }

  private String resolveStatPlaceholder(Player player, String identifier) {
    String statKey = identifier.replace("stats_", "").toLowerCase();
    PlayerData data = dataManager.get(player);

    if (data == null || !data.has("matches")) {
      return "---";
    }

    StatsHelper stats = new StatsHelper(data);

    return switch (statKey) {
      case "matches" -> String.valueOf(stats.getMatches());
      case "wins" -> String.valueOf(stats.getWins());
      case "losses" -> String.valueOf(stats.getLosses());
      case "ties" -> String.valueOf(stats.getTies());
      case "winspermatch" -> String.format("%.2f", stats.getWinsPerMatch());
      case "goals" -> String.valueOf(stats.getGoals());
      case "owngoals" -> String.valueOf(stats.getOwnGoals());
      case "assists" -> String.valueOf(stats.getAssists());
      case "goalspermatch" -> String.format("%.2f", stats.getGoalsPerMatch());
      case "bestwinstreak" -> String.valueOf(stats.getBestWinStreak());
      case "skill" -> String.format("%.2f", stats.getSkillLevel());
      case "rank" -> stats.getRankName();
      default -> "---";
    };
  }

  private int parseTypeFromSuffix(String identifier, String prefix) {
    String suffix = identifier.replace(prefix, "");
    return Integer.parseInt(suffix.substring(0, 1));
  }
}
