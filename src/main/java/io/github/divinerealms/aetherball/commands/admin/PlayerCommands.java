package io.github.divinerealms.aetherball.commands.admin;

import static io.github.divinerealms.aetherball.configs.Lang.ADMIN_STATSET;
import static io.github.divinerealms.aetherball.configs.Lang.CLEAR_STATS_SUCCESS;
import static io.github.divinerealms.aetherball.configs.Lang.FORCE_LEAVE;
import static io.github.divinerealms.aetherball.configs.Lang.PLAYER_NOT_FOUND;
import static io.github.divinerealms.aetherball.configs.Lang.STATSSET_IS_NOT_A_NUMBER;
import static io.github.divinerealms.aetherball.configs.Lang.STATSSET_NOT_A_STAT;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_ADMIN;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_CLEAR_STATS;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_FORCE_LEAVE;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_STAT_SET;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.aetherball.AetherBall;
import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import java.util.Arrays;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("aetherballadmin|abadmin|aba|fca")
public class PlayerCommands extends BaseCommand {

  private final Manager manager;
  private final AetherBall plugin;
  private final MatchManager matchManager;
  private final PlayerDataManager dataManager;

  public PlayerCommands(Manager manager) {
    this.manager = manager;
    this.plugin = manager.getPlugin();
    this.matchManager = manager.getMatchManager();
    this.dataManager = manager.getDataManager();
  }

  @Subcommand("statset")
  @CommandPermission(PERM_STAT_SET)
  @Syntax("<player> <stat> <amount|clear>")
  @CommandCompletion(
      "@players wins|matches|ties|goals|assists|owngoals|winstreak|bestwinstreak|all")
  @Description("Set player statistics")
  public void onStatSet(
      CommandSender sender, @Flags("other") Player target, String stat, String amountStr) {
    PlayerData playerData = dataManager.get(target);
    if (playerData == null) {
      sendMessage(sender, PLAYER_NOT_FOUND);
      return;
    }

    boolean clear = amountStr.equalsIgnoreCase("clear");
    int amount = 0;

    if (!clear) {
      try {
        amount = Integer.parseInt(amountStr);
      } catch (NumberFormatException e) {
        sendMessage(sender, STATSSET_IS_NOT_A_NUMBER, amountStr);
        return;
      }
    }

    String statLower = stat.toLowerCase();
    String[] validStats = {
      "wins", "matches", "ties", "goals", "assists", "owngoals", "winstreak", "bestwinstreak"
    };

    if (Arrays.asList(validStats).contains(statLower)) {
      playerData.set(statLower, clear ? 0 : amount);
    } else if (statLower.equals("all")) {
      int finalAmount = clear ? 0 : amount;
      Arrays.asList(validStats).forEach(s -> playerData.set(s, finalAmount));
    } else {
      sendMessage(sender, STATSSET_NOT_A_STAT, stat);
      return;
    }

    dataManager.savePlayerData(target.getName());
    sendMessage(sender, ADMIN_STATSET, stat, target.getName(), String.valueOf(amount));
  }

  @CommandAlias("forceleave|fl")
  @Subcommand("forceleave|fl")
  @CommandPermission(PERM_FORCE_LEAVE)
  @Syntax("<player>")
  @CommandCompletion("@players")
  @Description("Force a player to leave their match")
  public void onForceLeave(CommandSender sender, @Flags("other") Player target) {
    matchManager.leaveMatch(target);
    sendMessage(sender, FORCE_LEAVE, target.getDisplayName());
  }

  @CommandAlias("refreshprefix|rp")
  @Subcommand("refreshprefix|rp")
  @CommandPermission(PERM_ADMIN)
  @Syntax("<player>")
  @CommandCompletion("@players")
  @Description("Refresh player's prefix cache")
  public void onRefreshPrefix(CommandSender sender, @Flags("other") Player target) {
    manager.cachePrefixedName(target);
    sendMessage(sender, "Refreshing prefix for " + target.getName() + "...");

    plugin
        .getServer()
        .getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              String refreshed = manager.getPrefixedName(target.getUniqueId());
              sendMessage(sender, "Refreshed: " + refreshed);
            },
            20L);
  }

  @Subcommand("clear stats")
  @CommandPermission(PERM_CLEAR_STATS)
  @Description("Clear all player statistics")
  public void onClearStats(CommandSender sender) {
    dataManager.clearAllStats();
    sendMessage(sender, CLEAR_STATS_SUCCESS);
  }
}
