package io.github.divinerealms.aetherball.commands.admin;

import static io.github.divinerealms.aetherball.configs.Lang.HELP_ADMIN;
import static io.github.divinerealms.aetherball.configs.Lang.HELP_FOOTER;
import static io.github.divinerealms.aetherball.configs.Lang.HELP_HEADER;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_ADMIN;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import io.github.divinerealms.aetherball.core.Manager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("aetherballadmin|abadmin|aba|fca")
@CommandPermission(PERM_ADMIN)
@Description("AetherBall Admin Commands")
public class BaseAdmin extends BaseCommand {

  private final Manager manager;

  public BaseAdmin(Manager manager) {
    this.manager = manager;
  }

  @Default
  @CatchUnknown
  public void onDefault(CommandSender sender) {
    if (sender instanceof Player) {
      sendHelp(sender);
    } else {
      manager.sendBanner();
    }
  }

  @Subcommand("help|h")
  @Description("Show help menu")
  public void onHelp(CommandSender sender) {
    sendHelp(sender);
  }

  private void sendHelp(CommandSender sender) {
    sendMessage(sender, HELP_HEADER);
    sendMessage(sender, HELP_ADMIN);
    sendMessage(sender, HELP_FOOTER);
  }
}