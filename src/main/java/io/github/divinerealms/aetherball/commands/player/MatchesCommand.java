package io.github.divinerealms.aetherball.commands.player;

import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_FOOTER;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_HEADER;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_NO_MATCHES;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.getFormattedMatches;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.utils.Logger;
import java.util.List;
import org.bukkit.command.CommandSender;

@CommandAlias("matches|queues|q")
public class MatchesCommand extends BaseCommand {

  private final Logger logger;
  private final MatchManager matchManager;

  public MatchesCommand(Manager manager) {
    this.logger = manager.getLogger();
    this.matchManager = manager.getMatchManager();
  }

  @Default
  @CatchUnknown
  @Description("View all active matches")
  public void onMatches(CommandSender sender) {
    List<String> output = getFormattedMatches(matchManager.getData().getMatches());

    if (!output.isEmpty()) {
      logger.send(sender, MATCHES_LIST_HEADER);
      output.forEach(msg -> logger.send(sender, msg));
      logger.send(sender, MATCHES_LIST_FOOTER);
    } else {
      logger.send(sender, MATCHES_LIST_NO_MATCHES);
    }
  }
}