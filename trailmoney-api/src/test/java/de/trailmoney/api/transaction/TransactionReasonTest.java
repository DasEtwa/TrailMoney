package de.trailmoney.api.transaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionReasonTest {
    @Test
    void pluginReasonUsesPluginActorPrefix() {
        TransactionReason reason = TransactionReason.plugin("TrailRewards", "quest_reward");

        assertEquals("quest_reward", reason.key());
        assertEquals("plugin:TrailRewards", reason.actor());
    }
}
