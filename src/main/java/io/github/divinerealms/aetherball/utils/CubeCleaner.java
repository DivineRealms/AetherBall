package io.github.divinerealms.aetherball.utils;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.ConfigManager;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Slime;

import java.util.ArrayList;
import java.util.List;

import static io.github.divinerealms.aetherball.configs.Lang.PRACTICE_AREAS_EMPTY;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

public class CubeCleaner {

  private final PhysicsData data;
  private final FileConfiguration practice;

  private final List<PracticeArea> practiceAreas;

  @Getter
  private boolean empty;
  @Getter
  private int amount = 0;

  public CubeCleaner(Manager manager) {
    this.data = manager.getPhysicsData();
    ConfigManager configManager = manager.getConfigManager();
    this.practice = configManager.getConfig("practice.yml");
    this.practiceAreas = new ArrayList<>();

    if (noPracticeAreasSet()) {
      logConsole(PRACTICE_AREAS_EMPTY);
      return;
    }

    loadPracticeAreas();
  }

  private void loadPracticeAreas() {
    practiceAreas.clear();

    if (!practice.contains("practice-areas")) {
      return;
    }

    if (practice.getConfigurationSection("practice-areas") == null) {
      return;
    }

    for (String locationName : practice.getConfigurationSection("practice-areas").getKeys(false)) {
      Location location = (Location) practice.get("practice-areas." + locationName);
      if (location == null) {
        continue;
      }

      practiceAreas.add(new PracticeArea(location, Settings.PRACTICE_AREA_RADIUS.asDouble()));
    }
  }

  public void clearCubes() {
    this.empty = true;
    this.amount = 0;

    if (practiceAreas.isEmpty()) {
      return;
    }

    if (data == null) {
      return;
    }

    if (data.getCubes() == null) {
      return;
    }

    if (data.getCubes().isEmpty()) {
      return;
    }

    for (Slime cube : data.getCubes()) {
      if (cube == null) {
        continue;
      }

      if (cube.isDead()) {
        continue;
      }

      Location cubeLocation = cube.getLocation();
      if (cubeLocation == null) {
        continue;
      }

      for (PracticeArea area : practiceAreas) {
        if (area.contains(cubeLocation)) {
          this.amount++;
          this.empty = false;
          cube.setHealth(0);
          break;
        }
      }
    }
  }

  public boolean noPracticeAreasSet() {
    return !practice.contains("practice-areas");
  }

  private record PracticeArea(Location center, double radiusSquared) {

    private PracticeArea(Location center, double radiusSquared) {
      this.center = center;
      this.radiusSquared = radiusSquared * radiusSquared;
    }

    boolean contains(Location location) {
      if (location.getWorld() == null || center.getWorld() == null) {
        return false;
      }

      if (!location.getWorld().equals(center.getWorld())) {
        return false;
      }

      double radius = Math.sqrt(radiusSquared);
      if (Math.abs(location.getX() - center.getX()) > radius) {
        return false;
      }

      if (Math.abs(location.getY() - center.getY()) > radius) {
        return false;
      }

      if (Math.abs(location.getZ() - center.getZ()) > radius) {
        return false;
      }

      double distanceSquared = location.distanceSquared(center);
      return distanceSquared <= radiusSquared;
    }
  }
}
