package io.github.divinerealms.footcube.commands.player;

import static io.github.divinerealms.footcube.configs.Lang.AVAILABLE_TYPE;
import static io.github.divinerealms.footcube.configs.Lang.COLOR;
import static io.github.divinerealms.footcube.configs.Lang.INVALID_COLOR;
import static io.github.divinerealms.footcube.configs.Lang.INVALID_TYPE;
import static io.github.divinerealms.footcube.configs.Lang.OFF;
import static io.github.divinerealms.footcube.configs.Lang.ON;
import static io.github.divinerealms.footcube.configs.Lang.PARTICLE;
import static io.github.divinerealms.footcube.configs.Lang.SET_GOAL_CELEBRATION;
import static io.github.divinerealms.footcube.configs.Lang.SET_PARTICLE;
import static io.github.divinerealms.footcube.configs.Lang.SET_PARTICLE_REDSTONE;
import static io.github.divinerealms.footcube.configs.Lang.SET_SOUND_GOAL;
import static io.github.divinerealms.footcube.configs.Lang.SET_SOUND_KICK;
import static io.github.divinerealms.footcube.configs.Lang.SOUND;
import static io.github.divinerealms.footcube.configs.Lang.TOGGLES_GOAL;
import static io.github.divinerealms.footcube.configs.Lang.TOGGLES_HIT_DEBUG;
import static io.github.divinerealms.footcube.configs.Lang.TOGGLES_KICK;
import static io.github.divinerealms.footcube.configs.Lang.TOGGLES_PARTICLES;
import static io.github.divinerealms.footcube.configs.Lang.USAGE;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_SET_GOAL_CELEBRATION;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_SET_PARTICLE;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_SET_SOUND;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.footcube.configs.PlayerData;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.managers.PlayerDataManager;
import io.github.divinerealms.footcube.physics.PhysicsData;
import io.github.divinerealms.footcube.utils.Logger;
import io.github.divinerealms.footcube.utils.PlayerSettings;
import java.util.List;
import java.util.StringJoiner;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@CommandAlias("fc|footcube")
public class SettingsCommands extends BaseCommand {

  private final FCManager fcManager;
  private final Logger logger;
  private final PlayerDataManager dataManager;
  private final PhysicsData physicsData;

  public SettingsCommands(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
    this.dataManager = fcManager.getDataManager();
    this.physicsData = fcManager.getPhysicsData();
  }

  @CommandAlias("fctoggle|fct")
  @Subcommand("toggle|tg")
  @Syntax("<kick|goal|particles|hits>")
  @CommandCompletion("kick|goal|particles|hits")
  @Description("Toggle FootCube Settings")
  public void onToggle(Player player, String toggleType) {
    PlayerData data = dataManager.get(player);
    if (data == null) {
      return;
    }

    String toggleTypeLower = toggleType.toLowerCase();
    PlayerSettings settings = fcManager.getPlayerSettings(player);

    switch (toggleTypeLower) {
      case "kick":
        settings.setKickSoundEnabled(!settings.isKickSoundEnabled());
        data.set("sounds.kick.enabled", settings.isKickSoundEnabled());
        logger.send(player, TOGGLES_KICK,
            settings.isKickSoundEnabled() ? ON.toString() : OFF.toString());
        break;

      case "goal":
        settings.setGoalSoundEnabled(!settings.isGoalSoundEnabled());
        data.set("sounds.goal.enabled", settings.isGoalSoundEnabled());
        logger.send(player, TOGGLES_GOAL,
            settings.isGoalSoundEnabled() ? ON.toString() : OFF.toString());
        break;

      case "particles":
        settings.setParticlesEnabled(!settings.isParticlesEnabled());
        data.set("particles.enabled", settings.isParticlesEnabled());
        logger.send(player, TOGGLES_PARTICLES,
            settings.isParticlesEnabled() ? ON.toString() : OFF.toString());
        break;

      case "hits":
        boolean status = physicsData.getCubeHits().contains(player.getUniqueId());
        if (status) {
          physicsData.getCubeHits().remove(player.getUniqueId());
        } else {
          physicsData.getCubeHits().add(player.getUniqueId());
        }
        logger.send(player, TOGGLES_HIT_DEBUG, !status ? ON.toString() : OFF.toString());
        break;

      default:
        logger.send(player, USAGE, getExecSubcommand());
        break;
    }
  }

  @CommandAlias("fcsetsound|fcss")
  @Subcommand("setsound")
  @CommandPermission(PERM_SET_SOUND)
  @Syntax("<kick|goal> <soundName|list>")
  @CommandCompletion("kick|goal list")
  @Description("Set sounds")
  public void onSetSound(Player player, String soundType, String soundName) {
    String soundTypeLower = soundType.toLowerCase();

    if (!soundTypeLower.equals("kick") && !soundTypeLower.equals("goal")) {
      logger.send(player, USAGE, getExecSubcommand());
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
      logger.send(player, INVALID_TYPE, SOUND.toString());
      showAllowedSounds(player, allowedSounds);
      return;
    }

    PlayerSettings settings = fcManager.getPlayerSettings(player);

    if (soundTypeLower.equals("kick")) {
      settings.setKickSound(sound);
      data.set("sounds.kick.sound", sound.toString());
      logger.send(player, SET_SOUND_KICK, sound.name());
    } else {
      settings.setGoalSound(sound);
      data.set("sounds.goal.sound", sound.toString());
      logger.send(player, SET_SOUND_GOAL, sound.name());
    }
  }

  @CommandAlias("fcsetparticle|fcsp")
  @Subcommand("setparticle")
  @CommandPermission(PERM_SET_PARTICLE)
  @Syntax("<particleName|list> [color]")
  @CommandCompletion("list|@particles @colors")
  @Description("Set particle trail effect for cubes")
  public void onSetParticle(Player player, String particleName, @Optional String color) {
    if (particleName.equalsIgnoreCase("list")) {
      logger.send(player, AVAILABLE_TYPE, PARTICLE.toString(),
          String.join(", ", PlayerSettings.getAllowedParticles()));
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

    PlayerSettings settings = fcManager.getPlayerSettings(player);

    if (particle == EnumParticle.REDSTONE) {
      handleRedstoneParticle(player, data, settings, particle, color);
      return;
    }

    settings.setParticle(particle);
    data.set("particles.effect", particle.toString());
    logger.send(player, SET_PARTICLE, particle.name());
  }

  @CommandAlias("fcsetgoalcelebration|fcsgc")
  @Subcommand("setgoalcelebration|sgc")
  @CommandPermission(PERM_SET_GOAL_CELEBRATION)
  @Syntax("<default|minimal|simple|epic|list>")
  @CommandCompletion("default|minimal|simple|epic|list")
  @Description("Set goal celebration style")
  public void onSetGoalCelebration(Player player, String celebrationType) {
    if (celebrationType.equalsIgnoreCase("list")) {
      logger.send(player, AVAILABLE_TYPE, "goal celebrations", "default, minimal, simple, epic");
      return;
    }

    PlayerData data = dataManager.get(player);
    if (data == null) {
      return;
    }

    String type = celebrationType.toLowerCase();
    if (!type.equals("default") && !type.equals("minimal") && !type.equals("simple")
        && !type.equals("epic")) {
      logger.send(player, INVALID_TYPE, "goal celebrations");
      logger.send(player, AVAILABLE_TYPE, "goal celebrations", "default, minimal, simple, epic");
      return;
    }

    PlayerSettings settings = fcManager.getPlayerSettings(player);
    settings.setGoalMessage(type);
    data.set("goalcelebration", type);
    logger.send(player, SET_GOAL_CELEBRATION, type);
  }

  private void showAllowedSounds(Player player, List<Sound> sounds) {
    StringJoiner joiner = new StringJoiner(", ");
    for (Sound s : sounds) {
      joiner.add(s.name());
    }
    logger.send(player, AVAILABLE_TYPE, SOUND.toString(), joiner.toString());
  }

  private Sound parseSound(Player player, String soundName) {
    try {
      return Sound.valueOf(soundName.toUpperCase());
    } catch (Exception e) {
      logger.send(player, INVALID_TYPE, SOUND.toString());
      return null;
    }
  }

  private EnumParticle parseParticle(Player player, String particleName) {
    try {
      EnumParticle particle = EnumParticle.valueOf(particleName.toUpperCase());

      if (PlayerSettings.DISALLOWED_PARTICLES.contains(particle)) {
        logger.send(player, INVALID_TYPE, PARTICLE.toString());
        logger.send(player, AVAILABLE_TYPE, PARTICLE.toString(),
            String.join(", ", PlayerSettings.getAllowedParticles()));
        return null;
      }

      return particle;
    } catch (Exception e) {
      logger.send(player, INVALID_TYPE, PARTICLE.toString());
      logger.send(player, AVAILABLE_TYPE, PARTICLE.toString(),
          String.join(", ", PlayerSettings.getAllowedParticles()));
      return null;
    }
  }

  private void handleRedstoneParticle(Player player, PlayerData playerData,
      PlayerSettings settings, EnumParticle particle, String color) {
    String colorName = color != null ? color.toUpperCase() : "WHITE";
    try {
      settings.setCustomRedstoneColor(colorName);
      playerData.set("particles.effect", "REDSTONE:" + colorName);
      logger.send(player, SET_PARTICLE_REDSTONE, particle.name(), colorName);
    } catch (IllegalArgumentException e) {
      logger.send(player, INVALID_COLOR, colorName);
      logger.send(player, AVAILABLE_TYPE, COLOR.toString(),
          String.join(", ", PlayerSettings.getAllowedColorNames()));
      return;
    }
    settings.setParticle(EnumParticle.REDSTONE);
  }
}