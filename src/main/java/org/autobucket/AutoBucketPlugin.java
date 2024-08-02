package org.autobucket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;
import org.autobucket.modules.AutoBucketModule;

// Main plugin class for the AutoBucket functionality
public class AutoBucketPlugin extends Plugin {
    // Logger for the plugin to output information and errors
    private static final Logger LOGGER = LogManager.getLogger("AutoBucketPlugin");

    // Instance of the AutoBucketModule
    private final AutoBucketModule autoBucketModule = new AutoBucketModule();

    // Method called when the plugin is being loaded
    @Override
    public void onLoad() {
        LOGGER.info("Loading AutoBucket Plugin...");

        // Register the AutoBucketModule with the RusherHack API
        RusherHackAPI.getModuleManager().registerFeature(autoBucketModule);

        LOGGER.info("AutoBucket Plugin loaded successfully.");
    }

    // Method called when the plugin is being unloaded
    @Override
    public void onUnload() {
        LOGGER.info("Unloading AutoBucket Plugin...");

        LOGGER.info("AutoBucket Plugin unloaded.");
    }
}