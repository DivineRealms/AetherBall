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
    this.matches = data.getInt("matches");
    this.wins = data.getInt("wins");
    this.ties = data.getInt("ties");
    this.losses = data.getInt("losses");
    this.goals = data.getInt("goals");
    this.assists = data.getInt("assists");
    this.ownGoals = data.getInt("owngoals");
    this.bestWinStreak = data.getInt("bestwinstreak");
    this.skillLevel = calculateSkillLevel(matches, wins, ties, goals);
    this.rankName = calculateRankName((int) (skillLevel * 2.0 - 0.5));
  }

  private static String calculateRankName(int rank) {
    return switch (rank) {
      case 1 -> "Noob";
      case 2 -> "Loser";
      case 3 -> "Baby";
      case 4 -> "Pupil";
      case 5 -> "Bad";
      case 6 -> ":(";
      case 7 -> "Meh";
      case 8 -> "Player";
      case 9 -> "Okay";
      case 10 -> "Average";
      case 11 -> "Good";
      case 12 -> "Great";
      case 13 -> "King";
      case 14 -> "Super";
      case 15 -> "Pro";
      case 16 -> "Maradona";
      case 17 -> "Superman";
      case 18 -> "God";
      case 19 -> "h4x0r";
      default -> "None";
    };
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