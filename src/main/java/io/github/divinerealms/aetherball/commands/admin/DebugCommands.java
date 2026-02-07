package io.github.divinerealms.aetherball.commands.admin;

import static io.github.divinerealms.aetherball.configs.Lang.COMMAND_DISABLER_ALREADY_ADDED;
import static io.github.divinerealms.aetherball.configs.Lang.COMMAND_DISABLER_LIST;
import static io.github.divinerealms.aetherball.configs.Lang.COMMAND_DISABLER_SUCCESS;
import static io.github.divinerealms.aetherball.configs.Lang.COMMAND_DISABLER_SUCCESS_REMOVE;
import static io.github.divinerealms.aetherball.configs.Lang.COMMAND_DISABLER_WASNT_ADDED;
import static io.github.divinerealms.aetherball.configs.Lang.OFF;
import static io.github.divinerealms.aetherball.configs.Lang.ON;
import static io.github.divinerealms.aetherball.configs.Lang.TOGGLES_HIT_DEBUG;
import static io.github.divinerealms.aetherball.configs.Lang.USAGE;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_ADMIN;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_COMMAND_DISABLER;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_HIT_DEBUG;

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
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.utils.DisableCommands;
import io.github.divinerealms.aetherball.utils.Logger;
import org.bukkit.command.CommandSender;

@CommandAlias("aetherballadmin|abadmin|aba|fca")
public class DebugCommands extends BaseCommand {

  private final Manager manager;
  private final Logger logger;
  private final PhysicsData physicsData;
  private final DisableCommands disableCommands;

  public DebugCommands(Manager manager) {
    this.manager = manager;
    this.logger = manager.getLogger();
    this.physicsData = manager.getPhysicsData();
    this.disableCommands = manager.getDisableCommands();
  }

  @Subcommand("hitsdebug|hits")
  @CommandPermission(PERM_HIT_DEBUG)
  @Description("Toggle global hit debug visualization")
  public void onHitsDebug(CommandSender sender) {
    boolean status = physicsData.isHitDebugEnabled();
    physicsData.setHitDebugEnabled(!status);
    logger.send(sender, TOGGLES_HIT_DEBUG, status ? OFF.toString() : ON.toString());
  }

  @Subcommand("debug matches")
  @CommandPermission(PERM_ADMIN)
  public void onDebugMatches(CommandSender sender) {
    logger.send(sender, "{prefix-admin}&6Active Matches Debug:");
    for (Match m : manager.getMatchData().getMatches()) {
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