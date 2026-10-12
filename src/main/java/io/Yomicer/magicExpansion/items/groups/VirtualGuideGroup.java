package io.Yomicer.magicExpansion.items.groups;

import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Virtual level group: only used as a GuideHistory entry, so the back button works step by step in the 3-level menu
 * It isn't attached to any parent group and doesn't show in the normal group page. When clicked, openItemGroup calls its open() to open the matching custom-drawn page
 */
public class VirtualGuideGroup extends FlexItemGroup {

    private final GuidePage page;

    public VirtualGuideGroup(NamespacedKey key, ItemStack item, GuidePage page) {
        super(key, item);
        this.page = page;
    }

    @Override
    public boolean isVisible(Player player, PlayerProfile profile, SlimefunGuideMode mode) {
        return false;
    }

    @Override
    public void open(Player player, PlayerProfile profile, SlimefunGuideMode mode) {
        if (page != null) {
            page.open(player, profile, mode);
        }
    }

    @FunctionalInterface
    public interface GuidePage {
        void open(Player player, PlayerProfile profile, SlimefunGuideMode mode);
    }
}