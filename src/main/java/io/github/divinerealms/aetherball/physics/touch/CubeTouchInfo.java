package io.github.divinerealms.aetherball.physics.touch;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CubeTouchInfo {

  private long timestamp;
  private CubeTouchType type;
}
