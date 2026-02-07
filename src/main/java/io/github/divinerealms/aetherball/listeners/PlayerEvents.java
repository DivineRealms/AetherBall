package io.github.divinerealms.aetherball.listeners;

import static io.github.divinerealms.aetherball.configs.Lang.COMMAND_DISABLER_CANT_USE;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_DISBANDED;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.isPlayerOnline;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_BYPASS_DISABLED_COMMANDS;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.shouldPreventAbuse;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.matchmaking.MatchPhase;
import io.github.divinerealms.aetherball.matchmaking.player.MatchPlayer;
import io.github.divinerealms.aetherball.matchmaking.player.TeamColor;
import io.github.divinerealms.aetherball.matchmaking.team.Team;
import io.github.divinerealms.aetherball.matchmaking.team.TeamManager;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import io.github.divinerealms.aetherball.utils.DisableCommands;
import io.github.divinerealms.aetherball.utils.Logger;
import io.github.divinerealms.aetherball.utils.PlayerSettings;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class PlayerEvents implements Listener {

  private final Manager manager;
  private final Logger logger;
  private final Plugin plugin;
  private final MatchManager matchManager;
  private final TeamManager teamManager;
  private final PlayerDataManager dataManager;
  private final DisableCommands disableCommands;
  private final PhysicsSystem system;

  public PlayerEvents(Manager manager) {
    this.manager = manager;
    this.logger = manager.getLogger();
    this.plugin = manager.getPlugin();
    this.matchManager = manager.getMatchManager();
    this.teamManager = matchManager.getTeamManager();
    this.dataManager = manager.getDataManager();
    this.disableCommands = manager.getDisableCommands();
    this.system = manager.getPhysicsSystem();
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onDisabledCommand(PlayerCommandPreprocessEvent event) {
    Player player = event.getPlayer();
    if (event.getMessage().equalsIgnoreCase("/tab reload")) {
      manager.reloadTabAPI();
    }

    if (player.hasPermission(PERM_BYPASS_DISABLED_COMMANDS)) {
      return;
    }

    Optional<Match> matchOpt = matchManager.getMatch(player);
    if (matchOpt.isEmpty()) {
      return;
    }

    if (!shouldPreventAbuse(matchOpt.get().getPhase())) {
      return;
    }

    String raw = event.getMessage().toLowerCase().trim();
    if (raw.startsWith("/")) {
      raw = raw.substring(1);
    }

    String cmd = raw.split(" ")[0];
    if (cmd.contains(":")) {
      cmd = cmd.split(":")[1];
    }

    if (disableCommands.getCommands().contains(cmd)) {
      logger.send(player, COMMAND_DISABLER_CANT_USE);
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    final UUID playerUuid = player.getUniqueId();

    player.setExp(0);
    player.setLevel(0);
    system.recordPlayerAction(player);

    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
      Player asyncPlayer = plugin.getServer().getPlayer(playerUuid);
      if (!isPlayerOnline(asyncPlayer)) {
        return;
      }

      PlayerData playerData = dataManager.get(asyncPlayer);
      dataManager.addDefaults(playerData);
      manager.preloadSettings(asyncPlayer, playerData);
      manager.getCachedPlayers().add(asyncPlayer);
      manager.cachePrefixedName(asyncPlayer);
    });
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (!isPlayerOnline(player)) {
      return;
    }

    dataManager.unload(player);
    system.removePlayer(player);
    manager.getCachedPlayers().remove(player);
    manager.getPlayerSettings().remove(player.getUniqueId());
    manager.getCachedPrefixedNames().remove(player.getUniqueId());

    Collection<Queue<Player>> playerQueues = manager.getMatchData().getPlayerQueues().values();
    for (Queue<Player> queue : playerQueues) {
      if (queue != null) {
        queue.remove(player);
      }
    }

    List<Match> matches = manager.getMatchData().getMatches();
    if (matches != null) {
      for (Match match : matches) {
        if (match != null && match.getPhase() == MatchPhase.LOBBY) {
          List<MatchPlayer> players = match.getPlayers();
          if (players != null) {
            players.removeIf(matchPlayer -> !isPlayerOnline(matchPlayer)
                || matchPlayer.getPlayer().equals(player));
          }
        }
        manager.getScoreboardManager().updateScoreboard(match);
      }
    }

    if (teamManager.isInTeam(player)) {
      Team team = teamManager.getTeam(player);
      if (team != null && team.getMembers() != null) {
        for (Player player1 : team.getMembers()) {
          if (isPlayerOnline(player1) && !player1.equals(player)) {
            logger.send(player1, TEAM_DISBANDED, player.getName());
          }
        }
        teamManager.disbandTeam(team);
      }
    }

    Optional<Match> matchOpt = matchManager.getMatch(player);
    if (matchOpt.isPresent()) {
      Match match = matchOpt.get();
      MatchPlayer matchPlayer = null;
      List<MatchPlayer> players = match.getPlayers();
      if (players != null) {
        for (MatchPlayer matchPlayer1 : players) {
          if (isPlayerOnline(matchPlayer1)
              && matchPlayer1.getPlayer().equals(player)) {
            matchPlayer = matchPlayer1;
            break;
          }
        }
      }

      if (matchPlayer != null) {
        int playerScore = matchPlayer.getTeamColor() == TeamColor.RED
            ? match.getScoreRed()
            : match.getScoreBlue();
        int opponentScore = matchPlayer.getTeamColor() == TeamColor.RED
            ? match.getScoreBlue()
            : match.getScoreRed();

        if (match.getPhase() == MatchPhase.IN_PROGRESS && playerScore < opponentScore) {
          manager.getEconomy().withdrawPlayer(player, Settings.BAN_RAGEQUIT_PENALTY.asDouble());
          manager.getBanManager().banPlayer(player, Settings.getRageQuitBanDuration());
        }
      }
      matchManager.leaveMatch(player);
    }
  }

  @EventHandler
  public void onItemDrop(PlayerDropItemEvent event) {
    Player player = event.getPlayer();
    Optional<Match> matchOpt = matchManager.getMatch(player);
    if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onItemPickup(PlayerPickupItemEvent event) {
    Player player = event.getPlayer();
    Optional<Match> matchOpt = matchManager.getMatch(player);
    if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onInventoryInteract(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }

    Player player = (Player) event.getWhoClicked();
    Optional<Match> matchOpt = matchManager.getMatch(player);
    if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onHungerLoss(FoodLevelChangeEvent event) {
    event.setCancelled(true);
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    Optional<Match> matchOpt = matchManager.getMatch(player);
    if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
      event.setCancelled(true);
      return;
    }

    PlayerSettings settings = manager.getPlayerSettings(player);
    if (settings != null && !settings.isBuildEnabled()) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    Optional<Match> matchOpt = matchManager.getMatch(player);
    if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
      event.setCancelled(true);
      return;
    }

    PlayerSettings settings = manager.getPlayerSettings(player);
    if (settings != null && !settings.isBuildEnabled()) {
      event.setCancelled(true);
    }
  }
}
