package me.lordsaad.wizardry.spells.modules.modifiers;

import me.lordsaad.wizardry.api.modules.IModule;
import me.lordsaad.wizardry.spells.modules.ModuleType;
import net.minecraft.nbt.NBTTagCompound;

public class ModuleArea implements IModule {
    private IModule[] modules;

    public ModuleArea(IModule... modules) {
        this.modules = modules;
    }

    @Override
    public ModuleType getType() {
        return ModuleType.MODIFIER;
    }

    @Override
    public NBTTagCompound getModuleData() {
        return null;
    }
}