package org.autobucket.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.NumberSetting;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.utils.InventoryUtils;

import java.util.List;
import java.util.function.Predicate;

/**
 * AutoBucket module for automatically capturing various entities in buckets.
 * This module can capture entities such as Axolotls, Cod, Pufferfish, Salmon, Tadpoles, and Tropical Fish.
 * It also features rapid capture functionality with customizable cooldown settings.
 */
public class AutoBucketModule extends ToggleableModule {

    // Instance of the Minecraft game for accessing player and world data
    private final Minecraft minecraft = Minecraft.getInstance();

    // Settings to specify which entities should be captured
    private final BooleanSetting targetAxolotls = new BooleanSetting("Axolotls", "Bucket Axolotls", true);
    private final BooleanSetting targetCod = new BooleanSetting("Cod", "Bucket Cod", true);
    private final BooleanSetting targetPufferfish = new BooleanSetting("Pufferfish", "Bucket Pufferfish", true);
    private final BooleanSetting targetSalmon = new BooleanSetting("Salmon", "Bucket Salmon", true);
    private final BooleanSetting targetTadpoles = new BooleanSetting("Tadpoles", "Bucket Tadpoles", true);
    private final BooleanSetting targetTropicalFish = new BooleanSetting("TropicalFish", "Bucket Tropical Fish", true);

    // Rapid capture functionality settings
    private final BooleanSetting rapidCatch = new BooleanSetting("RapidCatch", "Enable rapid capture of entities", false);
    private final NumberSetting<Integer> rapidCooldown = new NumberSetting<>("RapidCooldown", "Cooldown between rapid captures (ticks)", 1, 1, 20)
            .setVisibility(rapidCatch::getValue);

    // Tick counter to throttle capture attempts
    private int tick;

    /**
     * Constructor for the AutoBucket module.
     * Initializes the settings and registers them with the module.
     */
    public AutoBucketModule() {
        super("AutoBucket", "Automatically buckets certain entities", ModuleCategory.MISC);

        // Register settings for the module
        this.registerSettings(
                this.rapidCatch,
                this.rapidCooldown,
                this.targetAxolotls,
                this.targetCod,
                this.targetPufferfish,
                this.targetSalmon,
                this.targetTadpoles,
                this.targetTropicalFish
        );
    }

    /**
     * Called when the module is enabled.
     * Subscribes to necessary events.
     */
    @Override
    public void onEnable() {
        RusherHackAPI.getEventBus().subscribe(this);
    }

    /**
     * Called when the module is disabled.
     * Unsubscribes from events.
     */
    @Override
    public void onDisable() {
        RusherHackAPI.getEventBus().unsubscribe(this);
    }

    /**
     * Event handler for the update event.
     * Throttles capture attempts using a tick counter.
     *
     * @param event The update event.
     */
    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (tick > 0) {
            tick--;
            return;
        }

        if (this.isToggled()) {
            autoBucketEntities(minecraft.player);

            // Use normal cooldown for regular capture and sub-setting for RapidCatch
            tick = rapidCatch.getValue() ? rapidCooldown.getValue() : 40;
        }
    }

    /**
     * Attempts to capture nearby entities using available water buckets.
     *
     * @param player The player entity performing the capture.
     */
    private void autoBucketEntities(Entity player) {
        // Predicate to filter bucketable entities based on settings
        Predicate<Entity> bucketableEntities = entity -> {
            if (entity instanceof Axolotl && targetAxolotls.getValue()) return true;
            if (entity instanceof Cod && targetCod.getValue()) return true;
            if (entity instanceof Pufferfish && targetPufferfish.getValue()) return true;
            if (entity instanceof Salmon && targetSalmon.getValue()) return true;
            if (entity instanceof Tadpole && targetTadpoles.getValue()) return true;
            return entity instanceof TropicalFish && targetTropicalFish.getValue();
        };

        // Find entities within a 5 block radius
        assert minecraft.level != null;
        List<Entity> entities = minecraft.level.getEntities((Entity) null, player.getBoundingBox().inflate(5.0), bucketableEntities);

        // Attempt to capture each entity
        for (Entity entity : entities) {
            if (useMainHandBucketOnEntity(entity) || useHotbarBucketOnEntity(entity) || useInventoryBucketOnEntity(entity)) {
                tick = rapidCatch.getValue() ? rapidCooldown.getValue() : 40;  // Set cooldown
                break;  // Stop after successfully capturing one entity
            }
        }
    }

    /**
     * Attempts to use the water bucket in the main hand to capture an entity.
     *
     * @param entity The entity to capture.
     * @return True if the entity was successfully captured, false otherwise.
     */
    private boolean useMainHandBucketOnEntity(Entity entity) {
        if (isHoldingWaterBucket()) {
            assert minecraft.player != null;
            minecraft.player.connection.send(ServerboundInteractPacket.createInteractionPacket(entity, false, InteractionHand.MAIN_HAND));
            return hasCapturedEntity(entity, minecraft.player.getMainHandItem());
        }
        return false;
    }

    /**
     * Checks if the player is holding a water bucket in their main hand.
     *
     * @return True if holding a water bucket, false otherwise.
     */
    private boolean isHoldingWaterBucket() {
        ItemStack mainHandItem = minecraft.player != null ? minecraft.player.getMainHandItem() : ItemStack.EMPTY;
        return mainHandItem.getItem() == Items.WATER_BUCKET;
    }

    /**
     * Attempts to use a water bucket from the hotbar to capture an entity.
     *
     * @param entity The entity to capture.
     * @return True if the entity was successfully captured, false otherwise.
     */
    private boolean useHotbarBucketOnEntity(Entity entity) {
        int slot = findWaterBucketInHotbar();
        return slot != -1 && useBucketOnEntityFromSlot(entity, slot);
    }

    /**
     * Attempts to use a water bucket from the inventory to capture an entity.
     *
     * @param entity The entity to capture.
     * @return True if the entity was successfully captured, false otherwise.
     */
    private boolean useInventoryBucketOnEntity(Entity entity) {
        int slot = findWaterBucketInInventory();
        if (slot != -1) {
            int hotbarSlot = findEmptyHotbarSlot();
            if (hotbarSlot != -1) {
                InventoryUtils.swapSlots(slot, hotbarSlot);  // Using InventoryUtils to swap slots
                boolean result = useBucketOnEntityFromSlot(entity, hotbarSlot);
                InventoryUtils.swapSlots(slot, hotbarSlot);  // Swap back to the original slot
                return result;
            }
        }
        return false;
    }

    /**
     * Uses a water bucket from a specified slot to capture an entity.
     *
     * @param entity The entity to capture.
     * @param slot The slot of the water bucket.
     * @return True if the entity was successfully captured, false otherwise.
     */
    private boolean useBucketOnEntityFromSlot(Entity entity, int slot) {
        int originalSlot = minecraft.player != null ? minecraft.player.getInventory().selected : -1;

        if (originalSlot != -1) {
            minecraft.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            minecraft.player.connection.send(ServerboundInteractPacket.createInteractionPacket(entity, false, InteractionHand.MAIN_HAND));
            minecraft.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
            ItemStack currentItem = minecraft.player.getInventory().getItem(slot);
            return hasCapturedEntity(entity, currentItem);
        }
        return false;
    }

    /**
     * Checks if the given item stack has captured the specified entity.
     *
     * @param entity The entity to check.
     * @param itemStack The item stack to check.
     * @return True if the item stack has captured the entity, false otherwise.
     */
    private boolean hasCapturedEntity(Entity entity, ItemStack itemStack) {
        if (entity instanceof Axolotl && itemStack.getItem() == Items.AXOLOTL_BUCKET) return true;
        if (entity instanceof Cod && itemStack.getItem() == Items.COD_BUCKET) return true;
        if (entity instanceof Pufferfish && itemStack.getItem() == Items.PUFFERFISH_BUCKET) return true;
        if (entity instanceof Salmon && itemStack.getItem() == Items.SALMON_BUCKET) return true;
        if (entity instanceof Tadpole && itemStack.getItem() == Items.TADPOLE_BUCKET) return true;
        return entity instanceof TropicalFish && itemStack.getItem() == Items.TROPICAL_FISH_BUCKET;
    }

    /**
     * Finds the slot of a water bucket in the hotbar.
     *
     * @return The slot index of the water bucket, or -1 if not found.
     */
    private int findWaterBucketInHotbar() {
        if (minecraft.player != null) {
            for (int i = 0; i < 9; i++) {
                if (minecraft.player.getInventory().getItem(i).getItem() == Items.WATER_BUCKET) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Finds the slot of a water bucket in the inventory.
     *
     * @return The slot index of the water bucket, or -1 if not found.
     */
    private int findWaterBucketInInventory() {
        if (minecraft.player != null) {
            for (int i = 9; i < 36; i++) {
                if (minecraft.player.getInventory().getItem(i).getItem() == Items.WATER_BUCKET) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Finds an empty slot in the hotbar.
     *
     * @return The slot index of an empty hotbar slot, or -1 if none are empty.
     */
    private int findEmptyHotbarSlot() {
        if (minecraft.player != null) {
            for (int i = 0; i < 9; i++) {
                if (minecraft.player.getInventory().getItem(i).isEmpty()) {
                    return i;
                }
            }
        }
        return -1;
    }
}
