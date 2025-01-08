package plugin.customcooking.util;

import org.bukkit.configuration.file.YamlConfiguration;
import plugin.customcooking.CustomCooking;
import plugin.customcooking.manager.configs.ConfigManager;
import plugin.customcooking.manager.configs.MessageManager;

import java.io.File;

public class ConfigUtil {

    public static YamlConfiguration getConfig(String configName) {
        File file = new File(CustomCooking.plugin.getDataFolder(), configName);
        if (!file.exists()) CustomCooking.plugin.saveResource(configName, false);
        return YamlConfiguration.loadConfiguration(file);
    }

    public static void reload() {
        ConfigManager.load();
        MessageManager.load();
        CustomCooking.getLayoutManager().unload();
        CustomCooking.getLayoutManager().load();
        CustomCooking.getEffectManager().unload();
        CustomCooking.getEffectManager().load();
        CustomCooking.getRecipeManager().unload();
        CustomCooking.getRecipeManager().load();
        CustomCooking.getCookingManager().unload();
        CustomCooking.getCookingManager().load();
        CustomCooking.getGuiManager().unload();
        CustomCooking.getGuiManager().load();
        CustomCooking.getCompetitionManager().unload();
        CustomCooking.getCompetitionManager().load();
        CustomCooking.getFurnitureManager().unload();
        CustomCooking.getFurnitureManager().load();
        CustomCooking.getJadeManager().unload();
        CustomCooking.getJadeManager().load();
        CustomCooking.getNodeManager().unload();
        CustomCooking.getNodeManager().load();
        CustomCooking.getWikiManager().unload();
        CustomCooking.getWikiManager().load();
    }
}


