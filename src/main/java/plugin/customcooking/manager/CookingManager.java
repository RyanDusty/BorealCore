package plugin.customcooking.manager;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import dev.lone.itemsadder.api.CustomFurniture;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import plugin.customcooking.CustomCooking;
import plugin.customcooking.cooking.*;
import plugin.customcooking.cooking.action.Action;
import plugin.customcooking.cooking.competition.Competition;
import plugin.customcooking.gui.Ingredient;
import plugin.customcooking.listener.ConsumeItemListener;
import plugin.customcooking.listener.InteractListener;
import plugin.customcooking.manager.configs.*;
import plugin.customcooking.object.Function;
import plugin.customcooking.util.AdventureUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static net.kyori.adventure.key.Key.key;
import static plugin.customcooking.gui.GuiManager.INGREDIENTS;
import static plugin.customcooking.manager.FurnitureManager.playCookingResultSFX;
import static plugin.customcooking.manager.configs.RecipeManager.RECIPES;
import static plugin.customcooking.manager.configs.ConfigManager.perfectChance;
import static plugin.customcooking.util.RecipeDataUtil.hasMastery;
import static plugin.customcooking.util.AdventureUtil.playerSound;
import static plugin.customcooking.util.InventoryUtil.*;

public class CookingManager extends Function {

    private final InteractListener interactListener;
    private final ConsumeItemListener consumeItemListener;
    private final HashMap<Player, Recipe> cookedRecipe;
    private final HashMap<Player, Location> cookingPotLocations;
    public final ConcurrentHashMap<Player, CookingPlayer> cookingPlayerCache;
    private BukkitRunnable soundTask;

    public CookingManager() {
        this.interactListener = new InteractListener(this);
        this.consumeItemListener = new ConsumeItemListener(this);
        this.cookedRecipe = new HashMap<>();
        this.cookingPotLocations = new HashMap<>();
        this.cookingPlayerCache = new ConcurrentHashMap<>();
        load();
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this.interactListener, CustomCooking.plugin);
        Bukkit.getPluginManager().registerEvents(this.consumeItemListener, CustomCooking.plugin);
    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(this.interactListener);
        HandlerList.unregisterAll(this.consumeItemListener);
    }

    public void handleCooking(String recipe, Player player, CustomFurniture clickedFurniture) {
        if (isPlayerCooking(player)) {
            AdventureUtil.playerMessage(player, MessageManager.infoNegative + MessageManager.alreadyCooking);
        } else {
            // get the bar config
            Recipe bar = RecipeManager.RECIPES.get(recipe);
            // checks if player has required ingredients
            List<String> ingredients = bar.getIngredients();
            if (handleIngredientCheck(player.getInventory(), ingredients, 1)) {
                // Delay removal of items if furniture is not null
                removeIngredients(player.getInventory(), ingredients, 1);
                if (clickedFurniture != null) {
                    Location loc = clickedFurniture.getArmorstand().getLocation();
                    FurnitureManager.ingredientsSFX(player, ingredients, loc);
                }
                    onCookedItem(player, bar, clickedFurniture);
            } else {
                AdventureUtil.playerMessage(player, MessageManager.infoNegative + MessageManager.noIngredients);
            }
        }
    }

    public void handleMaterialAutocooking(String recipeId, Player player, Integer amount) {
            Ingredient recipe = INGREDIENTS.get(recipeId);
            // checks if player has required ingredients
            List<String> ingredients = recipe.getIngredients();
            if (handleIngredientCheck(player.getInventory(), ingredients, amount)) {
                removeIngredients(player.getInventory(), ingredients, amount);
                giveItem(player, recipe.getKey(), amount, true);
                playerSound(player, Sound.Source.AMBIENT, key(ConfigManager.customNamespace, "done"), 1f, 1f);
                AdventureUtil.playerMessage(player, MessageManager.infoPositive + MessageManager.cookingAutocooked.replace("{recipe}", recipe.getNick()) + " x" + amount);
            } else {
                AdventureUtil.playerMessage(player, MessageManager.infoNegative + MessageManager.noIngredients);
            }
    }

    public void handleAutocooking(String recipeId, Player player, Integer amount) {
        if (isPlayerCooking(player)) {
            AdventureUtil.playerMessage(player, MessageManager.infoNegative + MessageManager.alreadyCooking);
        } else {
            Recipe recipe = RECIPES.get(recipeId);
            // checks if player has required ingredients
            List<String> ingredients = recipe.getIngredients();
            if (handleIngredientCheck(player.getInventory(), ingredients, amount)) {
                // Delay removal of items if furniture is not null
                removeIngredients(player.getInventory(), ingredients, amount);
                giveItem(player, String.valueOf(recipe.getCookedItems()), amount, true);
                playerSound(player, Sound.Source.AMBIENT, key(ConfigManager.customNamespace, "done"), 1f, 1f);
                AdventureUtil.playerMessage(player, MessageManager.infoPositive + MessageManager.cookingAutocooked.replace("{recipe}", recipe.getNick()) + " x" + amount);
            } else {
                AdventureUtil.playerMessage(player, MessageManager.infoNegative + MessageManager.noIngredients);
            }
        }

    }

    private void onCookedItem(Player player, Recipe recipe, CustomFurniture clickedFurniture) {

        player.closeInventory();
        cookedRecipe.put(player, recipe);

        if (clickedFurniture != null) {
            cookingPotLocations.put(player, clickedFurniture.getArmorstand().getLocation());
        }

        CookingPlayer cookingPlayer = cookingPlayerCache.remove(player);
        if (cookingPlayer == null) {
            if (recipe != Recipe.EMPTY) {
                // No custom recipe
                if (recipe == null) {
                    AdventureUtil.playerMessage(player, MessageManager.pluginError + ": <gray>There ain't no custom recipe");
                } else {
                    showPlayerBar(player, recipe);
                }
            }
        }
    }

    public void onBarInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        CookingPlayer cookingPlayer = cookingPlayerCache.remove(player);
        if (cookingPlayer != null) {
            proceedBarInteract(player, cookingPlayer);
        }
    }

    public void proceedBarInteract(Player player, CookingPlayer cookingPlayer) {
        cookingPlayer.cancel();
        Recipe loot = cookedRecipe.remove(player);
        Location cookingPot = cookingPotLocations.remove(player);
        stopSoundLoop();
        player.removePotionEffect(PotionEffectType.SLOW);

        if (!cookingPlayer.isSuccess()) {
            if (cookingPot != null) {
                playCookingResultSFX(cookingPot, build(ConfigManager.failureItem), false);
            }
            handleFailureResult(player);
            return;
        }

        if (!(loot instanceof DroppedItem droppedItem)) {
            return;
        }

        boolean perfect = Math.random() < perfectChance;
        String drop = loot.getCookedItems();

        if (perfect) {
            drop = loot.getCookedItems() + ConfigManager.perfectItemSuffix;
            AdventureUtil.playerMessage(player,MessageManager.infoPositive + MessageManager.cookingPerfect.replace("{recipe}", droppedItem.getNick()));
            if (!hasMastery(player, droppedItem.getKey())) {
                DataManager.handleMastery(player, droppedItem.getKey());
            }
        }

        if (cookingPot != null) {
            playCookingResultSFX(cookingPot, build(drop), true);
        }

        if (droppedItem.getSuccessActions() != null) {
            for (Action action : droppedItem.getSuccessActions()) {
                action.doOn(player, null);
            }
        }

        if (Competition.currentCompetition != null) {
            float score = ((float) droppedItem.getScore());
            Competition.currentCompetition.refreshData(player, score, perfect);
        }

        playerSound(player, Sound.Source.AMBIENT, key(ConfigManager.customNamespace, "cooking.done"), 1f, 1f);
        giveItem(player, drop, 1, true);
        sendSuccessTitle(player, droppedItem.getNick());

        DataManager.incrementRecipeCount(player);
    }

    private void handleFailureResult(Player player) {
        playerSound(player, Sound.Source.AMBIENT, key(ConfigManager.customNamespace, "fail"), 1f, 1f);
        AdventureUtil.playerTitle(
                player,
                ConfigManager.failureTitle[new Random().nextInt(ConfigManager.failureTitle.length)],
                ConfigManager.failureSubTitle[new Random().nextInt(ConfigManager.failureSubTitle.length)],
                ConfigManager.failureFadeIn,
                ConfigManager.failureFadeStay,
                ConfigManager.failureFadeOut
        );
        giveItem(player, ConfigManager.failureItem, 1, false);
    }


    private void sendSuccessTitle(Player player, String recipe) {
        AdventureUtil.playerTitle(
                player,
                ConfigManager.successTitle[new Random().nextInt(ConfigManager.successTitle.length)]
                        .replace("{recipe}", recipe)
                        .replace("{player}", player.getName()),
                ConfigManager.successSubTitle[new Random().nextInt(ConfigManager.successSubTitle.length)]
                        .replace("{recipe}", recipe)
                        .replace("{player}", player.getName()),
                ConfigManager.successFadeIn,
                ConfigManager.successFadeStay,
                ConfigManager.successFadeOut
        );
    }

    private void showPlayerBar(Player player, @Nullable Recipe recipe) {
        Layout layout;
        if (recipe != null && recipe.getLayout() != null) {
            layout = recipe.getLayout()[new Random().nextInt(recipe.getLayout().length)];
        } else {
            layout = (Layout) LayoutManager.LAYOUTS.values().toArray()[new Random().nextInt(LayoutManager.LAYOUTS.values().size())];
        }
        int speed;
        int timer;
        int time;
        if (recipe != null) {
            Difficulty difficulty = recipe.getDifficulty()[new Random().nextInt(recipe.getDifficulty().length)];
            speed = difficulty.speed();
            timer = difficulty.timer();
            time = recipe.getTime();
        } else {
            speed = new Random().nextInt(5);
            time = 10000;
            timer = 1;
        }
        if (speed < 1) {
            speed = 1;
        }
        Difficulty difficult = new Difficulty(timer, speed);

        if (Competition.currentCompetition != null) {
            Competition.currentCompetition.tryAddBossBarToPlayer(player);
        }

        CookingPlayer cookingPlayer = new CookingPlayer(System.currentTimeMillis() + time, player, layout, difficult, this);
        cookingPlayer.runTaskTimerAsynchronously(CustomCooking.plugin, 0, 1);
        cookingPlayerCache.put(player, cookingPlayer);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, time / 50, 3));
        playSoundLoop(player);
    }

    @Override
    public void onQuit(Player player) {
        cookedRecipe.remove(player);
    }

    @Nullable
    public CookingPlayer getCookingPlayer(Player player) {
        return cookingPlayerCache.get(player);
    }

    public void removeCookingPlayer(Player player) {
        cookingPlayerCache.remove(player);
    }

    private boolean isPlayerCooking(Player player) {
        CookingPlayer cookingPlayer = getCookingPlayer(player);
        return cookingPlayer != null;
    }


    public void playSoundLoop(Player player) {
        soundTask = new BukkitRunnable() {
            @Override
            public void run() {
                playerSound(player, Sound.Source.AMBIENT, key(ConfigManager.customNamespace, "cooking"), 1f, 1f);
            }
        };
        soundTask.runTaskTimerAsynchronously(CustomCooking.plugin, 0L, 60L);
    }

    public void stopSoundLoop() {
        if (soundTask != null) {
            soundTask.cancel();
        }
    }

    @Override
    public void onConsumeItem(PlayerItemConsumeEvent event) {
        ItemStack itemStack = event.getItem();
        NBTCompound nbtCompound = new NBTItem(itemStack).getCompound("CustomCooking");
        if (nbtCompound == null) {
            return;
        }
        String lootKey = nbtCompound.getString("id");
        Recipe recipe = RECIPES.get(lootKey);
        if (recipe == null) {
            return;
        }
        if (!(recipe instanceof DroppedItem droppedItem)) {
            return;
        }
        final Player player = event.getPlayer();
        if (droppedItem.getConsumeActions() != null) {
            for (Action action : droppedItem.getConsumeActions()) {
                action.doOn(player, null);
            }
        }
    }
}