package io.github.divinerealms.aetherball.listeners;

import static io.github.divinerealms.aetherball.configs.Lang.COMMAND_DISABLER_CANT_USE;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.MatchUtils.isPlayerOnline;
import static io.github.divinerealms.aetherball.utils.MatchUtils.shouldPreventAbuse;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_BYPASS_DISABLED_COMMANDS;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import io.github.divinerealms.aetherball.utils.DisableCommands;
import io.github.divinerealms.aetherball.utils.PlayerSettings;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;

/**
 * Handles core player lifecycle events and match-related restrictions.
 *
 * <p>Manages player join/quit processing, command blocking during matches, inventory protection,
 * team disbanding, rage-quit penalties, and build permission enforcement.
 */
public class PlayerEvents extends BaseListener {

  private final Manager manager;
  private final Plugin plugin;
  private final MatchManager matchManager;
  private final PlayerDataManager dataManager;
  private final DisableCommands disableCommands;
  private final PhysicsSystem system;

  public PlayerEvents(Manager manager) {
    this.manager = manager;
    this.plugin = manager.getPlugin();
    this.matchManager = manager.getMatchManager();
    this.dataManager = manager.getDataManager();
    this.disableCommands = manager.getDisableCommands();
    this.system = manager.getPhysicsSystem();
  }

  /**
   * Blocks configured commands during active matches.
   *
   * <p>Prevents command abuse during matches by checking against a configurable blacklist. Players
   * with bypass permission can use any command. Also handles TAB plugin reload triggers.
   *
   * @param event the {@link PlayerCommandPreprocessEvent} fired before command execution
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onDisabledCommand(PlayerCommandPreprocessEvent event) {
    monitoredExecution(
        () -> {
          Player player = event.getPlayer();

          // Handle TAB plugin reload command.
          if (event.getMessage().equalsIgnoreCase("/tab reload")) {
            manager.reloadTabAPI();
          }

          // Allow bypass permission holders to use any command.
          if (player.hasPermission(PERM_BYPASS_DISABLED_COMMANDS)) {
            return;
          }

          // Only enforce restrictions during active match phases.
          Optional<Match> matchOpt = matchManager.getMatch(player);
          if (matchOpt.isEmpty()) {
            return;
          }

          if (!shouldPreventAbuse(matchOpt.get().getPhase())) {
            return;
          }

          // Parse command from message, handling plugin prefixes like "pluginname:command".
          String raw = event.getMessage().toLowerCase().trim();
          if (raw.startsWith("/")) {
            raw = raw.substring(1);
          }

          String cmd = raw.split(" ")[0];
          if (cmd.contains(":")) {
            cmd = cmd.split(":")[1];
          }

          // Block if command is in the disabled list.
          if (disableCommands.getCommands().contains(cmd)) {
            sendMessage(player, COMMAND_DISABLER_CANT_USE);
            event.setCancelled(true);
          }
        });
  }

  /**
   * Initializes player data and settings when joining the server.
   *
   * <p>Resets experience bar, records player activity, loads persistent data asynchronously,
   * applies defaults, preloads settings, and caches player information for performance.
   *
   * @param event the {@link PlayerJoinEvent} fired when a player joins
   */
  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    monitoredExecution(
        () -> {
          Player player = event.getPlayer();
          final UUID playerUuid = player.getUniqueId();

          // Reset experience bar state.
          player.setExp(0);
          player.setLevel(0);
          system.recordPlayerAction(player);

          // Load player data asynchronously to avoid blocking main thread.
          plugin
              .getServer()
              .getScheduler()
              .runTaskAsynchronously(
                  plugin,
                  () -> {
                    Player asyncPlayer = plugin.getServer().getPlayer(playerUuid);
                    if (!isPlayerOnline(asyncPlayer)) {
                      return;
                    }

                    // Load persistent data and apply defaults if needed.
                    PlayerData playerData = dataManager.get(asyncPlayer);
                    dataManager.addDefaults(playerData);
                    manager.preloadSettings(asyncPlayer, playerData);

                    // Cache player for fast lookups.
                    manager.getCachedPlayers().add(asyncPlayer);
                    manager.cachePrefixedName(asyncPlayer);
                  });
        });
  }

  /**
   * Handles cleanup when a player leaves the server.
   *
   * <p>Unloads player data, removes from caches and queues, cleans up lobby matches, disbands teams
   * if leader quits, and applies rage-quit penalties if leaving a losing match in progress.
   *
   * @param event the {@link PlayerQuitEvent} fired when a player disconnects
   */
  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    monitoredExecution(
        () -> {
          Player player = event.getPlayer();
          if (!isPlayerOnline(player)) {
            return;
          }

          // Unload player data and clear all caches.
          manager.unloadPlayer(player);
        });
  }

  /**
   * Prevents item dropping during active matches.
   *
   * @param event the {@link PlayerDropItemEvent} fired when a player drops an item
   */
  @EventHandler
  public void onItemDrop(PlayerDropItemEvent event) {
    monitoredExecution(
        () -> {
          Player player = event.getPlayer();
          Optional<Match> matchOpt = matchManager.getMatch(player);
          if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
            event.setCancelled(true);
          }
        });
  }

  /**
   * Prevents item pickup during active matches.
   *
   * @param event the {@link PlayerPickupItemEvent} fired when a player picks up an item
   */
  @EventHandler
  public void onItemPickup(PlayerPickupItemEvent event) {
    monitoredExecution(
        () -> {
          Player player = event.getPlayer();
          Optional<Match> matchOpt = matchManager.getMatch(player);
          if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
            event.setCancelled(true);
          }
        });
  }

  /**
   * Prevents inventory interactions during active matches.
   *
   * @param event the {@link InventoryClickEvent} fired when inventory is clicked
   */
  @EventHandler
  public void onInventoryInteract(InventoryClickEvent event) {
    monitoredExecution(
        () -> {
          if (!(event.getWhoClicked() instanceof Player player)) {
            return;
          }

          Optional<Match> matchOpt = matchManager.getMatch(player);
          if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
            event.setCancelled(true);
          }
        });
  }

  /**
   * Disables hunger loss globally for all players.
   *
   * @param event the {@link FoodLevelChangeEvent} fired when hunger changes
   */
  @EventHandler
  public void onHungerLoss(FoodLevelChangeEvent event) {
    monitoredExecution(() -> event.setCancelled(true));
  }

  /**
   * Prevents block placement during matches or when build mode is disabled.
   *
   * @param event the {@link BlockPlaceEvent} fired when a block is placed
   */
  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    monitoredExecution(
        () -> {
          Player player = event.getPlayer();

          // Block placement during active matches.
          Optional<Match> matchOpt = matchManager.getMatch(player);
          if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
            event.setCancelled(true);
            return;
          }

          // Respect player's build mode setting.
          PlayerSettings settings = manager.getPlayerSettings(player);
          if (settings != null && !settings.isBuildEnabled()) {
            event.setCancelled(true);
          }
        });
  }

  /**
   * Prevents block breaking during matches or when build mode is disabled.
   *
   * @param event the {@link BlockBreakEvent} fired when a block is broken
   */
  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    monitoredExecution(
        () -> {
          Player player = event.getPlayer();

          // Block breaking during active matches.
          Optional<Match> matchOpt = matchManager.getMatch(player);
          if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
            event.setCancelled(true);
            return;
          }

          // Respect player's build mode setting.
          PlayerSettings settings = manager.getPlayerSettings(player);
          if (settings != null && !settings.isBuildEnabled()) {
            event.setCancelled(true);
          }
        });
  }
}
