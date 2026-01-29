package io.github.divinerealms.footcube.utils;

import static io.github.divinerealms.footcube.configs.Lang.FC_DISABLED;
import static io.github.divinerealms.footcube.configs.Lang.JOIN_ALREADYINGAME;
import static io.github.divinerealms.footcube.configs.Lang.JOIN_NOARENA;
import static io.github.divinerealms.footcube.configs.Lang.LEAVE_LOSING;
import static io.github.divinerealms.footcube.configs.Lang.LEAVE_NOT_INGAME;
import static io.github.divinerealms.footcube.configs.Lang.LEFT;
import static io.github.divinerealms.footcube.configs.Lang.MATCH_TYPE_UNAVAILABLE;
import static io.github.divinerealms.footcube.configs.Lang.TAKEPLACE_AVAILABLE_ENTRY;
import static io.github.divinerealms.footcube.configs.Lang.TAKEPLACE_AVAILABLE_HEADER;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_ACCEPT_OTHER;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_ACCEPT_SELF;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_ALREADY_IN_GAME;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_ALREADY_IN_TEAM;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_ALREADY_IN_TEAM_2;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_DECLINE_OTHER;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_DECLINE_SELF;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_NOT_ONLINE;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_NO_REQUEST;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_WANTS_TO_TEAM_OTHER;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_WANTS_TO_TEAM_SELF;
import static io.github.divinerealms.footcube.matchmaking.util.MatchUtils.isPlayerOnline;

import io.github.divinerealms.footcube.configs.Settings;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.managers.Utilities;
import io.github.divinerealms.footcube.matchmaking.Match;
import io.github.divinerealms.footcube.matchmaking.MatchManager;
import io.github.divinerealms.footcube.matchmaking.player.MatchPlayer;
import io.github.divinerealms.footcube.matchmaking.player.TeamColor;
import io.github.divinerealms.footcube.matchmaking.team.TeamManager;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;

public class GameCommandsHelper {

  public static void joinQueue(Player player, int type, FCManager fcManager) {
    Logger logger = fcManager.getLogger();
    MatchManager matchManager = fcManager.getMatchManager();

    if (!Settings.isMatchTypeEnabled(type)) {
      logger.send(player, MATCH_TYPE_UNAVAILABLE, Settings.getMatchTypeName(type),
          matchManager.getAvailableTypesString());
      return;
    }

    if (!matchManager.getData().isMatchesEnabled()) {
      logger.send(player, FC_DISABLED);
      return;
    }

    if (fcManager.getBanManager().isBanned(player)) {
      return;
    }

    if (isInQueueOrMatch(player, fcManager)) {
      logger.send(player, JOIN_ALREADYINGAME);
      return;
    }

    if (!fcManager.getArenaManager().hasArenaForType(type)) {
      logger.send(player, JOIN_NOARENA);
      return;
    }

    matchManager.joinQueue(player, type);
  }

  public static void handleInProgressLeave(Player player, Match match, FCManager fcManager) {
    MatchPlayer matchPlayer = null;
    for (MatchPlayer mp : match.getPlayers()) {
      if (isPlayerOnline(mp) && mp.getPlayer().equals(player)) {
        matchPlayer = mp;
        break;
      }
    }

    if (matchPlayer != null) {
      int playerScore =
          matchPlayer.getTeamColor() == TeamColor.RED ? match.getScoreRed() : match.getScoreBlue();
      int opponentScore =
          matchPlayer.getTeamColor() == TeamColor.RED ? match.getScoreBlue() : match.getScoreRed();

      if (playerScore < opponentScore) {
        double rageQuitPenalty = Settings.BAN_RAGEQUIT_PENALTY.asDouble();
        long rageQuitBanDuration = Settings.getRageQuitBanDuration();
        fcManager.getEconomy().withdrawPlayer(player, rageQuitPenalty);
        fcManager.getBanManager().banPlayer(player, rageQuitBanDuration);
        long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(rageQuitBanDuration);
        fcManager.getLogger().send(player, LEAVE_LOSING, String.format("%.0f", rageQuitPenalty),
            Utilities.formatTime(secondsLeft));
      }
    }
  }

  public static void handleQueueLeave(Player player, FCManager fcManager) {
    boolean leftQueue = false;
    MatchManager matchManager = fcManager.getMatchManager();
    for (int queueType : matchManager.getData().getPlayerQueues().keySet()) {
      Queue<Player> queue = matchManager.getData().getPlayerQueues().get(queueType);
      if (queue != null && queue.contains(player)) {
        matchManager.leaveQueue(player, queueType);
        leftQueue = true;
        fcManager.getTeamManager().disbandTeamIfInLobby(player);
      }
    }

    Logger logger = fcManager.getLogger();
    if (leftQueue) {
      logger.send(player, LEFT);
    } else {
      logger.send(player, LEAVE_NOT_INGAME);
    }
  }

  public static void showOpenMatches(Player player, List<Match> openMatches, FCManager fcManager) {
    Logger logger = fcManager.getLogger();
    logger.send(player, TAKEPLACE_AVAILABLE_HEADER);

    for (Match openMatch : openMatches) {
      int emptySlots = 0;
      for (MatchPlayer mp : openMatch.getPlayers()) {
        if (mp == null) {
          emptySlots++;
        }
      }

      logger.send(player, TAKEPLACE_AVAILABLE_ENTRY, String.valueOf(openMatch.getArena().getId()),
          Settings.getMatchTypeName(openMatch.getArena().getType()),
          String.valueOf(emptySlots));
    }
  }

  public static void handleAccept(Player player, FCManager fcManager) {
    Logger logger = fcManager.getLogger();
    TeamManager teamManager = fcManager.getTeamManager();

    if (teamManager.isInTeam(player)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM);
      return;
    }

    if (teamManager.noInvite(player)) {
      logger.send(player, TEAM_NO_REQUEST);
      return;
    }

    if (isInQueueOrMatch(player, fcManager)) {
      logger.send(player, JOIN_ALREADYINGAME);
      return;
    }

    Player target = teamManager.getInviter(player);
    String targetName = target != null && target.isOnline() ? target.getDisplayName() : "";

    if (!isPlayerOnline(target)) {
      logger.send(player, TEAM_NOT_ONLINE, targetName);
      return;
    }

    if (teamManager.isInTeam(target)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM_2, target.getDisplayName());
      teamManager.removeInvite(player);
      return;
    }

    if (isInQueueOrMatch(target, fcManager)) {
      logger.send(player, TEAM_ALREADY_IN_GAME, target.getDisplayName());
      return;
    }

    int matchType = teamManager.getInviteMatchType(player);
    teamManager.createTeam(target, player, matchType);
    logger.send(player, TEAM_ACCEPT_SELF, target.getDisplayName());
    logger.send(target, TEAM_ACCEPT_OTHER, player.getDisplayName());

    fcManager.getMatchManager().joinQueue(player, matchType);
    teamManager.removeInvite(player);
  }

  public static void handleDecline(Player player, FCManager fcManager) {
    Logger logger = fcManager.getLogger();
    TeamManager teamManager = fcManager.getTeamManager();

    if (teamManager.noInvite(player)) {
      logger.send(player, TEAM_NO_REQUEST);
      return;
    }

    Player target = teamManager.getInviter(player);
    if (isPlayerOnline(target)) {
      logger.send(target, TEAM_DECLINE_OTHER, player.getDisplayName());
    }

    logger.send(player, TEAM_DECLINE_SELF);
    teamManager.removeInvite(player);
  }

  public static void handleInvite(Player player, int matchType, Player target,
      FCManager fcManager) {
    Logger logger = fcManager.getLogger();
    TeamManager teamManager = fcManager.getTeamManager();

    if (teamManager.isInTeam(player)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM);
      return;
    }

    if (teamManager.isInTeam(target)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM_2, target.getDisplayName());
      return;
    }

    if (isInQueueOrMatch(player, fcManager)) {
      logger.send(player, JOIN_ALREADYINGAME);
      return;
    }

    if (isInQueueOrMatch(target, fcManager)) {
      logger.send(player, TEAM_ALREADY_IN_GAME, target.getDisplayName());
      return;
    }

    String matchTypeName = Settings.getMatchTypeName(matchType);
    teamManager.invite(player, target, matchType);
    logger.send(player, TEAM_WANTS_TO_TEAM_SELF, target.getDisplayName(), matchTypeName);
    logger.send(target, TEAM_WANTS_TO_TEAM_OTHER, player.getDisplayName(), matchTypeName);
  }

  public static boolean isInQueueOrMatch(Player player, FCManager fcManager) {
    return fcManager.getMatchSystem().isInAnyQueue(player)
        || fcManager.getMatchManager().getMatch(player).isPresent();
  }

  public static Integer parseMatchType(String input) {
    if (input == null || input.isEmpty()) {
      return null;
    }

    String lower = input.toLowerCase();
    if (lower.matches("\\d+v\\d+") || lower.matches("\\d+vs\\d+")) {
      try {
        String[] parts = lower.split("v");
        if (parts.length >= 1) {
          return Integer.parseInt(parts[0]);
        }
      } catch (NumberFormatException exception) {
        return null;
      }
    }

    try {
      return Integer.parseInt(input);
    } catch (NumberFormatException exception) {
      return null;
    }
  }
}
