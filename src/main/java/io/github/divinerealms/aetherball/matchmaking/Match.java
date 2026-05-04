package io.github.divinerealms.aetherball.matchmaking;

import io.github.divinerealms.aetherball.matchmaking.player.MatchPlayer;
import java.util.List;
import lombok.Data;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import org.bukkit.entity.Slime;

@Data
public class Match {

  private final ArenaManager.Arena arena;
  private final List<MatchPlayer> players;
  private MatchPhase phase;
  private int countdown, scoreRed, scoreBlue, tick;
  private Slime cube;
  private MatchPlayer lastTouch, secondLastTouch;
  private Scoreboard lobbyScoreboard, matchScoreboard;
  private long startTime, lastTakePlaceAnnounceTick;
  private boolean takePlaceNeeded;

  public Match(ArenaManager.Arena arena, List<MatchPlayer> players) {
    if (arena == null) {
      throw new IllegalArgumentException("Cannot create match with null arena!");
    }
    this.arena = arena;
    this.players = players;
    this.phase = MatchPhase.LOBBY;
    this.countdown = 0;
    this.scoreRed = 0;
    this.scoreBlue = 0;
    this.startTime = 0;
    this.tick = 0;
    this.lastTakePlaceAnnounceTick = 0;
    this.takePlaceNeeded = false;
  }
}
