package io.github.divinerealms.aetherball.tasks;

import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_LOBBY;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_WAITING;
import static io.github.divinerealms.aetherball.configs.Lang.QUEUE_ACTIONBAR;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendActionBar;
import static io.github.divinerealms.aetherball.utils.MatchUtils.isPlayerOnline;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchPhase;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchData;
import io.github.divinerealms.aetherball.matchmaking.player.MatchPlayer;
import org.bukkit.entity.Player;

public class QueueStatusTask extends BaseTask {

  private final MatchData matchData;

  public QueueStatusTask(Manager manager) {
    super(manager, "QueueStatus", 40, false);
    this.matchData = manager.getMatchData();
  }

  @Override
  protected void kaboom() {
    for (Match match : matchData.getMatches()) {
      if (match == null || match.getPhase() != MatchPhase.LOBBY) {
        continue;
      }

      if (match.getArena() == null || match.getPlayers() == null) {
        continue;
      }

      int matchType = match.getArena().type();
      int requiredPlayers = matchType * 2;
      String matchTypeString = Settings.getMatchTypeName(matchType);

      int currentPlayers = 0;
      for (MatchPlayer mp : match.getPlayers()) {
        if (mp != null && mp.getPlayer() != null && mp.getPlayer().isOnline()) {
          currentPlayers++;
        }
      }

      String colorCode = (currentPlayers == requiredPlayers) ? "&a" : "&e";

      for (MatchPlayer matchPlayer : match.getPlayers()) {
        if (!isPlayerOnline(matchPlayer)) {
          continue;
        }

        Player player = matchPlayer.getPlayer();
        sendActionBar(
            player,
            QUEUE_ACTIONBAR,
            MATCHES_LIST_LOBBY.replace(matchTypeString, String.valueOf(match.getArena().id())),
            MATCHES_LIST_WAITING.toString(),
            colorCode + currentPlayers,
            String.valueOf(requiredPlayers));
      }
    }
  }
}
