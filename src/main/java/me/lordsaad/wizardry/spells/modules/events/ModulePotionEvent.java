package me.lordsaad.wizardry.spells.modules.events;

import me.lordsaad.wizardry.api.modules.IModule;
import me.lordsaad.wizardry.spells.modules.ModuleType;
import net.minecraft.nbt.NBTTagCompound;

public class ModulePotionEvent implements IModule {
    private IModule[] modules;

    public ModulePotionEvent(IModule... modules) {
        this.modules = modules;
    }

    @Override
    public ModuleType getType() {
        return ModuleType.EFFECT;
    }

    @Override
    public NBTTagCompound getModuleData() {
        return null;
    }
}