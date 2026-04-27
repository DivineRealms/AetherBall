package io.github.divinerealms.aetherball.listeners;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.physics.touch.CubeTouchInfo;
import io.github.divinerealms.aetherball.physics.touch.CubeTouchType;
import io.github.divinerealms.aetherball.physics.utilities.PhysicsSystem;
import io.github.divinerealms.aetherball.utils.PlayerSettings;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.divinerealms.aetherball.configs.Lang.CUBE_CLEAR;
import static io.github.divinerealms.aetherball.configs.Lang.HITDEBUG_WHOLE;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_CLEAR_CUBE;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_HIT_DEBUG;

/**
 * Handles player interactions with cube entities through left-click attacks.
 * <p>
 * Replaces standard entity damage behavior with custom physics-driven kicking mechanics, including
 * velocity application, cooldown management, sound effects, and debug feedback.
 * </p>
 */
public class CubeKickListener extends BaseListener {

  private final Manager manager;
  private final BukkitScheduler scheduler;
  private final Plugin plugin;
  private final PhysicsSystem system;
  private final PhysicsData data;
  private final MatchManager matchManager;

  public CubeKickListener(Manager manager) {
    this.manager = manager;
    this.scheduler = manager.getScheduler();
    this.plugin = manager.getPlugin();
    this.system = manager.getPhysicsSystem();
    this.data = manager.getPhysicsData();
    this.matchManager = manager.getMatchManager();
  }

  /**
   * Handles custom hit detection for cube entities when attacked by players.
   * <p>
   * Replaces standard attack behavior with physics-driven logic including velocity application,
   * cooldown tracking, power calculations, and debug feedback. Creative mode players with
   * appropriate permissions can instantly remove cubes.
   * </p>
   *
   * @param event the {@link EntityDamageByEntityEvent} triggered when one entity damages another
   */
  @EventHandler
  public void leftClick(EntityDamageByEntityEvent event) {
    monitoredExecution(() -> {
      // Only process Slime entities (cubes).
      if (!(event.getEntity() instanceof Slime cube)) {
        return;
      }

      // Only process player attacks.
      if (!(event.getDamager() instanceof Player player)) {
        return;
      }

      // Only handle direct entity attacks, not projectiles or other damage causes.
      if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
        return;
      }

      // Only process tracked physics cubes.
      if (!data.getCubes().contains((Slime) event.getEntity())) {
        return;
      }

      UUID playerId = player.getUniqueId();

      // Creative players with permission can instantly remove cubes.
      if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission(PERM_CLEAR_CUBE)) {
        cube.setHealth(0);
        sendMessage(player, CUBE_CLEAR);
        return;
      }

      // Prevent unauthorized players from interacting with cubes.
      if (system.notAllowedToInteract(player)) {
        return;
      }

      // Determine kick type: charged (sneaking) or regular.
      CubeTouchType kickType =
          player.isSneaking() ? CubeTouchType.CHARGED_KICK : CubeTouchType.REGULAR_KICK;

      // Check if player is still on cooldown for this kick type.
      Map<CubeTouchType, CubeTouchInfo> touches = data.getLastTouches().get(playerId);
      if (touches != null && touches.containsKey(kickType)) {
        CubeTouchInfo lastTouch = touches.get(kickType);
        long elapsed = System.currentTimeMillis() - lastTouch.getTimestamp();

        if (elapsed < kickType.getCooldown()) {
          event.setCancelled(true);
          return; // Still on cooldown, prevent kick.
        }
      }

      // Calculate kick power based on player state and settings.
      PhysicsSystem.PlayerKickResult kickResult = system.calculateKickPower(player);

      // Apply kick velocity to cube: direction from player's view + vertical boost.
      Vector kick = player.getLocation().getDirection().normalize()
          .multiply(kickResult.finalKickPower()).setY(Settings.KICK_VERTICAL_BOOST.asDouble());
      cube.setVelocity(cube.getVelocity().add(kick));

      // Update cooldown timestamp for this kick type.
      data.getLastTouches().computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
          .put(kickType, new CubeTouchInfo(System.currentTimeMillis(), kickType));

      // Record player action for tracking and notify match manager.
      system.recordPlayerAction(player);
      matchManager.kick(player);

      // Play cube kick sound at cube location.
      cube.getWorld().playSound(cube.getLocation(), Sound.SLIME_WALK, 0.75F, 1.0F);

      // Schedule async tasks for player feedback (sounds and debug info).
      scheduler.runTask(plugin, () -> {
        // Play personalized kick sound if player has it enabled.
        PlayerSettings playerSettings = manager.getPlayerSettings(player);
        if (playerSettings != null && playerSettings.isKickSoundEnabled()) {
          player.playSound(player.getLocation(), playerSettings.getKickSound(), 0.5F, 1.0F);
        }

        // Send debug information to nearby players with debug permission.
        if (data.isHitDebugEnabled()) {
          sendMessage(PERM_HIT_DEBUG, player.getLocation(), 100, HITDEBUG_WHOLE,
              system.onHitDebug(player, kickResult));
        }

        // Show visual hit feedback if player has it enabled.
        if (data.getCubeHits().contains(playerId)) {
          system.showHits(player, kickResult);
        }
      });

      // Cancel the damage event to prevent standard Minecraft damage behavior.
      event.setCancelled(true);
    });
  }
}