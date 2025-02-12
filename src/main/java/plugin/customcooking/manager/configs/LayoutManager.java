package plugin.customcooking.manager.configs;

import org.bukkit.configuration.file.YamlConfiguration;
import plugin.customcooking.functions.cooking.object.Layout;
import plugin.customcooking.object.Function;
import plugin.customcooking.utility.AdventureUtil;
import plugin.customcooking.utility.ConfigUtil;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class LayoutManager extends Function {

    public static HashMap<String, Layout> LAYOUTS;

    @Override
    public void load() {
        LAYOUTS = new HashMap<>();
        YamlConfiguration config = ConfigUtil.getConfig("bars.yml");
        Set<String> keys = config.getKeys(false);
        for (String key : keys) {
            int range = config.getInt(key + ".range");
            Set<String> rates = Objects.requireNonNull(config.getConfigurationSection(key + ".layout")).getKeys(false);
            double[] successRate = new double[rates.size()];
            for (int i = 0; i < rates.size(); i++)
                successRate[i] = config.getDouble(key + ".layout." + (i + 1));
            int size = rates.size() * range - 1;
            Layout layout = new Layout(
                    range,
                    successRate,
                    size,
                    config.getString(key + ".subtitle.start", "<font:customcooking:default>"),
                    config.getString(key + ".subtitle.bar", "뀃"),
                    config.getString(key + ".subtitle.pointer", "뀄"),
                    config.getString(key + ".subtitle.offset", "뀁"),
                    config.getString(key + ".subtitle.end", "</font>"),
                    config.getString(key + ".subtitle.pointer_offset", "뀂"),
                    config.getString(key + ".title", " ")
            );
            LAYOUTS.put(key, layout);
        }
        AdventureUtil.consoleMessage("[CustomCooking] Loaded <green>" + LAYOUTS.size() + " <gray>bars");
    }

    @Override
    public void unload() {
        if (LAYOUTS != null) LAYOUTS.clear();
    }
}