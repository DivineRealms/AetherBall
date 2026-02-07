package io.github.divinerealms.aetherball.utils;

import static io.github.divinerealms.aetherball.configs.Lang.FC_DISABLED;
import static io.github.divinerealms.aetherball.configs.Lang.JOIN_ALREADYINGAME;
import static io.github.divinerealms.aetherball.configs.Lang.JOIN_NOARENA;
import static io.github.divinerealms.aetherball.configs.Lang.LEAVE_LOSING;
import static io.github.divinerealms.aetherball.configs.Lang.LEAVE_NOT_INGAME;
import static io.github.divinerealms.aetherball.configs.Lang.LEFT;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_TYPE_UNAVAILABLE;
import static io.github.divinerealms.aetherball.configs.Lang.TAKEPLACE_AVAILABLE_ENTRY;
import static io.github.divinerealms.aetherball.configs.Lang.TAKEPLACE_AVAILABLE_HEADER;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_ACCEPT_OTHER;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_ACCEPT_SELF;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_ALREADY_IN_GAME;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_ALREADY_IN_TEAM;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_ALREADY_IN_TEAM_2;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_DECLINE_OTHER;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_DECLINE_SELF;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_NOT_ONLINE;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_NO_REQUEST;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_WANTS_TO_TEAM_OTHER;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_WANTS_TO_TEAM_SELF;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.isPlayerOnline;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.matchmaking.player.MatchPlayer;
import io.github.divinerealms.aetherball.matchmaking.player.TeamColor;
import io.github.divinerealms.aetherball.matchmaking.team.TeamManager;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;

public class GameCommandsHelper {

  public static void joinQueue(Player player, int type, Manager manager) {
    Logger logger = manager.getLogger();
    MatchManager matchManager = manager.getMatchManager();

    if (!Settings.isMatchTypeEnabled(type)) {
      logger.send(player, MATCH_TYPE_UNAVAILABLE, Settings.getMatchTypeName(type),
          matchManager.getAvailableTypesString());
      return;
    }

    if (!matchManager.getData().isMatchesEnabled()) {
      logger.send(player, FC_DISABLED);
      return;
    }

    if (manager.getBanManager().isBanned(player)) {
      return;
    }

    if (isInQueueOrMatch(player, manager)) {
      logger.send(player, JOIN_ALREADYINGAME);
      return;
    }

    if (!manager.getArenaManager().hasArenaForType(type)) {
      logger.send(player, JOIN_NOARENA);
      return;
    }

    matchManager.joinQueue(player, type);
  }

  public static void handleInProgressLeave(Player player, Match match, Manager manager) {
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
        manager.getEconomy().withdrawPlayer(player, rageQuitPenalty);
        manager.getBanManager().banPlayer(player, rageQuitBanDuration);
        long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(rageQuitBanDuration);
        manager.getLogger().send(player, LEAVE_LOSING, String.format("%.0f", rageQuitPenalty),
            Utilities.formatTime(secondsLeft));
      }
    }
  }

  public static void handleQueueLeave(Player player, Manager manager) {
    boolean leftQueue = false;
    MatchManager matchManager = manager.getMatchManager();
    for (int queueType : matchManager.getData().getPlayerQueues().keySet()) {
      Queue<Player> queue = matchManager.getData().getPlayerQueues().get(queueType);
      if (queue != null && queue.contains(player)) {
        matchManager.leaveQueue(player, queueType);
        leftQueue = true;
        manager.getTeamManager().disbandTeamIfInLobby(player);
      }
    }

    Logger logger = manager.getLogger();
    if (leftQueue) {
      logger.send(player, LEFT);
    } else {
      logger.send(player, LEAVE_NOT_INGAME);
    }
  }

  public static void showOpenMatches(Player player, List<Match> openMatches, Manager manager) {
    Logger logger = manager.getLogger();
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

  public static void handleAccept(Player player, Manager manager) {
    Logger logger = manager.getLogger();
    TeamManager teamManager = manager.getTeamManager();

    if (teamManager.isInTeam(player)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM);
      return;
    }

    if (teamManager.noInvite(player)) {
      logger.send(player, TEAM_NO_REQUEST);
      return;
    }

    if (isInQueueOrMatch(player, manager)) {
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

    if (isInQueueOrMatch(target, manager)) {
      logger.send(player, TEAM_ALREADY_IN_GAME, target.getDisplayName());
      return;
    }

    int matchType = teamManager.getInviteMatchType(player);
    teamManager.createTeam(target, player, matchType);
    logger.send(player, TEAM_ACCEPT_SELF, target.getDisplayName());
    logger.send(target, TEAM_ACCEPT_OTHER, player.getDisplayName());

    manager.getMatchManager().joinQueue(player, matchType);
    teamManager.removeInvite(player);
  }

  public static void handleDecline(Player player, Manager manager) {
    Logger logger = manager.getLogger();
    TeamManager teamManager = manager.getTeamManager();

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
      Manager manager) {
    Logger logger = manager.getLogger();
    TeamManager teamManager = manager.getTeamManager();

    if (teamManager.isInTeam(player)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM);
      return;
    }

    if (teamManager.isInTeam(target)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM_2, target.getDisplayName());
      return;
    }

    if (isInQueueOrMatch(player, manager)) {
      logger.send(player, JOIN_ALREADYINGAME);
      return;
    }

    if (isInQueueOrMatch(target, manager)) {
      logger.send(player, TEAM_ALREADY_IN_GAME, target.getDisplayName());
      return;
    }

    String matchTypeName = Settings.getMatchTypeName(matchType);
    teamManager.invite(player, target, matchType);
    logger.send(player, TEAM_WANTS_TO_TEAM_SELF, target.getDisplayName(), matchTypeName);
    logger.send(target, TEAM_WANTS_TO_TEAM_OTHER, player.getDisplayName(), matchTypeName);
  }

  public static boolean isInQueueOrMatch(Player player, Manager manager) {
    return manager.getMatchSystem().isInAnyQueue(player)
        || manager.getMatchManager().getMatch(player).isPresent();
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
