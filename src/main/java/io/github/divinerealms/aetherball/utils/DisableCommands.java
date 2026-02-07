package io.github.divinerealms.aetherball.utils;

import io.github.divinerealms.aetherball.core.Manager;
import io.github.divinerealms.aetherball.managers.ConfigManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

public class DisableCommands {

  private final ConfigManager configManager;
  private final FileConfiguration config;

  @Getter
  private final Set<String> commands = new HashSet<>();
  private final String configPath = "enabledCommands";

  public DisableCommands(Manager manager) {
    this.configManager = manager.getConfigManager();
    this.config = configManager.getConfig("config.yml");

    String cfgCommands = config.getString(configPath, "").toLowerCase().trim();
    if (!cfgCommands.isEmpty()) {
      Collections.addAll(commands, cfgCommands.split("\\s+"));
    }
  }

  public boolean addCommand(String cmd) {
    boolean added = commands.add(cmd.toLowerCase());
    saveConfig();
    return added;
  }

  public boolean removeCommand(String cmd) {
    boolean removed = commands.remove(cmd.toLowerCase());
    saveConfig();
    return removed;
  }

  private void saveConfig() {
    config.set(configPath, String.join(" ", commands));
    configManager.saveConfig("config.yml");
  }
}
