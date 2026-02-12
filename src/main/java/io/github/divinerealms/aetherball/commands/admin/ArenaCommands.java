package io.github.divinerealms.aetherball.commands.admin;

import static io.github.divinerealms.aetherball.configs.Lang.CLEAR_ARENAS_SUCCESS;
import static io.github.divinerealms.aetherball.configs.Lang.CLEAR_ARENAS_TYPE_SUCCESS;
import static io.github.divinerealms.aetherball.configs.Lang.MATCH_TYPE_UNAVAILABLE;
import static io.github.divinerealms.aetherball.configs.Lang.PRACTICE_AREA_SET;
import static io.github.divinerealms.aetherball.configs.Lang.SETUP_ARENA_FIRST_SET;
import static io.github.divinerealms.aetherball.configs.Lang.SETUP_ARENA_START;
import static io.github.divinerealms.aetherball.configs.Lang.SETUP_ARENA_SUCCESS;
import static io.github.divinerealms.aetherball.configs.Lang.SET_BLOCK_SUCCESS;
import static io.github.divinerealms.aetherball.configs.Lang.SET_BLOCK_TOO_FAR;
import static io.github.divinerealms.aetherball.configs.Lang.UNDO;
import static io.github.divinerealms.aetherball.configs.Lang.USAGE;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.parseMatchType;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_ADMIN;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_CLEAR_ARENAS;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_SETBUTON;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_SETUP_ARENA;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_SET_LOBBY;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_SET_PRACTICE_AREA;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.managers.ConfigManager;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.matchmaking.arena.ArenaManager;
import java.util.Map;
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

  private final ConfigManager configManager;
  private final ArenaManager arenaManager;
  private final MatchManager matchManager;

  private final FileConfiguration config;
  private final FileConfiguration practice;

  public ArenaCommands(Manager manager) {
    this.configManager = manager.getConfigManager();
    this.arenaManager = manager.getArenaManager();
    this.matchManager = manager.getMatchManager();

    this.config = configManager.getConfig("config.yml");
    this.practice = configManager.getConfig("practice.yml");
  }

  @Subcommand("create|setup")
  @CommandPermission(PERM_SETUP_ARENA)
  @CommandCompletion("@allmatchtypes")
  @Syntax("<match_type>")
  @Description("Start arena setup wizard")
  public void onSetupArena(Player player, String type) {
    Integer arenaType = parseMatchType(type);
    if (arenaType == null || !Settings.isMatchTypeEnabled(arenaType)) {
      String availableTypes = matchManager.getAvailableTypesString();
      sendMessage(player, MATCH_TYPE_UNAVAILABLE, type, availableTypes);
      return;
    }

    arenaManager.getSetupWizards()
        .put(player, new ArenaManager.ArenaSetup(arenaType));
    sendMessage(player, SETUP_ARENA_START);
  }

  @Subcommand("list")
  @CommandPermission(PERM_ADMIN)
  @Description("Show arena statistics")
  public void onArenasInfo(CommandSender sender) {
    int total = arenaManager.getArenas().size();
    Map<Integer, Settings.MatchTypeConfig> allTypes = Settings.getAllMatchTypeConfigs();

    sendMessage(sender, "{prefix_info}Arena Statistics:");

    for (Map.Entry<Integer, Settings.MatchTypeConfig> entry : allTypes.entrySet()) {
      int type = entry.getKey();
      String typeName = Settings.getMatchTypeName(type);
      int count = arenaManager.getArenaCountType(type);
      String status = entry.getValue().isEnabled() ? "&a✔" : "&c✘";

      sendMessage(sender, "{prefix_info}" + status + " &e" + typeName + " Arenas: &f" + count);
    }

    sendMessage(sender, "{prefix_info}&eTotal Arenas: &f" + total);

    if (total == 0) {
      sendMessage(sender, "{prefix_error}No arenas configured!");
    }
  }

  @Subcommand("set")
  @CommandPermission(PERM_SETUP_ARENA)
  @Description("Set arena spawn point (use twice)")
  public void onSet(Player player) {
    ArenaManager.ArenaSetup setup = arenaManager.getSetupWizards().get(player);
    if (setup == null) {
      sendMessage(player, "{prefix_error}You are not setting up an arena.");
      return;
    }

    if (setup.getBlueSpawn() == null) {
      setup.setBlueSpawn(player.getLocation());
      sendMessage(player, SETUP_ARENA_FIRST_SET);
    } else {
      arenaManager.createArena(setup.getType(), setup.getBlueSpawn(), player.getLocation());
      arenaManager.getSetupWizards().remove(player);
      sendMessage(player, SETUP_ARENA_SUCCESS);
    }
  }

  @Subcommand("undo")
  @CommandPermission(PERM_SETUP_ARENA)
  @Description("Cancel arena setup")
  public void onUndo(Player player) {
    if (arenaManager.getSetupWizards().remove(player) != null) {
      sendMessage(player, UNDO);
    } else {
      sendMessage(player, "{prefix_error}You are not setting up an arena.");
    }
  }

  @Subcommand("clear")
  @CommandPermission(PERM_CLEAR_ARENAS)
  @CommandCompletion("@allmatchtypes")
  @Syntax("[match_type]")
  @Description("Clear arenas (optionally by type)")
  public void onClearArenas(Player player, @Optional String type) {
    if (type == null) {
      arenaManager.clearArenas();
      sendMessage(player, CLEAR_ARENAS_SUCCESS);
      return;
    }

    Integer arenaType = parseMatchType(type);
    if (arenaType == null) {
      String availableTypes = matchManager.getAvailableTypesString();
      sendMessage(player, MATCH_TYPE_UNAVAILABLE, availableTypes);
      return;
    }

    arenaManager.clearArenaType(arenaType);
    sendMessage(player, CLEAR_ARENAS_TYPE_SUCCESS, type);
  }

  @Subcommand("setlobby|sl")
  @CommandPermission(PERM_SET_LOBBY)
  @Description("Set lobby spawn location")
  public void onSetLobby(Player player) {
    config.set("lobby", player.getLocation());
    configManager.saveConfig("config.yml");
    sendMessage(player, PRACTICE_AREA_SET, "lobby",
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
    sendMessage(player, PRACTICE_AREA_SET, name,
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
      sendMessage(player, SET_BLOCK_TOO_FAR);
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
        sendMessage(player, USAGE, "fca setbutton <spawn|clearcube>");
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

    sendMessage(player, SET_BLOCK_SUCCESS, buttonType);
  }
}