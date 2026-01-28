package io.github.divinerealms.footcube.commands.admin;

import static io.github.divinerealms.footcube.configs.Lang.NOT_BANNED;
import static io.github.divinerealms.footcube.configs.Lang.PLAYER_BANNED;
import static io.github.divinerealms.footcube.configs.Lang.PLAYER_UNBANNED;
import static io.github.divinerealms.footcube.configs.Lang.USAGE;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_BAN;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_UNBAN;

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
import io.github.divinerealms.footcube.managers.Utilities;
import io.github.divinerealms.footcube.utils.Logger;
import java.util.concurrent.TimeUnit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("fca|fcadmin|footcubeadmin")
public class BanCommands extends BaseCommand {

  private final FCManager fcManager;
  private final Logger logger;

  public BanCommands(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
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
        logger.send(sender, USAGE, "fca ban <player> <time>");
        return;
      }

      fcManager.getBanManager().banPlayer(target, duration);
      logger.send(sender, PLAYER_BANNED, target.getDisplayName(),
          Utilities.formatTime(secondsLeft));
    } catch (NumberFormatException e) {
      logger.send(sender, USAGE, "fca ban <player> <time>");
    }
  }

  @Subcommand("unban")
  @CommandPermission(PERM_UNBAN)
  @Syntax("<player>")
  @CommandCompletion("@players")
  @Description("Unban a player")
  public void onUnban(CommandSender sender, @Flags("other") Player target) {
    fcManager.getBanManager().unbanPlayer(target);
    logger.send(sender, PLAYER_UNBANNED, target.getDisplayName());
  }

  @Subcommand("checkban")
  @CommandPermission(PERM_BAN)
  @Syntax("<player>")
  @CommandCompletion("@players")
  @Description("Check if a player is banned")
  public void onCheckBan(CommandSender sender, @Flags("other") Player target) {
    if (!fcManager.getBanManager().isBanned(target)) {
      logger.send(sender, NOT_BANNED, target.getDisplayName());
    }
  }
}