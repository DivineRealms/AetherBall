package io.github.divinerealms.aetherball.matchmaking.team;

import static io.github.divinerealms.aetherball.configs.Lang.TEAM_DISBANDED;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_INVITE_EXPIRED;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.isPlayerOnline;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchPhase;
import io.github.divinerealms.aetherball.utils.Logger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Getter
public class TeamManager {

  private final Manager manager;
  private final Plugin plugin;
  private final Logger logger;
  private final Map<Player, Map<Player, Integer>> teamInvites = new HashMap<>();
  private final Map<UUID, Team> playerTeams = new HashMap<>();

  public TeamManager(Manager manager) {
    this.manager = manager;
    this.plugin = manager.getPlugin();
    this.logger = manager.getLogger();
  }

  public void invite(Player inviter, Player invited, int matchType) {
    teamInvites.put(invited, Collections.singletonMap(inviter, matchType));
    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
      teamInvites.remove(invited);
      logger.send(inviter, TEAM_INVITE_EXPIRED);
    }, Settings.getTeamExpiry());
  }

  public Player getInviter(Player invited) {
    Map<Player, Integer> invite = teamInvites.get(invited);
    if (invite == null || invite.isEmpty()) {
      return null;
    }
    return invite.keySet().iterator().next();
  }

  public int getInviteMatchType(Player invited) {
    Map<Player, Integer> invite = teamInvites.get(invited);
    if (invite == null || invite.isEmpty()) {
      return -1;
    }
    return invite.values().iterator().next();
  }

  public void removeInvite(Player invited) {
    teamInvites.remove(invited);
  }

  public boolean noInvite(Player invited) {
    return !teamInvites.containsKey(invited);
  }

  public Team getTeam(Player player) {
    return playerTeams.get(player.getUniqueId());
  }

  public boolean isInTeam(Player player) {
    return playerTeams.containsKey(player.getUniqueId());
  }

  public void createTeam(Player leader, Player member, int matchType) {
    Team team = new Team(leader, member, matchType);
    playerTeams.put(leader.getUniqueId(), team);
    playerTeams.put(member.getUniqueId(), team);
  }

  public void disbandTeam(Team team) {
    team.getMembers().forEach(member -> playerTeams.remove(member.getUniqueId()));
  }

  public void disbandTeamIfInLobby(Player leaver) {
    Team team = getTeam(leaver);
    if (team == null) {
      return;
    }

    boolean anyInMatchLobby = false;
    Optional<Match> matchOpt = manager.getMatchManager().getMatch(leaver);
    if (matchOpt.isPresent()) {
      Match match = matchOpt.get();
      if (match.getPhase() == MatchPhase.LOBBY) {
        anyInMatchLobby = true;
      }
    }

    boolean anyInQueue = false;
    Collection<Queue<Player>> playerQueues = manager.getMatchManager().getData().getPlayerQueues()
        .values();

    for (Queue<Player> queue : playerQueues) {
      if (queue != null && queue.contains(leaver)) {
        anyInQueue = true;
        break;
      }
    }

    if (!anyInMatchLobby && !anyInQueue) {
      return;
    }

    List<Player> members = team.getMembers();
    if (members != null) {
      for (Player player : members) {
        if (isPlayerOnline(player) && !player.equals(leaver)) {
          logger.send(player, TEAM_DISBANDED, leaver.getName());
        }
      }
    }

    disbandTeam(team);
  }

  public void forceDisbandTeam(Player leaver) {
    Team team = getTeam(leaver);
    if (team == null) {
      return;
    }

    for (Player player : team.getMembers()) {
      if (isPlayerOnline(player) && !player.equals(leaver)) {
        logger.send(player, TEAM_DISBANDED, leaver.getName());
      }
    }

    disbandTeam(team);
  }
}
