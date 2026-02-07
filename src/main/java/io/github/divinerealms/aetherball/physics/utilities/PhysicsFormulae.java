package io.github.divinerealms.aetherball.physics.utilities;

import static io.github.divinerealms.aetherball.physics.PhysicsConstants.BALL_TOUCH_Y_OFFSET;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.CUBE_HITBOX_ADJUSTMENT;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.RANDOM;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.VERTICAL_INTERACTION_OFFSET;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_HIT_DEBUG;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.utils.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PhysicsFormulae {

  private final Logger logger;

  public PhysicsFormulae(Manager manager) {
    this.logger = manager.getLogger();
  }

  /**
   * Calculates the squared distance between two locations, optimized for physics calculations. This
   * variant avoids using {@link Math#sqrt(double)} for performance reasons and adjusts for cube
   * height.
   *
   * @param locA The first location (usually player).
   * @param locB The second location (usually cube/ball).
   * @return The squared distance between the two points.
   */
  public double getDistanceSquared(Location locA, Location locB) {
    long start = System.nanoTime();
    try {
      Location locAnew = locA.clone().add(0, -BALL_TOUCH_Y_OFFSET, 0);
      double dx = Math.abs(locAnew.getX() - locB.getX());
      double dy = Math.abs(locAnew.getY() - locB.getY() - VERTICAL_INTERACTION_OFFSET) - (
          CUBE_HITBOX_ADJUSTMENT - VERTICAL_INTERACTION_OFFSET);
      if (dy < 0) {
        dy = 0;
      }
      double dz = Math.abs(locAnew.getZ() - locB.getZ());

      return dx * dx + dy * dy + dz * dz;
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          logger.send(PERM_HIT_DEBUG,
              "{prefix-admin}&dPhysicsFormulae#getDistanceSquared() &ftook &e" + ms + "ms");
        }
      }
    }
  }

  /**
   * Calculates the squared perpendicular distance from a player's position to the path of the
   * cube's movement vector. Used for proximity and collision prediction.
   *
   * <p>Squared form avoids costly {@link Math#sqrt(double)} calls when only relative
   * distance comparisons are required.</p>
   *
   * @param newVelocity The velocity vector of the cube.
   * @param cubePos     The cube's current position.
   * @param player      The player whose position is used for distance checking.
   * @return The squared perpendicular distance between the player and the cube's velocity vector.
   */
  public double getPerpendicularDistanceSquared(Vector newVelocity, Vector cubePos, Player player) {
    long start = System.nanoTime();
    try {
      if (Math.abs(newVelocity.getX()) < 1e-6) {
        return Double.MAX_VALUE;
      }

      double slopeA = newVelocity.getZ() / newVelocity.getX();
      double interceptB = cubePos.getZ() - slopeA * cubePos.getX();

      double playerX = player.getLocation().getX();
      double playerZ = player.getLocation().getZ();

      // (|a*x - z + b| / sqrt(a² + 1))² = (a*x - z + b)² / (a² + 1)
      double numerator = slopeA * playerX - playerZ + interceptB;
      return (numerator * numerator) / (slopeA * slopeA + 1);
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          logger.send(PERM_HIT_DEBUG,
              "{prefix-admin}&dPhysicsFormulae#getPerpendicularDistanceSquared() &ftook &e" + ms
                  + "ms");
        }
      }
    }
  }

  /**
   * Non-squared version for when actual distance value is needed. Prefer
   * {@link #getPerpendicularDistanceSquared} for comparisons to avoid sqrt overhead.
   *
   * @param velocity The velocity vector of the cube
   * @param cubePos  The cube's current position vector
   * @param player   The player whose position is used for distance checking
   * @return The perpendicular distance, or {@code Double.MAX_VALUE} if undefined
   */
  public double getPerpendicularDistance(Vector velocity, Vector cubePos, Player player) {
    double distanceSquared = getPerpendicularDistanceSquared(velocity, cubePos, player);

    // Avoid sqrt if already at max value
    if (distanceSquared == Double.MAX_VALUE) {
      return Double.MAX_VALUE;
    }

    return Math.sqrt(distanceSquared);
  }

  /**
   * Determines if the cube is moving toward the player using sign-based quadrant logic.
   *
   * @param playerLocation The player's current location
   * @param cubePos        The cube's current position vector
   * @param velocity       The cube's velocity vector
   * @return true if cube is moving toward player
   */
  public boolean isMovingTowardPlayer(Location playerLocation, Vector cubePos, Vector velocity) {
    // Direction from cube to player (horizontal only)
    Vector playerDir = new Vector(
        playerLocation.getX() - cubePos.getX(),
        0.0,
        playerLocation.getZ() - cubePos.getZ()
    );

    // Normalize cube direction (horizontal only)
    Vector cubeDir = new Vector(velocity.getX(), 0.0, velocity.getZ()).normalize();

    // Calculate directional signs for quadrant detection
    int playerDirX = playerDir.getX() < 0.0 ? -1 : 1;
    int playerDirZ = playerDir.getZ() < 0.0 ? -1 : 1;
    int cubeDirX = cubeDir.getX() < 0.0 ? -1 : 1;
    int cubeDirZ = cubeDir.getZ() < 0.0 ? -1 : 1;

    // Both X and Z directions opposite - definitely not moving toward
    if (playerDirX != cubeDirX && playerDirZ != cubeDirZ) {
      return false;
    }

    // One direction different - use cross-product check
    if (playerDirX != cubeDirX || playerDirZ != cubeDirZ) {
      boolean xCrossCheck =
          cubeDirX * playerDir.getX() <= (cubeDirX * cubeDirZ * playerDirX) * cubeDir.getZ();
      boolean zCrossCheck =
          cubeDirZ * playerDir.getZ() <= (cubeDirZ * cubeDirX * playerDirZ) * cubeDir.getX();

      return !xCrossCheck && !zCrossCheck;
    }

    return true;
  }

  /**
   * Applies the soft cap to the calculated kick power.
   *
   * @param baseKickPower Initial kick power.
   * @return Randomized capped kick power.
   */
  public double capKickPower(double baseKickPower) {
    long start = System.nanoTime();
    try {
      if (baseKickPower <= Settings.MAX_KICK_POWER.asDouble()) {
        return baseKickPower;
      }
      double minRandomKP = Settings.MAX_KICK_POWER.asDouble() * Settings.SOFT_CAP.asDouble();
      return minRandomKP + RANDOM.nextDouble() * (Settings.MAX_KICK_POWER.asDouble() - minRandomKP);
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          logger.send(PERM_HIT_DEBUG,
              "{prefix-admin}&dPhysicsFormulae#capKickPower() &ftook &e" + ms + "ms");
        }
      }
    }
  }
}
