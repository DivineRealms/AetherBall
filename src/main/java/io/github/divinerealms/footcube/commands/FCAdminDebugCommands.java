package io.github.divinerealms.footcube.commands;

import static io.github.divinerealms.footcube.configs.Lang.COMMAND_DISABLER_ALREADY_ADDED;
import static io.github.divinerealms.footcube.configs.Lang.COMMAND_DISABLER_LIST;
import static io.github.divinerealms.footcube.configs.Lang.COMMAND_DISABLER_SUCCESS;
import static io.github.divinerealms.footcube.configs.Lang.COMMAND_DISABLER_SUCCESS_REMOVE;
import static io.github.divinerealms.footcube.configs.Lang.COMMAND_DISABLER_WASNT_ADDED;
import static io.github.divinerealms.footcube.configs.Lang.OFF;
import static io.github.divinerealms.footcube.configs.Lang.ON;
import static io.github.divinerealms.footcube.configs.Lang.TOGGLES_HIT_DEBUG;
import static io.github.divinerealms.footcube.configs.Lang.USAGE;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_ADMIN;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_COMMAND_DISABLER;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_HIT_DEBUG;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.matchmaking.Match;
import io.github.divinerealms.footcube.physics.PhysicsData;
import io.github.divinerealms.footcube.utils.DisableCommands;
import io.github.divinerealms.footcube.utils.Logger;
import org.bukkit.command.CommandSender;

@CommandAlias("fca|fcadmin|footcubeadmin")
public class FCAdminDebugCommands extends BaseCommand {

  private final FCManager fcManager;
  private final Logger logger;
  private final PhysicsData physicsData;
  private final DisableCommands disableCommands;

  public FCAdminDebugCommands(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
    this.physicsData = fcManager.getPhysicsData();
    this.disableCommands = fcManager.getDisableCommands();
  }

  @Subcommand("hitsdebug|hits")
  @CommandPermission(PERM_HIT_DEBUG)
  @Description("Toggle global hit debug visualization")
  public void onHitsDebug(CommandSender sender) {
    boolean status = physicsData.isHitDebugEnabled();
    physicsData.hitDebugEnabled = !status;
    logger.send(sender, TOGGLES_HIT_DEBUG, status ? OFF.toString() : ON.toString());
  }

  @Subcommand("debug matches")
  @CommandPermission(PERM_ADMIN)
  public void onDebugMatches(CommandSender sender) {
    logger.send(sender, "{prefix-admin}&6Active Matches Debug:");
    for (Match m : fcManager.getMatchData().getMatches()) {
      boolean hasArena = m.getArena() != null;
      int arenaId = hasArena ? m.getArena().getId() : -1;
      logger.send(sender, "{prefix-admin}&7- Match: Arena=" + arenaId +
          " (null=" + !hasArena + "), Phase=" + m.getPhase());
    }
  }

  @Subcommand("commanddisabler|cd")
  @CommandPermission(PERM_COMMAND_DISABLER)
  @CommandCompletion("add|remove|list")
  @Syntax("<add|remove|list> [command]")
  @Description("Manage blacklisted commands during matches")
  public void onCommandDisabler(CommandSender sender, String action, @Optional String command) {
    switch (action.toLowerCase()) {
      case "add":
        if (command == null) {
          logger.send(sender, USAGE, "fca commanddisabler add <command>");
          return;
        }
        if (disableCommands.addCommand(command)) {
          logger.send(sender, COMMAND_DISABLER_SUCCESS, command);
        } else {
          logger.send(sender, COMMAND_DISABLER_ALREADY_ADDED);
        }
        break;

      case "remove":
        if (command == null) {
          logger.send(sender, USAGE, "fca commanddisabler remove <command>");
          return;
        }
        if (disableCommands.removeCommand(command)) {
          logger.send(sender, COMMAND_DISABLER_SUCCESS_REMOVE, command);
        } else {
          logger.send(sender, COMMAND_DISABLER_WASNT_ADDED);
        }
        break;

      case "list":
        logger.send(sender, COMMAND_DISABLER_LIST);
        disableCommands.getCommands().forEach(c -> logger.send(sender, "&7" + c));
        break;

      default:
        logger.send(sender, USAGE, "fca commanddisabler <add|remove|list> [command]");
        break;
    }
  }
}