package io.github.divinerealms.aetherball.matchmaking;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.ConfigManager;
import io.github.divinerealms.aetherball.managers.Manager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

@Getter
public class ArenaManager {

  private final Manager manager;
  private final Server server;
  private final List<Arena> arenas = new ArrayList<>();
  private final ConfigManager configManager;

  @Setter private Map<Player, ArenaSetup> setupWizards = new HashMap<>();

  public ArenaManager(Manager manager) {
    this.manager = manager;
    this.server = manager.getPlugin().getServer();
    this.configManager = manager.getConfigManager();
    loadArenas();
  }

  public void reloadArenas() {
    arenas.clear();
    configManager.reloadConfig("arenas.yml");
    loadArenas();
    debugConsole("{prefix_success}Arenas reloaded successfully! Total arenas: &e" + arenas.size());
  }

  private void loadArenas() {
    FileConfiguration config = configManager.getConfig("arenas.yml");

    if (config == null) {
      logConsole("{prefix_error}arenas.yml not found!");
      return;
    }

    World world = server.getWorld(config.getString("arenas.world", "world"));
    if (world == null) {
      logConsole("{prefix_error}World for arenas not found!");
      return;
    }

    for (String type : new String[] {"1v1", "2v2", "3v3", "4v4", "5v5"}) {
      int amount = config.getInt("arenas." + type + ".amount", 0);
      for (int i = 1; i <= amount; i++) {
        String bluePath = "arenas." + type + "." + i + ".blue.";
        String redPath = "arenas." + type + "." + i + ".red.";

        Location blueSpawn = getLocation(config, world, bluePath);
        Location redSpawn = getLocation(config, world, redPath);
        addArena(Integer.parseInt(type.substring(0, 1)), blueSpawn, redSpawn);
      }
    }
  }

  private Location getLocation(FileConfiguration config, World world, String path) {
    Location location =
        new Location(
            world,
            config.getDouble(path + "x"),
            config.getDouble(path + "y"),
            config.getDouble(path + "z"));
    location.setPitch((float) config.getDouble(path + "pitch"));
    location.setYaw((float) config.getDouble(path + "yaw"));
    return location;
  }

  public void createArena(int type, Location blueSpawn, Location redSpawn) {
    blueSpawn = normalizeLocation(blueSpawn);
    redSpawn = normalizeLocation(redSpawn);

    blueSpawn.setYaw(normalizeYaw(blueSpawn.getYaw()));
    blueSpawn.setPitch(0.0F);

    redSpawn.setYaw(normalizeYaw(redSpawn.getYaw()));
    redSpawn.setPitch(0.0F);

    FileConfiguration config = configManager.getConfig("arenas.yml");

    String typeString = Settings.getMatchTypeName(type);
    int index = config.getInt("arenas." + typeString + ".amount", 0) + 1;

    config.set("arenas." + typeString + ".amount", index);
    config.set("arenas.world", blueSpawn.getWorld().getName());

    String bluePath = "arenas." + typeString + "." + index + ".blue.";
    String redPath = "arenas." + typeString + "." + index + ".red.";

    saveLocation(config, bluePath, blueSpawn);
    saveLocation(config, redPath, redSpawn);

    configManager.saveConfig("arenas.yml");
    addArena(type, blueSpawn, redSpawn);

    Arena newArena = arenas.getLast();

    debugConsole(
        "{prefix_success}Created "
            + typeString
            + " arena (ID: "
            + newArena.id()
            + ") at "
            + formatLocation(blueSpawn)
            + " and "
            + formatLocation(redSpawn));
  }

  private Location normalizeLocation(Location location) {
    Location normalized = location.clone();
    normalized.setX(Math.round(location.getX() - 0.5) + 0.5);
    normalized.setY(Math.floor(location.getY()));
    normalized.setZ(Math.round(location.getZ() - 0.5) + 0.5);
    return normalized;
  }

  private float normalizeYaw(float yaw) {
    while (yaw > 180) {
      yaw -= 360;
    }

    while (yaw < -180) {
      yaw += 360;
    }

    // Round to nearest cardinal direction
    if (yaw >= -45 && yaw < 45) {
      return 0.0F; // South
    } else if (yaw >= 45 && yaw < 135) {
      return 90.0F; // West
    } else if (yaw >= 135 || yaw < -135) {
      return 180.0F; // North
    } else {
      return -90.0F; // East
    }
  }

  private void addArena(int type, Location blue, Location red) {
    Location center =
        new Location(
            blue.getWorld(),
            (blue.getX() + red.getX()) / 2.0,
            (blue.getY() + red.getY()) / 2.0 + 2.0,
            (blue.getZ() + red.getZ()) / 2.0);

    boolean isXAxis = Math.abs(blue.getX() - red.getX()) > Math.abs(blue.getZ() - red.getZ());
    boolean redIsGreater = isXAxis ? red.getX() > blue.getX() : red.getZ() > blue.getZ();

    int id = arenas.size() + 1;
    arenas.add(new Arena(id, type, blue, red, center, isXAxis, redIsGreater));
  }

  private void saveLocation(FileConfiguration config, String path, Location location) {
    config.set(path + "x", location.getX());
    config.set(path + "y", location.getY());
    config.set(path + "z", location.getZ());
    config.set(path + "pitch", location.getPitch());
    config.set(path + "yaw", location.getYaw());
  }

  public void clearArenas() {
    FileConfiguration config = configManager.getConfig("arenas.yml");

    config.set("arenas", null);
    configManager.saveConfig("arenas.yml");

    arenas.clear();
    debugConsole("{prefix_success}All arenas cleared!");
  }

  public void clearArenaType(int type) {
    String typeString = Settings.getMatchTypeName(type);
    FileConfiguration config = configManager.getConfig("arenas.yml");

    config.set("arenas." + typeString, null);
    configManager.saveConfig("arenas.yml");

    arenas.removeIf(arena -> arena.type() == type);

    reassignArenaIds();

    debugConsole("{prefix_success}Cleared all " + typeString + " arenas!");
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean hasArenaForType(int matchType) {
    for (Arena arena : arenas) {
      if (arena.type() == matchType) {
        return true;
      }
    }
    return false;
  }

  public int getArenaCountType(int matchType) {
    int count = 0;
    for (Arena arena : arenas) {
      if (arena.type() == matchType) {
        count++;
      }
    }
    return count;
  }

  private void reassignArenaIds() {
    List<Arena> newArenas = new ArrayList<>();
    for (int i = 0; i < arenas.size(); i++) {
      Arena oldArena = arenas.get(i);
      Arena newArena =
          new Arena(
              i + 1,
              oldArena.type(),
              oldArena.blueSpawn(),
              oldArena.redSpawn(),
              oldArena.center(),
              oldArena.isXAxis(),
              oldArena.redIsGreater());
      newArenas.add(newArena);
    }
    arenas.clear();
    arenas.addAll(newArenas);
  }

  private String formatLocation(Location location) {
    return String.format(
        "%.1f, %.1f, %.1f (yaw: %.0f, pitch: %.0f)",
        location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
  }

  @Getter
  @Setter
  public static class ArenaSetup {

    private int type;
    private Location blueSpawn;

    public ArenaSetup(int type) {
      this.type = type;
    }
  }

  public record Arena(
      int id,
      int type,
      Location blueSpawn,
      Location redSpawn,
      Location center,
      boolean isXAxis,
      boolean redIsGreater) {}
}
