package org.autobucket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;
import org.autobucket.modules.AutoBucketModule;

/**
 * Main plugin class for the AutoBucket functionality.
 * Responsible for initializing and registering the AutoBucket module.
 */
public class AutoBucketPlugin extends Plugin {
    // Logger for the plugin to output information and errors
    private static final Logger LOGGER = LogManager.getLogger("AutoBucketPlugin");

    // Instance of the AutoBucketModule
    private final AutoBucketModule autoBucketModule = new AutoBucketModule();

    /**
     * Called when the plugin is loaded. Registers the AutoBucket module.
     */
    @Override
    public void onLoad() {
        LOGGER.info("Loading AutoBucket Plugin...");

        // Register the AutoBucket module with the RusherHack API
        RusherHackAPI.getModuleManager().registerFeature(autoBucketModule);

        LOGGER.info("AutoBucket Plugin loaded successfully.");
    }

    /**
     * Called when the plugin is unloaded. Currently, no specific unloading actions are performed.
     */
    @Override
    public void onUnload() {
        LOGGER.info("Unloading AutoBucket Plugin...");

        // Unregister or perform any necessary cleanup here if needed

        LOGGER.info("AutoBucket Plugin unloaded.");
    }
}
