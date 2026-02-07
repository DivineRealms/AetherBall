package io.github.divinerealms.aetherball.matchmaking.logic;

import io.github.divinerealms.aetherball.matchmaking.Match;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@Getter
public class MatchData {

  private final List<Match> matches = new CopyOnWriteArrayList<>();
  private final List<Match> openMatches = new CopyOnWriteArrayList<>();
  private final Map<Integer, Queue<Player>> playerQueues = new ConcurrentHashMap<>();
  private final Set<Integer> lockedQueues = ConcurrentHashMap.newKeySet();
  private final Map<Integer, ReentrantLock> queueLocks = new ConcurrentHashMap<>();

  @Setter
  private boolean matchesEnabled = true;

  public MatchData() {
  }

  /**
   * Initializes queues and locks for the given match types. Called by MatchSystem after config is
   * loaded.
   *
   * @param enabledTypes List of match type numbers from config (e.g., [2, 3, 4])
   */
  public void initializeForMatchTypes(List<Integer> enabledTypes) {
    // Clear existing queues and locks
    playerQueues.clear();
    queueLocks.clear();
    lockedQueues.clear();

    // Create queues and locks for each enabled match type
    for (int matchType : enabledTypes) {
      playerQueues.put(matchType, new ConcurrentLinkedQueue<>());
      queueLocks.put(matchType, new ReentrantLock());
    }
  }
}
