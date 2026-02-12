package io.github.divinerealms.aetherball.commands;

import static io.github.divinerealms.aetherball.configs.Lang.BANNER_PLAYER;
import static io.github.divinerealms.aetherball.configs.Lang.HELP_CATEGORIZED;
import static io.github.divinerealms.aetherball.configs.Lang.HELP_CATEGORY_1;
import static io.github.divinerealms.aetherball.configs.Lang.HELP_CATEGORY_2;
import static io.github.divinerealms.aetherball.configs.Lang.HELP_CATEGORY_3;
import static io.github.divinerealms.aetherball.configs.Lang.HELP_CATEGORY_4;
import static io.github.divinerealms.aetherball.configs.Lang.HELP_CATEGORY_UNKNOWN;
import static io.github.divinerealms.aetherball.configs.Lang.HELP_FOOTER;
import static io.github.divinerealms.aetherball.configs.Lang.HELP_HEADER;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.aetherball.core.Manager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("aetherball|ab|fc")
@Description("AetherBall base command.")
public class MainCommand extends BaseCommand {

  private final Manager manager;

  public MainCommand(Manager manager) {
    this.manager = manager;
  }

  @Default
  @CatchUnknown
  public void onDefault(CommandSender sender) {
    if (sender instanceof Player) {
      sendMessage(sender, BANNER_PLAYER,
          manager.getPlugin().getName(),
          manager.getPlugin().getDescription().getVersion(),
          String.join(", ", manager.getPlugin().getDescription().getAuthors())
      );
    } else {
      manager.sendBanner();
    }
  }

  @Subcommand("help|h")
  @Description("Show help menu")
  @Syntax("[category]")
  @CommandCompletion("gameplay|teams|settings|utility")
  public void onHelp(CommandSender sender, @Optional String category) {
    if (category == null) {
      sendMessage(sender, HELP_CATEGORIZED);
    } else {
      showCategoryHelp(sender, category);
    }
  }

  private void showCategoryHelp(CommandSender sender, String category) {
    sendMessage(sender, HELP_HEADER);

    switch (category) {
      case "gameplay":
        sendMessage(sender, HELP_CATEGORY_1);
        break;
      case "teams":
        sendMessage(sender, HELP_CATEGORY_2);
        break;
      case "settings":
        sendMessage(sender, HELP_CATEGORY_3);
        break;
      case "utility":
        sendMessage(sender, HELP_CATEGORY_4);
        break;
      default:
        sendMessage(sender, HELP_CATEGORY_UNKNOWN);
    }

    sendMessage(sender, HELP_FOOTER);
  }
}
