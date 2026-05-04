package io.github.divinerealms.aetherball.matchmaking;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.ConfigManager;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchData;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchSystem;
import io.github.divinerealms.aetherball.matchmaking.player.MatchPlayer;
import io.github.divinerealms.aetherball.matchmaking.player.TeamColor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static io.github.divinerealms.aetherball.configs.Lang.*;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.isInQueueOrMatch;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.MatchUtils.*;

@Getter
public class MatchManager {

  private final Manager manager;
  private final ArenaManager arenaManager;
  private final ScoreManager scoreboardManager;
  private final TeamManager teamManager;
  private final MatchData data;
  private final MatchSystem system;
  private final BanManager banManager;
  private final Utilities utilities;
  private final PlayerDataManager dataManager;
  private final ConfigManager configManager;

  public MatchManager(Manager manager) {
    this.manager = manager;
    this.arenaManager = manager.getArenaManager();
    this.scoreboardManager = manager.getScoreboardManager();
    this.data = manager.getMatchData();
    this.teamManager = manager.getTeamManager();
    this.system = manager.getMatchSystem();
    this.banManager = manager.getBanManager();
    this.utilities = manager.getUtilities();
    this.dataManager = manager.getDataManager();
    this.configManager = manager.getConfigManager();
  }

  public void joinQueue(Player player, int matchType) {
    String matchTypeString = Settings.getMatchTypeName(matchType);
    TeamManager.Team team = teamManager.getTeam(player);
    List<Player> playersToQueue =
        team != null ? new ArrayList<>(team.getMembers()) : Collections.singletonList(player);

    for (Player player1 : playersToQueue) {
      if (!isPlayerOnline(player1)) {
        continue;
      }

      if (isInQueueOrMatch(player, manager)) {
        sendMessage(player, JOIN_ALREADYINGAME);
        return;
      }
    }

    Match existingLobby = null;
    for (Match match : data.getMatches()) {
      if (match.getPhase() == MatchPhase.LOBBY
          && match.getArena() != null
          && match.getArena().type() == matchType) {
        int nullCount = 0;
        for (MatchPlayer matchPlayer : match.getPlayers()) {
          if (!isPlayerOnline(matchPlayer)) {
            nullCount++;
          }
        }

        if (nullCount >= playersToQueue.size()) {
          existingLobby = match;
          break;
        }
      }
    }

    for (Player player1 : playersToQueue) {
      if (!isPlayerOnline(player1)) {
        continue;
      }

      for (Queue<Player> otherQueue : data.getPlayerQueues().values()) {
        otherQueue.remove(player1);
      }

      sendMessage(player1, JOIN_SUCCESS, matchTypeString);
      player1.setLevel(0);
    }

    if (existingLobby != null) {
      for (Player player1 : playersToQueue) {
        for (int i = 0; i < existingLobby.getPlayers().size(); i++) {
          if (existingLobby.getPlayers().get(i) == null) {
            existingLobby.getPlayers().set(i, new MatchPlayer(player1, null));
            scoreboardManager.showLobbyScoreboard(existingLobby, player1);
            break;
          }
        }
      }

      scoreboardManager.updateScoreboard(existingLobby);
    } else {
      Queue<Player> queue =
          data.getPlayerQueues().computeIfAbsent(matchType, k -> new ConcurrentLinkedQueue<>());

      for (Player player1 : playersToQueue) {
        if (isPlayerOnline(player1)) {
          queue.add(player1);
        }
      }
    }

    system.processQueues();
  }

  public String getAvailableTypesString() {
    List<Integer> types = Settings.getEnabledMatchTypes();
    StringBuilder stringBuilder = new StringBuilder("&e");
    for (int i = 0; i < types.size(); i++) {
      int type = types.get(i);
      stringBuilder.append(Settings.getMatchTypeName(type));
      if (i < types.size() - 1) {
        stringBuilder.append("&7, &e");
      }
    }

    return stringBuilder.toString();
  }

  public void leaveQueue(Player player, int matchType) {
    Queue<Player> queue = data.getPlayerQueues().get(matchType);
    if (queue != null) {
      queue.remove(player);
      player.setLevel(0);
    }

    leaveMatch(player);
  }

  public void forceStartMatch(Player player) {
    Optional<Match> matchOpt = getMatch(player);
    if (matchOpt.isEmpty()) {
      sendMessage(player, MATCHES_LIST_NO_MATCHES);
      return;
    }

    Match targetMatch = matchOpt.get();
    if (targetMatch.getPhase() != MatchPhase.LOBBY) {
      sendMessage(player, MATCH_ALREADY_STARTED);
      return;
    }

    List<MatchPlayer> allPlayers = targetMatch.getPlayers();
    if (allPlayers == null) {
      sendMessage(player, MATCHES_LIST_NO_MATCHES);
      return;
    }

    List<MatchPlayer> playersInLobby = new ArrayList<>();
    for (MatchPlayer matchPlayer : allPlayers) {
      if (isPlayerOnline(matchPlayer)) {
        playersInLobby.add(matchPlayer);
      }
    }

    if (playersInLobby.size() == 2) {
      MatchPlayer player1 = playersInLobby.get(0);
      MatchPlayer player2 = playersInLobby.get(1);

      player1.setTeamColor(TeamColor.RED);
      player2.setTeamColor(TeamColor.BLUE);

      targetMatch.getPlayers().clear();
      targetMatch.getPlayers().addAll(Arrays.asList(player1, player2));

      targetMatch.setPhase(MatchPhase.STARTING);
      targetMatch.setCountdown(Settings.STARTING_COUNTDOWN.asInt());

      sendMessage(player, MATCHMAN_FORCE_START, "1v1");
      scoreboardManager.updateScoreboard(targetMatch);
      return;
    }

    int arenaSize = (targetMatch.getArena() != null) ? targetMatch.getArena().type() : 1;
    int requiredPlayers = arenaSize * 2;

    List<MatchPlayer> finalPlayerLineup = new ArrayList<>();
    List<MatchPlayer> soloPlayers = new ArrayList<>();

    TeamManager.Team foundTeam = MatchSystem.findTeam(playersInLobby, teamManager);

    if (foundTeam != null) {
      List<Player> teamMembers = foundTeam.getMembers();
      for (MatchPlayer matchPlayer : playersInLobby) {
        if (!isPlayerOnline(matchPlayer)) {
          continue;
        }

        Player player1 = matchPlayer.getPlayer();
        if (teamMembers.contains(player1)) {
          finalPlayerLineup.add(matchPlayer);
        } else {
          soloPlayers.add(matchPlayer);
        }
      }
      teamManager.disbandTeam(foundTeam);
    } else {
      soloPlayers.addAll(playersInLobby);
    }

    Collections.shuffle(soloPlayers);
    finalPlayerLineup.addAll(soloPlayers);

    targetMatch.getPlayers().clear();
    targetMatch.getPlayers().addAll(finalPlayerLineup);
    while (targetMatch.getPlayers().size() < requiredPlayers) {
      targetMatch.getPlayers().add(null);
    }

    for (int i = 0; i < requiredPlayers; i++) {
      MatchPlayer matchPlayer = targetMatch.getPlayers().get(i);
      if (isPlayerOnline(matchPlayer)) {
        matchPlayer.setTeamColor(i < arenaSize ? TeamColor.RED : TeamColor.BLUE);
      }
    }

    targetMatch.setPhase(MatchPhase.STARTING);
    targetMatch.setCountdown(Settings.STARTING_COUNTDOWN.asInt());

    sendMessage(
        player, MATCHMAN_FORCE_START, Settings.getMatchTypeName(targetMatch.getArena().type()));
    scoreboardManager.updateScoreboard(targetMatch);
  }

  public Optional<Match> getMatch(Player player) {
    if (!isPlayerOnline(player) || data.getMatches() == null) {
      return Optional.empty();
    }

    for (Match match : data.getMatches()) {
      if (match == null) {
        continue;
      }

      List<MatchPlayer> matchPlayers = match.getPlayers();
      if (matchPlayers == null) {
        continue;
      }

      for (MatchPlayer matchPlayer : matchPlayers) {
        if (!isPlayerOnline(matchPlayer)) {
          continue;
        }

        Player player1 = matchPlayer.getPlayer();
        if (player1.equals(player)) {
          return Optional.of(match);
        }
      }
    }

    return Optional.empty();
  }

  public void leaveMatch(Player player) {
    getMatch(player)
        .ifPresent(
            match -> {
              boolean activeMatch = match.getPhase() != MatchPhase.LOBBY;

              if (match.getPlayers() == null) {
                if (activeMatch) {
                  Location lobby = (Location) configManager.getConfig("config.yml").get("lobby");

                  if (lobby != null) {
                    player.teleport(lobby);
                  }

                  clearPlayer(player);
                }

                scoreboardManager.removeScoreboard(player);
                return;
              }

              int playerIndex = -1;
              for (int i = 0; i < match.getPlayers().size(); i++) {
                MatchPlayer matchPlayer = match.getPlayers().get(i);
                if (isPlayerOnline(matchPlayer) && matchPlayer.getPlayer().equals(player)) {
                  playerIndex = i;
                  break;
                }
              }

              if (playerIndex != -1) {
                match.getPlayers().set(playerIndex, null);
              }

              boolean allNull = true;
              for (MatchPlayer matchPlayer : match.getPlayers()) {
                if (isPlayerOnline(matchPlayer)) {
                  allNull = false;
                  break;
                }
              }

              if (allNull) {
                endMatch(match);
              } else {
                if (activeMatch && !data.getOpenMatches().contains(match)) {
                  data.getOpenMatches().add(match);
                }

                scoreboardManager.updateScoreboard(match);
              }

              if (activeMatch) {
                Location lobby = (Location) configManager.getConfig("config.yml").get("lobby");

                if (lobby != null) {
                  player.teleport(lobby);
                }

                clearPlayer(player);
              }

              match.setTakePlaceNeeded(true);
              match.setLastTakePlaceAnnounceTick(0);
              scoreboardManager.removeScoreboard(player);
            });
  }

  public void takePlace(Player player, int matchId) {
    Optional<Match> matchOpt = Optional.empty();
    List<Match> openMatches = data.getOpenMatches();
    if (openMatches != null) {
      for (Match match : openMatches) {
        if (match == null || match.getArena() == null) {
          continue;
        }

        if (match.getArena().id() == matchId) {
          matchOpt = Optional.of(match);
          break;
        }
      }
    }

    if (matchOpt.isEmpty()) {
      sendMessage(player, TAKEPLACE_INVALID_ID, String.valueOf(matchId));
      return;
    }

    Match match = matchOpt.get();
    long redTeamCount = 0, blueTeamCount = 0;
    List<MatchPlayer> players = match.getPlayers();
    if (players != null) {
      for (MatchPlayer matchPlayer : players) {
        if (!isPlayerOnline(matchPlayer)) {
          continue;
        }

        TeamColor teamColor = matchPlayer.getTeamColor();
        if (teamColor == TeamColor.RED) {
          redTeamCount++;
        } else {
          if (teamColor == TeamColor.BLUE) {
            blueTeamCount++;
          }
        }
      }
    }

    TeamColor teamToJoin = (redTeamCount <= blueTeamCount) ? TeamColor.RED : TeamColor.BLUE;
    int openSlotIndex = -1;
    if (players != null) {
      for (int i = 0; i < players.size(); i++) {
        if (players.get(i) == null) {
          openSlotIndex = i;
          break;
        }
      }
    }

    if (openSlotIndex != -1) {
      match.getPlayers().set(openSlotIndex, new MatchPlayer(player, teamToJoin));

      if (teamToJoin == TeamColor.RED) {
        player.teleport(match.getArena().redSpawn());
      } else {
        player.teleport(match.getArena().blueSpawn());
      }

      giveArmor(player, teamToJoin);

      if (match.getPhase() == MatchPhase.LOBBY || match.getPhase() == MatchPhase.STARTING) {
        scoreboardManager.showLobbyScoreboard(match, player);
      } else {
        if (match.getPhase() != MatchPhase.ENDED) {
          scoreboardManager.showMatchScoreboard(match, player);
        }
      }
      scoreboardManager.updateScoreboard(match);

      boolean hasNull = false;
      for (MatchPlayer matchPlayer : match.getPlayers()) {
        if (!isPlayerOnline(matchPlayer)) {
          hasNull = true;
          break;
        }
      }

      if (!hasNull) {
        data.getOpenMatches().remove(match);
      }

      match.setTakePlaceNeeded(false);
      match.setLastTakePlaceAnnounceTick(0);
      sendMessage(player, TAKEPLACE_SUCCESS, String.valueOf(match.getArena().id()));
    } else {
      sendMessage(player, TAKEPLACE_FULL, String.valueOf(matchId));
    }
  }

  public void endMatch(Match match) {
    TeamColor winner = null;
    if (match.getScoreRed() > match.getScoreBlue()) {
      winner = TeamColor.RED;
    } else {
      if (match.getScoreBlue() > match.getScoreRed()) {
        winner = TeamColor.BLUE;
      }
    }

    String winningTeam = winner == TeamColor.RED ? RED.toString() : BLUE.toString();
    boolean shouldCount = Settings.shouldCountStats(match.getArena().type());

    for (MatchPlayer matchPlayer : match.getPlayers()) {
      if (!isPlayerOnline(matchPlayer)) {
        continue;
      }

      Player player = matchPlayer.getPlayer();
      if (match.getPhase() == MatchPhase.ENDED) {
        PlayerData data = dataManager.get(player);

        boolean cleanSheet = false;
        if (matchPlayer.getTeamColor() == TeamColor.RED && match.getScoreBlue() == 0) {
          cleanSheet = true;
        } else if (matchPlayer.getTeamColor() == TeamColor.BLUE && match.getScoreRed() == 0) {
          cleanSheet = true;
        }

        if (winner == null) {
          if (shouldCount) {
            data.add("ties");
            data.set("winstreak", 0);
            data.add("matches");
          }

          manager.getEconomy().depositPlayer(player, Settings.ECONOMY_TIE.asDouble());
          sendMessage(player, MATCH_TIED);
          sendMessage(player, MATCH_TIED_CREDITS);

          if (cleanSheet) {
            manager.getEconomy().depositPlayer(player, Settings.ECONOMY_CLEAN_SHEET.asDouble());
            sendMessage(player, MATCH_CLEAN_SHEET_BONUS);
          }
        } else {
          if (matchPlayer.getTeamColor() == winner) {
            if (shouldCount) {
              data.add("wins");
              data.add("winstreak");
              data.add("matches");

              if (data.getInt("winstreak") > data.getInt("bestwinstreak")) {
                data.set("bestwinstreak", data.getInt("winstreak"));
              }

              if (data.getInt("winstreak") > 0 && data.getInt("winstreak") % 5 == 0) {
                manager.getEconomy().depositPlayer(player, Settings.ECONOMY_WIN_STREAK.asDouble());
                sendMessage(
                    player, MATCH_WINSTREAK_CREDITS, String.valueOf(data.getInt("winstreak")));
              }
            }

            manager.getEconomy().depositPlayer(player, Settings.ECONOMY_VICTORY.asDouble());
            sendMessage(player, MATCH_TIMES_UP, winningTeam);
            sendMessage(player, MATCH_WIN_CREDITS);

            if (cleanSheet) {
              manager.getEconomy().depositPlayer(player, Settings.ECONOMY_CLEAN_SHEET.asDouble());
              sendMessage(player, MATCH_CLEAN_SHEET_BONUS);
            }
          } else {
            if (shouldCount) {
              data.set("winstreak", 0);
              data.add("losses");
              data.add("matches");
            }
            sendMessage(player, MATCH_TIMES_UP, winningTeam);
          }
        }

        if (shouldCount) {
          data.set("goals", data.getInt("goals") + matchPlayer.getGoals());
          data.set("assists", data.getInt("assists") + matchPlayer.getAssists());
          data.set("owngoals", data.getInt("owngoals") + matchPlayer.getOwnGoals());
        }

        dataManager.savePlayerData(player.getName());
      }

      Location lobby = (Location) configManager.getConfig("config.yml").get("lobby");
      if (lobby != null) {
        player.teleport(lobby);
      }

      clearPlayer(player);
      teamManager.forceDisbandTeam(player);

      scoreboardManager.removeScoreboard(player);
      scoreboardManager.unregisterScoreboard(match.getMatchScoreboard());
      match.setMatchScoreboard(null);
    }

    if (match.getCube() != null) {
      match.getCube().setHealth(0);
    }

    match.setPhase(MatchPhase.ENDED);
    data.getMatches().remove(match);
    data.getOpenMatches().remove(match);
  }

  public void update() {
    List<Match> matches = data.getMatches();
    if (matches == null || matches.isEmpty()) {
      try {
        system.processQueues();
      } catch (Exception exception) {
        logConsole("{prefix_error}Error processing queues", exception.getMessage());
      }
      return;
    }

    List<Match> snapshot = new ArrayList<>(matches);
    for (Match match : snapshot) {
      if (match == null) {
        continue;
      }

      try {
        system.updateMatch(match);
      } catch (Exception exception) {
        String arenaId =
            match.getArena() != null ? String.valueOf(match.getArena().id()) : "unknown";
        logConsole(
            "{prefix_error}Error updating match (arena=" + arenaId + ")", exception.getMessage());
      }
    }

    try {
      system.processQueues();
    } catch (Exception exception) {
      logConsole("{prefix_error}Error processing queues", exception.getMessage());
    }
  }

  public void kick(Player player) {
    Optional<Match> matchOpt = getMatch(player);
    if (matchOpt.isEmpty()) {
      return;
    }

    Match match = matchOpt.get();
    if (match.getPlayers() == null) {
      return;
    }

    for (MatchPlayer matchPlayer : match.getPlayers()) {
      if (!isPlayerOnline(matchPlayer)) {
        continue;
      }

      Player player1 = matchPlayer.getPlayer();
      if (player1.equals(player)) {
        if (match.getLastTouch() != matchPlayer) {
          match.setSecondLastTouch(match.getLastTouch());
          match.setLastTouch(matchPlayer);
        }
        break;
      }
    }
  }

  public int countActiveLobbies(int matchType) {
    int count = 0;
    if (data.getMatches() == null) {
      return 0;
    }

    for (Match match : data.getMatches()) {
      if (match == null) {
        continue;
      }

      if (match.getArena() == null) {
        continue;
      }

      int type = match.getArena().type();
      if (type == matchType) {
        MatchPhase phase = match.getPhase();
        if (phase != MatchPhase.LOBBY && phase != MatchPhase.ENDED) {
          count++;
        }
      }
    }
    return count;
  }

  public int countPlayersInMatches(int matchType) {
    int total = 0;
    if (data.getMatches() == null) {
      return 0;
    }

    for (Match match : data.getMatches()) {
      if (match == null || match.getArena() == null) {
        continue;
      }

      if (match.getArena().type() != matchType) {
        continue;
      }

      List<MatchPlayer> players = match.getPlayers();
      total += (players == null) ? 0 : players.size();
    }
    return total;
  }

  public int countWaitingPlayers(int matchType) {
    return data.getPlayerQueues().getOrDefault(matchType, new ConcurrentLinkedQueue<>()).size();
  }

  public String listPlayersInMatches(int matchType) {
    StringBuilder stringBuilder = new StringBuilder();
    if (data.getMatches() == null) {
      return "";
    }

    boolean first = true;
    for (Match match : data.getMatches()) {
      if (match == null || match.getArena() == null) {
        continue;
      }

      if (match.getArena().type() != matchType) {
        continue;
      }

      List<MatchPlayer> players = match.getPlayers();
      if (players == null) {
        continue;
      }

      for (MatchPlayer matchPlayer : players) {
        if (!isPlayerOnline(matchPlayer)) {
          continue;
        }

        Player player = matchPlayer.getPlayer();
        if (!first) {
          stringBuilder.append(", ");
        }

        stringBuilder.append(player.getName());
        first = false;
      }
    }
    return stringBuilder.toString();
  }

  public void forceLeaveAllPlayers() {
    Map<Integer, Queue<Player>> queues = data.getPlayerQueues();
    if (queues != null) {
      for (Queue<Player> queue : queues.values()) {
        if (queue != null) {
          queue.clear();
        }
      }
    }

    List<Match> matches = data.getMatches();
    if (matches != null) {
      List<Match> snapshot = new ArrayList<>(matches);
      for (Match match : snapshot) {
        if (match == null) {
          continue;
        }

        endMatch(match);
      }
    }
  }

  public void clearLobbiesAndQueues() {
    Map<Integer, Queue<Player>> queues = data.getPlayerQueues();
    if (queues != null) {
      for (Queue<Player> queue : queues.values()) {
        if (queue != null) {
          queue.clear();
        }
      }
    }

    List<Match> matches = data.getMatches();
    if (matches == null) {
      return;
    }

    List<Match> snapshot = new ArrayList<>(matches);

    for (Match match : snapshot) {
      if (match == null) {
        continue;
      }

      MatchPhase phase = match.getPhase();
      if (phase != MatchPhase.LOBBY && phase != MatchPhase.STARTING) {
        continue;
      }

      List<MatchPlayer> players = match.getPlayers();
      if (players != null) {
        for (MatchPlayer matchPlayer : new ArrayList<>(players)) {
          if (!isPlayerOnline(matchPlayer)) {
            continue;
          }

          Player player = matchPlayer.getPlayer();
          clearPlayer(player);
          scoreboardManager.removeScoreboard(player);
          sendMessage(
              player, MATCHMAN_FORCE_END, Settings.getMatchTypeName(match.getArena().type()));
        }
      }

      data.getMatches().remove(match);
    }
  }

  public void recreateScoreboards() {
    if (scoreboardManager == null) {
      return;
    }

    List<Match> matches = data.getMatches();
    if (matches == null) {
      return;
    }

    for (Match match : new ArrayList<>(matches)) {
      if (match == null) {
        continue;
      }

      if (match.getPhase() == MatchPhase.ENDED) {
        continue;
      }

      if (match.getPhase() == MatchPhase.LOBBY || match.getPhase() == MatchPhase.STARTING) {
        scoreboardManager.createLobbyScoreboard(match);
      } else {
        scoreboardManager.createMatchScoreboard(match);
      }
    }
  }

  public void teamChat(Player sender, String message) {
    Optional<Match> matchOpt = getMatch(sender);
    if (matchOpt.isEmpty()) {
      sendMessage(sender, LEAVE_NOT_INGAME);
      return;
    }

    Match match = matchOpt.get();
    MatchPlayer senderMatchPlayer = null;
    List<MatchPlayer> matchPlayers = match.getPlayers();
    if (matchPlayers != null) {
      for (MatchPlayer matchPlayer : matchPlayers) {
        if (!isPlayerOnline(matchPlayer)) {
          continue;
        }

        Player player = matchPlayer.getPlayer();
        if (player.equals(sender)) {
          senderMatchPlayer = matchPlayer;
          break;
        }
      }
    }

    if (senderMatchPlayer == null) {
      return;
    }

    TeamColor teamColor = senderMatchPlayer.getTeamColor();
    if (teamColor == null) {
      return;
    }

    String prefixedName = utilities.getCachedPrefixedName(sender.getUniqueId(), sender.getName());
    String formattedMessage =
        (teamColor == TeamColor.RED
                ? TEAMCHAT_RED.replace(prefixedName)
                : TEAMCHAT_BLUE.replace(prefixedName))
            + message;

    List<MatchPlayer> players = match.getPlayers();
    if (players == null) {
      return;
    }

    for (MatchPlayer matchPlayer : players) {
      if (!isPlayerOnline(matchPlayer)) {
        continue;
      }

      Player player = matchPlayer.getPlayer();
      if (matchPlayer.getTeamColor() == teamColor) {
        sendMessage(player, formattedMessage);
      }
    }
  }

  public void handlePlayerDisconnect(Player player) {
    // Remove from all queues
    for (Queue<Player> queue : data.getPlayerQueues().values()) {
      if (queue != null) {
        queue.remove(player);
      }
    }

    // Clean up lobby match slots and refresh scoreboards
    List<Match> matches = data.getMatches();
    if (matches != null) {
      for (Match match : new ArrayList<>(matches)) {
        if (match == null) continue;

        if (match.getPhase() == MatchPhase.LOBBY) {
          List<MatchPlayer> players = match.getPlayers();
          if (players != null) {
            players.removeIf(mp -> !isPlayerOnline(mp) || mp.getPlayer().equals(player));
          }
        }

        scoreboardManager.updateScoreboard(match);
      }
    }

    // Apply rage-quit penalty and leave match if in one
    Optional<Match> matchOpt = getMatch(player);
    if (matchOpt.isPresent()) {
      Match match = matchOpt.get();
      applyRageQuitPenalty(player, match, manager, false);
      leaveMatch(player);
    }
  }
}
