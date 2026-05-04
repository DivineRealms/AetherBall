package io.github.divinerealms.aetherball.managers;

import lombok.Getter;
import me.neznamy.tab.api.TabAPI;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import static io.github.divinerealms.aetherball.utils.LoggerUtil.debugConsole;
import static io.github.divinerealms.aetherball.utils.LoggerUtil.logConsole;

public class DependencyLoader {
  private final JavaPlugin plugin;
  @Getter private Economy economy;
  @Getter private LuckPerms luckPerms;
  @Getter private TabAPI tabAPI;

  public DependencyLoader(JavaPlugin plugin) throws IllegalStateException {
    this.plugin = plugin;
    loadLuckPerms();
    loadVault();
    loadTab();
    debugConsole("{prefix_success}Hooked into dependencies successfully!");
  }

  private void loadLuckPerms() {
    RegisteredServiceProvider<LuckPerms> rsp =
        plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
    this.luckPerms = rsp == null ? null : rsp.getProvider();
    if (luckPerms == null) {
      throw new IllegalStateException("LuckPerms not found!");
    }
  }

  private void loadVault() {
    RegisteredServiceProvider<Economy> rsp =
        plugin.getServer().getServicesManager().getRegistration(Economy.class);
    this.economy = rsp == null ? null : rsp.getProvider();
    if (economy == null) {
      throw new IllegalStateException("Vault not found!");
    }
  }

  private boolean loadTab() {
    if (plugin.getServer().getPluginManager().isPluginEnabled("TAB")) {
      this.tabAPI = TabAPI.getInstance();
      return true;
    } else {
      this.tabAPI = null;
      logConsole("{prefix_warn}TAB plugin not found. Scoreboard features not available.");
      return false;
    }
  }

  public void reloadTab() {
    if (loadTab()) {
      debugConsole("{prefix_success}Re-hooked into TAB successfully!");
    }
  }
}
