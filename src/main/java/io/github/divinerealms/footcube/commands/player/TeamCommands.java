package io.github.divinerealms.footcube.commands.player;

import static io.github.divinerealms.footcube.utils.GameCommandsHelper.handleAccept;
import static io.github.divinerealms.footcube.utils.GameCommandsHelper.handleDecline;
import static io.github.divinerealms.footcube.utils.GameCommandsHelper.handleInvite;
import static io.github.divinerealms.footcube.utils.GameCommandsHelper.parseMatchType;
import static io.github.divinerealms.footcube.configs.Lang.FC_DISABLED;
import static io.github.divinerealms.footcube.configs.Lang.MATCH_TYPE_UNAVAILABLE;
import static io.github.divinerealms.footcube.configs.Lang.USAGE;
import static io.github.divinerealms.footcube.matchmaking.util.MatchUtils.isPlayerOnline;
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
import io.github.divinerealms.footcube.configs.Settings;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.utils.Logger;
import org.bukkit.entity.Player;

@CommandAlias("fc|footcube")
public class TeamCommands extends BaseCommand {

  private final FCManager fcManager;
  private final Logger logger;

  public TeamCommands(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
  }

  @Subcommand("team|t")
  @CommandPermission(PERM_PLAY)
  @Syntax("<accept|decline|match_type> [player]")
  @CommandCompletion("accept|decline|@matchtypes @players")
  @Description("Team management: accept/decline invites or invite players")
  public void onTeam(Player player, String action, @Optional @Flags("other") Player target) {
    if (!fcManager.getMatchManager().getData().isMatchesEnabled()) {
      logger.send(player, FC_DISABLED);
      return;
    }

    String actionLower = action.toLowerCase();
    if (actionLower.equals("accept")) {
      handleAccept(player, fcManager);
      return;
    }

    if (actionLower.equals("decline")) {
      handleDecline(player, fcManager);
      return;
    }

    Integer matchType = parseMatchType(actionLower);
    if (matchType != null && Settings.isMatchTypeEnabled(matchType)) {
      if (!isPlayerOnline(target)) {
        logger.send(player, USAGE, getExecSubcommand());
        return;
      }

      handleInvite(player, matchType, target, fcManager);
    } else {
      String availableTypes = fcManager.getMatchManager().getAvailableTypesString();
      logger.send(player, MATCH_TYPE_UNAVAILABLE, action, availableTypes);
    }
  }
}