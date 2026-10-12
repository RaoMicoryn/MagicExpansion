package io.Yomicer.magicExpansion.items.groups;

import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Hidden container: only used to hold sub-groups, not shown on the Slimefun Guide home page
 */
public class HiddenNestedItemGroup extends NestedItemGroup {

    public HiddenNestedItemGroup(NamespacedKey key, ItemStack item) {
        super(key, item);
    }

    @Override
    public boolean isVisible(Player player, PlayerProfile profile, SlimefunGuideMode mode) {
        return false;
    }
}