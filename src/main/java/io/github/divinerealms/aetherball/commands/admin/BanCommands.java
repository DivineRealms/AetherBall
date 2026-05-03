package io.github.divinerealms.aetherball.commands.admin;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.matchmaking.BanManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

import static io.github.divinerealms.aetherball.configs.Lang.*;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_BAN;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_UNBAN;

@CommandAlias("aetherballadmin|abadmin|aba|fca")
public class BanCommands extends BaseCommand {

  private final BanManager banManager;

  public BanCommands(Manager manager) {
    this.banManager = manager.getBanManager();
  }

  @Subcommand("ban")
  @CommandPermission(PERM_BAN)
  @Syntax("<player> <time>")
  @CommandCompletion("@players 10s|30s|5min|10min|30min|1h")
  @Description("Ban a player from matchmaking")
  public void onBan(CommandSender sender, @Flags("other") Player target, @Optional String timeStr) {
    try {
      long duration;
      long secondsLeft;
      if (timeStr == null) {
        duration = Settings.getDefaultBanDuration();
        secondsLeft = TimeUnit.MILLISECONDS.toSeconds(duration);
      } else {
        duration = Utilities.parseTime(timeStr) * 1000L;
        secondsLeft = Utilities.parseTime(timeStr);
      }

      if (duration <= 0) {
        sendMessage(sender, USAGE, "fca ban <player> <time>");
        return;
      }

      banManager.banPlayer(target, duration);
      sendMessage(sender, PLAYER_BANNED, target.getDisplayName(),
          Utilities.formatTime(secondsLeft));
    } catch (NumberFormatException e) {
      sendMessage(sender, USAGE, "fca ban <player> <time>");
    }
  }

  @Subcommand("unban")
  @CommandPermission(PERM_UNBAN)
  @Syntax("<player>")
  @CommandCompletion("@players")
  @Description("Unban a player")
  public void onUnban(CommandSender sender, @Flags("other") Player target) {
    banManager.unbanPlayer(target);
    sendMessage(sender, PLAYER_UNBANNED, target.getDisplayName());
  }

  @Subcommand("checkban")
  @CommandPermission(PERM_BAN)
  @Syntax("<player>")
  @CommandCompletion("@players")
  @Description("Check if a player is banned")
  public void onCheckBan(CommandSender sender, @Flags("other") Player target) {
    if (!banManager.isBanned(target)) {
      sendMessage(sender, NOT_BANNED, target.getDisplayName());
    }
  }
}