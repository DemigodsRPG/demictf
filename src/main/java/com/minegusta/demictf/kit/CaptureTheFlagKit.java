package com.minegusta.demictf.kit;

import com.demigodsrpg.demigames.kit.Kit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CaptureTheFlagKit implements Kit {

    String name;
    ItemStack[] armor;
    ItemStack[] contents;
    PotionEffect[] effects;

    public CaptureTheFlagKit(String name) {
        this.name = name;
        armor = new ItemStack[4];
        contents = new ItemStack[36];
        effects = new PotionEffect[0];
    }

    public CaptureTheFlagKit addItem(int slot, ItemStack item) {
        contents[slot] = item;
        return this;
    }

    public CaptureTheFlagKit addArmor(int slot, ItemStack item) {
        armor[slot] = item;
        return this;
    }

    public CaptureTheFlagKit addEffect(PotionEffect effect) {
        List<PotionEffect> current = new ArrayList<>(Arrays.asList(effects));
        effects = new PotionEffect[current.size() + 1];
        for (int i = 0; i < current.size(); i++) {
            effects[i] = current.get(i);
        }
        effects[current.size() + 1] = effect;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ItemStack[] getContents() {
        return contents;
    }

    @Override
    public ItemStack[] getArmor() {
        return armor;
    }

    @Override
    public PotionEffect[] getPotionEffects() {
        return effects;
    }

    @Override
    public double getHealthScale() {
        return 20;
    }

    @Override
    public double getMaxHealth() {
        return 20;
    }

    @Override
    public double getHealth() {
        return 20;
    }

    @Override
    public int getMaximumAir() {
        return 20;
    }

    @Override
    public int getRemainingAir() {
        return 20;
    }

    @Override
    public int getFoodLevel() {
        return 20;
    }

    @Override
    public float getExhaustion() {
        return 20;
    }

    @Override
    public float getSaturation() {
        return 20;
    }

    @Override
    public int getFireTicks() {
        return 0;
    }

    @Override
    public int getTotalExperience() {
        return 0;
    }
}
