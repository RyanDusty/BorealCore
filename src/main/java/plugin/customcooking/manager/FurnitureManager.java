package plugin.customcooking.manager;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.customcooking.CustomCooking;
import plugin.customcooking.gui.InventoryPopulator;
import plugin.customcooking.listener.FurnitureListener;
import plugin.customcooking.minigame.Function;
import plugin.customcooking.util.AdventureUtil;
import plugin.customcooking.util.FakeItemUtil;

import java.util.*;

import static net.kyori.adventure.key.Key.key;
import static plugin.customcooking.configs.ConfigManager.splashTime;
import static plugin.customcooking.util.AdventureUtil.playerSound;
import static plugin.customcooking.util.InventoryUtil.build;

public class FurnitureManager extends Function {

    private final FurnitureListener furnitureListener;
    private final Map<Player, Long> cooldowns;

    private Set<Location> activeFXLocations = new HashSet<>();
    private Map<Location, BukkitTask> activeFXTasks = new HashMap<>();

    public FurnitureManager() {
        this.furnitureListener = new FurnitureListener(this);
        this.cooldowns = new HashMap<>();
        load();
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this.furnitureListener, CustomCooking.plugin);
    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(this.furnitureListener);
    }

    public void onFurnitureInteract(FurnitureInteractEvent event) {
        Player player = event.getPlayer();
        CustomFurniture clickedFurniture = event.getFurniture();

        // Check if the clicked block is an unlit cookingpot
        if (clickedFurniture.getId().equals("cooking_pot_unlit")) {
            if (!cooldowns.containsKey(player) || (System.currentTimeMillis() - cooldowns.get(player) >= 2000)) {
                // Set a cooldown of 2 seconds
                cooldowns.put(player, System.currentTimeMillis());
                // Check if the player right-clicked with flint and steel
                if (player.getInventory().getItemInMainHand().getType() == Material.FLINT_AND_STEEL) {

                    ItemFrame unlitpot = (ItemFrame) Objects.requireNonNull(clickedFurniture).getArmorstand();
                    Rotation rot = unlitpot.getRotation();
                    ItemFrame litpot = (ItemFrame) CustomFurniture.spawnPreciseNonSolid("cooking_pot_lit", unlitpot.getLocation()).getArmorstand();
                    litpot.setRotation(rot);

                    // Replace the furniture block with the lit furniture
                    clickedFurniture.remove(false);
                    unlitpot.getLocation().getBlock().setType(Material.BARRIER);

                    AdventureUtil.playerMessage(player, "<grey>[<green><bold>!</bold><grey>] <green>You lit up the pot!");
                    playCookingPotFX(clickedFurniture.getArmorstand().getLocation());
                } else {
                    AdventureUtil.playerMessage(player, "<grey>[<red><bold>!</bold><grey>] <red>You can't cook in an cold pot.. try heating it up");
                }
            } else {
                AdventureUtil.playerMessage(player, "<grey>[<red><bold>!</bold><grey>] <red>You need to wait " + ((2000 - (System.currentTimeMillis() - cooldowns.get(player))) / 1000) + " seconds before interacting with that again.");
            }
        } else if (clickedFurniture.getId().equals("cooking_pot_lit")) {
            playCookingPotFX(clickedFurniture.getArmorstand().getLocation());
            InventoryPopulator.getRecipeBook(clickedFurniture, player).open(player);
        }
    }

    public void onFurnitureBreak(FurnitureBreakEvent event) {
        CustomFurniture clickedFurniture = event.getFurniture();

        if (clickedFurniture.getId().equals("cooking_pot_lit")) {
            cancelCookingPotFX(clickedFurniture.getArmorstand().getLocation());
        }
    }

    public static void ingredientsSFX(Player player, List<String> ingredients, CustomFurniture clickedFurniture) {
        Location loc = clickedFurniture.getArmorstand().getLocation();
        spawnNextIngredient(loc, player, ingredients, 0); // Start spawning ingredients from index 0
    }

    private static void spawnNextIngredient(Location loc, Player player, List<String> ingredients, int currentIndex) {
        if (currentIndex >= ingredients.size()) {
            // All ingredients have been spawned
            return;
        }

        String ingredient = ingredients.get(currentIndex);
        String[] parts = ingredient.split(":");

        if (parts[0].endsWith("*")) {
            parts[0] = parts[0].replaceAll("\\*", "");
        }

        spawnFakeIngredientItem(loc, parts[0], () -> {
            // After the fake ingredient is removed, spawn the next ingredient
            spawnNextIngredient(loc, player, ingredients, currentIndex + 1);
        });
        spawnSplashItem(loc);
        Random random = new Random();
        playerSound(player, net.kyori.adventure.sound.Sound.Source.AMBIENT, key("customcooking", "ingredient"+ random.nextInt(3) + 1), 1f, 1f);
    }

    private static void spawnFakeIngredientItem(Location loc, String ingredient, Runnable onComplete) {
        // Create a dropped item entity at the specified location
        Item itemEntity = loc.getWorld().dropItem(loc.add(0,1,0), build(ingredient));
        itemEntity.setCanPlayerPickup(false);

        // Schedule a task to remove the item entity after a set time (e.g., 10 seconds)
        int removalDelayTicks = 2 * 20; // 2 seconds (20 ticks per second)
        new BukkitRunnable() {
            @Override
            public void run() {
                itemEntity.remove(); // Remove the item entity from the world
                onComplete.run(); // Invoke the callback when the removal is complete
            }
        }.runTaskLater(CustomCooking.plugin, removalDelayTicks);
    }

    private static void spawnFakeRecipeItem(Location loc, ItemStack cookedStack) {
        // Create a dropped item entity at the specified location
        Item itemEntity = loc.getWorld().dropItem(loc.add(0,1,0), cookedStack);
        itemEntity.setCanPlayerPickup(false);
        itemEntity.setGravity(false);

        // Schedule a task to remove the item entity after a set time (e.g., 10 seconds)
        int removalDelayTicks = 2 * 20; // 2 seconds (20 ticks per second)
        new BukkitRunnable() {
            @Override
            public void run() {
                itemEntity.remove(); // Remove the item entity from the world
            }
        }.runTaskLater(CustomCooking.plugin, removalDelayTicks);
    }


    private static void spawnSplashItem(Location loc) {

        // Create an ArmorStand entity at the specified location
        ArmorStand armorStand = (ArmorStand) loc.getWorld().spawnEntity(loc.subtract(0,0.1, 0), EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);

        // Create a fake ingredient ItemStack
        ItemStack splashItem = build("pot_effect");

        // Set the fake ingredient ItemStack as the ArmorStand's helmet item
        armorStand.setItem(EquipmentSlot.HEAD, splashItem);

        new BukkitRunnable() {
            @Override
            public void run() {
                armorStand.remove(); // Remove the item entity from the world
            }
        }.runTaskLater(CustomCooking.plugin, splashTime);
    }

    public void playCookingResult(Location loc, Boolean success) {
        if (success) {
            // Particles: composter
            loc.getWorld().spawnParticle(Particle.COMPOSTER, loc.add(0, 1.25, 0), 15, 0.5, 0.5, 0.5);
        }
        else {
            // Particles: squid_ink
            loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.add(0, 1.25, 0), 15, 0.5, 0.5, 0.5);
        }
    }

    public void playCookingPreview(Location loc, ItemStack item) {
        // Particles: crit
        loc.getWorld().spawnParticle(Particle.CRIT, loc.add(0, 1.5, 0), 15, 0.25, 0.25, 0.25, 0.2);
        // Spawn recipe item preview
        spawnFakeRecipeItem(loc, item);
    }


    // Ambient Cooking pot SFX

    public void playCookingPotFX(Location location) {
        if (activeFXTasks.containsKey(location)) {
            return; // FX is already running at this location
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                playAmbientEffects(location);
            }
        }.runTaskTimerAsynchronously(CustomCooking.plugin, 0L, 20L);

        activeFXTasks.put(location, task);
    }

    public void playAmbientEffects(Location loc) {
        // Particles: flame
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0.25, 0.25, 0.25, 0.01);

        // Particles: campfire_cosy_smoke
        loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 2, 0.25, 0.25, 0.25, 0.01, null, true);

        // Sound: block.fire.ambient
        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);

        // Sound: block.lava.ambient
        loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 1f, 1f);
    }

    public void cancelCookingPotFX(Location location) {
        BukkitTask task = activeFXTasks.remove(location);
        if (task != null) {
            task.cancel();
        }
    }
}

