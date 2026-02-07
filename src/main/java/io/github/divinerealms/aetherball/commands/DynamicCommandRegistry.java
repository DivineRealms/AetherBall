package io.github.divinerealms.aetherball.commands;

import static io.github.divinerealms.aetherball.configs.Lang.INGAME_ONLY;
import static io.github.divinerealms.aetherball.matchmaking.util.MatchUtils.joinStrings;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.joinQueue;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_PLAY;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.utils.Logger;
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

  private final Manager manager;
  private final Logger logger;
  private final Map<String, DynamicMatchCommand> registeredCommands = new HashMap<>();
  private CommandMap commandMap;

  public DynamicCommandRegistry(Manager manager) {
    this.manager = manager;
    this.logger = manager.getLogger();
    initializeCommandMap();
  }

  private void initializeCommandMap() {
    try {
      Server server = manager.getPlugin().getServer();
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
      manager.getLogger().info("&cCannot register dynamic commands - CommandMap is null");
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
      DynamicMatchCommand command = new DynamicMatchCommand(commandName, type, manager);
      commandMap.register(manager.getPlugin().getName().toLowerCase(), command);
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
        knownCommands.remove("aetherball:" + commandName);
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
    private final Manager manager;
    private final Logger logger;

    public DynamicMatchCommand(String name, int matchType, Manager manager) {
      super(name);
      this.matchType = matchType;
      this.manager = manager;
      this.logger = manager.getLogger();

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

      joinQueue(player, matchType, manager);
      return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
      return new ArrayList<>();
    }
  }
}
