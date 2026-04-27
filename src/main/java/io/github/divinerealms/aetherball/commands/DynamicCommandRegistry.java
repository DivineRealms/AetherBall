package io.github.divinerealms.aetherball.commands;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.divinerealms.aetherball.configs.Lang.*;
import static io.github.divinerealms.aetherball.utils.MatchUtils.joinStrings;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.joinQueue;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.*;
import static io.github.divinerealms.aetherball.utils.Permissions.PERM_PLAY;

public class DynamicCommandRegistry {

  private final Manager manager;
  private final Map<String, Command> registeredCommands = new HashMap<>();
  private final String pluginName;
  private CommandMap commandMap;

  public DynamicCommandRegistry(Manager manager) {
    this.manager = manager;
    this.pluginName = manager.getPlugin().getName().toLowerCase();
    initializeCommandMap();
  }

  private void initializeCommandMap() {
    try {
      Server server = manager.getPlugin().getServer();
      Field field = server.getClass().getDeclaredField("commandMap");
      field.setAccessible(true);
      commandMap = (CommandMap) field.get(server);
    } catch (Exception exception) {
      logConsole("{prefix_error}Failed to initialize command map for dynamic commands",
          exception.getMessage());
    }
  }

  public void registerAllMatchTypeCommands() {
    if (commandMap == null) {
      logConsole("{prefix_error}Cannot register dynamic commands - CommandMap is null");
      return;
    }

    List<Integer> enabledTypes = Settings.getEnabledMatchTypes();
    List<String> registered = new ArrayList<>();

    for (int type : enabledTypes) {
      String commandName = Settings.getMatchTypeName(type);

      if (type == 1 && Settings.DUEL_ENABLED.asBoolean()) {
        if (registerDuelCommand()) {
          registered.add(commandName + " (with duels)");
        }
      } else {
        if (registerMatchTypeCommand(type, commandName)) {
          registered.add(commandName);
        }
      }
    }

    if (!registered.isEmpty()) {
      debugConsole(
          "{prefix_success}Registered dynamic match commands: &e" + joinStrings(registered));
    }
  }

  private boolean registerMatchTypeCommand(int type, String commandName) {
    try {
      DynamicMatchCommand command = new DynamicMatchCommand(commandName, type, manager);
      commandMap.register(pluginName, command);
      registeredCommands.put(commandName, command);
      return true;
    } catch (Exception exception) {
      logConsole("{prefix_warn}Failed to register command: /" + commandName,
          exception.getMessage());
      return false;
    }
  }

  private boolean registerDuelCommand() {
    try {
      DuelCommand command = new DuelCommand(manager);
      commandMap.register(pluginName, command);
      registeredCommands.put("1v1", command);
      return true;
    } catch (Exception exception) {
      logConsole("{prefix_warn}Failed to register duel command: /1v1", exception.getMessage());
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
        knownCommands.remove(pluginName + ":" + commandName);
        removed.add(commandName);
      }

      registeredCommands.clear();

      if (!removed.isEmpty()) {
        debugConsole("{prefix_success}Unregistered dynamic commands: " + joinStrings(removed));
      }
    } catch (Exception exception) {
      logConsole("{prefix_warn}Failed to unregister dynamic commands", exception.getMessage());
    }
  }

  public void reloadCommands() {
    unregisterAllMatchTypeCommands();
    registerAllMatchTypeCommands();
  }

  private static class DynamicMatchCommand extends Command {

    private final int matchType;
    private final Manager manager;

    public DynamicMatchCommand(String name, int matchType, Manager manager) {
      super(name);
      this.matchType = matchType;
      this.manager = manager;

      setDescription("Join " + name + " matchmaking queue");
      setUsage("/" + name);
      setPermission(PERM_PLAY);
      setPermissionMessage(NO_PERM.toString());
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
      if (!(sender instanceof Player player)) {
        sendMessage(sender, INGAME_ONLY);
        return true;
      }

      if (!player.hasPermission(PERM_PLAY)) {
        sendMessage(player, getPermissionMessage());
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

  private static class DuelCommand extends Command {

    private final Manager manager;

    public DuelCommand(Manager manager) {
      super("1v1");
      this.manager = manager;

      setDescription("Join 1v1 queue or send/manage duel requests");
      setUsage("/1v1 [player|accept|decline|cancel]");
      setPermission(PERM_PLAY);
      setPermissionMessage(NO_PERM.toString());
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
      if (!(sender instanceof Player player)) {
        sendMessage(sender, INGAME_ONLY);
        return true;
      }

      if (!player.hasPermission(PERM_PLAY)) {
        sendMessage(sender, getPermissionMessage());
        return true;
      }

      // No arguments = join 1v1 queue.
      if (args.length == 0) {
        joinQueue(player, 1, manager);
        return true;
      }

      String sub = args[0].toLowerCase();
      switch (sub) {
        case "accept":
          manager.getDuelManager().acceptDuelRequest(player);
          break;

        case "decline":
          manager.getDuelManager().declineDuelRequest(player);
          break;

        case "cancel":
          manager.getDuelManager().cancelDuelRequest(player);
          break;

        default:
          // Treat as player name for duel request.
          Player target = Bukkit.getPlayer(args[0]);
          if (target == null || !target.isOnline()) {
            sendMessage(player, DUEL_PLAYER_NOT_FOUND, args[0]);
            return true;
          }

          if (target.equals(player)) {
            sendMessage(player, DUEL_CANNOT_DUEL_SELF);
            return true;
          }

          manager.getDuelManager().sendDuelRequest(player, target);
          break;
      }

      return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
      if (!(sender instanceof Player player)) {
        return new ArrayList<>();
      }

      if (args.length == 1) {
        List<String> completions = new ArrayList<>();
        completions.addAll(Arrays.asList("accept", "decline", "cancel"));

        // Add online players.
        completions.addAll(Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> !name.equals(player.getName()))
            .toList());

        // Filter by what the user has typed.
        String input = args[0].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
      }

      return new ArrayList<>();
    }
  }
}
