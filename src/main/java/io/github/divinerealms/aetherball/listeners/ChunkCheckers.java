package io.github.divinerealms.aetherball.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Handles cleanup of cube entities when chunks unload.
 *
 * <p>Removes all Slime entities from unloading chunks to prevent ghost cubes and memory issues when
 * chunks reload later.
 */
public class ChunkCheckers extends BaseListener {

  /**
   * Removes all cube entities from a chunk before it unloads.
   *
   * <p>Iterates through entities in the unloading chunk and kills any Slime instances to ensure
   * proper cleanup and prevent entity persistence issues.
   *
   * @param event the {@link ChunkUnloadEvent} fired when a chunk is about to unload
   */
  @EventHandler
  public void onUnloadChunk(ChunkUnloadEvent event) {
    monitoredExecution(
        () -> {
          // Kill all Slime entities in the unloading chunk.
          for (Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof Slime)) {
              continue;
            }

            ((Slime) entity).setHealth(0);
          }
        });
  }
}
