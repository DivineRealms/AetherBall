package io.github.divinerealms.aetherball.matchmaking.logic;

import static io.github.divinerealms.aetherball.configs.Lang.BLUE;
import static io.github.divinerealms.aetherball.configs.Lang.CLEARED_CUBE_INGAME;
import static io.github.divinerealms.aetherball.configs.Lang.JOIN_NOARENA;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_LOBBY;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_MATCH;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_STARTING;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_PREPARATION_SUBTITLE;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_PREPARATION_TITLE;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_PREPARING;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_PREVENT_ABUSE;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_PROCEED;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_STARTED;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_STARTED_ACTIONBAR;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_STARTING_ACTIONBAR;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_STARTING_SUBTITLE;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_STARTING_TITLE;
import static io.github.divinerealms.aetherball.configs.Lang.RED;
import static io.github.divinerealms.aetherball.configs.Lang.STARTING;
import static io.github.divinerealms.aetherball.configs.Lang.STATS;
import static io.github.divinerealms.aetherball.configs.Lang.STATS_NONE;
import static io.github.divinerealms.aetherball.configs.Lang.TAKE_PLACE_ANNOUNCEMENT_LOBBY;
import static io.github.divinerealms.aetherball.configs.Lang.TAKE_PLACE_ANNOUNCEMENT_MATCH;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.awardCreditsForGoal;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.broadcastGoalMessage;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.determineScoringPlayers;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.isPlayerOnline;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.playGoalEffects;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.preparePlayer;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.preventPlayerAbuse;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.shouldPreventAbuse;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchPhase;
import io.github.divinerealms.aetherball.matchmaking.arena.Arena;
import io.github.divinerealms.aetherball.matchmaking.arena.ArenaManager;
import io.github.divinerealms.aetherball.matchmaking.player.MatchPlayer;
import io.github.divinerealms.aetherball.matchmaking.player.StatsHelper;
import io.github.divinerealms.aetherball.matchmaking.player.TeamColor;
import io.github.divinerealms.aetherball.matchmaking.scoreboard.ScoreManager;
import io.github.divinerealms.aetherball.matchmaking.team.Team;
import io.github.divinerealms.aetherball.matchmaking.team.TeamManager;
import io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.ScoringResult;
import io.github.divinerealms.aetherball.utils.Logger;
import io.github.divinerealms.aetherball.utils.PlayerSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.util.Vector;

public class MatchSystem {

  private final Manager manager;
  private final Logger logger;
  private final ScoreManager scoreboardManager;
  private final MatchData data;
  private final ArenaManager arenaManager;
  private final TeamManager teamManager;

  public MatchSystem(Manager manager) {
    this.manager = manager;
    this.logger = manager.getLogger();
    this.scoreboardManager = manager.getScoreboardManager();
    this.data = manager.getMatchData();
    this.arenaManager = manager.getArenaManager();
    this.teamManager = manager.getTeamManager();
  }

  public void initializeMatchTypes() {
    List<Integer> enabledTypes = Settings.getEnabledMatchTypes();

    if (enabledTypes.isEmpty()) {
      logger.info("&c⚠ No match types enabled in settings.yml!");
      return;
    }

    data.initializeForMatchTypes(enabledTypes);

    StringBuilder typesStr = new StringBuilder();
    for (int i = 0; i < enabledTypes.size(); i++) {
      int type = enabledTypes.get(i);
      typesStr.append("&e").append(Settings.getMatchTypeName(type));
      if (i < enabledTypes.size() - 1) {
        typesStr.append("&7, ");
      }
    }

    logger.info("&a✔ &2Initialized &e" + enabledTypes.size() + " &2match types: " + typesStr);
  }

  public void startMatch(Match match) {
    if (match == null || match.getPlayers() == null) {
      return;
    }

    scoreboardManager.createMatchScoreboard(match);

    for (MatchPlayer matchPlayer : match.getPlayers()) {
      if (!isPlayerOnline(matchPlayer)) {
        continue;
      }

      Player player = matchPlayer.getPlayer();
      scoreboardManager.showMatchScoreboard(match, player);
      logger.send(player, MATCH_STARTED);
    }

    startRound(match);
  }

  public void startRound(Match match) {
    if (match == null || match.getPlayers() == null) {
      return;
    }

    for (MatchPlayer matchPlayer : match.getPlayers()) {
      if (!isPlayerOnline(matchPlayer)) {
        continue;
      }

      Player player = matchPlayer.getPlayer();
      if (matchPlayer.getTeamColor() == TeamColor.RED) {
        player.getPlayer().teleport(match.getArena().getRedSpawn());
      } else {
        player.getPlayer().teleport(match.getArena().getBlueSpawn());
      }
      player.playSound(player.getLocation(), Sound.EXPLODE, 1, 1);
    }

    handleCubeSpawn(match);
  }

  public void handleCubeSpawn(Match match) {
    if (match.getCube() != null && !match.getCube().isDead()) {
      return;
    }

    Arena arena = match.getArena();
    Slime cube = manager.getPhysicsSystem().spawnCube(arena.getCenter());
    match.setCube(cube);

    ThreadLocalRandom random = ThreadLocalRandom.current();
    double vertical = 0.3 * random.nextDouble() + 0.2;
    double horizontal = 0.3 * random.nextDouble() + 0.3;
    if (random.nextBoolean()) {
      horizontal *= -1;
    }

    boolean x = Math.abs(arena.getBlueSpawn().getX() - arena.getRedSpawn().getX()) > Math.abs(
        arena.getBlueSpawn().getZ() - arena.getRedSpawn().getZ());
    if (x) {
      match.getCube().setVelocity(new Vector(0, vertical, horizontal));
    } else {
      match.getCube().setVelocity(new Vector(horizontal, vertical, 0));
    }
  }

  public void handleCubeRespawn(Match match) {
    if (match.getCube() != null && !match.getCube().isDead()) {
      return;
    }

    if (match.getPhase() != MatchPhase.IN_PROGRESS) {
      return;
    }

    startRound(match);
    if (match.getPlayers() == null) {
      return;
    }

    for (MatchPlayer matchPlayer : match.getPlayers()) {
      if (!isPlayerOnline(matchPlayer)) {
        continue;
      }

      logger.send(matchPlayer.getPlayer(), CLEARED_CUBE_INGAME);
    }
  }

  public void handleMatchTimer(Match match) {
    if (match.getPhase() != MatchPhase.IN_PROGRESS) {
      return;
    }

    long matchDuration = Settings.getMatchDuration(match.getArena().getType());
    long totalActiveElapsedMillis = (System.currentTimeMillis() - match.getStartTime());
    long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(totalActiveElapsedMillis);

    int maxScore = Settings.getMaxScore(match.getArena().getType());
    if (maxScore > 0) {
      if (match.getScoreRed() >= maxScore || match.getScoreBlue() >= maxScore) {
        match.setPhase(MatchPhase.ENDED);
        return;
      }
    }

    if (elapsedSeconds >= matchDuration) {
      match.setPhase(MatchPhase.ENDED);
    }
  }

  public void handleGoalDetection(Match match) {
    Slime cube = match.getCube();
    if (cube == null) {
      return;
    }

    Location cubeLocation = cube.getLocation();
    Arena arena = match.getArena();
    double cubeRadius = 0.26;

    if (arena.isXAxis()) {
      if (arena.isRedIsGreater() && cubeLocation.getX() + cubeRadius > arena.getRedSpawn().getX()
          || !arena.isRedIsGreater() && cubeLocation.getX() - cubeRadius < arena.getRedSpawn()
          .getX()) {
        score(match, TeamColor.BLUE);
      } else {
        if (arena.isRedIsGreater() && cubeLocation.getX() - cubeRadius < arena.getBlueSpawn().getX()
            || !arena.isRedIsGreater() && cubeLocation.getX() + cubeRadius > arena.getBlueSpawn()
            .getX()) {
          score(match, TeamColor.RED);
        }
      }
    } else {
      if (arena.isRedIsGreater() && cubeLocation.getZ() + cubeRadius > arena.getRedSpawn().getZ()
          || !arena.isRedIsGreater() && cubeLocation.getZ() - cubeRadius < arena.getRedSpawn()
          .getZ()) {
        score(match, TeamColor.BLUE);
      } else {
        if (arena.isRedIsGreater() && cubeLocation.getZ() - cubeRadius < arena.getBlueSpawn().getZ()
            || !arena.isRedIsGreater() && cubeLocation.getZ() + cubeRadius > arena.getBlueSpawn()
            .getZ()) {
          score(match, TeamColor.RED);
        }
      }
    }
  }

  private void score(Match match, TeamColor scoringTeam) {
    if (match.getPhase() != MatchPhase.IN_PROGRESS) {
      return;
    }

    Arena arena = match.getArena();
    if (scoringTeam == TeamColor.RED) {
      match.setScoreRed(match.getScoreRed() + 1);
    } else {
      match.setScoreBlue(match.getScoreBlue() + 1);
    }

    if (match.getCube() != null) {
      match.getCube().setHealth(0);
      match.setCube(null);
    }

    boolean shouldCountStats = Settings.shouldCountStats(arena.getType());
    ScoringResult scoringResult = determineScoringPlayers(match, scoringTeam, shouldCountStats);
    if (scoringResult.shouldAwardCredits()) {
      awardCreditsForGoal(scoringResult, logger, manager);
    }

    Location goalLoc = scoringTeam == TeamColor.RED ? arena.getBlueSpawn() : arena.getRedSpawn();
    playGoalEffects(match, goalLoc, manager);
    broadcastGoalMessage(match, scoringResult, goalLoc, manager, logger);

    match.setPhase(MatchPhase.CONTINUING);
    match.setCountdown(5);
    match.setTick(0);
    scoreboardManager.updateScoreboard(match);
  }

  public void updateMatch(Match match) {
    match.setTick(match.getTick() + 1);

    List<MatchPlayer> players = match.getPlayers();
    int currentPlayers = 0;
    if (players == null) {
      return;
    }

    for (MatchPlayer matchPlayer : players) {
      if (isPlayerOnline(matchPlayer)) {
        currentPlayers++;

        if (shouldPreventAbuse(match.getPhase())) {
          Player player = matchPlayer.getPlayer();
          PlayerSettings playerSettings = manager.getPlayerSettings(player);
          if (preventPlayerAbuse(player, playerSettings)) {
            logger.send(player, MATCH_PREVENT_ABUSE);
          }
        }
      }
    }

    final int requiredPlayers = match.getArena().getType() * 2;

    switch (match.getPhase()) {
      case LOBBY:
        if (currentPlayers >= requiredPlayers) {
          List<MatchPlayer> playersToAssign = new ArrayList<>(currentPlayers);
          for (MatchPlayer matchPlayer : players) {
            if (isPlayerOnline(matchPlayer)) {
              playersToAssign.add(matchPlayer);
            }
          }

          Team firstTeam = null;
          for (MatchPlayer matchPlayer : playersToAssign) {
            if (!isPlayerOnline(matchPlayer)) {
              continue;
            }

            Team team = teamManager.getTeam(matchPlayer.getPlayer());
            if (team != null) {
              firstTeam = team;
              break;
            }
          }

          if (firstTeam != null) {
            List<Player> teamMembers = firstTeam.getMembers();
            List<MatchPlayer> teamMatchPlayers = new ArrayList<>();
            List<MatchPlayer> soloPlayers = new ArrayList<>();

            for (MatchPlayer matchPlayer : playersToAssign) {
              if (isPlayerOnline(matchPlayer) && teamMembers.contains(matchPlayer.getPlayer())) {
                teamMatchPlayers.add(matchPlayer);
              } else {
                soloPlayers.add(matchPlayer);
              }
            }

            for (MatchPlayer teamPlayer : teamMatchPlayers) {
              teamPlayer.setTeamColor(TeamColor.RED);
            }

            Collections.shuffle(soloPlayers);

            long redTeamSize = teamMatchPlayers.size();
            for (MatchPlayer soloPlayer : soloPlayers) {
              if (redTeamSize < requiredPlayers / 2) {
                soloPlayer.setTeamColor(TeamColor.RED);
                redTeamSize++;
              } else {
                soloPlayer.setTeamColor(TeamColor.BLUE);
              }
            }

            teamManager.disbandTeam(firstTeam);
          } else {
            Collections.shuffle(playersToAssign);
            for (int i = 0; i < playersToAssign.size(); i++) {
              playersToAssign.get(i)
                  .setTeamColor(i < requiredPlayers / 2 ? TeamColor.RED : TeamColor.BLUE);
            }
          }

          for (MatchPlayer matchPlayer : playersToAssign) {
            if (!isPlayerOnline(matchPlayer)) {
              continue;
            }

            logger.send(matchPlayer.getPlayer(), STARTING);
          }

          match.setPhase(MatchPhase.STARTING);
          match.setCountdown(Settings.STARTING_COUNTDOWN.asInt());
          match.setTick(0);
          scoreboardManager.updateScoreboard(match);
        }
        break;

      case STARTING:
        if (shouldUpdateScoreboard(match)) {
          match.setCountdown(match.getCountdown() - 1);
          scoreboardManager.updateScoreboard(match);
          String matchType = Settings.getMatchTypeName(match.getArena().getType());
          String matchId = String.valueOf(match.getArena().getId());

          String matchTitle =
              match.getCountdown() == 0 ? MATCHES_LIST_MATCH.replace(matchType, matchId)
                  : MATCHES_LIST_LOBBY.replace(matchType, matchId);

          for (MatchPlayer matchPlayer : players) {
            if (!isPlayerOnline(matchPlayer)) {
              continue;
            }

            Player player = matchPlayer.getPlayer();
            player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1, 1);

            if (match.getCountdown() != 0) {
              logger.sendActionBar(player, MATCH_STARTING_ACTIONBAR, matchTitle,
                  MATCHES_LIST_STARTING.replace(String.valueOf(match.getCountdown())));
            } else {
              logger.sendActionBar(player, MATCH_STARTING_ACTIONBAR, matchTitle,
                  MATCH_STARTED_ACTIONBAR.toString());
            }

            if (match.getCountdown() == 10) {
              PlayerSettings playerSettings = manager.getPlayerSettings(player);
              preparePlayer(player, matchPlayer.getTeamColor(), match.getArena(), playerSettings);
              logger.title(player, matchTitle, MATCH_PREPARING, 10, 50, 10);
            } else {
              if (match.getCountdown() == 5) {
                logger.title(player, MATCH_PREPARATION_TITLE, MATCH_PREPARATION_SUBTITLE, 10, 50,
                    10);
              } else {
                if (match.getCountdown() <= 0) {
                  logger.title(player, MATCH_STARTING_TITLE, MATCH_STARTING_SUBTITLE, 5, 30, 5);
                }
              }
            }
          }

          if (match.getCountdown() <= 0) {
            match.setPhase(MatchPhase.IN_PROGRESS);
            match.setStartTime(System.currentTimeMillis());
            startMatch(match);
          }
        }
        break;

      case IN_PROGRESS:
        if (shouldUpdateScoreboard(match)) {
          scoreboardManager.updateScoreboard(match);
        }

        handleGoalDetection(match);
        handleMatchTimer(match);
        handleCubeRespawn(match);

        if (currentPlayers >= requiredPlayers) {
          match.setTakePlaceNeeded(false);
          match.setLastTakePlaceAnnounceTick(0);
        }
        break;

      case CONTINUING:
        long matchDuration = Settings.getMatchDuration(match.getArena().getType());
        long totalActiveElapsedMillis = (System.currentTimeMillis() - match.getStartTime());
        long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(totalActiveElapsedMillis);

        if (elapsedSeconds >= matchDuration) {
          match.setPhase(MatchPhase.ENDED);
          break;
        }

        if (shouldUpdateScoreboard(match)) {
          if (match.getCountdown() <= 0) {
            match.setPhase(MatchPhase.IN_PROGRESS);

            for (MatchPlayer matchPlayer : players) {
              if (!isPlayerOnline(matchPlayer)) {
                continue;
              }

              logger.send(matchPlayer.getPlayer(), MATCH_PROCEED);
            }

            startRound(match);
          } else {
            match.setCountdown(match.getCountdown() - 1);
            scoreboardManager.updateScoreboard(match);

            for (MatchPlayer matchPlayer : players) {
              if (!isPlayerOnline(matchPlayer)) {
                continue;
              }

              Player player = matchPlayer.getPlayer();
              player.setLevel(match.getCountdown());
              player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1, 1);
            }
          }
        }
        break;

      case ENDED:
        if (currentPlayers >= requiredPlayers) {
          match.setTakePlaceNeeded(false);
          match.setLastTakePlaceAnnounceTick(0);
        }

        manager.getMatchManager().endMatch(match);
        break;
    }

    match.setTakePlaceNeeded(currentPlayers < requiredPlayers && !match.isTakePlaceNeeded()
        && match.getPhase() != MatchPhase.LOBBY);
    if (match.isTakePlaceNeeded()) {
      announceTakePlace(match);
    }
  }

  public void processQueues() {
    if (arenaManager.getArenas().isEmpty()) {
      logger.info("&c⚠ Cannot process queues - no arenas configured!");
      return;
    }

    List<Integer> enabledTypes = Settings.getEnabledMatchTypes();
    for (int matchType : enabledTypes) {
      if (!Settings.isMatchTypeEnabled(matchType)) {
        continue;
      }

      Queue<Player> queue = data.getPlayerQueues().get(matchType);
      if (queue == null || queue.isEmpty()) {
        continue;
      }

      ReentrantLock lock = data.getQueueLocks().get(matchType);
      if (lock == null) {
        continue;
      }

      if (!lock.tryLock()) {
        continue;
      }

      try {
        processSingleQueue(matchType, queue);
      } finally {
        lock.unlock();
      }
    }
  }

  private void processSingleQueue(int matchType, Queue<Player> queue) {
    while (true) {
      Player head = queue.peek();
      while (head != null && !head.isOnline()) {
        queue.poll();
        head = queue.peek();
      }

      if (head == null) {
        break;
      }

      if (manager.getMatchManager().getMatch(head).isPresent()) {
        queue.poll();
        continue;
      }

      if (!arenaManager.hasArenaForType(matchType)) {
        List<Player> playersToKick = new ArrayList<>(queue);
        queue.clear();
        for (Player player : playersToKick) {
          if (isPlayerOnline(player)) {
            logger.send(player, JOIN_NOARENA);
          }
        }
        return;
      }

      Team team = teamManager.getTeam(head);
      List<Player> playerGroup;

      if (team != null) {
        Set<Player> snapshot = new HashSet<>(queue);
        List<Player> members = new ArrayList<>();
        for (Player member : team.getMembers()) {
          if (isPlayerOnline(member)) {
            members.add(member);
          }
        }

        if (!snapshot.containsAll(members)) {
          return;
        }

        playerGroup = new ArrayList<>();
        for (Player player : queue) {
          if (members.contains(player)) {
            playerGroup.add(player);
          }
        }

        if (playerGroup.isEmpty()) {
          return;
        }
      } else {
        playerGroup = Collections.singletonList(head);
      }

      Match targetMatch = null;
      int maxPlayers = matchType * 2;
      int groupSize = playerGroup.size();

      for (Match match : data.getMatches()) {
        if (match.getArena().getType() == matchType && match.getPhase() == MatchPhase.LOBBY && (
            match.getPlayers().size() + groupSize <= maxPlayers)) {
          if (targetMatch == null || match.getPlayers().size() < targetMatch.getPlayers().size()) {
            targetMatch = match;
          }
        }
      }

      if (targetMatch == null) {
        targetMatch = createNewLobby(matchType);
      }

      if (targetMatch == null) {
        for (Player player : playerGroup) {
          logger.send(player, JOIN_NOARENA);
        }
        return;
      }

      if (targetMatch.getLobbyScoreboard() == null) {
        scoreboardManager.createLobbyScoreboard(targetMatch);
      }

      for (Player player : playerGroup) {
        queue.remove(player);
        targetMatch.getPlayers().add(new MatchPlayer(player, null));
        scoreboardManager.showLobbyScoreboard(targetMatch, player);
      }

      scoreboardManager.updateScoreboard(targetMatch);
    }
  }

  public void checkStats(String playerName, CommandSender asker) {
    PlayerData data = manager.getDataManager().get(playerName);
    if (data == null || !data.has("matches")) {
      logger.send(asker, STATS_NONE, playerName);
      return;
    }

    StatsHelper stats = new StatsHelper(data);

    logger.send(asker, STATS, playerName, String.valueOf(stats.getMatches()),
        String.valueOf(stats.getWins()), String.valueOf(stats.getLosses()),
        String.valueOf(stats.getTies()), String.format("%.2f", stats.getWinsPerMatch()),
        String.valueOf(stats.getBestWinStreak()), String.valueOf(stats.getGoals()),
        String.format("%.2f", stats.getGoalsPerMatch()), String.valueOf(stats.getAssists()),
        String.format("%.2f", stats.getSkillLevel()), stats.getRankName(),
        String.valueOf(stats.getOwnGoals()));
  }

  public boolean isInAnyQueue(Player player) {
    for (Queue<Player> queue : data.getPlayerQueues().values()) {
      if (queue != null && queue.contains(player)) {
        return true;
      }
    }
    return false;
  }

  private synchronized Match createNewLobby(int matchType) {
    List<Arena> available = new ArrayList<>();
    for (Arena arena : arenaManager.getArenas()) {
      if (arena == null || arena.getType() != matchType) {
        continue;
      }

      boolean inUse = false;
      for (Match match : data.getMatches()) {
        if (match == null || match.getArena() == null) {
          continue;
        }

        if (match.getArena().getId() == arena.getId()) {
          inUse = true;
          break;
        }
      }

      if (!inUse) {
        available.add(arena);
      }
    }

    if (available.isEmpty()) {
      return null;
    }

    Collections.shuffle(available);
    Match newMatch = new Match(available.get(0), new ArrayList<>());
    data.getMatches().add(newMatch);
    return newMatch;
  }

  private boolean shouldUpdateScoreboard(Match match) {
    return match.getTick() % 20 == 0;
  }

  private void announceTakePlace(Match match) {
    boolean firstAnnouncement = match.getLastTakePlaceAnnounceTick() == 0;
    if (firstAnnouncement || match.getTick() - match.getLastTakePlaceAnnounceTick()
        >= Settings.getTakePlaceAnnouncementInterval()) {
      match.setLastTakePlaceAnnounceTick(match.getTick());

      long matchDuration = Settings.getMatchDuration(match.getArena().getType());
      long elapsedMillis = System.currentTimeMillis() - match.getStartTime();
      long remainingSeconds = matchDuration - TimeUnit.MILLISECONDS.toSeconds(elapsedMillis);

      int matchType = match.getArena().getType();
      String matchIdString = String.valueOf(match.getArena().getId());
      String matchTypeString = Settings.getMatchTypeName(matchType);
      boolean activeMatch = match.getPhase() == MatchPhase.IN_PROGRESS;

      String matchTitle = activeMatch ? "&a&l" + matchTypeString + " Meča #" + matchIdString
          : "&b&l" + matchTypeString + " Queue #" + matchIdString;

      String announcement =
          activeMatch ? TAKE_PLACE_ANNOUNCEMENT_MATCH.replace(matchTitle, RED.toString(),
              String.valueOf(match.getScoreRed()), String.valueOf(match.getScoreBlue()),
              BLUE.toString(), Utilities.formatTimePretty((int) remainingSeconds))
              : TAKE_PLACE_ANNOUNCEMENT_LOBBY.replace(matchTitle);

      for (Player player : Bukkit.getOnlinePlayers()) {
        if (!isPlayerOnline(player)) {
          continue;
        }

        if (manager.getMatchManager().getMatch(player).isEmpty()) {
          logger.send(player, announcement);
        }
      }
    }
  }
}