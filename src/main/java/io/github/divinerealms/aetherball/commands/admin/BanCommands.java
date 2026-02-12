package io.github.divinerealms.aetherball.commands.admin;

import static io.github.divinerealms.aetherball.configs.Lang.NOT_BANNED;
import static io.github.divinerealms.aetherball.configs.Lang.PLAYER_BANNED;
import static io.github.divinerealms.aetherball.configs.Lang.PLAYER_UNBANNED;
import static io.github.divinerealms.aetherball.configs.Lang.USAGE;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_BAN;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_UNBAN;

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
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.matchmaking.ban.BanManager;
import java.util.concurrent.TimeUnit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        duration = Utilities.parseTime(timeStr);
        secondsLeft = duration;
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