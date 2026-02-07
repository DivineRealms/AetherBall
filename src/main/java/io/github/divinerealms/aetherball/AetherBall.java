package io.github.divinerealms.aetherball;

import io.github.divinerealms.aetherball.core.Manager;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class AetherBall extends JavaPlugin {

  private Manager manager;

  @Override
  public void onEnable() {
    try {
      this.manager = new Manager(this);
      manager.setEnabling(true);
      manager.reload();
      manager.getLogger()
          .info(
              "&a✔ &2Successfully enabled &bAetherBall v" + getDescription().getVersion() + "&2!");
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
      manager.getLogger().info("&c✘ &4Successfully disabled.");
    } catch (Exception exception) {
      getLogger().log(Level.SEVERE, "Error during plugin shutdown: " + exception.getMessage(),
          exception);
    }
  }
}
