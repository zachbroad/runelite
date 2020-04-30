package net.runelite.client.plugins.staker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("staker")
public interface StakerConfig extends Config {
    @ConfigItem(
            position = 1,
            keyName = "combat",
            name = "Combat Level Range",
            description = "How many combat levels would you like to search?"
    )
    default int combat() {
        return 7;
    }

    @ConfigItem(
            position = 2,
            keyName = "highlight",
            name = "Highlight Players",
            description = "Highlight players with the best odds."
    )
    default boolean highlight() {
        return true;
    }

    @ConfigItem(
            position = 3,
            keyName = "interval",
            name = "Scan Interval",
            description = "Seconds between searches."
    )
    default int interval() {
        return 5;
    }

    @ConfigItem(
            position = 4,
            keyName = "goodStake",
            name = "Good Stake",
            description = "How many levels is a \"good stake\"?"
    )
    default int goodStake() {
        return 5;
    }

    @ConfigItem(
            position = 5,
            keyName = "maxCombat",
            name = "Max Combat Level",
            description = "Hide players above this combat level."
    )
    default int maxCombat() {
        return 121;
    }
}
