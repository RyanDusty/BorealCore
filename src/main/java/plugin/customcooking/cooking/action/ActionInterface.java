package plugin.customcooking.cooking.action;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface ActionInterface {

    void doOn(Player player, @Nullable Player anotherPlayer);

}
