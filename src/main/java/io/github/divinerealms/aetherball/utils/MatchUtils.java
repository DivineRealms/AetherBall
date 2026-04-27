package io.github.divinerealms.aetherball.utils;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchPhase;
import io.github.divinerealms.aetherball.matchmaking.ArenaManager;
import io.github.divinerealms.aetherball.matchmaking.player.MatchPlayer;
import io.github.divinerealms.aetherball.matchmaking.player.TeamColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import static io.github.divinerealms.aetherball.configs.Lang.*;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.*;

public class MatchUtils {

  public static ItemStack createColoredArmor(Material material, org.bukkit.Color color) {
    ItemStack itemStack = new ItemStack(material);
    ItemMeta itemMeta = itemStack.getItemMeta();
    if (itemMeta instanceof LeatherArmorMeta leatherMeta) {
      leatherMeta.setColor(color);
      itemStack.setItemMeta(leatherMeta);
    }

    return itemStack;
  }

  public static void giveArmor(Player player, TeamColor color) {
    ItemStack chestplate = createColoredArmor(Material.LEATHER_CHESTPLATE, color == TeamColor.RED
        ? Color.RED
        : Color.BLUE);
    ItemStack leggings = createColoredArmor(Material.LEATHER_LEGGINGS, color == TeamColor.RED
        ? Color.RED
        : Color.BLUE);

    PlayerInventory inventory = player.getInventory();
    inventory.setChestplate(chestplate);
    inventory.setLeggings(leggings);
  }

  public static void clearPlayer(Player player) {
    if (!isPlayerOnline(player)) {
      return;
    }

    player.getInventory().setArmorContents(null);
    player.getInventory().clear();
    player.setExp(0);
    player.setLevel(0);
  }

  public static List<String> getFormattedMatches(List<Match> matches) {
    List<String> output = new ArrayList<>();
    if (matches == null) {
      return output;
    }

    boolean firstBlock = true;

    for (Match match : matches) {
      if (match == null || match.getPlayers() == null) {
        continue;
      }

      boolean allNull = true;
      for (MatchPlayer matchPlayer : match.getPlayers()) {
        if (isPlayerOnline(matchPlayer)) {
          allNull = false;
          break;
        }
      }

      if (allNull) {
        continue;
      }

      if (!firstBlock) {
        output.add("");
      }

      firstBlock = false;
      String type = Settings.getMatchTypeName(match.getArena().type());

      List<String> redPlayers = new ArrayList<>();
      List<String> bluePlayers = new ArrayList<>();
      List<String> waitingPlayers = new ArrayList<>();

      for (MatchPlayer matchPlayer : match.getPlayers()) {
        if (!isPlayerOnline(matchPlayer)) {
          continue;
        }

        String name = matchPlayer.getPlayer().getName();
        if (matchPlayer.getTeamColor() == TeamColor.RED) {
          redPlayers.add(name);
        } else {
          if (matchPlayer.getTeamColor() == TeamColor.BLUE) {
            bluePlayers.add(name);
          } else {
            if (matchPlayer.getTeamColor() == null) {
              waitingPlayers.add(name);
            }
          }
        }
      }

      String timeDisplay;
      if (match.getPhase() == MatchPhase.LOBBY) {
        timeDisplay = MATCHES_LIST_WAITING.toString();
      } else if (match.getPhase() == MatchPhase.STARTING) {
        timeDisplay = MATCHES_LIST_STARTING.replace(String.valueOf(match.getCountdown()));
      } else {
        long matchDuration = Settings.getMatchDuration(match.getArena().type());
        long elapsedMillis =
            System.currentTimeMillis() - match.getStartTime();
        long remainingSeconds = matchDuration - TimeUnit.MILLISECONDS.toSeconds(elapsedMillis);

        timeDisplay = Utilities.formatTimePretty((int) remainingSeconds);
      }

      if (match.getPhase() == MatchPhase.LOBBY) {
        output.add(MATCHES_LIST_LOBBY.replace(type, String.valueOf(match.getArena().id())));
        output.add(MATCHES_LIST_WAITINGPLAYERS.replace(waitingPlayers.isEmpty()
            ? "/"
            : joinStrings(waitingPlayers)));
        output.add(MATCHES_LIST_STATUS.replace(timeDisplay));
      } else {
        if (match.getPhase() == MatchPhase.STARTING) {
          output.add(MATCHES_LIST_LOBBY.replace(type, String.valueOf(match.getArena().id())));
          output.add(MATCHES_LIST_REDPLAYERS.replace(redPlayers.isEmpty()
              ? "/"
              : joinStrings(redPlayers)));
          output.add(MATCHES_LIST_BLUEPLAYERS.replace(bluePlayers.isEmpty()
              ? "/"
              : joinStrings(bluePlayers)));
          output.add(MATCHES_LIST_STATUS.replace(timeDisplay));
        } else {
          output.add(MATCHES_LIST_MATCH.replace(type, String.valueOf(match.getArena().id())));
          output.add(MATCHES_LIST_RESULT.replace(
              String.valueOf(match.getScoreRed()),
              String.valueOf(match.getScoreBlue()),
              MATCHES_LIST_TIMELEFT.replace(timeDisplay))
          );
          output.add(MATCHES_LIST_REDPLAYERS.replace(redPlayers.isEmpty()
              ? "/"
              : joinStrings(redPlayers)));
          output.add(MATCHES_LIST_BLUEPLAYERS.replace(bluePlayers.isEmpty()
              ? "/"
              : joinStrings(bluePlayers)));
        }
      }
    }
    return output;
  }

  public static String joinStrings(List<String> list) {
    if (list == null || list.isEmpty()) {
      return "/";
    }

    StringJoiner joiner = new StringJoiner(", ");
    for (String s : list) {
      if (s != null) {
        joiner.add(s);
      }
    }

    return joiner.toString();
  }

  public static void displayGoalMessage(Player player, String style, boolean ownGoal,
                                        boolean isHatTrick, boolean isViewerScorer,
                                        String scorerName, String assistText,
                                        String teamColorText, double distance,
                                        Match match, Plugin plugin) {
    String distanceString = String.format("%.0f", distance);
    String redTeam = RED.toString(), blueTeam = BLUE.toString();

    switch (style) {
      case "epic":
        displayEpicGoal(player, ownGoal, isHatTrick, isViewerScorer, scorerName, assistText,
            teamColorText, distanceString, redTeam, blueTeam, match, plugin);
        break;

      case "simple":
        displaySimpleGoal(player, ownGoal, scorerName, distanceString);
        break;

      case "minimal":
        displayMinimalGoal(player, ownGoal, scorerName, redTeam, blueTeam, match);
        break;

      default:
        displayDefaultGoal(player, ownGoal, isHatTrick, isViewerScorer, scorerName, assistText,
            teamColorText, distanceString, redTeam, blueTeam, match);
        break;
    }
  }

  public static boolean isValidGoalMessage(String style) {
    return style != null && (style.equals("default") || style.equals("epic")
        || style.equals("simple") || style.equals("minimal") || style.equals("custom"));
  }

  private static void displayEpicGoal(Player player, boolean ownGoal, boolean isHatTrick,
                                      boolean isViewerScorer, String scorerName, String assistText,
                                      String teamColorText, String distance, String redTeam,
                                      String blueTeam, Match match, Plugin plugin) {
    String initialTitle = ownGoal
        ? GM_EPIC_TITLE_1.toString()
        : isHatTrick
        ? GM_EPIC_TITLE_1_HATTY.toString()
        : GM_EPIC_TITLE_1_GOAL.toString();

    String initialSubtitle = ownGoal
        ? GM_EPIC_SUBTITLE_1.toString()
        : isViewerScorer
        ? GM_EPIC_SUBTITLE_1_SCORER.toString()
        : GM_EPIC_SUBTITLE_1_OTHER.replace(scorerName);

    title(player, initialTitle, initialSubtitle, 5, 35, 10);

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      String secondTitle = ownGoal
          ? GM_EPIC_TITLE_2.replace(teamColorText)
          : GM_EPIC_TITLE_2_GOAL.replace(teamColorText);

      String secondSubtitle = GM_EPIC_SUBTITLE_2
          .replace(
              scorerName,
              assistText.isEmpty()
                  ? ""
                  : assistText
          );

      title(player, secondTitle, secondSubtitle, 0, 35, 10);
    }, 40L);

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      String thirdTitle = GM_EPIC_TITLE_3.replace(distance);

      String thirdSubtitle = GM_EPIC_SUBTITLE_3.replace(
          redTeam,
          String.valueOf(match.getScoreRed()),
          String.valueOf(match.getScoreBlue()),
          blueTeam
      );

      title(player, thirdTitle, thirdSubtitle, 0, 30, 10);
    }, 80L);
  }

  private static void displaySimpleGoal(Player player, boolean ownGoal,
                                        String scorerName, String distance) {
    String title = ownGoal
        ? GM_SIMPLE_TITLE.toString()
        : GM_SIMPLE_TITLE_GOAL.toString();

    String subtitle = GM_SIMPLE_SUBTITLE.replace(scorerName, distance);

    title(player, title, subtitle, 5, 40, 10);
  }

  private static void displayMinimalGoal(Player player, boolean ownGoal,
                                         String scorerName, String redTeam,
                                         String blueTeam, Match match) {
    sendActionBar(player, ownGoal
            ? GM_MINIMAL_OWN
            : GM_MINIMAL_GOAL,
        scorerName, redTeam,
        String.valueOf(match.getScoreRed()),
        String.valueOf(match.getScoreBlue()),
        blueTeam
    );
  }

  private static void displayDefaultGoal(Player player, boolean ownGoal,
                                         boolean isHatTrick, boolean isViewerScorer,
                                         String scorerName, String assistText,
                                         String teamColorText, String distance,
                                         String redTeam, String blueTeam,
                                         Match match) {
    String title = ownGoal
        ? GM_DEFAULT_TITLE_OWN.toString()
        : isHatTrick
        ? GM_DEFAULT_TITLE_HATTY.toString()
        : isViewerScorer
        ? GM_DEFAULT_TITLE_SCORER.toString()
        : GM_DEFAULT_TITLE_GOAL.toString();

    String subtitle = ownGoal
        ? GM_DEFAULT_SUBTITLE_OWN.replace(scorerName, teamColorText)
        : GM_DEFAULT_SUBTITLE_GOAL.replace(scorerName, distance, assistText.isEmpty()
        ? ""
        : assistText);

    title(player, title, subtitle, 10, 50, 10);

    sendActionBar(player, GM_DEFAULT_ACTIONBAR,
        redTeam, String.valueOf(match.getScoreRed()),
        String.valueOf(match.getScoreBlue()), blueTeam
    );
  }

  public static boolean isPlayerOnline(Player player) {
    return player != null && player.isOnline() && player.isValid();
  }

  public static boolean isPlayerOnline(MatchPlayer matchPlayer) {
    return matchPlayer != null && isPlayerOnline(matchPlayer.getPlayer());
  }

  private static boolean isHatTrickGoal(ScoringResult result) {
    return !result.ownGoal()
        && result.scorer() != null
        && result.scorer().getPlayer() != null
        && result.scorer().getGoals() > 0
        && result.scorer().getGoals() % 3 == 0;
  }

  private static double calculateScoringDistance(ScoringResult result,
                                                 Location goalLoc) {
    if (result.scorer() != null && result.scorer().getPlayer() != null) {
      return result.scorer().getPlayer().getLocation().distance(goalLoc);
    }
    return 0;
  }

  private static String getGoalMessageStyle(PlayerSettings settings) {
    if (settings != null && isValidGoalMessage(settings.getGoalMessage())) {
      return settings.getGoalMessage();
    }
    return "default";
  }

  public static ScoringResult determineScoringPlayers(Match match,
                                                      TeamColor scoringTeam, boolean shouldCountStats) {
    MatchPlayer scorer = match.getLastTouch();
    MatchPlayer assister = null;
    boolean ownGoal = false;

    if (scorer == null) {
      return new ScoringResult(null, null,
          false, scoringTeam);
    }

    MatchPlayer secondLastTouch = match.getSecondLastTouch();

    if (scorer.getTeamColor() != scoringTeam) {
      if (secondLastTouch != null && secondLastTouch.getTeamColor() == scoringTeam) {
        scorer = secondLastTouch;
      } else {
        ownGoal = true;
        if (shouldCountStats) {
          scorer.incrementOwnGoals();
        }
      }
    }

    if (!ownGoal && secondLastTouch != null
        && secondLastTouch.getTeamColor() == scoringTeam
        && !scorer.equals(secondLastTouch)) {
      assister = secondLastTouch;
      if (shouldCountStats) {
        assister.incrementAssists();
      }
    }

    if (!ownGoal && shouldCountStats) {
      scorer.incrementGoals();
    }

    return new ScoringResult(scorer, assister, ownGoal, scoringTeam);
  }

  public static void awardCreditsForGoal(ScoringResult result,
                                         Manager manager) {
    MatchPlayer scorer = result.scorer();
    MatchPlayer assister = result.assister();

    if (isPlayerOnline(scorer)) {
      Player scoringPlayer = scorer.getPlayer();
      sendMessage(scoringPlayer, MATCH_SCORE_CREDITS);
      manager.getEconomy().depositPlayer(scoringPlayer, Settings.ECONOMY_GOAL.asDouble());

      if (scorer.getGoals() > 0 && scorer.getGoals() % 3 == 0) {
        sendMessage(scoringPlayer, MATCH_SCORE_HATTRICK);
        manager.getEconomy().depositPlayer(scoringPlayer, Settings.ECONOMY_HAT_TRICK.asDouble());
      }
    }

    if (isPlayerOnline(assister)) {
      Player assistingPlayer = assister.getPlayer();
      sendMessage(assistingPlayer, MATCH_ASSIST_CREDITS);
      manager.getEconomy().depositPlayer(assistingPlayer, Settings.ECONOMY_ASSIST.asDouble());
    }
  }

  public static void playGoalEffects(Match match, Location goalLoc, Manager manager) {
    for (MatchPlayer matchPlayer : match.getPlayers()) {
      if (!isPlayerOnline(matchPlayer)) {
        continue;
      }

      Player player = matchPlayer.getPlayer();

      PlayerSettings playerSettings = manager.getPlayerSettings(player);
      if (playerSettings != null && playerSettings.isGoalSoundEnabled()) {
        player.playSound(player.getLocation(), playerSettings.getGoalSound(), 1, 1);
      }

      player.playEffect(goalLoc, Effect.EXPLOSION_HUGE, null);

      double distanceToGoal = player.getLocation().distance(goalLoc);
      if (distanceToGoal <= 30) {
        Vector launchDir = player.getLocation().toVector()
            .subtract(goalLoc.toVector())
            .normalize()
            .setY(0.5)
            .multiply(1.5);
        player.setVelocity(launchDir);
      }
    }
  }

  public static void broadcastGoalMessage(Match match, ScoringResult result,
                                          Location goalLoc, Manager manager) {
    String prefixedScorer = NOBODY.toString();
    String prefixedAssister = null;

    if (result.scorer() != null && result.scorer().getPlayer() != null) {
      Player scorer = result.scorer().getPlayer();
      prefixedScorer = manager.getPrefixedName(scorer.getUniqueId());
      if (prefixedScorer == null) {
        prefixedScorer = scorer.getName();
        manager.cachePrefixedName(scorer);
      }
    }

    if (result.assister() != null && result.assister().getPlayer() != null) {
      Player assister = result.assister().getPlayer();
      prefixedAssister = manager.getPrefixedName(assister.getUniqueId());
      if (prefixedAssister == null) {
        prefixedAssister = assister.getName();
        manager.cachePrefixedName(assister);
      }
    }

    sendGoalMessages(match, result, prefixedScorer, prefixedAssister, goalLoc, manager);
  }

  private static void sendGoalMessages(Match match, ScoringResult result,
                                       String prefixedScorer, String prefixedAssister,
                                       Location goalLoc, Manager manager) {
    boolean isHatTrick = isHatTrickGoal(result);
    String teamColorText = result.scoringTeam() == TeamColor.RED
        ? RED.toString()
        : BLUE.toString();

    double distance = calculateScoringDistance(result, goalLoc);
    String assistText = prefixedAssister != null
        ? GM_ASSISTS_TEXT.replace(prefixedAssister)
        : "";

    String goalMessage = result.ownGoal()
        ? MATCH_SCORE_OWN_GOAL_ANNOUNCE.replace(prefixedScorer, teamColorText)
        : MATCH_GOAL.replace(
        isHatTrick
            ? MATCH_HATTRICK.toString()
            : MATCH_GOALLL.toString(),
        prefixedScorer,
        teamColorText,
        String.format("%.0f", distance),
        assistText
    );

    String goalMessageStyle = "default";
    if (result.scorer() != null && result.scorer().getPlayer() != null) {
      PlayerSettings scorerSettings = manager.getPlayerSettings(result.scorer().getPlayer());
      goalMessageStyle = getGoalMessageStyle(scorerSettings);
    }

    for (MatchPlayer matchPlayer : match.getPlayers()) {
      if (!isPlayerOnline(matchPlayer)) {
        continue;
      }

      Player player = matchPlayer.getPlayer();
      boolean isViewerScorer = matchPlayer.equals(result.scorer());

      displayGoalMessage(player, goalMessageStyle, result.ownGoal(), isHatTrick,
          isViewerScorer, prefixedScorer, assistText, teamColorText,
          distance, match, manager.getPlugin()
      );

      sendMessage(player, goalMessage);
      sendMessage(player, MATCH_SCORE_STATS,
          String.valueOf(match.getScoreRed()),
          String.valueOf(match.getScoreBlue())
      );
    }
  }

  public static void preparePlayer(Player player, TeamColor color, ArenaManager.Arena arena,
                                   PlayerSettings playerSettings) {
    giveArmor(player, color);
    preventPlayerAbuse(player, playerSettings);
    player.teleport(color == TeamColor.RED ? arena.redSpawn() : arena.blueSpawn());
  }

  public static boolean preventPlayerAbuse(Player player, PlayerSettings playerSettings) {
    if (player.getGameMode() != GameMode.SURVIVAL) {
      player.setGameMode(GameMode.SURVIVAL);
      return true;
    }

    if (player.isFlying() || player.getAllowFlight()) {
      player.setFlying(false);
      player.setAllowFlight(false);
      return true;
    }

    if (playerSettings.isBuildEnabled()) {
      playerSettings.setBuildEnabled(false);
      return true;
    }

    return false;
  }

  public static boolean shouldPreventAbuse(MatchPhase matchPhase) {
    return matchPhase == MatchPhase.STARTING || matchPhase == MatchPhase.IN_PROGRESS
        || matchPhase == MatchPhase.CONTINUING;
  }

  /**
   * Represents the result of determining who scored and assisted. This is a traditional Java class
   * that holds immutable data about a scoring event.
   */
  public record ScoringResult(MatchPlayer scorer, MatchPlayer assister, boolean ownGoal, TeamColor scoringTeam) {

    /**
     * Determines if credits should be awarded for this goal. Credits are only awarded for regular
     * goals, not own goals.
     */
    public boolean shouldAwardCredits() {
      return !ownGoal && scorer != null;
    }
  }
}