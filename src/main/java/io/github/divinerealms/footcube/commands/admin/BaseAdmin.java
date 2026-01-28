package io.github.divinerealms.footcube.commands.admin;

import static io.github.divinerealms.footcube.configs.Lang.HELP_ADMIN;
import static io.github.divinerealms.footcube.configs.Lang.HELP_FOOTER;
import static io.github.divinerealms.footcube.configs.Lang.HELP_HEADER;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_ADMIN;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.utils.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("fca|fcadmin|footcubeadmin")
@CommandPermission(PERM_ADMIN)
@Description("FootCube Admin Commands")
public class BaseAdmin extends BaseCommand {

  private final FCManager fcManager;
  private final Logger logger;

  public BaseAdmin(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
  }

  @Default
  @CatchUnknown
  public void onDefault(CommandSender sender) {
    if (sender instanceof Player) {
      sendHelp(sender);
    } else {
      fcManager.sendBanner();
    }
  }

  @Subcommand("help|h")
  @Description("Show help menu")
  public void onHelp(CommandSender sender) {
    sendHelp(sender);
  }

  private void sendHelp(CommandSender sender) {
    logger.send(sender, HELP_HEADER);
    logger.send(sender, HELP_ADMIN);
    logger.send(sender, HELP_FOOTER);
  }
}