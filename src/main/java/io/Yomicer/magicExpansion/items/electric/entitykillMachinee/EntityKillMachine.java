package io.Yomicer.magicExpansion.items.electric.entitykillMachinee;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Comparator;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.Utils.doGlow;

public class EntityKillMachine extends SlimefunItem implements EnergyNetComponent {


    private final int power;
    private final int craftPerTick;
    private final EntityType entityType;
    private final int[] pinkBorder = {0,1,2,3,4,5,6,7,8,9,17,18,19,20,21,22,23,24,25,26};
    private final int[] blueBorder = {10,11,12, 14,15,16};
    private final String name;

    public EntityKillMachine(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int power, int craftPerTick, EntityType entityType, String name) {
        super(category, item, recipeType, recipe);

        this.power = power;
        this.craftPerTick = craftPerTick;
        this.entityType = entityType;
        this.name = name;

        constructMenu(name+" Suppressor");
    }



    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, SlimefunBlockData data) {
                EntityKillMachine.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return true;
            }
        });
    }

    // F2: Entity scan throttle counter. The getNearbyEntities(19,19,19) cleanup only runs once every 20 ticks
    private int scanTickCounter = 0;

    protected void tick(Block block) {

        BlockMenu menu = StorageCacheUtils.getMenu(block.getLocation());

        // F3: Combine getCharge calls in the same tick into one local variable to avoid repeated lookups
        int charge = getCharge(block.getLocation());

        // F1: The energy check sits outside hasViewer(). When energy is too low, no entities are removed whether or not anyone is watching,
        // which stops the loophole of removing entities for free when nobody has the UI open. The UI message is only updated when someone is viewing it
        if (charge < getEnergyConsumption()) {
            if (menu != null && menu.hasViewer()) {
                menu.addItem(13, new CustomItemStack(new ItemStack (Material.GHAST_TEAR), "§cNot enough energy"),
                        (p, slot, item, action) -> false);
            }
            return;
        }

        // The UI status message is still updated every tick (only when someone is viewing), reusing the charge variable from F3
        if (menu != null && menu.hasViewer()) {
            menu.addItem(13, new CustomItemStack(new ItemStack(Material.BLUE_BED), "§bSuppressing",
                            "§bType: §e" + name,
                            "§bEnergy use: §e" + getEnergyConsumption() * 2 + " J/s",
                            "§bStored energy: §e" + charge + " J"),
                    (p, slot, item, action) -> false);

        }

        // F2: Entity scan throttle. The cleanup only runs once every 20 ticks; UI updates are not affected
        scanTickCounter++;
        if (scanTickCounter < 20) {
            return;
        }
        scanTickCounter = 0;

        Location center = block.getLocation();
        int radius = 19;

        Runnable removeEntitiesTask = () -> {
            for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (entity.getType() == entityType) {
                    entity.remove();
                }
            }
        };

        if (Bukkit.isPrimaryThread()) {
            // Already on the main thread, run it directly
            removeEntitiesTask.run();
        } else {
            // On an async thread, schedule it to run on the main thread
            Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), removeEntitiesTask);
        }



    }



    private void constructMenu(String displayName) {
        new BlockMenuPreset(getId(), displayName) {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public boolean canOpen(@Nonnull Block b, @Nonnull Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(),
                        Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow itemTransportFlow) {
                return new int[0];
            }
        };
    }

    protected void constructMenu(BlockMenuPreset preset) {

        for (int i : pinkBorder ) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack (Material.PINK_STAINED_GLASS_PANE)), " "),
                    (p, slot, item, action) -> false);
        }

        for (int i : blueBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack (Material.LIGHT_BLUE_CANDLE)), " "),
                    (p, slot, item, action) -> false);
        }

        preset.addItem(13, new CustomItemStack(new ItemStack (Material.GHAST_TEAR), " "),
                (p, slot, item, action) -> false);

    }


    private int getEnergyConsumption() {
        return craftPerTick;
    }


    @Override
    public @NotNull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return power;
    }
}
