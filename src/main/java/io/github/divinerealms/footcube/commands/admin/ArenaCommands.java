package io.github.divinerealms.footcube.commands.admin;

import static io.github.divinerealms.footcube.configs.Lang.CLEAR_ARENAS_SUCCESS;
import static io.github.divinerealms.footcube.configs.Lang.CLEAR_ARENAS_TYPE_SUCCESS;
import static io.github.divinerealms.footcube.configs.Lang.PRACTICE_AREA_SET;
import static io.github.divinerealms.footcube.configs.Lang.PREFIX_ADMIN;
import static io.github.divinerealms.footcube.configs.Lang.SETUP_ARENA_FIRST_SET;
import static io.github.divinerealms.footcube.configs.Lang.SETUP_ARENA_START;
import static io.github.divinerealms.footcube.configs.Lang.SETUP_ARENA_SUCCESS;
import static io.github.divinerealms.footcube.configs.Lang.SET_BLOCK_SUCCESS;
import static io.github.divinerealms.footcube.configs.Lang.SET_BLOCK_TOO_FAR;
import static io.github.divinerealms.footcube.configs.Lang.UNDO;
import static io.github.divinerealms.footcube.configs.Lang.USAGE;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_ADMIN;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_CLEAR_ARENAS;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_SETBUTON;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_SETUP_ARENA;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_SET_LOBBY;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_SET_PRACTICE_AREA;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.managers.ConfigManager;
import io.github.divinerealms.footcube.matchmaking.arena.ArenaManager;
import io.github.divinerealms.footcube.utils.Logger;
import java.util.Set;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.material.Button;
import org.bukkit.material.Wool;

@CommandAlias("arena")
public class ArenaCommands extends BaseCommand {

  private final Logger logger;
  private final ArenaManager arenaManager;
  private final ConfigManager configManager;
  private final FileConfiguration config;
  private final FileConfiguration practice;

  public ArenaCommands(FCManager fcManager) {
    this.logger = fcManager.getLogger();
    this.arenaManager = fcManager.getArenaManager();
    this.configManager = fcManager.getConfigManager();
    this.config = configManager.getConfig("config.yml");
    this.practice = configManager.getConfig("practice.yml");
  }

  @Subcommand("create|setup")
  @CommandPermission(PERM_SETUP_ARENA)
  @Syntax("<1v1|2v2|3v3|4v4|5v5>")
  @CommandCompletion("1v1|2v2|3v3|4v4|5v5")
  @Description("Start arena setup wizard")
  public void onSetupArena(Player player, String type) {
    int arenaType;
    switch (type.toLowerCase()) {
      case "1v1":
        arenaType = 1;
        break;
      case "2v2":
        arenaType = 2;
        break;
      case "3v3":
        arenaType = 3;
        break;
      case "4v4":
        arenaType = 4;
        break;
      case "5v5":
        arenaType = 5;
        break;
      default:
        logger.send(player, "&cInvalid type. Use 1v1, 2v2, 3v3, 4v4 or 5v5.");
        return;
    }

    arenaManager.getSetupWizards().put(player, new ArenaManager.ArenaSetup(arenaType));
    logger.send(player, SETUP_ARENA_START);
  }

  @Subcommand("list")
  @CommandPermission(PERM_ADMIN)
  @Description("Show arena statistics")
  public void onArenasInfo(CommandSender sender) {
    int total = arenaManager.getArenas().size();
    int oneVOne = arenaManager.getArenaCountType(1);
    int twoVTwo = arenaManager.getArenaCountType(2);
    int threeVThree = arenaManager.getArenaCountType(3);
    int fourVFour = arenaManager.getArenaCountType(4);
    int fiveVFive = arenaManager.getArenaCountType(5);

    logger.send(sender, "{prefix-admin}&6Arena Statistics:");
    logger.send(sender, "&e1v1 Arenas: &f" + oneVOne);
    logger.send(sender, "&e2v2 Arenas: &f" + twoVTwo);
    logger.send(sender, "&e3v3 Arenas: &f" + threeVThree);
    logger.send(sender, "&e4v4 Arenas: &f" + fourVFour);
    logger.send(sender, "&e5v5 Arenas: &f" + fiveVFive);
    logger.send(sender, "&eTotal Arenas: &f" + total);

    if (total == 0) {
      logger.send(sender, "{prefix-admin}&c⚠ WARNING: No arenas configured! Players cannot play.");
    }
  }

  @Subcommand("set")
  @CommandPermission(PERM_SETUP_ARENA)
  @Description("Set arena spawn point (use twice)")
  public void onSet(Player player) {
    ArenaManager.ArenaSetup setup = arenaManager.getSetupWizards().get(player);
    if (setup == null) {
      logger.send(player, PREFIX_ADMIN + "You are not setting up an arena.");
      return;
    }

    if (setup.getBlueSpawn() == null) {
      setup.setBlueSpawn(player.getLocation());
      logger.send(player, SETUP_ARENA_FIRST_SET);
    } else {
      arenaManager.createArena(setup.getType(), setup.getBlueSpawn(), player.getLocation());
      arenaManager.getSetupWizards().remove(player);
      logger.send(player, SETUP_ARENA_SUCCESS);
    }
  }

  @Subcommand("undo")
  @CommandPermission(PERM_SETUP_ARENA)
  @Description("Cancel arena setup")
  public void onUndo(Player player) {
    if (arenaManager.getSetupWizards().remove(player) != null) {
      logger.send(player, UNDO);
    } else {
      logger.send(player, PREFIX_ADMIN + "You are not setting up an arena.");
    }
  }

  @Subcommand("clear")
  @CommandPermission(PERM_CLEAR_ARENAS)
  @CommandCompletion("1v1|2v2|3v3|4v4|5v5")
  @Syntax("[1v1|2v2|3v3|4v4|5v5]")
  @Description("Clear arenas (optionally by type)")
  public void onClearArenas(Player player, @Optional String type) {
    if (type == null) {
      arenaManager.clearArenas();
      logger.send(player, CLEAR_ARENAS_SUCCESS);
      return;
    }

    int arenaType;
    switch (type.toLowerCase()) {
      case "1v1":
        arenaType = 1;
        break;
      case "2v2":
        arenaType = 2;
        break;
      case "3v3":
        arenaType = 3;
        break;
      case "4v4":
        arenaType = 4;
        break;
      case "5v5":
        arenaType = 5;
        break;
      default:
        logger.send(player, "&cInvalid type. Use 1v1, 2v2, 3v3, 4v4, or 5v5.");
        return;
    }

    arenaManager.clearArenaType(arenaType);
    logger.send(player, CLEAR_ARENAS_TYPE_SUCCESS, type);
  }

  @Subcommand("setlobby|sl")
  @CommandPermission(PERM_SET_LOBBY)
  @Description("Set lobby spawn location")
  public void onSetLobby(Player player) {
    config.set("lobby", player.getLocation());
    configManager.saveConfig("config.yml");
    logger.send(player, PRACTICE_AREA_SET, "lobby",
        String.valueOf(player.getLocation().getX()),
        String.valueOf(player.getLocation().getY()),
        String.valueOf(player.getLocation().getZ())
    );
  }

  @Subcommand("setpracticearea|spa")
  @CommandPermission(PERM_SET_PRACTICE_AREA)
  @Syntax("<name>")
  @Description("Set practice area location")
  public void onSetPracticeArea(Player player, String name) {
    practice.set("practice-areas." + name, player.getLocation());
    configManager.saveConfig("practice.yml");
    logger.send(player, PRACTICE_AREA_SET, name,
        String.valueOf(player.getLocation().getX()),
        String.valueOf(player.getLocation().getY()),
        String.valueOf(player.getLocation().getZ())
    );
  }

  @Subcommand("setbutton|sb")
  @CommandPermission(PERM_SETBUTON)
  @Syntax("<spawn|clearcube>")
  @CommandCompletion("spawn|clearcube")
  @Description("Set button for cube spawn or clear")
  public void onSetButton(Player player, String buttonType) {
    Block targetBlock = player.getTargetBlock((Set<Material>) null, 5);
    if (targetBlock == null || targetBlock.getType() == Material.AIR) {
      logger.send(player, SET_BLOCK_TOO_FAR);
      return;
    }

    targetBlock.setType(Material.WOOL);
    BlockState targetBlockState = targetBlock.getState();

    switch (buttonType.toLowerCase()) {
      case "spawn":
        targetBlockState.setData(new Wool(DyeColor.LIME));
        break;
      case "clearcube":
        targetBlockState.setData(new Wool(DyeColor.RED));
        break;
      default:
        logger.send(player, USAGE, "fca setbutton <spawn|clearcube>");

        return;
    }

    targetBlockState.update(true);

    Block aboveTargetBlock = targetBlock.getRelative(BlockFace.UP);
    aboveTargetBlock.setType(Material.STONE_BUTTON);
    BlockState aboveTargetBlockState = aboveTargetBlock.getState();
    Button buttonData = new Button();
    buttonData.setFacingDirection(BlockFace.UP);
    aboveTargetBlockState.setData(buttonData);
    aboveTargetBlockState.update(true);

    logger.send(player, SET_BLOCK_SUCCESS, buttonType);
  }
}