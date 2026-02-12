package io.github.divinerealms.aetherball.listeners;

import static io.github.divinerealms.aetherball.configs.Lang.ALREADY_ENOUGH_CUBES;
import static io.github.divinerealms.aetherball.configs.Lang.BALANCE;
import static io.github.divinerealms.aetherball.configs.Lang.CLEARED_CUBES;
import static io.github.divinerealms.aetherball.configs.Lang.CUBE_NO_CUBES;
import static io.github.divinerealms.aetherball.configs.Lang.CUBE_SPAWN;
import static io.github.divinerealms.aetherball.configs.Lang.FC_DISABLED;
import static io.github.divinerealms.aetherball.configs.Lang.JOIN_ALREADYINGAME;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_FOOTER;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_HEADER;
import static io.github.divinerealms.aetherball.configs.Lang.MATCHES_LIST_NO_MATCHES;
import static io.github.divinerealms.aetherball.configs.Lang.NO_PERM_PARAMETERS;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.getFormattedMatches;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_PLAY;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.matchmaking.ban.BanManager;
import io.github.divinerealms.aetherball.matchmaking.highscore.HighScoreManager;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchData;
import io.github.divinerealms.aetherball.matchmaking.logic.MatchSystem;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;
import org.bukkit.util.Vector;

/**
 * Handles creation and interaction with AetherBall signs and colored wool buttons.
 * <p>
 * Provides sign-based interfaces for joining matches, spawning cubes, checking stats, and viewing
 * highscores. Also manages cube spawn/clear buttons using colored wool blocks.
 * </p>
 */
public class SignManipulation extends BaseListener {

  private final Manager manager;
  private final MatchManager matchManager;
  private final MatchData matchData;
  private final MatchSystem matchSystem;
  private final BanManager banManager;
  private final HighScoreManager highScoreManager;
  private final PhysicsSystem physicsSystem;
  private final PhysicsData physicsData;

  public SignManipulation(Manager manager) {
    this.manager = manager;
    this.matchManager = manager.getMatchManager();
    this.matchData = manager.getMatchData();
    this.matchSystem = manager.getMatchSystem();
    this.banManager = manager.getBanManager();
    this.highScoreManager = manager.getHighscoreManager();
    this.physicsSystem = manager.getPhysicsSystem();
    this.physicsData = manager.getPhysicsData();
  }

  /**
   * Formats signs with AetherBall functionality when created.
   * <p>
   * Converts shorthand [fc] signs into formatted [AetherBall] signs with colored text and
   * descriptions. Supports join, stats, cube, balance, highscores, and matches signs.
   * </p>
   *
   * @param event the {@link SignChangeEvent} fired when a sign is created or edited
   */
  @EventHandler
  public void onSignChange(SignChangeEvent event) {
    monitoredExecution(() -> {
      Player player = event.getPlayer();

      // Require permission and feature flag for AetherBall signs.
      if (event.getLine(0) != null && event.getLine(0).equalsIgnoreCase("[AetherBall]")
          && !player.hasPermission("aetherball.admin") && !Settings.FEATURES_SIGNS.asBoolean()) {
        return;
      }

      if (event.getLine(0) != null && event.getLine(0).equalsIgnoreCase("[fc]")) {
        switch (event.getLine(1).toLowerCase()) {
          case "join":
            String arena = event.getLine(2).toLowerCase();
            event.setLine(0, "[AetherBall]");
            event.setLine(1, ChatColor.AQUA + "join");

            // Format arena type with color.
            switch (arena) {
              case "2v2":
                event.setLine(2, ChatColor.GREEN + "2v2");
                break;

              case "3v3":
                event.setLine(2, ChatColor.GREEN + "3v3");
                break;

              case "4v4":
                event.setLine(2, ChatColor.GREEN + "4v4");
                break;
            }

            event.setLine(3, "");
            break;

          case "stats":
            event.setLine(0, "[AetherBall]");
            event.setLine(1, ChatColor.AQUA + "stats");
            event.setLine(2, "See how much");
            event.setLine(3, "you score & win");
            break;

          case "cube":
            event.setLine(0, "[AetherBall]");
            event.setLine(1, ChatColor.AQUA + "cube");
            event.setLine(2, "Spawn a");
            event.setLine(3, "cube");
            break;

          case "balance":
          case "money":
            event.setLine(0, "[AetherBall]");
            event.setLine(1, ChatColor.AQUA + "balance");
            event.setLine(2, "Check your");
            event.setLine(3, "balance");
            break;

          case "highscores":
          case "best":
            event.setLine(0, "[AetherBall]");
            event.setLine(1, ChatColor.AQUA + "highscores");
            event.setLine(2, "Check all");
            event.setLine(3, "highscores");
            break;

          case "matches":
            event.setLine(0, "[AetherBall]");
            event.setLine(1, ChatColor.AQUA + "matches");
            event.setLine(2, "See currently");
            event.setLine(3, "active matches");
            break;
        }
      }
    });
  }

  /**
   * Handles right-click interactions with AetherBall signs and colored wool buttons.
   * <p>
   * Processes sign-based commands (join queue, check stats, spawn cube, view balance/highscores)
   * and colored button interactions (green for spawn, red for clear cubes).
   * </p>
   *
   * @param event the {@link PlayerInteractEvent} fired when a player interacts with a block
   */
  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    monitoredExecution(() -> {
      Player player = event.getPlayer();
      Action action = event.getAction();

      // Only process right-clicks on blocks.
      if (action != Action.RIGHT_CLICK_BLOCK) {
        return;
      }

      Block block = event.getClickedBlock();
      if (block == null) {
        return;
      }

      // Handle colored wool button interactions for cube spawning/clearing.
      if (block.getType() == Material.STONE_BUTTON) {
        Block below = block.getRelative(BlockFace.DOWN);
        if (below == null || below.getType() != Material.WOOL) {
          return;
        }

        MaterialData materialData = below.getState().getData();
        if (!(materialData instanceof Wool)) {
          return;
        }

        DyeColor color = ((Wool) materialData).getColor();
        Location playerLocation = player.getLocation();
        switch (color) {
          // Green button spawns a cube if area isn't full.
          case LIME:
            if (physicsSystem.cantSpawnYet(player)) {
              return;
            }

            Collection<Entity> nearbyEntities = playerLocation.getWorld()
                .getNearbyEntities(playerLocation, 100, 100, 100);
            int slimeCount = 0;
            if (nearbyEntities != null) {
              for (Entity entity : nearbyEntities) {
                if (entity instanceof Slime) {
                  slimeCount++;
                }
              }
            }

            if (slimeCount < Settings.CUBE_MAX_PER_AREA.asInt()) {
              physicsSystem.spawnCube(playerLocation.add(new Vector(0.5, 0.5, 0.5)));
              physicsSystem.setButtonCooldown(player);
              sendMessage(player, CUBE_SPAWN);
            } else {
              sendMessage(player, ALREADY_ENOUGH_CUBES);
            }
            break;

          // Red button clears nearby cubes within 30 blocks.
          case RED:
            double closestDistance = 30;
            int removed = 0;
            for (Slime cube : new HashSet<>(physicsData.getCubes())) {
              if (cube == null || cube.isDead()) {
                continue;
              }

              if (cube.getLocation().distance(playerLocation) <= closestDistance) {
                cube.setHealth(0);
                removed++;
              }
            }

            if (removed > 0) {
              sendMessage(player, CLEARED_CUBES, String.valueOf(removed));
            } else {
              sendMessage(player, CUBE_NO_CUBES);
            }
            break;

          default:
            break;
        }
      }

      // Handle AetherBall sign interactions.
      if (block.getType() == Material.SIGN_POST || block.getType() == Material.WALL_SIGN) {
        Sign sign = (Sign) block.getState();
        if (sign.getLine(0) == null || !sign.getLine(0).equalsIgnoreCase("[AetherBall]")
            || !Settings.FEATURES_SIGNS.asBoolean()) {
          return;
        }

        String line1 = ChatColor.stripColor(sign.getLine(1));
        switch (line1.toLowerCase()) {
          case "join":
            // Join matchmaking queue for specified arena type.
            if (!player.hasPermission(PERM_PLAY)) {
              sendMessage(player, NO_PERM_PARAMETERS, PERM_PLAY, "fc join");
              return;
            }

            if (!matchData.isMatchesEnabled()) {
              sendMessage(player, FC_DISABLED);
              return;
            }

            if (banManager.isBanned(player)) {
              return;
            }

            if (banManager.isBanned(player)) {
              return;
            }

            if (matchManager.getMatch(player).isPresent()) {
              sendMessage(player, JOIN_ALREADYINGAME);
              return;
            }

            String arenaType = ChatColor.stripColor(sign.getLine(2)).toLowerCase();
            int type;
            switch (arenaType) {
              case "1v1":
                type = 1;
                break;

              case "2v2":
                type = 2;
                break;

              case "3v3":
                type = 3;
                break;

              case "4v4":
                type = 4;
                break;

              case "5v5":
                type = 5;
                break;

              default:
                return;
            }

            matchManager.joinQueue(player, type);
            break;

          case "stats":
            // Display player statistics.
            matchSystem.checkStats(player.getName(), player);
            break;

          case "cube":
            // Spawn a cube at player location if area limit not reached.
            if (physicsSystem.cantSpawnYet(player)) {
              return;
            }

            Location location = player.getLocation();
            Collection<Entity> nearbyEntities = location.getWorld()
                .getNearbyEntities(location, 100, 100, 100);
            int slimeCount = 0;

            if (nearbyEntities != null) {
              for (Entity entity : nearbyEntities) {
                if (entity instanceof Slime) {
                  slimeCount++;
                }
              }
            }

            if (slimeCount < Settings.CUBE_MAX_PER_AREA.asInt()) {
              physicsSystem
                  .spawnCube(player.getLocation().add(new Vector(0.5, 0.5, 0.5)));
              physicsSystem.setButtonCooldown(player);
              sendMessage(player, CUBE_SPAWN);
            } else {
              sendMessage(player, ALREADY_ENOUGH_CUBES);
            }
            break;

          case "balance":
            // Display player's economy balance.
            sendMessage(player, BALANCE,
                String.valueOf(manager.getEconomy().getBalance(player)));
            break;

          case "highscores":
            // Display global highscores.
            highScoreManager.showHighScores(player);
            break;

          case "matches":
            // List all currently active matches.
            List<String> matches = getFormattedMatches(matchData.getMatches());
            if (!matches.isEmpty()) {
              sendMessage(player, MATCHES_LIST_HEADER);
              matches.forEach(msg -> sendMessage(player, msg));
              sendMessage(player, MATCHES_LIST_FOOTER);
            } else {
              sendMessage(player, MATCHES_LIST_NO_MATCHES);
            }
            break;
        }
      }
    });
  }
}
