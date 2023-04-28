package plugin.customcooking.Util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.customcooking.CustomCooking;

import java.time.Duration;

public class AdventureUtil {

    public static void sendMessage(CommandSender sender, String s) {
        if (sender instanceof Player player) playerMessage(player, s);
        else consoleMessage(s);
    }

    public static void consoleMessage(String s) {
        Audience au = CustomCooking.adventure.sender(Bukkit.getConsoleSender());
        MiniMessage mm = MiniMessage.miniMessage();
        Component parsed = mm.deserialize(replaceLegacy(s));
        au.sendMessage(parsed);
    }

    public static void playerMessage(Player player, String s) {
        Audience au = CustomCooking.adventure.player(player);
        MiniMessage mm = MiniMessage.miniMessage();
        Component parsed = mm.deserialize(replaceLegacy(s));
        au.sendMessage(parsed);
    }

    public static void playerTitle(Player player, String s1, String s2, int in, int duration, int out) {
        Audience au = CustomCooking.adventure.player(player);
        MiniMessage mm = MiniMessage.miniMessage();
        Title.Times times = Title.Times.times(Duration.ofMillis(in), Duration.ofMillis(duration), Duration.ofMillis(out));
        Title title = Title.title(mm.deserialize(replaceLegacy(s1)), mm.deserialize(replaceLegacy(s2)), times);
        au.showTitle(title);
    }

    public static void playerTitle(Player player, Component s1, Component s2, int in, int duration, int out) {
        Audience au = CustomCooking.adventure.player(player);
        Title.Times times = Title.Times.times(Duration.ofMillis(in), Duration.ofMillis(duration), Duration.ofMillis(out));
        Title title = Title.title(s1, s2, times);
        au.showTitle(title);
    }

    public static void playerActionbar(Player player, String s) {
        Audience au = CustomCooking.adventure.player(player);
        MiniMessage mm = MiniMessage.miniMessage();
        au.sendActionBar(mm.deserialize(replaceLegacy(s)));
    }

    public static void playerSound(Player player, Sound.Source source, Key key, float volume, float pitch) {
        Sound sound = Sound.sound(key, source, volume, pitch);
        Audience au = CustomCooking.adventure.player(player);
        au.playSound(sound);
    }

    public static String replaceLegacy(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        char[] chars = s.replaceAll("&", "§").toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '§') {
                if (i + 1 < chars.length) {
                    switch (chars[i + 1]) {
                        case '0' -> {
                            i++;
                            stringBuilder.append("<black>");
                        }
                        case '1' -> {
                            i++;
                            stringBuilder.append("<dark_blue>");
                        }
                        case '2' -> {
                            i++;
                            stringBuilder.append("<dark_green>");
                        }
                        case '3' -> {
                            i++;
                            stringBuilder.append("<dark_aqua>");
                        }
                        case '4' -> {
                            i++;
                            stringBuilder.append("<dark_red>");
                        }
                        case '5' -> {
                            i++;
                            stringBuilder.append("<dark_purple>");
                        }
                        case '6' -> {
                            i++;
                            stringBuilder.append("<gold>");
                        }
                        case '7' -> {
                            i++;
                            stringBuilder.append("<gray>");
                        }
                        case '8' -> {
                            i++;
                            stringBuilder.append("<dark_gray>");
                        }
                        case '9' -> {
                            i++;
                            stringBuilder.append("<blue>");
                        }
                        case 'a' -> {
                            i++;
                            stringBuilder.append("<green>");
                        }
                        case 'b' -> {
                            i++;
                            stringBuilder.append("<aqua>");
                        }
                        case 'c' -> {
                            i++;
                            stringBuilder.append("<red>");
                        }
                        case 'd' -> {
                            i++;
                            stringBuilder.append("<light_purple>");
                        }
                        case 'e' -> {
                            i++;
                            stringBuilder.append("<yellow>");
                        }
                        case 'f' -> {
                            i++;
                            stringBuilder.append("<white>");
                        }
                        case 'r' -> {
                            i++;
                            stringBuilder.append("<reset><!italic>");
                        }
                        case 'l' -> {
                            i++;
                            stringBuilder.append("<bold>");
                        }
                        case 'm' -> {
                            i++;
                            stringBuilder.append("<strikethrough>");
                        }
                        case 'o' -> {
                            i++;
                            stringBuilder.append("<italic>");
                        }
                        case 'n' -> {
                            i++;
                            stringBuilder.append("<underlined>");
                        }
                        case 'x' -> {
                            stringBuilder.append("<#").append(chars[i + 3]).append(chars[i + 5]).append(chars[i + 7]).append(chars[i + 9]).append(chars[i + 11]).append(chars[i + 13]).append(">");
                            i += 13;
                        }
                        case 'k' -> {
                            i++;
                            stringBuilder.append("<obfuscated>");
                        }
                    }
                }
            } else {
                stringBuilder.append(chars[i]);
            }
        }
        return stringBuilder.toString();
    }
}

