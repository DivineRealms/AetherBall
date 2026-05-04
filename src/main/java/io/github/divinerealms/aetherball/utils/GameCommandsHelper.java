package io.github.divinerealms.aetherball.utils;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.matchmaking.TeamManager;
import io.github.divinerealms.aetherball.matchmaking.player.MatchPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Queue;

import static io.github.divinerealms.aetherball.configs.Lang.*;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.MatchUtils.isPlayerOnline;

public class GameCommandsHelper {

  public static void joinQueue(Player player, int type, Manager manager) {
    MatchManager matchManager = manager.getMatchManager();

    if (!Settings.isMatchTypeEnabled(type)) {
      sendMessage(
          player,
          MATCH_TYPE_UNAVAILABLE,
          Settings.getMatchTypeName(type),
          matchManager.getAvailableTypesString());
      return;
    }

    if (!matchManager.getData().isMatchesEnabled()) {
      sendMessage(player, FC_DISABLED);
      return;
    }

    if (manager.getBanManager().checkAndNotify(player)) {
      return;
    }

    if (isInQueueOrMatch(player, manager)) {
      sendMessage(player, JOIN_ALREADYINGAME);
      return;
    }

    if (!manager.getArenaManager().hasArenaForType(type)) {
      sendMessage(player, JOIN_NOARENA);
      manager.getMatchManager().leaveQueue(player, type);
      return;
    }

    matchManager.joinQueue(player, type);
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

    if (leftQueue) {
      sendMessage(player, LEFT);
    } else {
      sendMessage(player, LEAVE_NOT_INGAME);
    }
  }

  public static void showOpenMatches(Player player, List<Match> openMatches) {
    sendMessage(player, TAKEPLACE_AVAILABLE_HEADER);

    for (Match openMatch : openMatches) {
      int emptySlots = 0;
      for (MatchPlayer mp : openMatch.getPlayers()) {
        if (mp == null) {
          emptySlots++;
        }
      }

      sendMessage(
          player,
          TAKEPLACE_AVAILABLE_ENTRY,
          String.valueOf(openMatch.getArena().id()),
          Settings.getMatchTypeName(openMatch.getArena().type()),
          String.valueOf(emptySlots));
    }
  }

  public static void handleAccept(Player player, Manager manager) {
    TeamManager teamManager = manager.getTeamManager();

    if (teamManager.isInTeam(player)) {
      sendMessage(player, TEAM_ALREADY_IN_TEAM);
      return;
    }

    if (teamManager.noInvite(player)) {
      sendMessage(player, TEAM_NO_REQUEST);
      return;
    }

    if (isInQueueOrMatch(player, manager)) {
      sendMessage(player, JOIN_ALREADYINGAME);
      return;
    }

    Player target = teamManager.getInviter(player);
    String targetName = target != null && target.isOnline() ? target.getDisplayName() : "";

    if (!isPlayerOnline(target)) {
      sendMessage(player, TEAM_NOT_ONLINE, targetName);
      return;
    }

    if (teamManager.isInTeam(target)) {
      sendMessage(player, TEAM_ALREADY_IN_TEAM_2, target.getDisplayName());
      teamManager.removeInvite(player);
      return;
    }

    if (isInQueueOrMatch(target, manager)) {
      sendMessage(player, TEAM_ALREADY_IN_GAME, target.getDisplayName());
      return;
    }

    int matchType = teamManager.getInviteMatchType(player);
    teamManager.createTeam(target, player, matchType);
    sendMessage(player, TEAM_ACCEPT_SELF, target.getDisplayName());
    sendMessage(target, TEAM_ACCEPT_OTHER, player.getDisplayName());

    manager.getMatchManager().joinQueue(player, matchType);
    teamManager.removeInvite(player);
  }

  public static void handleDecline(Player player, Manager manager) {
    TeamManager teamManager = manager.getTeamManager();

    if (teamManager.noInvite(player)) {
      sendMessage(player, TEAM_NO_REQUEST);
      return;
    }

    Player target = teamManager.getInviter(player);
    if (isPlayerOnline(target)) {
      sendMessage(target, TEAM_DECLINE_OTHER, player.getDisplayName());
    }

    sendMessage(player, TEAM_DECLINE_SELF);
    teamManager.removeInvite(player);
  }

  public static void handleInvite(Player player, int matchType, Player target, Manager manager) {
    TeamManager teamManager = manager.getTeamManager();

    if (teamManager.isInTeam(player)) {
      sendMessage(player, TEAM_ALREADY_IN_TEAM);
      return;
    }

    if (teamManager.isInTeam(target)) {
      sendMessage(player, TEAM_ALREADY_IN_TEAM_2, target.getDisplayName());
      return;
    }

    if (isInQueueOrMatch(player, manager)) {
      sendMessage(player, JOIN_ALREADYINGAME);
      return;
    }

    if (isInQueueOrMatch(target, manager)) {
      sendMessage(player, TEAM_ALREADY_IN_GAME, target.getDisplayName());
      return;
    }

    String matchTypeName = Settings.getMatchTypeName(matchType);
    teamManager.invite(player, target, matchType);
    sendMessage(player, TEAM_WANTS_TO_TEAM_SELF, target.getDisplayName(), matchTypeName);
    sendMessage(target, TEAM_WANTS_TO_TEAM_OTHER, player.getDisplayName(), matchTypeName);
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
