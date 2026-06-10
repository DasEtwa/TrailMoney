package de.trailmoney.core;

import de.trailmoney.api.EconomyService;
import de.trailmoney.core.command.EcoCommand;
import de.trailmoney.core.command.MoneyCommand;
import de.trailmoney.core.command.PayCommand;
import de.trailmoney.core.config.TrailMoneySettings;
import de.trailmoney.core.listener.PlayerAccountListener;
import de.trailmoney.core.service.TrailEconomyService;
import de.trailmoney.core.storage.EconomyStorage;
import de.trailmoney.core.storage.sqlite.SqliteEconomyStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class TrailMoneyPlugin extends JavaPlugin {
    private EconomyStorage storage;
    private TrailEconomyService economyService;
    private PlayerAccountListener accountListener;
    private TrailMoneySettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = TrailMoneySettings.load(this);

        storage = new SqliteEconomyStorage(settings.sqliteFile());
        storage.initialize();

        economyService = new TrailEconomyService(this, storage, settings);
        getServer().getServicesManager().register(EconomyService.class, economyService, this, ServicePriority.Normal);

        accountListener = new PlayerAccountListener(economyService, settings);
        getServer().getPluginManager().registerEvents(accountListener, this);

        registerCommands();
        getLogger().info("TrailMoney enabled with SQLite storage at " + settings.sqliteFile());
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(EconomyService.class, economyService);
        if (storage != null) {
            storage.close();
        }
    }

    public TrailMoneySettings settings() {
        return settings;
    }

    public TrailEconomyService economyService() {
        return economyService;
    }

    public void reloadTrailMoney() {
        reloadConfig();
        settings = TrailMoneySettings.load(this);
        economyService.updateSettings(settings);
        accountListener.updateSettings(settings);
    }

    private void registerCommands() {
        MoneyCommand moneyCommand = new MoneyCommand(this);
        PayCommand payCommand = new PayCommand(this);
        EcoCommand ecoCommand = new EcoCommand(this);

        setCommandExecutor("money", moneyCommand);
        setCommandExecutor("pay", payCommand);
        setCommandExecutor("eco", ecoCommand);
    }

    private void setCommandExecutor(String commandName, org.bukkit.command.TabExecutor executor) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + commandName);
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
