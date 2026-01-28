package io.github.divinerealms.footcube.managers;

import io.github.divinerealms.footcube.FootCube;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.listeners.ChunkCheckers;
import io.github.divinerealms.footcube.listeners.CubeDamageListener;
import io.github.divinerealms.footcube.listeners.CubeKickListener;
import io.github.divinerealms.footcube.listeners.CubeTapListener;
import io.github.divinerealms.footcube.listeners.PlayerChargeListener;
import io.github.divinerealms.footcube.listeners.PlayerEvents;
import io.github.divinerealms.footcube.listeners.PlayerMovementListener;
import io.github.divinerealms.footcube.listeners.SignManipulation;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;

public class ListenerManager {

  private final FCManager fcManager;
  private final FootCube plugin;
  private final PluginManager pluginManager;

  @Getter
  private ChunkCheckers chunkCheckers;
  @Getter
  private PlayerEvents playerEvents;
  @Getter
  private SignManipulation signManipulation;
  @Getter
  private CubeDamageListener cubeDamageListener;
  @Getter
  private CubeKickListener cubeKickListener;
  @Getter
  private CubeTapListener cubeTapListener;
  @Getter
  private PlayerChargeListener playerChargeListener;
  @Getter
  private PlayerMovementListener playerMovementListener;

  public ListenerManager(FCManager fcManager) {
    this.fcManager = fcManager;
    this.plugin = fcManager.getPlugin();
    this.pluginManager = fcManager.getPlugin().getServer().getPluginManager();
    initializeAll();
  }

  public void registerAll() {
    unregisterAll();

    pluginManager.registerEvents(chunkCheckers, plugin);
    pluginManager.registerEvents(playerEvents, plugin);
    pluginManager.registerEvents(signManipulation, plugin);
    pluginManager.registerEvents(cubeDamageListener, plugin);
    pluginManager.registerEvents(cubeKickListener, plugin);
    pluginManager.registerEvents(cubeTapListener, plugin);
    pluginManager.registerEvents(playerChargeListener, plugin);
    pluginManager.registerEvents(playerMovementListener, plugin);

    fcManager.getLogger().info("&a✔ &2Registered &e5 &2listeners.");
  }

  private void initializeAll() {
    this.chunkCheckers = new ChunkCheckers();
    this.playerEvents = new PlayerEvents(fcManager);
    this.signManipulation = new SignManipulation(fcManager);
    this.cubeDamageListener = new CubeDamageListener(fcManager);
    this.cubeKickListener = new CubeKickListener(fcManager);
    this.cubeTapListener = new CubeTapListener(fcManager);
    this.playerChargeListener = new PlayerChargeListener(fcManager);
    this.playerMovementListener = new PlayerMovementListener(fcManager);
  }

  public void unregisterAll() {
    HandlerList.unregisterAll(plugin);
    fcManager.getLogger().info("&a✔ &2Unregistered listeners.");
  }
}
