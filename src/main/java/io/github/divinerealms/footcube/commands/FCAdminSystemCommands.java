package io.github.divinerealms.footcube.commands;

import static io.github.divinerealms.footcube.configs.Lang.FC_TOGGLE;
import static io.github.divinerealms.footcube.configs.Lang.OFF;
import static io.github.divinerealms.footcube.configs.Lang.ON;
import static io.github.divinerealms.footcube.configs.Lang.RELOAD;
import static io.github.divinerealms.footcube.configs.Lang.TASKS_REPORT_ENTRY;
import static io.github.divinerealms.footcube.configs.Lang.TASKS_REPORT_FOOTER;
import static io.github.divinerealms.footcube.configs.Lang.TASKS_REPORT_HEADER;
import static io.github.divinerealms.footcube.configs.Lang.TASKS_RESET_STATS;
import static io.github.divinerealms.footcube.configs.Lang.TASKS_RESTART;
import static io.github.divinerealms.footcube.configs.Lang.USAGE;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_ADMIN;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_TOGGLE;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.managers.TaskManager;
import io.github.divinerealms.footcube.matchmaking.MatchManager;
import io.github.divinerealms.footcube.matchmaking.arena.ArenaManager;
import io.github.divinerealms.footcube.tasks.BaseTask;
import io.github.divinerealms.footcube.utils.Logger;
import io.github.divinerealms.footcube.utils.TaskStats;
import org.bukkit.command.CommandSender;

@CommandAlias("fca|fcadmin|footcubeadmin")
public class FCAdminSystemCommands extends BaseCommand {

  private final FCManager fcManager;
  private final Logger logger;
  private final MatchManager matchManager;
  private final ArenaManager arenaManager;

  public FCAdminSystemCommands(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
    this.matchManager = fcManager.getMatchManager();
    this.arenaManager = fcManager.getArenaManager();
  }

  @Subcommand("reload")
  @CommandPermission(PERM_ADMIN)
  @CommandCompletion("all|configs|arenas")
  @Syntax("[all|configs|arenas]")
  @Description("Reload plugin components")
  public void onReload(CommandSender sender, @Default("all") String type) {
    String typeUpper = type.toUpperCase();

    switch (type.toLowerCase()) {
      case "configs":
        fcManager.getConfigManager().reloadAllConfigs();
        break;
      case "arenas":
        arenaManager.reloadArenas();
        break;
      case "all":
      default:
        fcManager.reload();
        typeUpper = "ALL";
        break;
    }

    logger.send(sender, RELOAD, typeUpper);
  }

  @Subcommand("tasks")
  @CommandPermission(PERM_ADMIN)
  @CommandCompletion("restart|reset|@nothing")
  @Syntax("[restart|reset]")
  @Description("Manage plugin tasks")
  public void onTasks(CommandSender sender, @Optional String action) {
    TaskManager taskManager = fcManager.getTaskManager();

    if (action == null) {
      logger.send(sender, TASKS_REPORT_HEADER,
          String.valueOf(taskManager.getRunningTaskCount()),
          String.valueOf(taskManager.getTaskCount())
      );

      for (BaseTask task : taskManager.getTasks()) {
        double average = task.getAverageExecutionTime();
        String status = task.isRunning() ? "&a✔" : "&c✘";
        String timeColor = getColorForTime(average);

        logger.send(sender, TASKS_REPORT_ENTRY,
            status, task.getTaskName(),
            timeColor + String.format("%.3f", average),
            String.valueOf(task.getTotalExecutions())
        );
      }

      TaskStats stats = taskManager.getStats();
      double totalAverage = stats.getAveragePerTask();
      logger.send(sender, TASKS_REPORT_FOOTER,
          getColorForTime(totalAverage) + String.format("%.3f", totalAverage)
      );
      return;
    }

    switch (action.toLowerCase()) {
      case "restart":
        taskManager.restart();
        logger.send(sender, TASKS_RESTART);
        break;

      case "reset":
        taskManager.resetAllStats();
        logger.send(sender, TASKS_RESET_STATS);
        break;

      default:
        logger.send(sender, USAGE, "fca tasks [restart|reset]");
        break;
    }
  }

  @Subcommand("toggle")
  @CommandPermission(PERM_TOGGLE)
  @Description("Toggle matchmaking system on/off")
  public void onToggle(CommandSender sender) {
    boolean state = matchManager.getData().isMatchesEnabled();
    matchManager.getData().setMatchesEnabled(!state);

    if (!matchManager.getData().isMatchesEnabled()) {
      matchManager.clearLobbiesAndQueues();
    }

    logger.send(sender, FC_TOGGLE, state ? OFF.toString() : ON.toString());
  }

  private String getColorForTime(double ms) {
    if (ms < 0.05) {
      return "&a";
    }
    if (ms < 0.15) {
      return "&e";
    }
    return "&c";
  }
}