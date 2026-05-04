package io.github.divinerealms.aetherball.commands.player;

import static io.github.divinerealms.aetherball.configs.Lang.COMMAND_DISABLER_CANT_USE;
import static io.github.divinerealms.aetherball.configs.Lang.NO_PERM;
import static io.github.divinerealms.aetherball.configs.Lang.NO_PERM_PARAMETERS;
import static io.github.divinerealms.aetherball.configs.Lang.OFF;
import static io.github.divinerealms.aetherball.configs.Lang.ON;
import static io.github.divinerealms.aetherball.configs.Lang.SET_BUILD_MODE;
import static io.github.divinerealms.aetherball.configs.Lang.SET_BUILD_MODE_OTHER;
import static io.github.divinerealms.aetherball.configs.Lang.TEAM_ALREADY_IN_GAME;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;
import static io.github.divinerealms.aetherball.utils.MatchUtils.shouldPreventAbuse;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_BUILD;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_BUILD_OTHER;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import io.github.divinerealms.aetherball.managers.Manager;
import io.github.divinerealms.aetherball.matchmaking.Match;
import io.github.divinerealms.aetherball.matchmaking.MatchManager;
import io.github.divinerealms.aetherball.utils.PlayerSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("build")
@Description("Toggle build mode")
public class BuildCommand extends BaseCommand {

  private final Manager manager;
  private final MatchManager matchManager;

  public BuildCommand(Manager manager) {
    this.manager = manager;
    this.matchManager = manager.getMatchManager();
  }

  @Default
  @CommandCompletion("@players")
  @Syntax("[player]")
  @Description("Toggle build mode for yourself or another player")
  public void onBuild(CommandSender sender, @Optional @Flags("other") Player target) {
    if (target == null) {
      if (!(sender instanceof Player player)) {
        sendMessage(sender, "{prefix_error}You must specify a player from console.");
        return;
      }

      if (!sender.hasPermission(PERM_BUILD)) {
        sendMessage(sender, NO_PERM);
        return;
      }

      java.util.Optional<Match> matchOpt = matchManager.getMatch(player);
      if (matchOpt.isPresent() && shouldPreventAbuse(matchOpt.get().getPhase())) {
        sendMessage(player, COMMAND_DISABLER_CANT_USE);
        return;
      }

      PlayerSettings settings = manager.getPlayerSettings(player);
      settings.toggleBuild();
      sendMessage(
          player, SET_BUILD_MODE, settings.isBuildEnabled() ? ON.toString() : OFF.toString());
      return;
    }

    if (!sender.hasPermission(PERM_BUILD_OTHER)) {
      sendMessage(sender, NO_PERM_PARAMETERS, getExecCommandLabel() + " [other]");
      return;
    }

    if (matchManager.getMatch(target).isPresent()) {
      sendMessage(sender, TEAM_ALREADY_IN_GAME, target.getDisplayName());
      return;
    }

    PlayerSettings settings = manager.getPlayerSettings(target);
    settings.toggleBuild();
    String status = settings.isBuildEnabled() ? ON.toString() : OFF.toString();

    sendMessage(target, SET_BUILD_MODE, status);
    sendMessage(sender, SET_BUILD_MODE_OTHER, target.getDisplayName(), status);
  }
}
