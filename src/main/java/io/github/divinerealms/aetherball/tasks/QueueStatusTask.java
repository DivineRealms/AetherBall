package io.github.divinerealms.aetherball.tasks;

import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_LOBBY;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_WAITING;
import static io.github.divinerealms.aetherball.configs.Lang.QUEUE_ACTIONBAR;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.isPlayerOnline;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchPhase;
import io.github.divinerealms.aetherball.matchmaking.player.MatchPlayer;
import io.github.divinerealms.aetherball.utils.Logger;
import org.bukkit.entity.Player;

public class QueueStatusTask extends BaseTask {

  private final Logger logger;

  public QueueStatusTask(Manager manager) {
    super(manager, "QueueStatus", 40, false);
    this.logger = manager.getLogger();
  }

  @Override
  protected void kaboom() {
    for (Match match : manager.getMatchData().getMatches()) {
      if (match == null || match.getPhase() != MatchPhase.LOBBY) {
        continue;
      }

      if (match.getArena() == null || match.getPlayers() == null) {
        continue;
      }

      int matchType = match.getArena().getType();
      int requiredPlayers = matchType * 2;
      String matchTypeString = Settings.getMatchTypeName(matchType);

      int currentPlayers = 0;
      for (MatchPlayer mp : match.getPlayers()) {
        if (mp != null && mp.getPlayer() != null && mp.getPlayer().isOnline()) {
          currentPlayers++;
        }
      }

      String colorCode = (currentPlayers == requiredPlayers)
          ? "&a"
          : "&e";

      for (MatchPlayer matchPlayer : match.getPlayers()) {
        if (!isPlayerOnline(matchPlayer)) {
          continue;
        }

        Player player = matchPlayer.getPlayer();
        logger.sendActionBar(player, QUEUE_ACTIONBAR,
            MATCHES_LIST_LOBBY.replace(
                matchTypeString, String.valueOf(match.getArena().getId())
            ),
            MATCHES_LIST_WAITING.toString(),
            colorCode + currentPlayers,
            String.valueOf(requiredPlayers)
        );
      }
    }
  }
}
