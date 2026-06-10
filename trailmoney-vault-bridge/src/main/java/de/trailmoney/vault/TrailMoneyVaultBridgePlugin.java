package de.trailmoney.vault;

import de.trailmoney.api.EconomyService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class TrailMoneyVaultBridgePlugin extends JavaPlugin {
    private TrailMoneyVaultEconomy vaultEconomy;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<EconomyService> provider = getServer().getServicesManager().getRegistration(EconomyService.class);
        if (provider == null) {
            getLogger().severe("TrailMoney EconomyService is not registered. Disabling Vault bridge.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        vaultEconomy = new TrailMoneyVaultEconomy(provider.getProvider());
        getServer().getServicesManager().register(Economy.class, vaultEconomy, this, ServicePriority.Highest);
        getLogger().info("Registered TrailMoney as a Vault Economy provider.");
    }

    @Override
    public void onDisable() {
        if (vaultEconomy != null) {
            getServer().getServicesManager().unregister(Economy.class, vaultEconomy);
        }
    }
}
