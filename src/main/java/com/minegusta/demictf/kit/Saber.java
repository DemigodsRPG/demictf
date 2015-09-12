package com.minegusta.demictf.kit;

import com.demigodsrpg.demigames.game.impl.util.ItemStackBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class Saber extends CaptureTheFlagKit {

    final ItemStack EXCALIBER = new ItemStackBuilder(Material.DIAMOND_SWORD).
            displayName(ChatColor.BLUE + "Excalibur").
            lore("The legendary sword of old.").
            enchant(Enchantment.KNOCKBACK, 1).
            enchant(Enchantment.DAMAGE_ALL, 1).
            durability((short) 300).
            itemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES).
            build();

    public Saber() {
        super("Saber");
        addItem(0, EXCALIBER);
    }
}
