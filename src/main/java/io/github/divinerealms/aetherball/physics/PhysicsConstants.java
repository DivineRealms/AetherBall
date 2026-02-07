package io.github.divinerealms.aetherball.physics;

import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;

@Getter
public class PhysicsConstants {

  // --- Task Intervals (Ticks) ---
  public static final long PHYSICS_TASK_INTERVAL_TICKS = 1;
  public static final long MATCH_TASK_INTERVAL_TICKS = 1;
  public static final long GLOW_TASK_INTERVAL_TICKS = 2;
  public static final int CLEANUP_LAST_TOUCHES_INTERVAL = 1;
  public static final int EXP_UPDATE_INTERVAL_TICKS = 1;

  // --- Slime / Entity Configuration ---
  public static final int JUMP_POTION_DURATION = Integer.MAX_VALUE;
  public static final int JUMP_POTION_AMPLIFIER = -3;

  // --- Kick Power & Charge Settings ---
  public static final double CHARGE_MULTIPLIER = 7;
  public static final double CHARGE_BASE_VALUE = 1;
  public static final double CHARGE_RECOVERY_RATE = 0.95;

  // --- Velocity & Motion Modifiers ---
  public static final double MIN_SPEED_FOR_DAMPENING = 0.5;
  public static final double DRIBBLE_SPEED_LIMIT = 0.5;
  public static final double WALL_BOUNCE_FACTOR = 0.8;
  public static final double AIR_DRAG_FACTOR = 0.98;
  public static final double CUBE_JUMP_RIGHT_CLICK = 0.7;

  // --- Distance & Collision Thresholds ---
  public static final double HIT_RADIUS = 1.2;
  public static final double HIT_RADIUS_SQUARED = HIT_RADIUS * HIT_RADIUS;
  public static final double HIT_RADIUS_SQUARED_3X = (HIT_RADIUS * 3) * (HIT_RADIUS * 3);
  public static final double MIN_RADIUS = 0.8;
  public static final double MIN_RADIUS_SQUARED = MIN_RADIUS * MIN_RADIUS;
  public static final double BOUNCE_THRESHOLD = 0.3;

  // --- Physics Multipliers ---
  public static final double BALL_TOUCH_Y_OFFSET = 1;
  public static final double CUBE_HITBOX_ADJUSTMENT = 1.5;
  public static final double KICK_POWER_SPEED_MULTIPLIER = 2;
  public static final double PLAYER_SPEED_TOUCH_DIVISOR = 3;
  public static final double CUBE_SPEED_TOUCH_DIVISOR = 6;
  public static final double PROXIMITY_THRESHOLD_MULTIPLIER = 1.3;

  // --- Physics Math Thresholds ---
  public static final double VECTOR_CHANGE_THRESHOLD = 0.1;
  public static final double VERTICAL_BOUNCE_THRESHOLD = 0.05;

  // --- Player / Location Offsets ---
  public static final double PLAYER_HEAD_LEVEL = 2;
  public static final double PLAYER_HEIGHT_SCALE = 2.0;
  public static final double PLAYER_FOOT_LEVEL = 1;
  public static final double VERTICAL_INTERACTION_OFFSET = 0.25;

  // --- Sound Defaults ---
  public static final double MIN_SOUND_POWER = 0.15;
  public static final float SOUND_VOLUME = 0.5F;
  public static final float SOUND_PITCH = 1;

  // --- Particle Defaults ---
  public static final double DISTANCE_PARTICLE_THRESHOLD = 32;
  public static final double DISTANCE_PARTICLE_THRESHOLD_SQUARED =
      DISTANCE_PARTICLE_THRESHOLD * DISTANCE_PARTICLE_THRESHOLD;
  public static final double MAX_PARTICLE_DISTANCE = 160;
  public static final double MAX_PARTICLE_DISTANCE_SQUARED =
      MAX_PARTICLE_DISTANCE * MAX_PARTICLE_DISTANCE;
  public static final double PARTICLE_Y_OFFSET = 0.25;
  public static final float GENERIC_PARTICLE_OFFSET = 0.01F;
  public static final float GENERIC_PARTICLE_SPEED = 0.1F;
  public static final int GENERIC_PARTICLE_COUNT = 10;

  // --- Utility ---
  public static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
}