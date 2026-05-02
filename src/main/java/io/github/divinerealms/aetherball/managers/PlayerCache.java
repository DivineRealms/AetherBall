package io.github.divinerealms.aetherball.managers;

import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.utils.PlayerSettings;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

public class PlayerCache {
  private final Manager manager;
  private final JavaPlugin plugin;

  @Getter
  private final Set<Player> cachedPlayers = ConcurrentHashMap.newKeySet();
  @Getter
  private final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();
  @Getter
  private final Map<UUID, String> cachedPrefixedNames = new ConcurrentHashMap<>();

  public PlayerCache(Manager manager) {
    this.manager = manager;
    this.plugin = manager.getPlugin();
  }

  public void initialize() {
    cachedPlayers.clear();
    cachedPlayers.addAll(plugin.getServer().getOnlinePlayers());
  }

  public PlayerSettings getSettings(Player player) {
    return playerSettings.get(player.getUniqueId());
  }

  public String getPrefixedName(UUID uuid) {
    return cachedPrefixedNames.get(uuid);
  }

  public void cachePrefixedName(Player player) {
    UUID uuid = player.getUniqueId();
    manager.getUtilities().getPrefixedName(uuid, player.getName()).thenAccept(name -> cachedPrefixedNames.put(uuid, name));
    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> cachedPrefixedNames.remove(uuid), Settings.getPrefixExpiry());
  }

  public void preloadSettings(Player player, PlayerData playerData) {
    PlayerSettings settings = playerSettings.computeIfAbsent(
        player.getUniqueId(),
        uuid -> PlayerSettings.withCurrentDefaults()
    );

    loadParticleSettings(player, playerData, settings);
    loadSoundSettings(playerData, settings);
    loadBanData(player, playerData);
    loadGoalCelebration(playerData, settings);
  }

  public void reloadDefaults() {
    for (PlayerSettings settings : playerSettings.values()) {
      settings.applyConfigDefaults();
    }
  }

  private void loadParticleSettings(Player player, PlayerData playerData, PlayerSettings settings) {
    if (!playerData.has("particles.effect")) {
      return;
    }

    String effect = playerData.getString("particles.effect");
    if (effect == null) {
      return;
    }

    try {
      EnumParticle particle = EnumParticle.valueOf(effect.split(":")[0]);
      settings.setParticle(particle);
      if (particle == EnumParticle.REDSTONE && effect.contains(":")) {
        try {
          settings.setCustomRedstoneColor(effect.split(":")[1]);
        } catch (IllegalArgumentException ignored) {
        }
      }
    } catch (IllegalArgumentException exception) {
      logConsole("{prefix_warn}Invalid particle for " + player.getName() + ": " + effect);
    }

    if (playerData.has("particles.enabled")) {
      settings.setParticlesEnabled(playerData.getBoolean("particles.enabled"));
    }

    if (playerData.has("particles.always-show")) {
      settings.setAlwaysShowParticles(playerData.getBoolean("particles.always-show"));
    }
  }

  private void loadSoundSettings(PlayerData playerData, PlayerSettings playerSettings) {
    if (playerData.has("sounds.kick.enabled")) {
      playerSettings.setKickSoundEnabled(playerData.getBoolean("sounds.kick.enabled"));
    }

    if (playerData.has("sounds.kick.sound")) {
      String name = playerData.getString("sounds.kick.sound");
      if (name != null) {
        try {
          playerSettings.setKickSound(Sound.valueOf(name));
        } catch (IllegalArgumentException ignored) {

        }
      }
    }

    if (playerData.has("sounds.goal.enabled")) {
      playerSettings.setGoalSoundEnabled(playerData.getBoolean("sounds.goal.enabled"));
    }

    if (playerData.has("sounds.goal.sound")) {
      String name = playerData.getString("sounds.goal.sound");
      if (name != null) {
        try {
          playerSettings.setGoalSound(Sound.valueOf(name));
        } catch (IllegalArgumentException ignored) {

        }
      }
    }
  }

  private void loadBanData(Player player, PlayerData playerData) {
    long banUntil = playerData.getLong("ban");
    if (banUntil > 0) {
      manager.getBanManager().getBannedPlayers().put(player.getUniqueId(), banUntil);
    }
  }

  private void loadGoalCelebration(PlayerData playerData, PlayerSettings playerSettings) {
    String goalCelebration = playerData.getString("goalcelebration");
    playerSettings.setGoalMessage(goalCelebration != null ? goalCelebration : "default");
  }

  public void removePlayer(UUID uuid) {
    playerSettings.remove(uuid);
    cachedPrefixedNames.remove(uuid);
  }

  public void clear() {
    cachedPlayers.clear();
    playerSettings.clear();
    cachedPrefixedNames.clear();
  }
}
