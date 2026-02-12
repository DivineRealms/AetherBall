package io.github.divinerealms.aetherball.commands.player;

import static io.github.divinerealms.aetherball.configs.Lang.FC_DISABLED;
import static io.github.divinerealms.aetherball.configs.Lang.JOIN_INVALIDTYPE;
import static io.github.divinerealms.aetherball.configs.Lang.LEFT;
import static io.github.divinerealms.aetherball.configs.Lang.STATSSET_IS_NOT_A_NUMBER;
import static io.github.divinerealms.aetherball.configs.Lang.TAKEPLACE_INGAME;
import static io.github.divinerealms.aetherball.configs.Lang.TAKEPLACE_NOPLACE;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.shouldPreventAbuse;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.handleInProgressLeave;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.handleQueueLeave;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.joinQueue;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.parseMatchType;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.showOpenMatches;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_PLAY;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.matchmaking.ban.BanManager;
import io.github.divinerealms.aetherball.matchmaking.highscore.HighScoreManager;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchData;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchSystem;
import io.github.divinerealms.aetherball.matchmaking.team.TeamManager;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("aetherball|ab|fc")
public class GameCommands extends BaseCommand {

  private final Manager manager;
  private final MatchManager matchManager;
  private final MatchSystem matchSystem;
  private final MatchData matchData;
  private final TeamManager teamManager;
  private final BanManager banManager;
  private final HighScoreManager highScoreManager;

  public GameCommands(Manager manager) {
    this.manager = manager;
    this.matchManager = manager.getMatchManager();
    this.matchSystem = manager.getMatchSystem();
    this.matchData = manager.getMatchData();
    this.teamManager = manager.getTeamManager();
    this.banManager = manager.getBanManager();
    this.highScoreManager = manager.getHighscoreManager();
  }

  @CommandAlias("fcjoin|fcj")
  @Subcommand("join|j")
  @CommandPermission(PERM_PLAY)
  @CommandCompletion("@matchtypes")
  @Syntax("<match_type>")
  @Description("Join a matchmaking queue")
  public void onJoin(Player player, String matchType) {
    Integer type = parseMatchType(matchType);
    if (type == null) {
      String availableTypes = matchManager.getAvailableTypesString();
      sendMessage(player, JOIN_INVALIDTYPE, matchType, availableTypes);
      return;
    }

    joinQueue(player, type, manager);
  }

  @CommandAlias("fcleave|fcl|leave")
  @Subcommand("leave|l")
  @CommandPermission(PERM_PLAY)
  @Description("Leave current match or queue")
  public void onLeave(Player player) {
    java.util.Optional<Match> matchOpt = matchManager.getMatch(player);

    if (matchOpt.isPresent()) {
      Match match = matchOpt.get();

      if (shouldPreventAbuse(match.getPhase())) {
        handleInProgressLeave(player, match, manager);
      }

      matchManager.leaveMatch(player);
      sendMessage(player, LEFT);
      teamManager.forceDisbandTeam(player);
    } else {
      handleQueueLeave(player, manager);
    }
  }

  @CommandAlias("takeplace|tkp")
  @Subcommand("takeplace|tkp")
  @CommandPermission(PERM_PLAY)
  @Syntax("[matchId|list]")
  @Description("Take an open spot in an ongoing match")
  public void onTakePlace(Player player, @Optional String arg) {
    if (!matchData.isMatchesEnabled()) {
      sendMessage(player, FC_DISABLED);
      return;
    }

    if (banManager.isBanned(player)) {
      return;
    }

    if (matchManager.getMatch(player).isPresent()) {
      sendMessage(player, TAKEPLACE_INGAME);
      return;
    }

    if (matchData.getOpenMatches().isEmpty()) {
      sendMessage(player, TAKEPLACE_NOPLACE);
      return;
    }

    List<Match> openMatches = matchData.getOpenMatches();

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
      sendMessage(player, STATSSET_IS_NOT_A_NUMBER, arg);
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
      Player player = (Player) sender;
      matchSystem.checkStats(targetName != null ? targetName : player.getName(), sender);
    } else {
      if (targetName == null) {
        sendMessage(sender, "{prefix_error}You need to specify a player.");
      } else {
        matchSystem.checkStats(targetName, sender);
      }
    }
  }

  @CommandAlias("highscores|best")
  @Subcommand("highscores|best")
  @Description("View top players leaderboard")
  public void onHighScores(CommandSender sender) {
    highScoreManager.showHighScores(sender);
  }
}
