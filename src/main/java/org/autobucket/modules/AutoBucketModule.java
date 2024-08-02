package org.autobucket.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.client.api.events.client.EventUpdate;

import java.util.List;
import java.util.function.Predicate;

public class AutoBucketModule extends ToggleableModule implements EventListener {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final BooleanSetting targetAxolotls;
    private final BooleanSetting targetCod;
    private final BooleanSetting targetPufferfish;
    private final BooleanSetting targetSalmon;
    private final BooleanSetting targetTadpoles;
    private final BooleanSetting targetTropicalFish;
    private int tick;

    public AutoBucketModule() {
        super("AutoBucket", ModuleCategory.MISC);
        // Initialize settings for each type of fish
        targetAxolotls = new BooleanSetting("Axolotls", "Bucket Axolotls", true);
        targetCod = new BooleanSetting("Cod", "Bucket Cod", true);
        targetPufferfish = new BooleanSetting("Pufferfish", "Bucket Pufferfish", true);
        targetSalmon = new BooleanSetting("Salmon", "Bucket Salmon", true);
        targetTadpoles = new BooleanSetting("Tadpoles", "Bucket Tadpoles", true);
        targetTropicalFish = new BooleanSetting("TropicalFish", "Bucket Tropical Fish", true);
        this.registerSettings(targetAxolotls, targetCod, targetPufferfish, targetSalmon, targetTadpoles, targetTropicalFish);
        tick = 0;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        RusherHackAPI.getEventBus().subscribe(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        RusherHackAPI.getEventBus().unsubscribe(this);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (tick > 0) {
            tick--;
            return;
        }

        ItemStack mainHandItem = minecraft.player.getMainHandItem();
        if (mainHandItem.getItem() == Items.WATER_BUCKET) {
            // Define which entities can be bucketed based on settings
            Predicate<Entity> bucketableEntities = entity ->
                    (entity instanceof Axolotl && targetAxolotls.getValue()) ||
                            (entity instanceof Cod && targetCod.getValue()) ||
                            (entity instanceof Pufferfish && targetPufferfish.getValue()) ||
                            (entity instanceof Salmon && targetSalmon.getValue()) ||
                            (entity instanceof Tadpole && targetTadpoles.getValue()) ||
                            (entity instanceof TropicalFish && targetTropicalFish.getValue());

            // Get nearby entities that can be bucketed
            List<Entity> entities = minecraft.level.getEntities((Entity) null, minecraft.player.getBoundingBox().inflate(5.0), bucketableEntities);

            for (Entity entity : entities) {
                if (interactWithEntity(entity)) {
                    tick = 20; // Set cooldown after successful interaction
                    break;
                }
            }
        }
    }

    private boolean interactWithEntity(Entity entity) {
        if (minecraft.player != null) {
            // Send interaction packet to the server
            minecraft.player.connection.send(ServerboundInteractPacket.createInteractionPacket(entity, false, InteractionHand.MAIN_HAND));

            // Check if the bucket state has changed
            ItemStack mainHandItem = minecraft.player.getMainHandItem();
            return mainHandItem.getItem() != Items.WATER_BUCKET;
        }
        return false;
    }
}