package io.github.divinerealms.aetherball.managers;

import co.aikar.commands.*;
import io.github.divinerealms.aetherball.commands.CubeCommands;
import io.github.divinerealms.aetherball.commands.MainCommand;
import io.github.divinerealms.aetherball.commands.MatchMan;
import io.github.divinerealms.aetherball.commands.admin.*;
import io.github.divinerealms.aetherball.commands.player.*;
import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.utils.PlayerSettings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginIdentifiableCommand;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.divinerealms.aetherball.configs.Lang.*;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

public class CommandRegistry {
  private final Manager manager;
  private PaperCommandManager acf;

  public CommandRegistry(Manager manager) {
    this.manager = manager;
  }

  public void register() {
    unregister();

    acf = new PaperCommandManager(manager.getPlugin());
    configureMessages();
    registerCompletions();
    registerPlayerCommands();
    registerAdminCommands();
    manager.getDynamicCommandRegistry().reloadCommands();

    debugConsole("{prefix_success}Registered commands via ACF successfully.");
  }

  public void unregister() {
    if (acf == null) {
      return;
    }

    try {
      acf.unregisterCommands();
      clearBukkitCommands();
      acf = null;
      debugConsole("{prefix_success}Unregistered ACF commands.");
    } catch (Exception exception) {
      logConsole("{prefix_error}Failed to unregister commands: ", exception.getMessage());
    }
  }

  private void configureMessages() {
    acf.getLocales().addMessage(Locale.ENGLISH, MessageKeys.PERMISSION_DENIED, NO_PERM.toString());
    acf.getLocales().addMessage(Locale.ENGLISH, MessageKeys.PERMISSION_DENIED_PARAMETER, NO_PERM_PARAMETERS.toString());
    acf.getLocales().addMessage(Locale.ENGLISH, MessageKeys.INVALID_SYNTAX, HELP_USAGE.toString());
    acf.getLocales().addMessage(Locale.ENGLISH, MessageKeys.COULD_NOT_FIND_PLAYER, PLAYER_NOT_FOUND.toString());
    acf.getLocales().addMessage(Locale.ENGLISH, MessageKeys.NOT_ALLOWED_ON_CONSOLE, INGAME_ONLY.toString());
    acf.getLocales().addMessage(Locale.ENGLISH, MessageKeys.UNKNOWN_COMMAND, UNKNOWN_COMMAND.toString());
  }

  private void registerCompletions() {
    CommandCompletions<BukkitCommandCompletionContext> context = acf.getCommandCompletions();
    context.registerStaticCompletion("particles", PlayerSettings.getAllowedParticles());
    context.registerStaticCompletion("colors", PlayerSettings.getAllowedColorNames());
    context.registerCompletion("matchtypes", ctx -> Settings.getEnabledMatchTypes().stream().map(Settings::getMatchTypeName).collect(Collectors.toList()));
    context.registerCompletion("allmatchtypes", ctx -> Settings.getAllMatchTypeConfigs().keySet().stream().map(Settings::getMatchTypeName).collect(Collectors.toList()));
  }

  private void registerPlayerCommands() {
    acf.registerCommand(new MainCommand(manager));
    acf.registerCommand(new GameCommands(manager));
    acf.registerCommand(new TeamCommands(manager));
    acf.registerCommand(new CubeCommands(manager));
    acf.registerCommand(new SettingsCommands(manager));
    acf.registerCommand(new BuildCommand(manager));
    acf.registerCommand(new MatchesCommand(manager));
  }

  private void registerAdminCommands() {
    acf.registerCommand(new BaseAdmin(manager));
    acf.registerCommand(new SystemCommands(manager));
    acf.registerCommand(new BanCommands(manager));
    acf.registerCommand(new ArenaCommands(manager));
    acf.registerCommand(new PlayerCommands(manager));
    acf.registerCommand(new MatchMan(manager));
    acf.registerCommand(new DebugCommands(manager));
  }

  private void clearBukkitCommands() throws Exception {
    Field field = manager.getPlugin().getServer().getClass().getDeclaredField("commandMap");
    field.setAccessible(true);
    CommandMap commandMap = (CommandMap) field.get(manager.getPlugin().getServer());
    Field knownField = commandMap.getClass().getDeclaredField("knownCommands");
    knownField.setAccessible(true);
    //noinspection unchecked
    Map<String, Command> known = (Map<String, Command>) knownField.get(commandMap);
    known.entrySet().removeIf(entry -> {
      Command command = entry.getValue();
      if (command instanceof PluginIdentifiableCommand pic) {
        return pic.getPlugin().equals(manager.getPlugin());
      }
      return false;
    });
  }
}
