package io.github.divinerealms.aetherball.commands.admin;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static io.github.divinerealms.aetherball.configs.Lang.*;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_ADMIN;

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

  @Subcommand("configuration|c")
  @Description("Show current configuration")
  public void onConfiguration(CommandSender sender) {
    sendMessage(
        sender,
        CONFIGURATION,
        String.valueOf(Settings.KICK_BASE_POWER_REGULAR.asDouble()),
        String.valueOf(Settings.KICK_COOLDOWN_REGULAR.asLong()),
        String.valueOf(Settings.KICK_BASE_POWER_CHARGED.asDouble()),
        String.valueOf(Settings.KICK_COOLDOWN_CHARGED.asLong()),
        String.valueOf(Settings.SOFT_CAP.asDouble()),
        String.valueOf(Settings.MAX_KICK_POWER.asInt()),
        String.valueOf(Settings.KICK_COOLDOWN_RISE.asLong()),
        String.valueOf(Settings.KICK_VERTICAL_BOOST.asDouble()));
  }
}
