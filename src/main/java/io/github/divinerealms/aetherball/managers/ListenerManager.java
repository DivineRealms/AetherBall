package io.github.divinerealms.aetherball.managers;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;

import io.github.divinerealms.aetherball.AetherBall;
import io.github.divinerealms.aetherball.listeners.ChunkCheckers;
import io.github.divinerealms.aetherball.listeners.CubeDamageListener;
import io.github.divinerealms.aetherball.listeners.CubeKickListener;
import io.github.divinerealms.aetherball.listeners.CubeTapListener;
import io.github.divinerealms.aetherball.listeners.PlayerChargeListener;
import io.github.divinerealms.aetherball.listeners.PlayerEvents;
import io.github.divinerealms.aetherball.listeners.PlayerMovementListener;
import io.github.divinerealms.aetherball.listeners.SignManipulation;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;

public class ListenerManager {

  private final Manager manager;
  private final AetherBall plugin;
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

  public ListenerManager(Manager manager) {
    this.manager = manager;
    this.plugin = manager.getPlugin();
    this.pluginManager = manager.getPlugin().getServer().getPluginManager();
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

    debugConsole("{prefix_success}Registered 5 listeners.");
  }

  private void initializeAll() {
    this.chunkCheckers = new ChunkCheckers();
    this.playerEvents = new PlayerEvents(manager);
    this.signManipulation = new SignManipulation(manager);
    this.cubeDamageListener = new CubeDamageListener(manager);
    this.cubeKickListener = new CubeKickListener(manager);
    this.cubeTapListener = new CubeTapListener(manager);
    this.playerChargeListener = new PlayerChargeListener(manager);
    this.playerMovementListener = new PlayerMovementListener(manager);
  }

  public void unregisterAll() {
    HandlerList.unregisterAll(plugin);
    debugConsole("{prefix_success}Unregistered listeners.");
  }
}
