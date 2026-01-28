package io.github.divinerealms.footcube.commands.player;

import static io.github.divinerealms.footcube.configs.Lang.FC_DISABLED;
import static io.github.divinerealms.footcube.configs.Lang.JOIN_ALREADYINGAME;
import static io.github.divinerealms.footcube.configs.Lang.JOIN_INVALIDTYPE;
import static io.github.divinerealms.footcube.configs.Lang.OR;
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
import static io.github.divinerealms.footcube.configs.Lang.USAGE;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_PLAY;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.matchmaking.MatchManager;
import io.github.divinerealms.footcube.matchmaking.team.TeamManager;
import io.github.divinerealms.footcube.utils.Logger;
import org.bukkit.entity.Player;

@CommandAlias("fc|footcube")
public class TeamCommands extends BaseCommand {

  private final FCManager fcManager;
  private final Logger logger;
  private final MatchManager matchManager;
  private final TeamManager teamManager;

  public TeamCommands(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
    this.matchManager = fcManager.getMatchManager();
    this.teamManager = matchManager.getTeamManager();
  }

  @Subcommand("team|t")
  @CommandPermission(PERM_PLAY)
  @Syntax("<accept|decline|2v2|3v3|4v4> [player]")
  @CommandCompletion("accept|decline|2v2|3v3|4v4 @players")
  @Description("Team management: accept/decline invites or invite players")
  public void onTeam(Player player, String action, @Optional @Flags("other") Player target) {
    if (!checkMatchesEnabled(player)) {
      return;
    }

    String actionLower = action.toLowerCase();
    switch (actionLower) {
      case "accept":
        handleAccept(player);
        break;
      case "decline":
        handleDecline(player);
        break;
      case "1v1":
      case "2v2":
      case "3v3":
      case "4v4":
      case "5v5":
        if (target == null) {
          logger.send(player, USAGE, getExecSubcommand());
          return;
        }
        handleInvite(player, actionLower, target);
        break;
      default:
        logger.send(player, JOIN_INVALIDTYPE, action, OR.toString());
        break;
    }
  }

  private boolean checkMatchesEnabled(Player player) {
    if (!matchManager.getData().isMatchesEnabled()) {
      logger.send(player, FC_DISABLED);
      return false;
    }
    return true;
  }

  private void handleAccept(Player player) {
    if (teamManager.isInTeam(player)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM);
      return;
    }

    if (teamManager.noInvite(player)) {
      logger.send(player, TEAM_NO_REQUEST);
      return;
    }

    if (isInQueueOrMatch(player)) {
      logger.send(player, JOIN_ALREADYINGAME);
      return;
    }

    Player target = teamManager.getInviter(player);
    String targetName = target != null && target.isOnline() ? target.getDisplayName() : "";

    if (target == null || !target.isOnline()) {
      logger.send(player, TEAM_NOT_ONLINE, targetName);
      return;
    }

    if (teamManager.isInTeam(target)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM_2, target.getDisplayName());
      teamManager.removeInvite(player);
      return;
    }

    if (isInQueueOrMatch(target)) {
      logger.send(player, TEAM_ALREADY_IN_GAME, target.getDisplayName());
      return;
    }

    int mType = teamManager.getInviteMatchType(player);
    teamManager.createTeam(target, player, mType);
    logger.send(player, TEAM_ACCEPT_SELF, target.getDisplayName());
    logger.send(target, TEAM_ACCEPT_OTHER, player.getDisplayName());

    matchManager.joinQueue(player, mType);
    teamManager.removeInvite(player);
  }

  private void handleDecline(Player player) {
    if (teamManager.noInvite(player)) {
      logger.send(player, TEAM_NO_REQUEST);
      return;
    }

    Player target = teamManager.getInviter(player);
    if (target != null && target.isOnline()) {
      logger.send(target, TEAM_DECLINE_OTHER, player.getDisplayName());
    }

    logger.send(player, TEAM_DECLINE_SELF);
    teamManager.removeInvite(player);
  }

  private void handleInvite(Player player, String matchType, Player target) {
    if (teamManager.isInTeam(player)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM);
      return;
    }

    if (teamManager.isInTeam(target)) {
      logger.send(player, TEAM_ALREADY_IN_TEAM_2, target.getDisplayName());
      return;
    }

    if (isInQueueOrMatch(player)) {
      logger.send(player, JOIN_ALREADYINGAME);
      return;
    }

    if (isInQueueOrMatch(target)) {
      logger.send(player, TEAM_ALREADY_IN_GAME, target.getDisplayName());
      return;
    }

    int inviteType = getMatchType(matchType);
    teamManager.invite(player, target, inviteType);
    logger.send(player, TEAM_WANTS_TO_TEAM_SELF, target.getDisplayName(), matchType);
    logger.send(target, TEAM_WANTS_TO_TEAM_OTHER, player.getDisplayName(), matchType);
  }

  private boolean isInQueueOrMatch(Player player) {
    return fcManager.getMatchSystem().isInAnyQueue(player)
        || matchManager.getMatch(player).isPresent();
  }

  private int getMatchType(String matchType) {
    switch (matchType) {
      case "3v3":
        return 3;
      case "4v4":
        return 4;
      case "5v5":
        return 5;
      case "2v2":
      default:
        return 2;
    }
  }
}