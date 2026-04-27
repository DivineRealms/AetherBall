package io.github.divinerealms.aetherball.utils;

import static io.github.divinerealms.aetherball.utils.MatchUtils.isPlayerOnline;

import io.github.divinerealms.aetherball.configs.Lang;
import io.github.divinerealms.aetherball.configs.Settings;

import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * The LoggerUtil class provides a utility for managing formatted logging and messaging
 * functionalities in a Minecraft server environment. It handles message customization, replacement
 * of placeholders, and formatted broadcasting to players and the console, ensuring consistent and
 * readable output.
 */
public class LoggerUtil {

  private static Logger logger;

  // Private constructor prevents instantiation.
  private LoggerUtil() {
    throw new UnsupportedOperationException("Logger is a utility class and cannot be instantiated");
  }

  /**
   * Initializes the Logger utility with the necessary server references. This must be called once
   * during plugin startup before any logging methods are used.
   *
   * @param plugin the Plugin instance
   */
  public static void initialize(Plugin plugin) {
    logger = plugin.getLogger();
  }

  /**
   * Represents the various logging levels supported by the application.
   */
  public enum LoggingLevel {
    INFO(Level.INFO),
    SUCCESS(Level.INFO),
    WARN(Level.WARNING),
    ERROR(Level.SEVERE),
    DEBUG(Level.INFO),
    OTHER(Level.INFO);

    @Getter
    private final Level level;

    LoggingLevel(Level level) {
      this.level = level;
    }
  }

  /**
   * Logs a message to the console with automatic level detection.
   *
   * @param messageObject the message to be logged (Lang or String)
   * @param args          optional arguments for placeholder replacement
   */
  public static void logConsole(Object messageObject, String... args) {
    if (logger == null) {
      throw new IllegalStateException("LoggerUtil not initialized");
    }

    LoggingLevel level = detectLoggingLevel(messageObject);
    String message = resolveMessage(messageObject, true, args);
    String cleanMessage = ChatColor.stripColor(message);

    logger.log(level.getLevel(), cleanMessage);
  }

  /**
   * Logs a debug message to the console if debug mode is enabled.
   *
   * @param messageObject the debug message to log (Lang or String)
   * @param args          optional arguments for placeholder replacement
   */
  public static void debugConsole(Object messageObject, String... args) {
    if (Settings.DEBUG_MODE.asBoolean()) {
      logConsole(messageObject, args);
    }
  }

  /**
   * Sends a formatted message to a CommandSender.
   *
   * @param sender        the recipient of the message (player or console)
   * @param messageObject the message to send (Lang or String)
   * @param args          optional arguments for placeholder replacement
   */
  public static void sendMessage(CommandSender sender, Object messageObject, String... args) {
    if (sender instanceof Player) {
      if (!isPlayerOnline((Player) sender)) {
        return;
      }

      String message = resolveMessage(messageObject, false, args);
      sender.sendMessage(message);
    } else {
      logConsole(messageObject, args);
    }
  }

  /**
   * Sends a formatted message to all players with a specific permission and logs to console.
   *
   * @param permission    the permission required for players to receive the message
   * @param messageObject the message to send (Lang or String)
   * @param args          optional arguments for placeholder replacement
   */
  public static void sendMessage(String permission, Object messageObject, String... args) {
    String message = resolveMessage(messageObject, false, args);
    Bukkit.getServer().broadcast(message, permission);
    logConsole(messageObject, args);
  }

  /**
   * Sends a formatted message to all players within a radius who have the given permission.
   *
   * @param permission    the permission required for players to receive the message
   * @param center        the center location to define the area
   * @param radius        the radius around the center
   * @param messageObject the message to send (Lang or String)
   * @param args          optional arguments for placeholder replacement
   */
  public static void sendMessage(String permission, Location center, double radius,
                                 Object messageObject, String... args) {
    if (center == null || radius <= 0) {
      return;
    }

    String message = resolveMessage(messageObject, false, args);
    double radiusSquared = radius * radius;

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.getWorld() != center.getWorld()) {
        continue;
      }

      if (!player.hasPermission(permission)) {
        continue;
      }

      if (player.getLocation().distanceSquared(center) > radiusSquared) {
        continue;
      }

      player.sendMessage(message);
    }

    logConsole(messageObject, args);
  }

  /**
   * Sends a broadcast message to all players on the server.
   *
   * @param messageObject the message to be broadcasted (Lang or String)
   * @param args          optional arguments for placeholder replacement
   */
  public static void broadcast(Object messageObject, String... args) {
    String formatted = resolveMessage(messageObject, false, args);
    Bukkit.getServer().broadcastMessage(formatted);
  }

  /**
   * Sends an action bar message to the specified player.
   *
   * @param player     the player to whom the action bar message will be sent
   * @param messageObj the message to send (Lang or String)
   * @param args       optional arguments for placeholder replacement
   */
  public static void sendActionBar(Player player, Object messageObj, String... args) {
    if (!isPlayerOnline(player)) {
      return;
    }

    String message = resolveMessage(messageObj, false, args);
    IChatBaseComponent iChatBaseComponent = ChatSerializer.a(
        "{\"text\":\"" + escapeJson(message) + "\"}");
    PacketPlayOutChat packet = new PacketPlayOutChat(iChatBaseComponent, (byte) 2);
    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
  }

  /**
   * Sends a formatted action bar message to all online players.
   *
   * @param messageObj the message to be broadcasted in the action bar (Lang or String)
   * @param args       optional arguments for placeholder replacement
   */
  @SuppressWarnings("unused")
  public static void broadcastBar(Object messageObj, String... args) {
    String message = resolveMessage(messageObj, false, args);
    IChatBaseComponent iChatBaseComponent = ChatSerializer.a(
        "{\"text\":\"" + escapeJson(message) + "\"}");
    PacketPlayOutChat packet = new PacketPlayOutChat(iChatBaseComponent, (byte) 2);

    for (Player player : Bukkit.getOnlinePlayers()) {
      ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
  }

  /**
   * Sends a title and subtitle to a specified player with customized durations.
   *
   * @param player      the player to whom the title and subtitle will be sent
   * @param titleObj    the main title text (Lang or String)
   * @param subtitleObj the subtitle text (Lang or String)
   * @param fadeIn      the time in ticks for fade in
   * @param stay        the time in ticks to remain on screen
   * @param fadeOut     the time in ticks for fade out
   */
  public static void title(Player player, Object titleObj, Object subtitleObj, int fadeIn, int stay,
                           int fadeOut) {
    if (!isPlayerOnline(player)) {
      return;
    }

    String title = resolveMessage(titleObj, false);
    String subtitle = resolveMessage(subtitleObj, false);

    CraftPlayer craftPlayer = (CraftPlayer) player;
    IChatBaseComponent titleJSON = ChatSerializer.a("{\"text\":\"" + escapeJson(title) + "\"}");
    IChatBaseComponent subtitleJSON = ChatSerializer.a(
        "{\"text\":\"" + escapeJson(subtitle) + "\"}");

    craftPlayer.getHandle().playerConnection.sendPacket(
        new PacketPlayOutTitle(EnumTitleAction.TITLE, titleJSON));
    craftPlayer.getHandle().playerConnection.sendPacket(
        new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, subtitleJSON));
    craftPlayer.getHandle().playerConnection.sendPacket(
        new PacketPlayOutTitle(fadeIn, stay, fadeOut));
  }

  /**
   * Detects the appropriate LoggingLevel for a message.
   *
   * @param messageObject original message object
   * @return detected logging level
   */
  private static LoggingLevel detectLoggingLevel(Object messageObject) {
    if (messageObject instanceof Lang) {
      try {
        return LoggingLevel.valueOf(((Lang) messageObject).name());
      } catch (IllegalArgumentException ignored) {
        // Not a prefix constant.
      }
    }
    return LoggingLevel.INFO;
  }

  /**
   * Resolves a message object into its formatted string form.
   *
   * @param messageObject Lang or raw object
   * @param forConsole    whether this is for console output
   * @param args          optional placeholder arguments
   * @return resolved and formatted message
   */
  private static String resolveMessage(Object messageObject, boolean forConsole, String... args) {
    if (messageObject instanceof Lang) {
      return ((Lang) messageObject).replace(forConsole, args);
    }

    String message = String.valueOf(messageObject);
    return Lang.color(Lang.replacePrefixes(message, forConsole));
  }

  private static String escapeJson(String text) {
    return text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "")
        .replace("\r", "")
        .replace("\t", "");
  }
}
