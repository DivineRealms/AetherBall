package io.github.divinerealms.footcube.physics.utilities;

import static io.github.divinerealms.footcube.physics.PhysicsConstants.BALL_TOUCH_Y_OFFSET;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.CUBE_HITBOX_ADJUSTMENT;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.RANDOM;
import static io.github.divinerealms.footcube.physics.PhysicsConstants.VERTICAL_INTERACTION_OFFSET;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_HIT_DEBUG;

import io.github.divinerealms.footcube.configs.Settings;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.utils.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PhysicsFormulae {

  private final Logger logger;

  public PhysicsFormulae(FCManager fcManager) {
    this.logger = fcManager.getLogger();
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
   * Applies the soft cap to the calculated kick power.
   *
   * @param baseKickPower Initial kick power.
   * @return Randomized capped kick power.
   */
  public double capKickPower(double baseKickPower) {
    long start = System.nanoTime();
    try {
      if (baseKickPower <= Settings.MAX_KICK_POWER.asInt()) {
        return baseKickPower;
      }
      double minRandomKP = Settings.MAX_KICK_POWER.asInt() * Settings.SOFT_CAP.asDouble();
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
