package io.github.divinerealms.footcube.tasks;

import static io.github.divinerealms.footcube.configs.Lang.HITDEBUG_VELOCITY_CAP;
import static io.github.divinerealms.footcube.matchmaking.util.MatchUtils.isPlayerOnline;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.AIR_DRAG_FACTOR;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.ANTI_CLIP_DOT_THRESHOLD;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.BOUNCE_THRESHOLD;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.CUBE_SPEED_TOUCH_DIVISOR;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.DRIBBLE_SPEED_LIMIT;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.HIT_RADIUS_SQUARED;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.HIT_RADIUS_SQUARED_3X;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.MIN_RADIUS_SQUARED;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.MIN_SOUND_POWER;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.MIN_SPEED_FOR_DAMPENING;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.PHYSICS_TASK_INTERVAL_TICKS;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.PLAYER_FOOT_LEVEL;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.PLAYER_HEAD_LEVEL;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.PLAYER_SPEED_TOUCH_DIVISOR;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.PROXIMITY_THRESHOLD_MULTIPLIER;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.SOUND_PITCH;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.SOUND_VOLUME;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.VECTOR_CHANGE_THRESHOLD;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.VERTICAL_BOUNCE_THRESHOLD;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.WALL_BOUNCE_FACTOR;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_HIT_DEBUG;

import io.github.divinerealms.footcube.configs.Settings;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.matchmaking.MatchManager;
import io.github.divinerealms.footcube.physics.PhysicsData;
import io.github.divinerealms.footcube.physics.utilities.PhysicsFormulae;
import io.github.divinerealms.footcube.physics.utilities.PhysicsSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.util.Vector;

public class PhysicsTask extends BaseTask {

  private final PhysicsData data;
  private final PhysicsSystem system;
  private final PhysicsFormulae formulae;
  private final MatchManager matchManager;

  public PhysicsTask(FCManager fcManager) {
    super(fcManager, "Physics", PHYSICS_TASK_INTERVAL_TICKS, false);
    this.data = fcManager.getPhysicsData();
    this.system = fcManager.getPhysicsSystem();
    this.formulae = fcManager.getPhysicsFormulae();
    this.matchManager = fcManager.getMatchManager();
  }

  @Override
  protected void kaboom() {
    // Skip processing if there are no active players or cubes.
    if (fcManager.getCachedPlayers().isEmpty() || data.getCubes().isEmpty()) {
      return;
    }

    // Build player cache once per tick for all cubes to reuse.
    Map<UUID, PlayerPhysicsCache> playerCache = buildPlayerCache();
    if (playerCache.isEmpty()) {
      return; // No valid players to process
    }

    ++data.tickRate;

    // Main cube processing loop.
    for (Slime cube : data.getCubes()) {
      // --- Cube validity check ---
      if (cube.isDead()) {
        data.getCubesToRemove().add(cube);
        continue;
      }

      // --- Initialization and state retrieval ---
      UUID cubeId = cube.getUniqueId();
      Location cubeLocation = cube.getLocation();
      if (cubeLocation == null) {
        continue;
      }

      // Retrieve or initialize previous velocity for collision calculations.
      Vector previousVelocity = data.getVelocities().get(cubeId);
      // Initialize if this is the first tick tracking this cube.
      if (previousVelocity == null) {
        previousVelocity = cube.getVelocity().clone();
        data.getVelocities().put(cubeId, previousVelocity);
      }

      // --- Player interaction and velocity adjustment ---
      Vector newVelocity = cube.getVelocity();
      boolean wasMoved = false;
      boolean playSound = false;

      // Pre-calculate cube position once for this iteration
      Vector cubePos = cubeLocation.toVector();

      // Store player interaction data to avoid recalculation in anti-clipping.
      Map<UUID, PlayerInteraction> playerInteractions = new HashMap<>();
      for (Player player : fcManager.getCachedPlayers()) {
        UUID playerId = player.getUniqueId();

        PlayerPhysicsCache cache = playerCache.get(playerId);
        if (cache == null || !cache.canInteract()) {
          continue;
        }

        // --- Player proximity and touch detection ---
        // Determines if the player is close enough to directly affect the cube.
        double distanceSquared = formulae.getDistanceSquared(cubeLocation, cache.location);

        // Skip players beyond 3x hit radius for performance,
        // as they cannot meaningfully interact with the cube.
        if (distanceSquared > HIT_RADIUS_SQUARED_3X) {
          continue;
        }

        // Calculate real distance only for players close enough to interact
        // This avoids sqrt for ~70% of players (those filtered by early exit).
        double distance = Math.sqrt(distanceSquared);

        // Cache the interaction data for later use.
        playerInteractions.put(playerId, new PlayerInteraction(player, cache, distance, cubePos));

        // Skip if player cannot interact or is out of touch range.
        if (distanceSquared < HIT_RADIUS_SQUARED) {
          double cubeSpeed = newVelocity.length(); // Current speed of the cube.

          // Apply speed dampening if cube is very close to player for dribbling effect.
          if (distanceSquared < MIN_RADIUS_SQUARED && cubeSpeed > MIN_SPEED_FOR_DAMPENING) {
            // Apply speed cap for dribbling effect.
            newVelocity.multiply(DRIBBLE_SPEED_LIMIT / cubeSpeed);
          }

          // Compute the resulting power from player movement and cube velocity.
          double previousSpeed = Math.max(previousVelocity.length(), VECTOR_CHANGE_THRESHOLD);
          double impactPower =
              cache.speed / PLAYER_SPEED_TOUCH_DIVISOR + previousSpeed / CUBE_SPEED_TOUCH_DIVISOR;

          // Directional push vector from player to cube.
          newVelocity.add(cache.direction.clone().multiply(impactPower));

          // Register the touch interaction with the organization system.
          matchManager.kick(player);
          wasMoved = true;

          // Trigger sound effect if impact force exceeds threshold.
          if (impactPower > MIN_SOUND_POWER) {
            playSound = true;
          }
        }
      }

      // --- Handle wall collisions and air drag effects ---

      // X-axis collision and drag adjustment.
      // If the cube stops moving horizontally (X=0), bounce it back with reduced energy.
      if (newVelocity.getX() == 0) {
        // Reverse and reduce X velocity on collision.
        newVelocity.setX(-previousVelocity.getX() * WALL_BOUNCE_FACTOR);
        if (Math.abs(previousVelocity.getX()) > BOUNCE_THRESHOLD) {
          playSound = true; // Trigger sound if impact force is strong enough.
        }
      } else {
        // If cube wasn't recently kicked and velocity change is small, apply gradual air drag slowdown.
        if (!wasMoved
            && Math.abs(previousVelocity.getX() - newVelocity.getX()) < VECTOR_CHANGE_THRESHOLD) {
          newVelocity.setX(previousVelocity.getX() * AIR_DRAG_FACTOR); // Apply air drag.
        }
      }

      // Z-axis collision and drag adjustment (mirrors X-axis logic).
      if (newVelocity.getZ() == 0) {
        // Reverse and reduce Z velocity on collision.
        newVelocity.setZ(-previousVelocity.getZ() * WALL_BOUNCE_FACTOR);
        if (Math.abs(previousVelocity.getZ()) > BOUNCE_THRESHOLD) {
          playSound = true; // Trigger sound if impact force is strong enough.
        }
      } else {
        // If cube wasn't recently kicked and velocity change is small, apply gradual air drag slowdown.
        if (!wasMoved
            && Math.abs(previousVelocity.getZ() - newVelocity.getZ()) < VECTOR_CHANGE_THRESHOLD) {
          newVelocity.setZ(previousVelocity.getZ() * AIR_DRAG_FACTOR); // Apply air drag.
        }
      }

      // Y-axis bounce (vertical collision against floor or ceiling).
      // This ensures realistic vertical rebound, preventing velocity loss bugs on impact.
      if (newVelocity.getY() < 0 && previousVelocity.getY() < 0
          && previousVelocity.getY() < newVelocity.getY() - VERTICAL_BOUNCE_THRESHOLD) {
        // Reverse and reduce Y velocity on downward collision.
        newVelocity.setY(-previousVelocity.getY() * WALL_BOUNCE_FACTOR);
        if (Math.abs(previousVelocity.getY()) > BOUNCE_THRESHOLD) {
          playSound = true; // Trigger sound if impact force is strong enough.
        }
      }

      // Queue impact sound effect if any significant collision occurred.
      if (playSound) {
        cube.getWorld().playSound(cubeLocation, Sound.SLIME_WALK, SOUND_VOLUME, SOUND_PITCH);
      }

      // --- Anti-clipping / Proximity Logic ---
      // Prevents the cube from passing through players at high speeds.
      double cubeSpeed = newVelocity.length();
      if (cubeSpeed > VECTOR_CHANGE_THRESHOLD) {
        double minScaleFactor = 1; // Track minimum scale factor needed to prevent clipping.

        // Evaluate each player interaction for potential clipping.
        for (PlayerInteraction interaction : playerInteractions.values()) {
          if (interaction == null || interaction.cache == null) {
            continue;
          }

          // Retrieve cached player data and distance.
          PlayerPhysicsCache cache = interaction.cache;
          double distance = interaction.distance;

          // Skip if player is too far away for clipping to be possible.
          if (distance >= cubeSpeed * PROXIMITY_THRESHOLD_MULTIPLIER) {
            continue;
          }

          // Check vertical alignment with player height.
          double playerLocationY = cache.location.getY();
          Vector projectedNextPos = cubePos.clone().add(newVelocity);

          // Check if the cube's vertical position aligns with player's height.
          boolean withinY = (cubePos.getY() >= playerLocationY + PLAYER_FOOT_LEVEL
              && cubePos.getY() <= playerLocationY + PLAYER_HEAD_LEVEL) && (
              projectedNextPos.getY() >= playerLocationY + PLAYER_FOOT_LEVEL
                  && projectedNextPos.getY() <= playerLocationY + PLAYER_HEAD_LEVEL);

          // If vertically aligned, check if the cube's path intersects player's collision radius.
          if (withinY
              && formulae.getPerpendicularDistanceSquared(newVelocity, cubePos, interaction.player)
              < MIN_RADIUS_SQUARED) {
            // Horizontal vector to player.
            Vector toPlayer = interaction.toPlayer;

            // Horizontal movement direction.
            Vector ballDirection = new Vector(newVelocity.getX(), 0,
                newVelocity.getZ()).normalize();

            // Cosine of angle between cube movement and direction to player.
            double dot = toPlayer.dot(ballDirection);

            // Scale back velocity if moving toward player to prevent clipping.
            if (dot > ANTI_CLIP_DOT_THRESHOLD) {
              // Scale factor based on distance and speed.
              double scaleFactor = distance / cubeSpeed;
              if (scaleFactor < minScaleFactor) {
                minScaleFactor = scaleFactor; // Track minimum scale factor needed.
              }
            }
          }
        }

        // Apply the most restrictive scale factor to the cube's velocity.
        if (minScaleFactor < 1) {
          newVelocity.multiply(minScaleFactor);
        }
      }

      // --- Velocity Capping ---
      // If the ball exceeds MAX_KP, we scale the vector back to prevent "unreal" speeds.
      double finalSpeed = newVelocity.length(); // Calculate final speed after all adjustments.
      double maxKickPower = Settings.MAX_KICK_POWER.asDouble();
      if (finalSpeed > maxKickPower) {
        newVelocity.multiply(maxKickPower / finalSpeed); // Scale back to MAX_KP.
        // Log violation to players with debugging permissions.
        logger.send(PERM_HIT_DEBUG, HITDEBUG_VELOCITY_CAP, String.format("%.2f", finalSpeed),
            String.valueOf(maxKickPower));
      }

      // Apply final computed velocity to the cube and update its tracked state.
      cube.setVelocity(newVelocity);
      data.getVelocities().put(cubeId, newVelocity.clone());
    }

    // Finalize scheduled physics actions.
    system.scheduleCubeRemoval(); // Safely remove dead or invalid cube entities.
  }

  /**
   * Builds a cache of player physics data for the current tick. This cache is reused for all
   * cube-player interactions during this tick, reducing redundant calculations and improving
   * performance.
   *
   * @return a map of player UUIDs to their corresponding physics cache
   *
   **/
  private Map<UUID, PlayerPhysicsCache> buildPlayerCache() {
    Map<UUID, PlayerPhysicsCache> cache = new HashMap<>();

    for (Player player : fcManager.getCachedPlayers()) {
      // Validate player is in a processable state.
      if (!isPlayerOnline(player)) {
        continue;
      }

      try {
        PlayerPhysicsCache physicsCache = new PlayerPhysicsCache(player, system, data);
        cache.put(player.getUniqueId(), physicsCache);
      } catch (Exception exception) {
        Bukkit.getLogger().log(Level.WARNING,
            "Failed to cache player " + player.getName() + ": " + exception.getMessage(),
            exception);
      }
    }

    return cache;
  }

  /**
   * Temporary structure to store player interaction data during a single cube's processing.
   * Prevents recalculating distances and player lookups in the anti-clipping phase.
   *
   * <p><b>Lifecycle:</b> Created during touch detection, reused in anti-clipping,
   * then discarded at the end of each cube's processing.</p>
   */
  private static class PlayerInteraction {

    final Player player;
    final PlayerPhysicsCache cache;
    final double distance;
    final Vector toPlayer;

    PlayerInteraction(Player player, PlayerPhysicsCache cache, double distance, Vector cubePos) {
      this.player = player;
      this.cache = cache;
      this.distance = distance;
      this.toPlayer = cache.location.toVector().subtract(cubePos).setY(0).normalize();
    }
  }

  /**
   * Immutable cache containing pre-calculated physics data for a player during a single tick.
   * <p>
   * This cache reduces redundant calculations when processing multiple cubes against the same
   * player, improving performance by storing commonly accessed values like location, direction, and
   * eligibility.
   * </p>
   *
   * <p><b>Lifecycle:</b> Created once per player per tick in {@code buildPlayerCache()},
   * then reused for all cube-player interactions during that tick.</p>
   */
  private static class PlayerPhysicsCache {

    final Location location;
    final Vector direction;
    final double speed;
    final boolean isIneligible;
    final UUID playerId;

    PlayerPhysicsCache(Player player, PhysicsSystem system, PhysicsData data) {
      this.playerId = player.getUniqueId();
      this.location = player.getLocation().clone();
      this.direction = location.getDirection().clone().setY(0).normalize();
      this.speed = data.getSpeed().getOrDefault(playerId, 1D);
      this.isIneligible = system.notAllowedToInteract(player) || system.isAFK(player);
    }

    /**
     * Checks if this player can interact with cubes during this physics tick.
     *
     * @return true if the player is eligible for cube interactions
     */
    public boolean canInteract() {
      return !isIneligible;
    }
  }
}
