package io.github.divinerealms.aetherball.commands.player;

import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_FOOTER;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_HEADER;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_NO_MATCHES;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.getFormattedMatches;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchData;
import java.util.List;
import org.bukkit.command.CommandSender;

@CommandAlias("matches|queues|q")
public class MatchesCommand extends BaseCommand {

  private final MatchData matchData;

  public MatchesCommand(Manager manager) {
    this.matchData = manager.getMatchData();
  }

  @Default
  @CatchUnknown
  @Description("View all active matches")
  public void onMatches(CommandSender sender) {
    List<String> output = getFormattedMatches(matchData.getMatches());

    if (!output.isEmpty()) {
      sendMessage(sender, MATCHES_LIST_HEADER);
      output.forEach(msg -> sendMessage(sender, msg));
      sendMessage(sender, MATCHES_LIST_FOOTER);
    } else {
      sendMessage(sender, MATCHES_LIST_NO_MATCHES);
    }
  }
}