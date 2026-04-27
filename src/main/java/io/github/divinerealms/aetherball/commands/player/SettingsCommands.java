package io.github.divinerealms.aetherball.commands.player;

import static io.github.divinerealms.aetherball.configs.Lang.AVAILABLE_TYPE;
import static io.github.divinerealms.aetherball.configs.Lang.COLOR;
import static io.github.divinerealms.aetherball.configs.Lang.INVALID_COLOR;
import static io.github.divinerealms.aetherball.configs.Lang.INVALID_TYPE;
import static io.github.divinerealms.aetherball.configs.Lang.OFF;
import static io.github.divinerealms.aetherball.configs.Lang.ON;
import static io.github.divinerealms.aetherball.configs.Lang.PARTICLE;
import static io.github.divinerealms.aetherball.configs.Lang.SET_GOAL_CELEBRATION;
import static io.github.divinerealms.aetherball.configs.Lang.SET_PARTICLE;
import static io.github.divinerealms.aetherball.configs.Lang.SET_PARTICLE_REDSTONE;
import static io.github.divinerealms.aetherball.configs.Lang.SET_SOUND_GOAL;
import static io.github.divinerealms.aetherball.configs.Lang.SET_SOUND_KICK;
import static io.github.divinerealms.aetherball.configs.Lang.SOUND;
import static io.github.divinerealms.aetherball.configs.Lang.TOGGLES_GOAL;
import static io.github.divinerealms.aetherball.configs.Lang.TOGGLES_HIT_DEBUG;
import static io.github.divinerealms.aetherball.configs.Lang.TOGGLES_KICK;
import static io.github.divinerealms.aetherball.configs.Lang.TOGGLES_PARTICLES;
import static io.github.divinerealms.aetherball.configs.Lang.TOGGLES_PARTICLES_MODE;
import static io.github.divinerealms.aetherball.configs.Lang.USAGE;
import static io.github.divinerealms.aetherball.utils.MatchUtils.joinStrings;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_SET_GOAL_CELEBRATION;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_SET_PARTICLE;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_SET_SOUND;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.aetherball.configs.PlayerData;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.managers.PlayerDataManager;
import io.github.divinerealms.aetherball.physics.PhysicsData;
import io.github.divinerealms.aetherball.utils.PlayerSettings;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@CommandAlias("aetherball|ab|fc")
public class SettingsCommands extends BaseCommand {

  private final Manager manager;
  private final PlayerDataManager dataManager;
  private final PhysicsData physicsData;

  public SettingsCommands(Manager manager) {
    this.manager = manager;
    this.dataManager = manager.getDataManager();
    this.physicsData = manager.getPhysicsData();
  }

  @CommandAlias("abtoggle|fctoggle|abt|fct")
  @Subcommand("toggle|tg")
  @Syntax("<kick|goal|particles|particlemode|hits>")
  @CommandCompletion("kick|goal|particles|particlemode|hits")
  @Description("Toggle AetherBall Settings")
  public void onToggle(Player player, String toggleType) {
    PlayerData data = dataManager.get(player);
    if (data == null) {
      return;
    }

    String toggleTypeLower = toggleType.toLowerCase();
    PlayerSettings settings = manager.getPlayerSettings(player);

    switch (toggleTypeLower) {
      case "kick":
        settings.setKickSoundEnabled(!settings.isKickSoundEnabled());
        data.set("sounds.kick.enabled", settings.isKickSoundEnabled());
        sendMessage(player, TOGGLES_KICK,
            settings.isKickSoundEnabled() ? ON.toString() : OFF.toString());
        break;

      case "goal":
        settings.setGoalSoundEnabled(!settings.isGoalSoundEnabled());
        data.set("sounds.goal.enabled", settings.isGoalSoundEnabled());
        sendMessage(player, TOGGLES_GOAL,
            settings.isGoalSoundEnabled() ? ON.toString() : OFF.toString());
        break;

      case "particles":
        settings.setParticlesEnabled(!settings.isParticlesEnabled());
        data.set("particles.enabled", settings.isParticlesEnabled());
        sendMessage(player, TOGGLES_PARTICLES,
            settings.isParticlesEnabled() ? ON.toString() : OFF.toString());
        break;

      case "particlemode":
      case "pmode":
        if (!settings.isParticlesEnabled()) {
          sendMessage(player,
              "{prefix_error}You must enable particles first with &e/fct particles");
          return;
        }

        settings.setAlwaysShowParticles(!settings.isAlwaysShowParticles());
        data.set("particles.always-show", settings.isAlwaysShowParticles());

        String mode = settings.isAlwaysShowParticles() ? "always" : "far only";
        sendMessage(player, TOGGLES_PARTICLES_MODE, mode);
        break;
      case "hits":
        boolean status = physicsData.getCubeHits().contains(player.getUniqueId());
        if (status) {
          physicsData.getCubeHits().remove(player.getUniqueId());
        } else {
          physicsData.getCubeHits().add(player.getUniqueId());
        }
        sendMessage(player, TOGGLES_HIT_DEBUG, !status ? ON.toString() : OFF.toString());
        break;

      default:
        sendMessage(player, USAGE, getExecSubcommand());
        break;
    }
  }

  @CommandAlias("absetsound|fcsetsound|abss|fcss")
  @Subcommand("setsound")
  @CommandPermission(PERM_SET_SOUND)
  @Syntax("<kick|goal> <soundName|list>")
  @CommandCompletion("kick|goal list")
  @Description("Set sounds")
  public void onSetSound(Player player, String soundType, String soundName) {
    String soundTypeLower = soundType.toLowerCase();

    if (!soundTypeLower.equals("kick") && !soundTypeLower.equals("goal")) {
      sendMessage(player, USAGE, getExecSubcommand());
      return;
    }

    List<Sound> allowedSounds = soundTypeLower.equals("kick")
        ? PlayerSettings.ALLOWED_KICK_SOUNDS
        : PlayerSettings.ALLOWED_GOAL_SOUNDS;

    if (soundName.equalsIgnoreCase("list")) {
      showAllowedSounds(player, allowedSounds);
      return;
    }

    PlayerData data = dataManager.get(player);
    if (data == null) {
      return;
    }

    Sound sound = parseSound(player, soundName);
    if (sound == null) {
      return;
    }

    if (!allowedSounds.contains(sound)) {
      sendMessage(player, INVALID_TYPE, SOUND.toString());
      showAllowedSounds(player, allowedSounds);
      return;
    }

    PlayerSettings settings = manager.getPlayerSettings(player);

    if (soundTypeLower.equals("kick")) {
      settings.setKickSound(sound);
      data.set("sounds.kick.sound", sound.toString());
      sendMessage(player, SET_SOUND_KICK, sound.name());
    } else {
      settings.setGoalSound(sound);
      data.set("sounds.goal.sound", sound.toString());
      sendMessage(player, SET_SOUND_GOAL, sound.name());
    }
  }

  @CommandAlias("absetparticle|fcsetparticle|absp|fcsp")
  @Subcommand("setparticle")
  @CommandPermission(PERM_SET_PARTICLE)
  @Syntax("<particleName|list> [color]")
  @CommandCompletion("list|@particles @colors")
  @Description("Set particle trail effect for cubes")
  public void onSetParticle(Player player, String particleName, @Optional String color) {
    if (particleName.equalsIgnoreCase("list")) {
      sendMessage(player, AVAILABLE_TYPE, PARTICLE.toString(),
          joinStrings(PlayerSettings.getAllowedParticles()));
      return;
    }

    PlayerData data = dataManager.get(player);
    if (data == null) {
      return;
    }

    EnumParticle particle = parseParticle(player, particleName);
    if (particle == null) {
      return;
    }

    PlayerSettings settings = manager.getPlayerSettings(player);

    if (particle == EnumParticle.REDSTONE) {
      handleRedstoneParticle(player, data, settings, particle, color);
      return;
    }

    settings.setParticle(particle);
    data.set("particles.effect", particle.toString());
    sendMessage(player, SET_PARTICLE, particle.name());
  }

  @CommandAlias("absetgoalcelebration|fcsetgoalcelebration|absgc|fcsgc")
  @Subcommand("setgoalcelebration|sgc")
  @CommandPermission(PERM_SET_GOAL_CELEBRATION)
  @Syntax("<default|minimal|simple|epic|list>")
  @CommandCompletion("default|minimal|simple|epic|list")
  @Description("Set goal celebration style")
  public void onSetGoalCelebration(Player player, String celebrationType) {
    if (celebrationType.equalsIgnoreCase("list")) {
      sendMessage(player, AVAILABLE_TYPE, "goal celebrations",
          "default, minimal, simple, epic");
      return;
    }

    PlayerData data = dataManager.get(player);
    if (data == null) {
      return;
    }

    String type = celebrationType.toLowerCase();
    if (!type.equals("default") && !type.equals("minimal") && !type.equals("simple")
        && !type.equals("epic")) {
      sendMessage(player, INVALID_TYPE, "goal celebrations");
      sendMessage(player, AVAILABLE_TYPE, "goal celebrations",
          "default, minimal, simple, epic");
      return;
    }

    PlayerSettings settings = manager.getPlayerSettings(player);
    settings.setGoalMessage(type);
    data.set("goalcelebration", type);
    sendMessage(player, SET_GOAL_CELEBRATION, type);
  }

  private void showAllowedSounds(Player player, List<Sound> sounds) {
    List<String> soundNames = new ArrayList<>();
    sounds.forEach(sound -> soundNames.add(sound.name()));
    sendMessage(player, AVAILABLE_TYPE, SOUND.toString(), joinStrings(soundNames));
  }

  private Sound parseSound(Player player, String soundName) {
    try {
      return Sound.valueOf(soundName.toUpperCase());
    } catch (Exception e) {
      sendMessage(player, INVALID_TYPE, SOUND.toString());
      return null;
    }
  }

  private EnumParticle parseParticle(Player player, String particleName) {
    try {
      EnumParticle particle = EnumParticle.valueOf(particleName.toUpperCase());

      if (PlayerSettings.DISALLOWED_PARTICLES.contains(particle)) {
        sendMessage(player, INVALID_TYPE, PARTICLE.toString());
        sendMessage(player, AVAILABLE_TYPE, PARTICLE.toString(),
            joinStrings(PlayerSettings.getAllowedParticles()));
        return null;
      }

      return particle;
    } catch (Exception e) {
      sendMessage(player, INVALID_TYPE, PARTICLE.toString());
      sendMessage(player, AVAILABLE_TYPE, PARTICLE.toString(),
          joinStrings(PlayerSettings.getAllowedParticles()));
      return null;
    }
  }

  private void handleRedstoneParticle(Player player, PlayerData playerData,
                                      PlayerSettings settings, EnumParticle particle, String color) {
    String colorName = color != null ? color.toUpperCase() : "WHITE";
    try {
      settings.setCustomRedstoneColor(colorName);
      playerData.set("particles.effect", "REDSTONE:" + colorName);
      sendMessage(player, SET_PARTICLE_REDSTONE, particle.name(), colorName);
    } catch (IllegalArgumentException e) {
      sendMessage(player, INVALID_COLOR, colorName);
      sendMessage(player, AVAILABLE_TYPE, COLOR.toString(),
          joinStrings(PlayerSettings.getAllowedColorNames()));
      return;
    }
    settings.setParticle(EnumParticle.REDSTONE);
  }
}