package io.github.divinerealms.aetherball.commands;

import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_NO_MATCHES;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHMAN_FORCE_END;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_MATCHMAN;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.matchmaking.MatchPhase;
import java.util.Optional;
import org.bukkit.entity.Player;

@CommandAlias("matchman|mm")
@CommandPermission(PERM_MATCHMAN)
@Description("Manipulate AetherBall matches")
public class MatchMan extends BaseCommand {

  private final MatchManager matchManager;

  public MatchMan(Manager manager) {
    this.matchManager = manager.getMatchManager();
  }

  @Subcommand("start")
  @Description("Force start current match")
  public void onMatchStart(Player player) {
    matchManager.forceStartMatch(player);
  }

  @Subcommand("end")
  @Description("Force end current match")
  public void onMatchEnd(Player player) {
    Optional<Match> matchOptional = matchManager.getMatch(player);

    if (matchOptional.isPresent()) {
      Match match = matchOptional.get();

      if (match.getPhase() == MatchPhase.LOBBY || match.getPhase() == MatchPhase.STARTING) {
        matchManager.endMatch(match);
      } else {
        match.setPhase(MatchPhase.ENDED);
      }

      sendMessage(player, MATCHMAN_FORCE_END,
          Settings.getMatchTypeName(match.getArena().getType()));
    } else {
      sendMessage(player, MATCHES_LIST_NO_MATCHES);
    }
  }
}