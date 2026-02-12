package io.github.divinerealms.aetherball.physics.utilities;

import static io.github.divinerealms.aetherball.configs.Lang.BLOCK_INTERACT_COOLDOWN;
import static io.github.divinerealms.aetherball.configs.Lang.HITDEBUG_CHARGED;
import static io.github.divinerealms.aetherball.configs.Lang.HITDEBUG_PLAYER_CHARGED;
import static io.github.divinerealms.aetherball.configs.Lang.HITDEBUG_PLAYER_COOLDOWN;
import static io.github.divinerealms.aetherball.configs.Lang.HITDEBUG_PLAYER_REGULAR;
import static io.github.divinerealms.aetherball.configs.Lang.HITDEBUG_PLAYER_WHOLE;
import static io.github.divinerealms.aetherball.configs.Lang.HITDEBUG_REGULAR;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.CHARGE_BASE_VALUE;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.CHARGE_MULTIPLIER;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.JUMP_POTION_AMPLIFIER;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.JUMP_POTION_DURATION;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.KICK_POWER_SPEED_MULTIPLIER;
import static io.github.divinerealms.aetherball.physics.PhysicsConstants.MIN_SPEED_FOR_DAMPENING;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendActionBar;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_BYPASS_COOLDOWN;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_HIT_DEBUG;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.managers.Utilities;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.touch.CubeTouchInfo;
import io.github.divinerealms.aetherball.physics.touch.CubeTouchType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

public class PhysicsSystem {

  private final PhysicsData data;
  private final BukkitScheduler scheduler;
  private final Plugin plugin;
  private final PhysicsFormulae formulae;

  public PhysicsSystem(Manager manager) {
    this.data = manager.getPhysicsData();
    this.scheduler = manager.getScheduler();
    this.plugin = manager.getPlugin();
    this.formulae = manager.getPhysicsFormulae();
  }

  /**
   * Schedules the removal of dead or invalid cubes from the world. Ensures the cubes list remains
   * synchronized and prevents entity leaks.
   */
  public void scheduleCubeRemoval() {
    if (data.getCubesToRemove().isEmpty()) {
      return;
    }

    Set<Slime> toRemove = new HashSet<>(data.getCubesToRemove());
    data.getCubesToRemove().clear();
    long removalDelay = TimeUnit.SECONDS.toMillis(Settings.REMOVAL_DELAY.asInt());

    scheduler.runTaskLater(plugin, () -> {
      for (Slime cube : toRemove) {
        UUID cubeId = cube.getUniqueId();

        data.getCubes().remove(cube);
        data.getVelocities().remove(cubeId);
        data.getRaised().remove(cubeId);
        data.getPreviousCubeLocations().remove(cubeId);

        if (!cube.isDead()) {
          cube.remove();
        }
      }
    }, removalDelay);
  }

  /**
   * Calculates the final kick power based on player speed and current charge level.
   *
   * @param player Player who initiated the calculation (who kicked the ball).
   * @return Player's kick power
   */
  public PlayerKickResult calculateKickPower(Player player) {
    long start = System.nanoTime();
    try {
      boolean isCharged = player.isSneaking();
      double charge = CHARGE_BASE_VALUE
          + data.getCharges().getOrDefault(player.getUniqueId(), 0D) * CHARGE_MULTIPLIER;
      double speed = data.getSpeed().getOrDefault(player.getUniqueId(), MIN_SPEED_FOR_DAMPENING);
      double power = isCharged ? speed * KICK_POWER_SPEED_MULTIPLIER
          + Settings.KICK_BASE_POWER_CHARGED.asDouble()
          : speed * KICK_POWER_SPEED_MULTIPLIER + Settings.KICK_BASE_POWER_REGULAR.asDouble();
      double baseKickPower = charge * power;
      double finalKickPower = formulae.capKickPower(baseKickPower);

      return new PlayerKickResult(power, charge, baseKickPower, finalKickPower, isCharged);
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          sendMessage(PERM_HIT_DEBUG,
              "{prefix_debug}&dPhysicsSystem#calculateKickPower() &ftook &e" + ms + "ms");
        }
      }
    }
  }

  /**
   * Removes all Slime entities in the main world. Used only on plugin reload.
   */
  public void removeCubes() {
    long start = System.nanoTime();
    try {
      List<Entity> entities = plugin.getServer().getWorlds().get(0).getEntities();
      for (Entity entity : entities) {
        if (entity instanceof Slime) {
          ((Slime) entity).setHealth(0);
          if (!data.getCubes().contains(entity)) {
            entity.remove();
          }
        }
      }
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          sendMessage(PERM_HIT_DEBUG,
              "{prefix_debug}&dPhysicsSystem#removeCubes() &ftook &e" + ms + "ms");
        }
      }
    }
  }

  /**
   * Spawns a new ball at the given location.
   *
   * @param location The location to spawn the cube.
   * @return The spawned entity.
   */
  public Slime spawnCube(Location location) {
    long start = System.nanoTime();
    try {
      Slime cube = (Slime) location.getWorld().spawnEntity(location, EntityType.SLIME);
      cube.setRemoveWhenFarAway(false);
      cube.setSize(1);
      // Permanent jump effect that stops the cube from hopping.
      cube.addPotionEffect(
          new PotionEffect(PotionEffectType.JUMP, JUMP_POTION_DURATION, JUMP_POTION_AMPLIFIER,
              true), true);
      data.getCubes().add(cube);
      return cube;
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          sendMessage(PERM_HIT_DEBUG,
              "{prefix_debug}&dPhysicsSystem#spawnCube() &ftook &e" + ms + "ms");
        }
      }
    }
  }

  /**
   * Checks if the given player is not allowed to interact based on their game mode.
   *
   * @param player The player to check. Must not be null.
   * @return True if the player is not in survival game mode, otherwise false.
   */
  public boolean notAllowedToInteract(Player player) {
    return player.getGameMode() != GameMode.SURVIVAL;
  }

  /**
   * Determines if the specified player is currently away-from-keyboard (AFK) based on their last
   * recorded action time.
   *
   * @param player The player to check for AFK status. Must not be null.
   * @return True if the player is considered AFK (time since last action exceeds the set
   * threshold), false otherwise.
   */
  public boolean isAFK(Player player) {
    long last = data.getLastAction().getOrDefault(player.getUniqueId(), 0L);
    return System.currentTimeMillis() - last > Settings.getAFKThreshold();
  }

  /**
   * Records the most recent action performed by a player by updating the timestamp of their last
   * action.
   *
   * @param player The player whose action is being recorded. Must not be null.
   */
  public void recordPlayerAction(Player player) {
    data.getLastAction().put(player.getUniqueId(), System.currentTimeMillis());
  }

  /**
   * Removes a player and cleans up associated data in various system states. This includes clearing
   * cached player data, settings, physics data, cooldowns, and actions associated with the
   * specified player.
   *
   * @param player The player to be removed. Must not be null.
   */
  public void removePlayer(Player player) {
    long start = System.nanoTime();
    try {
      UUID uuid = player.getUniqueId();

      data.getSpeed().remove(uuid);
      data.getCharges().remove(uuid);
      data.getLastTouches().remove(uuid);
      data.getLastAction().remove(uuid);
      data.getCubeHits().remove(uuid);
      data.getButtonCooldowns().remove(uuid);
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          sendMessage(PERM_HIT_DEBUG,
              "{prefix_debug}&dPhysicsSystem#removePlayer() &ftook &e" + ms + "ms");
        }
      }
    }
  }

  /**
   * Displays hit-related information to the player and manages the cooldown logic for hits based on
   * whether the hit is a charged or regular hit. This method also logs any performance overhead if
   * the execution time exceeds a millisecond.
   *
   * @param player     The player who performed the hit. This parameter must not be null.
   * @param kickResult The result of the kick action, containing details such as kick power, charge
   *                   level, and whether the hit was charged. This parameter must not be null.
   */
  public void showHits(Player player, PlayerKickResult kickResult) {
    long start = System.nanoTime();
    try {
      UUID playerId = player.getUniqueId();
      boolean isChargedHit = kickResult.isChargedHit();
      double finalKickPower = kickResult.getFinalKickPower();
      CubeTouchType type = isChargedHit ? CubeTouchType.CHARGED_KICK : CubeTouchType.REGULAR_KICK;

      Map<CubeTouchType, CubeTouchInfo> touches = data.getLastTouches().get(playerId);
      long lastHitTime = 0L;
      if (touches != null && touches.containsKey(type)) {
        lastHitTime = touches.get(type).getTimestamp();
      }

      long cooldownDuration = isChargedHit ? Settings.KICK_COOLDOWN_CHARGED.asLong()
          : Settings.KICK_COOLDOWN_REGULAR.asLong();
      long currentTime = System.currentTimeMillis();
      long timeElapsed = currentTime - lastHitTime;
      long timeRemainingMillis = Math.max(0, cooldownDuration - timeElapsed);

      String timeFormatted = String.format("%.1f", timeRemainingMillis / 1000.0);
      String color = timeRemainingMillis > 50 ? "&c" : "&a";

      sendActionBar(player, HITDEBUG_PLAYER_WHOLE,
          isChargedHit ? HITDEBUG_PLAYER_CHARGED.replace(String.format("%.2f", finalKickPower),
              String.format("%.2f", kickResult.getPower()),
              String.format("%.2f", kickResult.getCharge()))
              : HITDEBUG_PLAYER_REGULAR.replace(String.format("%.2f", finalKickPower)),
          HITDEBUG_PLAYER_COOLDOWN.replace(color, timeFormatted));
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          sendMessage(PERM_HIT_DEBUG,
              "{prefix_debug}&dPhysicsSystem#showHits() &ftook &e" + ms + "ms");
        }
      }
    }
  }

  /**
   * Generates a debug message for a player's hit action based on whether the hit is charged or
   * regular. The method formats and returns a string with details including the player's name, kick
   * power, base power, and charge level. Logs execution time if it exceeds a defined threshold.
   *
   * @param player The player who performed the hit. This parameter must not be null.
   * @param result The result of the hit action, which contains information such as kick power,
   *               charge level, and whether the hit was charged. This parameter must not be null.
   * @return A string containing the formatted debug information about the hit action.
   */
  public String onHitDebug(Player player, PlayerKickResult result) {
    long start = System.nanoTime();
    try {
      String coloredKickPower =
          result.getFinalKickPower() != result.getBaseKickPower() ? "&c" : "&a";
      return result.isChargedHit() ? HITDEBUG_CHARGED.replace(player.getDisplayName(),
          coloredKickPower + String.format("%.2f", result.getFinalKickPower()),
          String.format("%.2f", result.getBaseKickPower()),
          String.format("%.2f", result.getPower()), String.format("%.2f", result.getCharge()))
          : HITDEBUG_REGULAR.replace(player.getDisplayName(),
              String.format("%.2f", result.getFinalKickPower()));
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          sendMessage(PERM_HIT_DEBUG,
              "{prefix_debug}&dPhysicsSystem#onHitDebug() &ftook &e" + ms + "ms");
        }
      }
    }
  }

  /**
   * Checks if a player is currently under a cooldown preventing cube (ball) spawning. If the
   * cooldown is active, a message is sent to the player and {@code true} is returned.
   *
   * @param player The player attempting to spawn a cube.
   * @return True if the player must wait before spawning again, false otherwise.
   */
  public boolean cantSpawnYet(Player player) {
    long start = System.nanoTime();
    try {
      if (player.hasPermission(PERM_BYPASS_COOLDOWN)) {
        return false;
      }

      UUID playerId = player.getUniqueId();
      long now = System.currentTimeMillis();
      long last = data.getButtonCooldowns().getOrDefault(playerId, 0L);
      long diff = now - last;

      if (diff < Settings.getSpawnCooldown()) {
        long remainingMs = Settings.getSpawnCooldown() - diff;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs);
        sendMessage(player, BLOCK_INTERACT_COOLDOWN, Utilities.formatTime(seconds));
        return true;
      }

      return false;
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          sendMessage(PERM_HIT_DEBUG,
              "{prefix_debug}&dPhysicsSystem#cantSpawnYet() &ftook &e" + ms + "ms");
        }
      }
    }
  }

  /**
   * Sets a cooldown timer preventing the player from spawning another cube immediately. Used to
   * rate-limit spawn button presses.
   *
   * @param player The player who triggered the spawn action.
   */
  public void setButtonCooldown(Player player) {
    data.getButtonCooldowns().put(player.getUniqueId(), System.currentTimeMillis());
  }
}
