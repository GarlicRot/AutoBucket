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
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.client.api.events.client.EventUpdate;

import java.util.List;
import java.util.function.Predicate;

/**
 * AutoBucket module for automatically capturing various entities in buckets.
 * This module can capture entities such as Axolotls, Cod, Pufferfish, Salmon, Tadpoles, and Tropical Fish.
 */
public class AutoBucketModule extends ToggleableModule implements EventListener {
    private final Minecraft minecraft = Minecraft.getInstance();

    // Settings for automatically capturing entities
    private final BooleanSetting targetAxolotls;
    private final BooleanSetting targetCod;
    private final BooleanSetting targetPufferfish;
    private final BooleanSetting targetSalmon;
    private final BooleanSetting targetTadpoles;
    private final BooleanSetting targetTropicalFish;

    // Tick counter to throttle capture attempts
    private int tick;

    /**
     * Constructor for the AutoBucket module.
     * Initializes the settings and registers them with the module.
     */
    public AutoBucketModule() {
        super("AutoBucket", ModuleCategory.MISC);

        // Settings to target specific entities
        targetAxolotls = new BooleanSetting("Axolotls", "Bucket Axolotls", true);
        targetCod = new BooleanSetting("Cod", "Bucket Cod", true);
        targetPufferfish = new BooleanSetting("Pufferfish", "Bucket Pufferfish", true);
        targetSalmon = new BooleanSetting("Salmon", "Bucket Salmon", true);
        targetTadpoles = new BooleanSetting("Tadpoles", "Bucket Tadpoles", true);
        targetTropicalFish = new BooleanSetting("TropicalFish", "Bucket Tropical Fish", true);

        // Add target settings as settings
        this.registerSettings(targetAxolotls, targetCod, targetPufferfish, targetSalmon, targetTadpoles, targetTropicalFish);

        tick = 0;
    }

    /**
     * Called when the module is enabled.
     * Subscribes to necessary events.
     */
    @Override
    public void onEnable() {
        super.onEnable();
        RusherHackAPI.getEventBus().subscribe(this);
    }

    /**
     * Called when the module is disabled.
     * Unsubscribes from events.
     */
    @Override
    public void onDisable() {
        super.onDisable();
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
            autoBucketEntities();
        }
    }

    /**
     * Attempts to capture nearby entities using available water buckets.
     */
    private void autoBucketEntities() {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        // Predicate to filter bucketable entities based on settings
        Predicate<Entity> bucketableEntities = entity ->
                (entity instanceof Axolotl && targetAxolotls.getValue()) ||
                        (entity instanceof Cod && targetCod.getValue()) ||
                        (entity instanceof Pufferfish && targetPufferfish.getValue()) ||
                        (entity instanceof Salmon && targetSalmon.getValue()) ||
                        (entity instanceof Tadpole && targetTadpoles.getValue()) ||
                        (entity instanceof TropicalFish && targetTropicalFish.getValue());

        // Find entities within a 5 block radius
        List<Entity> entities = minecraft.level.getEntities((Entity) null, minecraft.player.getBoundingBox().inflate(5.0), bucketableEntities);

        for (Entity entity : entities) {
            if (useMainHandBucketOnEntity(entity) || useHotbarBucketOnEntity(entity) || useInventoryBucketOnEntity(entity)) {
                tick = 20;  // Throttle capture attempts
                break;
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
        assert minecraft.player != null;
        ItemStack mainHandItem = minecraft.player.getMainHandItem();
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
                InventoryUtils.swapSlots(slot, hotbarSlot);
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
        assert minecraft.player != null;
        int originalSlot = minecraft.player.getInventory().selected;

        // Swap to the water bucket slot
        minecraft.player.connection.send(new ServerboundSetCarriedItemPacket(slot));

        // Send interaction packet
        minecraft.player.connection.send(ServerboundInteractPacket.createInteractionPacket(entity, false, InteractionHand.MAIN_HAND));

        // Restore the original slot
        minecraft.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));

        // Check if the interaction was successful
        ItemStack currentItem = minecraft.player.getInventory().getItem(slot);
        return hasCapturedEntity(entity, currentItem);
    }

    /**
     * Checks if the given item stack has captured the specified entity.
     *
     * @param entity The entity to check.
     * @param itemStack The item stack to check.
     * @return True if the item stack has captured the entity, false otherwise.
     */
    private boolean hasCapturedEntity(Entity entity, ItemStack itemStack) {
        return (entity instanceof Axolotl && itemStack.getItem() == Items.AXOLOTL_BUCKET) ||
                (entity instanceof Cod && itemStack.getItem() == Items.COD_BUCKET) ||
                (entity instanceof Pufferfish && itemStack.getItem() == Items.PUFFERFISH_BUCKET) ||
                (entity instanceof Salmon && itemStack.getItem() == Items.SALMON_BUCKET) ||
                (entity instanceof Tadpole && itemStack.getItem() == Items.TADPOLE_BUCKET) ||
                (entity instanceof TropicalFish && itemStack.getItem() == Items.TROPICAL_FISH_BUCKET);
    }

    /**
     * Finds the slot of a water bucket in the hotbar.
     *
     * @return The slot index of the water bucket, or -1 if not found.
     */
    private int findWaterBucketInHotbar() {
        for (int i = 0; i < 9; i++) {
            assert minecraft.player != null;
            if (minecraft.player.getInventory().getItem(i).getItem() == Items.WATER_BUCKET) {
                return i;
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
        for (int i = 9; i < 36; i++) {
            assert minecraft.player != null;
            if (minecraft.player.getInventory().getItem(i).getItem() == Items.WATER_BUCKET) {
                return i;
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
        for (int i = 0; i < 9; i++) {
            assert minecraft.player != null;
            if (minecraft.player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
