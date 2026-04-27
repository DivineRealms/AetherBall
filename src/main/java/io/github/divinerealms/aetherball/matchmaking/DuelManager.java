package io.github.divinerealms.aetherball.matchmaking;

import io.github.divinerealms.aetherball.configs.Settings;
import io.github.divinerealms.aetherball.managers.Manager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.divinerealms.aetherball.configs.Lang.*;
import static io.github.divinerealms.aetherball.utils.MatchUtils.isPlayerOnline;
import static io.github.divinerealms.aetherball.utils.GameCommandsHelper.isInQueueOrMatch;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.sendMessage;

/**
 * Manages 1v1 duel requests between players.
 * <p>
 * Handles request creation, expiry, acceptance, and cancellation.
 * </p>
 */
public class DuelManager {

  private final Manager manager;
  private final Map<UUID, DuelRequest> activeRequests = new HashMap<>();

  public DuelManager(Manager manager) {
    this.manager = manager;
  }

  /**
   * Sends a duel request from one player to another.
   *
   * @param sender the player sending the duel request
   * @param target the player receiving the duel request
   */
  public void sendDuelRequest(Player sender, Player target) {
    UUID senderUuid = sender.getUniqueId();
    UUID targetUuid = target.getUniqueId();

    // Check if sender already has an active request.
    if (activeRequests.containsKey(senderUuid)) {
      sendMessage(sender, DUEL_ALREADY_SENT);
      return;
    }

    // Check if target already has a pending request from this sender.
    DuelRequest existingRequest = activeRequests.get(targetUuid);
    if (existingRequest != null && existingRequest.getSenderUuid().equals(senderUuid)) {
      sendMessage(sender, DUEL_ALREADY_SENT);
      return;
    }

    // Check if sender is still available (not in a match, etc.)
    if (isInQueueOrMatch(sender, manager)) {
      sendMessage(sender, DUEL_YOU_IN_MATCH);
      return;
    }

    if (isInQueueOrMatch(target, manager)) {
      sendMessage(sender, DUEL_SENDER_IN_MATCH, target.getName());
      return;
    }

    if (manager.getMatchData().isInDuelPair(senderUuid)) {
      sendMessage(sender, DUEL_ALREADY_SENT);
      return;
    }

    // Create and store the request.
    DuelRequest request = new DuelRequest(senderUuid, targetUuid);
    activeRequests.put(targetUuid, request);

    // Schedule expiry task.
    long expiryTicks = Settings.DUEL_REQUEST_EXPIRY.asLong() * 20L;
    BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(
        manager.getPlugin(),
        () -> expireRequest(targetUuid),
        expiryTicks
    );
    request.setExpiryTask(expiryTask);

    // Notify both players.
    sendMessage(sender, DUEL_REQUEST_SENT, target.getName());
    sendMessage(target, DUEL_REQUEST_RECEIVED, sender.getName());

  }

  /**
   * Accepts a duel request.
   *
   * @param acceptor the player accepting the request
   */
  public void acceptDuelRequest(Player acceptor) {
    UUID acceptorUuid = acceptor.getUniqueId();
    DuelRequest request = activeRequests.remove(acceptorUuid);

    if (request == null) {
      sendMessage(acceptor, DUEL_NO_PENDING_REQUEST);
      return;
    }

    // Cancel expiry task.
    request.cancelExpiryTask();

    // Get the sender.
    Player sender = Bukkit.getPlayer(request.getSenderUuid());
    if (!isPlayerOnline(sender)) {
      sendMessage(acceptor, DUEL_SENDER_OFFLINE);
      return;
    }

    // Check if sender is still available (not in a match, etc.)
    if (isInQueueOrMatch(sender, manager)) {
      sendMessage(acceptor, DUEL_SENDER_IN_MATCH, sender.getName());
      return;
    }

    if (isInQueueOrMatch(acceptor, manager)) {
      sendMessage(acceptor, DUEL_YOU_IN_MATCH);
      return;
    }

    // Notify both players.
    sendMessage(acceptor, DUEL_ACCEPTED_ACCEPTOR, sender.getName());
    sendMessage(sender, DUEL_ACCEPTED_SENDER, acceptor.getName());

    // Join them both into a 1v1 match.
    manager.getMatchData().createDuelPair(sender.getUniqueId(), acceptorUuid);
    manager.getMatchManager().joinQueue(sender, 1);
    manager.getMatchManager().joinQueue(acceptor, 1);

  }

  /**
   * Declines a duel request.
   *
   * @param decliner the player declining the request
   */
  public void declineDuelRequest(Player decliner) {
    UUID declinerUuid = decliner.getUniqueId();
    DuelRequest request = activeRequests.remove(declinerUuid);

    if (request == null) {
      sendMessage(decliner, DUEL_NO_PENDING_REQUEST);
      return;
    }

    // Cancel expiry task.
    request.cancelExpiryTask();

    // Notify both players.
    Player sender = Bukkit.getPlayer(request.getSenderUuid());
    sendMessage(sender, DUEL_DECLINED_SENDER, decliner.getName());
    sendMessage(decliner, DUEL_DECLINED_DECLINER);

  }

  /**
   * Cancels an outgoing duel request.
   *
   * @param sender the player canceling their sent request
   */
  public void cancelDuelRequest(Player sender) {
    UUID senderUuid = sender.getUniqueId();
    UUID targetUuid = null;
    DuelRequest removed = null;

    for (Map.Entry<UUID, DuelRequest> entry : activeRequests.entrySet()) {
      if (entry.getValue().getSenderUuid().equals(senderUuid)) {
        targetUuid = entry.getKey();
        removed = entry.getValue();
        break;
      }
    }

    if (removed == null) {
      sendMessage(sender, DUEL_NO_OUTGOING_REQUEST);
      return;
    }

    activeRequests.remove(targetUuid);

    // Cancel expiry task.
    removed.cancelExpiryTask();

    // Notify both players.
    Player target = Bukkit.getPlayer(removed.getTargetUuid());
    sendMessage(target, DUEL_CANCELED_TARGET, sender.getName());
    sendMessage(sender, DUEL_CANCELED_SENDER);

  }

  /**
   * Expires a duel request.
   *
   * @param targetUuid the UUID of the player who received the request
   */
  private void expireRequest(UUID targetUuid) {
    DuelRequest request = activeRequests.remove(targetUuid);
    if (request == null) {
      return;
    }

    // Notify both players if online.
    Player sender = Bukkit.getPlayer(request.getSenderUuid());
    Player target = Bukkit.getPlayer(targetUuid);

    sendMessage(sender, DUEL_EXPIRED_SENDER);
    sendMessage(target, DUEL_EXPIRED_TARGET);
  }

  /**
   * Cleans up all requests associated with a player (called on quit/match join).
   *
   * @param player the player to clean up requests for
   */
  public void cleanupPlayerRequests(Player player) {
    UUID playerUuid = player.getUniqueId();

    // Remove as target.
    DuelRequest asTarget = activeRequests.remove(playerUuid);
    if (asTarget != null) {
      asTarget.cancelExpiryTask();
    }

    // Remove as sender.
    activeRequests.entrySet().removeIf(entry -> {
      if (entry.getValue().getSenderUuid().equals(playerUuid)) {
        entry.getValue().cancelExpiryTask();
        return true;
      }
      return false;
    });

    UUID partnerUuid = manager.getMatchData().getDuelPair(playerUuid);
    if (partnerUuid != null) {
      manager.getMatchData().removeDuelPairs(playerUuid, partnerUuid);
    }
  }

  /**
   * Represents a duel request between two players.
   */
  @Getter
  private static class DuelRequest {

    private final UUID senderUuid;
    private final UUID targetUuid;
    private final long createdAt;
    @Setter
    private BukkitTask expiryTask;

    public DuelRequest(UUID senderUuid, UUID targetUuid) {
      this.senderUuid = senderUuid;
      this.targetUuid = targetUuid;
      this.createdAt = System.currentTimeMillis();
    }

    public void cancelExpiryTask() {
      if (expiryTask != null) {
        expiryTask.cancel();
      }
    }
  }
}