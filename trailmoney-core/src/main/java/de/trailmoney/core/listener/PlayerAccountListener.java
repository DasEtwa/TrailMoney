package de.trailmoney.core.listener;

import de.trailmoney.core.config.TrailMoneySettings;
import de.trailmoney.core.service.TrailEconomyService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerAccountListener implements Listener {
    private final TrailEconomyService economyService;
    private TrailMoneySettings settings;

    public PlayerAccountListener(TrailEconomyService economyService, TrailMoneySettings settings) {
        this.economyService = economyService;
        this.settings = settings;
    }

    public void updateSettings(TrailMoneySettings settings) {
        this.settings = settings;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!settings.createAccountOnJoin()) {
            return;
        }
        economyService.getOrCreatePlayerAccount(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
}
