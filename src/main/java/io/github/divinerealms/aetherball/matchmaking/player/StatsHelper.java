package io.github.divinerealms.aetherball.matchmaking.player;

import io.github.divinerealms.aetherball.configs.PlayerData;
import lombok.Getter;

@Getter
public class StatsHelper {

  private final int matches;
  private final int wins;
  private final int ties;
  private final int losses;
  private final int goals;
  private final int assists;
  private final int ownGoals;
  private final int bestWinStreak;
  private final double skillLevel;
  private final String rankName;

  public StatsHelper(PlayerData data) {
    this.matches = (int) data.get("matches");
    this.wins = (int) data.get("wins");
    this.ties = (int) data.get("ties");
    this.losses = (int) data.get("losses");
    this.goals = (int) data.get("goals");
    this.assists = (int) data.get("assists");
    this.ownGoals = (int) data.get("owngoals");
    this.bestWinStreak = (int) data.get("bestwinstreak");
    this.skillLevel = calculateSkillLevel(matches, wins, ties, goals);
    this.rankName = calculateRankName((int) (skillLevel * 2.0 - 0.5));
  }

  private static String calculateRankName(int rank) {
    switch (rank) {
      case 1:
        return "Noob";
      case 2:
        return "Loser";
      case 3:
        return "Baby";
      case 4:
        return "Pupil";
      case 5:
        return "Bad";
      case 6:
        return ":(";
      case 7:
        return "Meh";
      case 8:
        return "Player";
      case 9:
        return "Okay";
      case 10:
        return "Average";
      case 11:
        return "Good";
      case 12:
        return "Great";
      case 13:
        return "King";
      case 14:
        return "Super";
      case 15:
        return "Pro";
      case 16:
        return "Maradona";
      case 17:
        return "Superman";
      case 18:
        return "God";
      case 19:
        return "h4x0r";
      default:
        return "None";
    }
  }

  private static double calculateSkillLevel(int matches, int wins, int ties, int goals) {
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
    return Math.min(5 + goalBonus + addition * multiplier, 10);
  }

  public double getWinsPerMatch() {
    return matches > 0 ? (double) wins / matches : 0;
  }

  public double getGoalsPerMatch() {
    return matches > 0 ? (double) goals / matches : 0;
  }
}