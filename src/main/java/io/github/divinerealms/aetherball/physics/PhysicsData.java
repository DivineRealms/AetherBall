package io.github.divinerealms.aetherball.physics;

import io.github.divinerealms.aetherball.physics.touch.CubeTouchInfo;
import io.github.divinerealms.aetherball.physics.touch.CubeTouchType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Slime;
import org.bukkit.util.Vector;

@Getter
public class PhysicsData {

  private final Set<Slime> cubes = ConcurrentHashMap.newKeySet();
  private final Set<Slime> cubesToRemove = ConcurrentHashMap.newKeySet();

  private final Map<UUID, Vector> velocities = new ConcurrentHashMap<>();
  private final Map<UUID, Double> speed = new ConcurrentHashMap<>();
  private final Map<UUID, Double> charges = new ConcurrentHashMap<>();

  private final Map<UUID, Map<CubeTouchType, CubeTouchInfo>> lastTouches = new ConcurrentHashMap<>();
  private final Map<UUID, Long> raised = new ConcurrentHashMap<>();

  private final Map<UUID, Long> lastAction = new ConcurrentHashMap<>();
  private final Set<UUID> cubeHits = ConcurrentHashMap.newKeySet();
  private final Map<UUID, Long> buttonCooldowns = new ConcurrentHashMap<>();

  private final Map<UUID, Location> previousCubeLocations = new ConcurrentHashMap<>();
  public long tickRate = 0;
  @Setter
  private boolean hitDebugEnabled = false;

  public void cleanup() {
    cubes.clear();
    cubesToRemove.clear();
    velocities.clear();
    speed.clear();
    charges.clear();
    lastTouches.clear();
    raised.clear();
    lastAction.clear();
    cubeHits.clear();
    buttonCooldowns.clear();
    previousCubeLocations.clear();
    tickRate = 0;
  }
}