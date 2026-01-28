package io.github.divinerealms.footcube.listeners;

import static io.github.divinerealms.footcube.configs.Lang.CUBE_CLEAR;
import static io.github.divinerealms.footcube.configs.Lang.HITDEBUG_WHOLE;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_CLEAR_CUBE;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_HIT_DEBUG;

import io.github.divinerealms.footcube.configs.Settings;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.physics.PhysicsData;
import io.github.divinerealms.footcube.physics.touch.CubeTouchInfo;
import io.github.divinerealms.footcube.physics.touch.CubeTouchType;
import io.github.divinerealms.footcube.physics.utilities.PhysicsSystem;
import io.github.divinerealms.footcube.physics.utilities.PlayerKickResult;
import io.github.divinerealms.footcube.utils.Logger;
import io.github.divinerealms.footcube.utils.PlayerSettings;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class CubeKickListener implements Listener {

  private final FCManager fcManager;
  private final BukkitScheduler scheduler;
  private final Plugin plugin;
  private final Logger logger;
  private final PhysicsSystem system;
  private final PhysicsData data;

  public CubeKickListener(FCManager fcManager) {
    this.fcManager = fcManager;
    this.scheduler = fcManager.getScheduler();
    this.plugin = fcManager.getPlugin();
    this.logger = fcManager.getLogger();
    this.system = fcManager.getPhysicsSystem();
    this.data = fcManager.getPhysicsData();
  }

  /**
   * Handles custom hit detection for cube entities when attacked by players.
   * <p>Replaces standard attack behavior with physics-driven logic such as applying
   * kick velocity, cooldown tracking, and custom hit effects.</p>
   *
   * @param event the {@link EntityDamageByEntityEvent} triggered when one entity damages another
   */
  @EventHandler
  public void leftClick(EntityDamageByEntityEvent event) {
    long start = System.nanoTime();
    try {
      if (!(event.getEntity() instanceof Slime)) {
        return;
      }

      if (!(event.getDamager() instanceof Player)) {
        return;
      }

      if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
        return;
      }

      if (!data.getCubes().contains((Slime) event.getEntity())) {
        return;
      }

      Slime cube = (Slime) event.getEntity();
      Player player = (Player) event.getDamager();
      UUID playerId = player.getUniqueId();

      // Creative players can remove cubes directly.
      if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission(PERM_CLEAR_CUBE)) {
        cube.setHealth(0);
        logger.send(player, CUBE_CLEAR);
        return;
      }

      // Prevent unauthorized players from interacting.
      if (system.notAllowedToInteract(player)) {
        return;
      }

      // Check & Enforce cooldown.
      CubeTouchType kickType =
          player.isSneaking() ? CubeTouchType.CHARGED_KICK : CubeTouchType.REGULAR_KICK;

      Map<CubeTouchType, CubeTouchInfo> touches = data.getLastTouches().get(playerId);
      if (touches != null && touches.containsKey(kickType)) {
        CubeTouchInfo lastTouch = touches.get(kickType);
        long elapsed = System.currentTimeMillis() - lastTouch.getTimestamp();

        if (elapsed < kickType.getCooldown()) {
          event.setCancelled(true);
          return; // Still on cooldown.
        }
        // Cooldown expired, allow kick and update below.
      }

      // Calculate kick result.
      PlayerKickResult kickResult = system.calculateKickPower(player);

      // Compute final kick direction and apply impulse.
      Vector kick = player.getLocation().getDirection().normalize()
          .multiply(kickResult.getFinalKickPower()).setY(Settings.KICK_VERTICAL_BOOST.asDouble());
      cube.setVelocity(cube.getVelocity().add(kick));

      // Update cooldown entry (overwrites old one if exists).
      data.getLastTouches().computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
          .put(kickType, new CubeTouchInfo(System.currentTimeMillis(), kickType));

      // Record interaction.
      system.recordPlayerAction(player);
      fcManager.getMatchManager().kick(player);

      // Sound effects.
      cube.getWorld().playSound(cube.getLocation(), Sound.SLIME_WALK, 0.75F, 1.0F);

      // Schedule post-processing for player sound feedback and debug info.
      scheduler.runTask(plugin, () -> {
        PlayerSettings playerSettings = fcManager.getPlayerSettings(player);
        if (playerSettings != null && playerSettings.isKickSoundEnabled()) {
          player.playSound(player.getLocation(), playerSettings.getKickSound(), 0.5F, 1.0F);
        }

        if (data.isHitDebugEnabled()) {
          logger.send(PERM_HIT_DEBUG, player.getLocation(), 100, HITDEBUG_WHOLE,
              system.onHitDebug(player, kickResult));
        }

        if (data.getCubeHits().contains(playerId)) {
          system.showHits(player, kickResult);
        }
      });

      event.setCancelled(true);
    } finally {
      if (Settings.DEBUG_MODE.asBoolean()) {
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > Settings.DEBUG_THRESHOLD.asLong()) {
          logger.send("group.fcfa", "{prefix-admin}&dCubeKickListener &ftook &e" + ms + "ms");
        }
      }
    }
  }
}