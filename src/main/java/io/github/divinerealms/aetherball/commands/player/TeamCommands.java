package io.github.divinerealms.aetherball.commands.player;

import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.handleAccept;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.handleDecline;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.handleInvite;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.parseMatchType;
import static io.github.divinerealms.aetherball.configs.Lang.FC_DISABLED;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_TYPE_UNAVAILABLE;
import static io.github.divinerealms.aetherball.configs.Lang.USAGE;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.isPlayerOnline;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_PLAY;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.utils.Logger;
import org.bukkit.entity.Player;

@CommandAlias("aetherball|ab|fc")
public class TeamCommands extends BaseCommand {

  private final Manager manager;
  private final Logger logger;

  public TeamCommands(Manager manager) {
    this.manager = manager;
    this.logger = manager.getLogger();
  }

  @Subcommand("team|t")
  @CommandPermission(PERM_PLAY)
  @Syntax("<accept|decline|match_type> [player]")
  @CommandCompletion("accept|decline|@matchtypes @players")
  @Description("Team management: accept/decline invites or invite players")
  public void onTeam(Player player, String action, @Optional @Flags("other") Player target) {
    if (!manager.getMatchManager().getData().isMatchesEnabled()) {
      logger.send(player, FC_DISABLED);
      return;
    }

    String actionLower = action.toLowerCase();
    if (actionLower.equals("accept")) {
      handleAccept(player, manager);
      return;
    }

    if (actionLower.equals("decline")) {
      handleDecline(player, manager);
      return;
    }

    Integer matchType = parseMatchType(actionLower);
    if (matchType != null && Settings.isMatchTypeEnabled(matchType)) {
      if (!isPlayerOnline(target)) {
        logger.send(player, USAGE, getExecSubcommand());
        return;
      }

      handleInvite(player, matchType, target, manager);
    } else {
      String availableTypes = manager.getMatchManager().getAvailableTypesString();
      logger.send(player, MATCH_TYPE_UNAVAILABLE, action, availableTypes);
    }
  }
}