package io.github.divinerealms.footcube.commands.player;

import static io.github.divinerealms.footcube.configs.Lang.FC_DISABLED;
import static io.github.divinerealms.footcube.configs.Lang.JOIN_ALREADYINGAME;
import static io.github.divinerealms.footcube.configs.Lang.JOIN_INVALIDTYPE;
import static io.github.divinerealms.footcube.configs.Lang.JOIN_NOARENA;
import static io.github.divinerealms.footcube.configs.Lang.LEAVE_LOSING;
import static io.github.divinerealms.footcube.configs.Lang.LEAVE_NOT_INGAME;
import static io.github.divinerealms.footcube.configs.Lang.LEAVE_QUEUE_ACTIONBAR;
import static io.github.divinerealms.footcube.configs.Lang.LEFT;
import static io.github.divinerealms.footcube.configs.Lang.OR;
import static io.github.divinerealms.footcube.configs.Lang.STATSSET_IS_NOT_A_NUMBER;
import static io.github.divinerealms.footcube.configs.Lang.TAKEPLACE_AVAILABLE_ENTRY;
import static io.github.divinerealms.footcube.configs.Lang.TAKEPLACE_AVAILABLE_HEADER;
import static io.github.divinerealms.footcube.configs.Lang.TAKEPLACE_INGAME;
import static io.github.divinerealms.footcube.configs.Lang.TAKEPLACE_NOPLACE;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_PLAY;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.footcube.configs.Settings;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.matchmaking.Match;
import io.github.divinerealms.footcube.matchmaking.MatchManager;
import io.github.divinerealms.footcube.matchmaking.MatchPhase;
import io.github.divinerealms.footcube.matchmaking.arena.ArenaManager;
import io.github.divinerealms.footcube.matchmaking.player.MatchPlayer;
import io.github.divinerealms.footcube.matchmaking.player.TeamColor;
import io.github.divinerealms.footcube.matchmaking.team.TeamManager;
import io.github.divinerealms.footcube.utils.Logger;
import java.util.List;
import java.util.Queue;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("fc|footcube")
public class GameCommands extends BaseCommand {

  private final FCManager fcManager;
  private final Logger logger;
  private final MatchManager matchManager;
  private final TeamManager teamManager;
  private final ArenaManager arenaManager;

  public GameCommands(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
    this.matchManager = fcManager.getMatchManager();
    this.teamManager = fcManager.getTeamManager();
    this.arenaManager = fcManager.getArenaManager();
  }

  @CommandAlias("fcjoin|fcj")
  @Subcommand("join|j")
  @CommandPermission(PERM_PLAY)
  @Syntax("<1v1|2v2|3v3|4v4|5v5>")
  @CommandCompletion("1v1|2v2|3v3|4v4|5v5")
  @Description("Join a matchmaking queue")
  public void onJoin(Player player, String matchType) {
    int type;
    switch (matchType.toLowerCase()) {
      case "1v1":
        type = 1;
        break;
      case "2v2":
        type = 2;
        break;
      case "3v3":
        type = 3;
        break;
      case "4v4":
        type = 4;
        break;
      case "5v5":
        type = 5;
        break;
      default:
        logger.send(player, JOIN_INVALIDTYPE, matchType, OR.toString());
        return;
    }

    joinQueue(player, type);
  }

  @CommandAlias("1v1")
  @CommandPermission(PERM_PLAY)
  @Description("Join 1v1 matchmaking queue")
  public void on1v1(Player player) {
    joinQueue(player, 1);
  }

  @CommandAlias("2v2")
  @CommandPermission(PERM_PLAY)
  @Description("Join 2v2 matchmaking queue")
  public void on2v2(Player player) {
    joinQueue(player, 2);
  }

  @CommandAlias("3v3")
  @CommandPermission(PERM_PLAY)
  @Description("Join 3v3 matchmaking queue")
  public void on3v3(Player player) {
    joinQueue(player, 3);
  }

  @CommandAlias("4v4")
  @CommandPermission(PERM_PLAY)
  @Description("Join 4v4 matchmaking queue")
  public void on4v4(Player player) {
    joinQueue(player, 4);
  }

  @CommandAlias("5v5")
  @CommandPermission(PERM_PLAY)
  @Description("Join 5v5 matchmaking queue")
  public void on5v5(Player player) {
    joinQueue(player, 5);
  }

  @CommandAlias("fcleave|fcl|leave")
  @Subcommand("leave|l")
  @CommandPermission(PERM_PLAY)
  @Description("Leave current match or queue")
  public void onLeave(Player player) {
    java.util.Optional<Match> matchOpt = matchManager.getMatch(player);

    if (matchOpt.isPresent()) {
      Match match = matchOpt.get();

      if (match.getPhase() == MatchPhase.IN_PROGRESS) {
        handleInProgressLeave(player, match);
      }

      matchManager.leaveMatch(player);
      logger.send(player, LEFT);
      logger.sendActionBar(player, LEAVE_QUEUE_ACTIONBAR,
          match.getArena().getType() + "v" + match.getArena().getType());
      teamManager.forceDisbandTeam(player);
    } else {
      handleQueueLeave(player);
    }
  }

  @CommandAlias("takeplace|tkp")
  @Subcommand("takeplace|tkp")
  @CommandPermission(PERM_PLAY)
  @Syntax("[matchId|list]")
  @Description("Take an open spot in an ongoing match")
  public void onTakePlace(Player player, @Optional String arg) {
    if (!matchManager.getData().isMatchesEnabled()) {
      logger.send(player, FC_DISABLED);
      return;
    }

    if (matchManager.getBanManager().isBanned(player)) {
      return;
    }

    if (matchManager.getMatch(player).isPresent()) {
      logger.send(player, TAKEPLACE_INGAME);
      return;
    }

    if (matchManager.getData().getOpenMatches().isEmpty()) {
      logger.send(player, TAKEPLACE_NOPLACE);
      return;
    }

    List<Match> openMatches = matchManager.getData().getOpenMatches();

    if (arg == null) {
      Match openMatch = openMatches.iterator().next();
      matchManager.takePlace(player, openMatch.getArena().getId());
      return;
    }

    if (arg.equalsIgnoreCase("list")) {
      showOpenMatches(player, openMatches);
      return;
    }

    try {
      int matchId = Integer.parseInt(arg);
      matchManager.takePlace(player, matchId);
    } catch (NumberFormatException e) {
      logger.send(player, STATSSET_IS_NOT_A_NUMBER, arg);
    }
  }

  @CommandAlias("teamchat|tc")
  @Subcommand("teamchat|tc")
  @Syntax("<message>")
  @Description("Send a message to your team")
  public void onTeamChat(Player player, String message) {
    matchManager.teamChat(player, message);
  }

  @CommandAlias("stats")
  @Subcommand("stats")
  @Syntax("[player]")
  @CommandCompletion("@players")
  @Description("View player statistics")
  public void onStats(CommandSender sender, @Optional String targetName) {
    if (sender instanceof Player) {
      Player p = (Player) sender;
      fcManager.getMatchSystem().checkStats(targetName != null ? targetName : p.getName(), sender);
    } else {
      if (targetName == null) {
        logger.send(sender, "&cYou need to specify a player.");
      } else {
        fcManager.getMatchSystem().checkStats(targetName, sender);
      }
    }
  }

  @CommandAlias("highscores|best")
  @Subcommand("highscores|best")
  @Description("View top players leaderboard")
  public void onHighScores(CommandSender sender) {
    fcManager.getHighscoreManager().showHighScores(sender);
  }

  private void joinQueue(Player player, int type) {
    if (!Settings.isMatchTypeEnabled(type)) {
      logger.send(player, "&c&l✘ &cThis match type is not available!");
      logger.send(player, "&7Available types: " + matchManager.getAvailableTypesString());
      return;
    }

    if (!matchManager.getData().isMatchesEnabled()) {
      logger.send(player, FC_DISABLED);
      return;
    }

    if (matchManager.getBanManager().isBanned(player)) {
      return;
    }

    if (matchManager.getMatch(player).isPresent()) {
      logger.send(player, JOIN_ALREADYINGAME);
      return;
    }

    if (!arenaManager.hasArenaForType(type)) {
      logger.send(player, JOIN_NOARENA);
      return;
    }

    matchManager.joinQueue(player, type);
  }

  private void handleInProgressLeave(Player player, Match match) {
    MatchPlayer matchPlayer = null;
    for (MatchPlayer mp : match.getPlayers()) {
      if (mp != null && mp.getPlayer() != null && mp.getPlayer().equals(player)) {
        matchPlayer = mp;
        break;
      }
    }

    if (matchPlayer != null) {
      int playerScore = matchPlayer.getTeamColor() == TeamColor.RED
          ? match.getScoreRed()
          : match.getScoreBlue();
      int opponentScore = matchPlayer.getTeamColor() == TeamColor.RED
          ? match.getScoreBlue()
          : match.getScoreRed();

      if (playerScore < opponentScore) {
        fcManager.getEconomy().withdrawPlayer(player, 200);
        matchManager.getBanManager().banPlayer(player, 30 * 60 * 1000);
        logger.send(player, LEAVE_LOSING);
      }
    }
  }

  private void handleQueueLeave(Player player) {
    boolean leftQueue = false;
    for (int queueType : matchManager.getData().getPlayerQueues().keySet()) {
      Queue<Player> queue = matchManager.getData().getPlayerQueues().get(queueType);
      if (queue != null && queue.contains(player)) {
        matchManager.leaveQueue(player, queueType);
        leftQueue = true;
        teamManager.disbandTeamIfInLobby(player);
      }
    }

    if (leftQueue) {
      logger.send(player, LEFT);
    } else {
      logger.send(player, LEAVE_NOT_INGAME);
    }
  }

  private void showOpenMatches(Player player, List<Match> openMatches) {
    logger.send(player, TAKEPLACE_AVAILABLE_HEADER);
    for (Match openMatch : openMatches) {
      int emptySlots = 0;
      for (MatchPlayer mp : openMatch.getPlayers()) {
        if (mp == null) {
          emptySlots++;
        }
      }
      logger.send(player, TAKEPLACE_AVAILABLE_ENTRY,
          String.valueOf(openMatch.getArena().getId()),
          openMatch.getArena().getType() + "v" + openMatch.getArena().getType(),
          String.valueOf(emptySlots));
    }
  }
}
