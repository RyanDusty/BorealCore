package plugin.customcooking.action;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import plugin.customcooking.BorealCore;

import javax.annotation.Nullable;

public record CommandActionImpl(String[] commands, String nick) implements Action {

    public CommandActionImpl(String[] commands, @Nullable String nick) {
        this.commands = commands;
        this.nick = nick == null ? "" : nick;
    }

    @Override
    public void doOn(Player player, @Nullable Player anotherPlayer) {
        for (String command : commands) {
            BorealCore.plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                    command.replace("{player}", player.getName())
                            .replace("{x}", String.valueOf(player.getLocation().getBlockX()))
                            .replace("{y}", String.valueOf(player.getLocation().getBlockY()))
                            .replace("{z}", String.valueOf(player.getLocation().getBlockZ()))
                            .replace("{loot}", nick)
                            .replace("{world}", player.getWorld().getName())
                            .replace("{activator}", anotherPlayer == null ? "" : anotherPlayer.getName())
            );
        }
    }
}
