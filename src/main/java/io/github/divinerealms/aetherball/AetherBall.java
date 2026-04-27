package io.github.divinerealms.aetherball;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.utils.LoggerUtil;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

public class AetherBall extends JavaPlugin {

  private Manager manager;

  @Override
  public void onEnable() {
    try {
      LoggerUtil.initialize(this);
      this.manager = new Manager(this);
      manager.setEnabling(true);
      manager.reload();
      logConsole("{prefix_success}Successfully enabled AetherBall v" + getDescription().getVersion()
          + "!");
    } catch (Exception exception) {
      getLogger().log(Level.SEVERE, "Failed to initialize AetherBall: " + exception.getMessage(),
          exception);
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  public void onDisable() {
    if (manager == null) {
      return;
    }

    try {
      manager.setDisabling(true);
      if (manager.getMatchManager() != null) {
        manager.getMatchManager().forceLeaveAllPlayers();
      }
      manager.getPhysicsSystem().removeCubes();
      manager.getTaskManager().stopAll();
      manager.saveAll();
      manager.cleanup();
      getServer().getScheduler().cancelTasks(this);
      logConsole("{prefix_success}Successfully disabled.");
    } catch (Exception exception) {
      getLogger().log(Level.SEVERE, "Error during plugin shutdown: " + exception.getMessage(),
          exception);
    }
  }
}
