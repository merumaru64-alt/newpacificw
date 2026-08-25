package com.districtx.pacifica;

import com.districtx.pacifica.command.PacificaCommand;
import com.districtx.pacifica.api.PacificaAPI;
import com.districtx.pacifica.api.PacificaApiProvider;
import com.districtx.pacifica.api.DiamondCurrencyService;
import com.districtx.pacifica.gui.GuiListener;
import com.districtx.pacifica.gui.GuiManager;
import com.districtx.pacifica.listener.PacificaListener;
import com.districtx.pacifica.service.EconomyManager;
import com.districtx.pacifica.service.CombatTagManager;
import com.districtx.pacifica.service.HologramManager;
import com.districtx.pacifica.service.HousingBridge;
import com.districtx.pacifica.service.StatusManager;
import com.districtx.pacifica.service.TeleportManager;
import com.districtx.pacifica.service.TpaManager;
import com.districtx.pacifica.service.WarpManager;
import com.districtx.pacifica.service.WastedManager;
import com.districtx.pacifica.service.BackpackManager;
import com.districtx.pacifica.service.DiamondCurrencyManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

public final class PacificaPlugin extends JavaPlugin {
    private ConfigStore store;
    private StatusManager statusManager;
    private TeleportManager teleportManager;
    private WarpManager warpManager;
    private EconomyManager economyManager;
    private CombatTagManager combatTagManager;
    private HousingBridge housingBridge;
    private TpaManager tpaManager;
    private HologramManager hologramManager;
    private GuiManager guiManager;
    private WastedManager wastedManager;
    private BackpackManager backpackManager;
    private DiamondCurrencyManager diamondCurrencyManager;
    private PacificaApiProvider pacificaApi;

    @Override
    public void onEnable() {
        store = new ConfigStore(this);
        statusManager = new StatusManager(this);
        teleportManager = new TeleportManager(this);
        warpManager = new WarpManager(store);
        economyManager = new EconomyManager(this);
        combatTagManager = new CombatTagManager(this);
        housingBridge = new HousingBridge(this);
        pacificaApi = new PacificaApiProvider(this, housingBridge);
        Bukkit.getServicesManager().register(PacificaAPI.class, pacificaApi, this, ServicePriority.Normal);
        tpaManager = new TpaManager(this);
        hologramManager = new HologramManager(this);
        guiManager = new GuiManager();
        wastedManager = new WastedManager(this);
        backpackManager = new BackpackManager(this);
        diamondCurrencyManager = new DiamondCurrencyManager(this);
        pacificaApi.registerService(DiamondCurrencyService.class, diamondCurrencyManager);

        PacificaCommand command = new PacificaCommand(this);
        for (String name : new String[]{"spawn", "warp", "setspawn", "rndmwarpset", "taxi", "trashcan", "tpa", "ct", "diamonds", "admindias"}) {
            if (getCommand(name) != null) {
                getCommand(name).setExecutor(command);
                getCommand(name).setTabCompleter(command);
            }
        }
        Bukkit.getPluginManager().registerEvents(new GuiListener(guiManager), this);
        Bukkit.getPluginManager().registerEvents(new PacificaListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.districtx.pacifica.listener.CombatTagListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.districtx.pacifica.listener.ModerationListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.districtx.pacifica.listener.BackpackListener(this), this);
        hologramManager.load();
    }

    @Override
    public void onDisable() {
        if (tpaManager != null) tpaManager.clear();
        if (teleportManager != null) teleportManager.clear();
        if (combatTagManager != null) combatTagManager.clear();
        if (wastedManager != null) wastedManager.clear();
        if (store != null) store.save();
        if (pacificaApi != null) {
            pacificaApi.unregisterAll();
            Bukkit.getServicesManager().unregister(PacificaAPI.class, pacificaApi);
        }
    }

    public ConfigStore store() { return store; }
    public StatusManager statuses() { return statusManager; }
    public TeleportManager teleports() { return teleportManager; }
    public WarpManager warps() { return warpManager; }
    public EconomyManager economy() { return economyManager; }
    public CombatTagManager combatTags() { return combatTagManager; }
    public HousingBridge housing() { return housingBridge; }
    public PacificaAPI api() { return pacificaApi; }
    public TpaManager tpa() { return tpaManager; }
    public HologramManager holograms() { return hologramManager; }
    public GuiManager guis() { return guiManager; }
    public WastedManager wasted() { return wastedManager; }
    public BackpackManager backpacks() { return backpackManager; }
    public DiamondCurrencyManager diamonds() { return diamondCurrencyManager; }
}