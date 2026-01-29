package io.github.divinerealms.footcube.commands;

import static io.github.divinerealms.footcube.configs.Lang.INGAME_ONLY;
import static io.github.divinerealms.footcube.matchmaking.util.MatchUtils.joinStrings;
import static io.github.divinerealms.footcube.utils.GameCommandsHelper.joinQueue;
import static io.github.divinerealms.footcube.utils.Permissions.PERM_PLAY;

import io.github.divinerealms.footcube.configs.Settings;
import io.github.divinerealms.footcube.core.FCManager;
import io.github.divinerealms.footcube.utils.Logger;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;

public class DynamicCommandRegistry {

  private final FCManager fcManager;
  private final Logger logger;
  private final Map<String, DynamicMatchCommand> registeredCommands = new HashMap<>();
  private CommandMap commandMap;

  public DynamicCommandRegistry(FCManager fcManager) {
    this.fcManager = fcManager;
    this.logger = fcManager.getLogger();
    initializeCommandMap();
  }

  private void initializeCommandMap() {
    try {
      Server server = fcManager.getPlugin().getServer();
      Field field = server.getClass().getDeclaredField("commandMap");
      field.setAccessible(true);
      commandMap = (CommandMap) field.get(server);
    } catch (Exception exception) {
      Bukkit.getLogger()
          .log(Level.SEVERE, "Failed to initialize command map for dynamic commands", exception);
    }
  }

  public void registerAllMatchTypeCommands() {
    if (commandMap == null) {
      fcManager.getLogger().info("&cCannot register dynamic commands - CommandMap is null");
    }

    List<Integer> enabledTypes = Settings.getEnabledMatchTypes();
    List<String> registered = new ArrayList<>();

    for (int type : enabledTypes) {
      String commandName = Settings.getMatchTypeName(type);

      if (registerMatchTypeCommand(type, commandName)) {
        registered.add(commandName);
      }
    }

    if (!registered.isEmpty()) {
      logger.info("&a✔ &2Registered dynamic match commands: &e" + joinStrings(registered));
    }
  }

  private boolean registerMatchTypeCommand(int type, String commandName) {
    try {
      DynamicMatchCommand command = new DynamicMatchCommand(commandName, type, fcManager);
      commandMap.register(fcManager.getPlugin().getName().toLowerCase(), command);
      registeredCommands.put(commandName, command);
      return true;
    } catch (Exception exception) {
      Bukkit.getLogger()
          .log(Level.WARNING, "Failed to register command: /" + commandName, exception);
      return false;
    }
  }

  public void unregisterAllMatchTypeCommands() {
    if (commandMap == null) {
      return;
    }

    try {
      Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, Command> knownCommands = (Map<String, Command>) field.get(commandMap);

      List<String> removed = new ArrayList<>();
      for (String commandName : new ArrayList<>(registeredCommands.keySet())) {
        knownCommands.remove(commandName);
        knownCommands.remove("footcube:" + commandName);
        removed.add(commandName);
      }

      registeredCommands.clear();

      if (!removed.isEmpty()) {
        logger.info("&e⟳ &6Unregistered dynamic commands: &f" + joinStrings(removed));
      }
    } catch (Exception exception) {
      Bukkit.getLogger().log(Level.WARNING, "Failed to unregister dynamic commands", exception);
    }
  }

  public void reloadCommands() {
    unregisterAllMatchTypeCommands();
    registerAllMatchTypeCommands();
  }

  private static class DynamicMatchCommand extends Command {

    private final int matchType;
    private final FCManager fcManager;
    private final Logger logger;

    public DynamicMatchCommand(String name, int matchType, FCManager fcManager) {
      super(name);
      this.matchType = matchType;
      this.fcManager = fcManager;
      this.logger = fcManager.getLogger();

      setDescription("Join " + name + " matchmaking queue");
      setUsage("/" + name);
      setPermission(PERM_PLAY);
      setPermissionMessage("&cNedovoljno dozvola.");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
      if (!(sender instanceof Player)) {
        logger.send(sender, INGAME_ONLY);
        return true;
      }

      Player player = (Player) sender;
      if (!player.hasPermission(PERM_PLAY)) {
        logger.send(player, getPermissionMessage());
        return true;
      }

      joinQueue(player, matchType, fcManager);
      return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
      return new ArrayList<>();
    }
  }
}
