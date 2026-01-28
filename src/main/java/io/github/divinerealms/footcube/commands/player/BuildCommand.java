package io.github.divinerealms.footcube.commands.player;

import static io.github.divinerealms.footcube.configs.Lang.COMMAND_DISABLER_CANT_USE;
import static io.github.divinerealms.footcube.configs.Lang.NO_PERM;
import static io.github.divinerealms.footcube.configs.Lang.NO_PERM_PARAMETERS;
import static io.github.divinerealms.footcube.configs.Lang.OFF;
import static io.github.divinerealms.footcube.configs.Lang.ON;
import static io.github.divinerealms.footcube.configs.Lang.SET_BUILD_MODE;
import static io.github.divinerealms.footcube.configs.Lang.SET_BUILD_MODE_OTHER;
import static io.github.divinerealms.footcube.configs.Lang.TEAM_ALREADY_IN_GAME;
import static io.github.divinerealms.footcube.matchmaking.util.MatchUtils.shouldPreventAbuse;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_BUILD;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_BUILD_OTHER;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.matchmaking.Match;
import io.github.divinerealms.footcube.matchmaking.MatchManager;
import io.github.divinerealms.footcube.utils.Logger;
import io.github.divinerealms.footcube.utils.PlayerSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("build")
@Description("Toggle build mode")
public class BuildCommand extends BaseCommand {

  private final FCManager fcManager;
  private final Logger logger;
  private final MatchManager matchManager;

  public BuildCommand(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
    this.matchManager = fcManager.getMatchManager();
  }

  @Default
  @CommandCompletion("@players")
  @Syntax("[player]")
  @Description("Toggle build mode for yourself or another player")
  public void onBuild(CommandSender sender, @Optional @Flags("other") Player target) {
    if (target == null) {
      if (!(sender instanceof Player)) {
        logger.send(sender, "&cYou must specify a player from console.");
        return;
      }

      if (!sender.hasPermission(PERM_BUILD)) {
        logger.send(sender, NO_PERM);
        return;
      }

      Player player = (Player) sender;
      java.util.Optional<Match> matchOpt = matchManager.getMatch(player);
      if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
        logger.send(player, COMMAND_DISABLER_CANT_USE);
        return;
      }

      PlayerSettings settings = fcManager.getPlayerSettings(player);
      settings.toggleBuild();
      logger.send(player, SET_BUILD_MODE, settings.isBuildEnabled()
          ? ON.toString()
          : OFF.toString());
      return;
    }

    if (!sender.hasPermission(PERM_BUILD_OTHER)) {
      logger.send(sender, NO_PERM_PARAMETERS, getExecCommandLabel() + " [other]");
      return;
    }

    if (matchManager.getMatch(target).isPresent()) {
      logger.send(sender, TEAM_ALREADY_IN_GAME, target.getDisplayName());
      return;
    }

    PlayerSettings settings = fcManager.getPlayerSettings(target);
    settings.toggleBuild();
    String status = settings.isBuildEnabled()
        ? ON.toString()
        : OFF.toString();

    logger.send(target, SET_BUILD_MODE, status);
    logger.send(sender, SET_BUILD_MODE_OTHER, target.getDisplayName(), status);
  }
}