package io.github.divinerealms.footcube.physics.touch;

import io.github.divinerealms.footcube.configs.Settings;
import lombok.Getter;

@Getter
public enum CubeTouchType {
  REGULAR_KICK, CHARGED_KICK, RISE;

  /**
   * Gets the cooldown for this touch type from the config.
   *
   * @return Cooldown in milliseconds
   */
  public long getCooldown() {
    switch (this) {
      case REGULAR_KICK:
        return Settings.KICK_COOLDOWN_REGULAR.asLong();
      case CHARGED_KICK:
        return Settings.KICK_COOLDOWN_CHARGED.asLong();
      case RISE:
        return Settings.KICK_COOLDOWN_RISE.asLong();
      default:
        throw new IllegalStateException("Unknown touch type: " + this);
    }
  }
}