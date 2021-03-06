package com.teamwizardry.wizardry.client.render.entity;

import com.teamwizardry.wizardry.common.entity.EntityFairy;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * Created by Demoniaque on 8/25/2016.
 */
public class RenderFairy extends RenderLiving<EntityFairy> {

	public RenderFairy(RenderManager renderManager, ModelBase modelBase) {
		super(renderManager, modelBase, 0.0f);
	}

	@Override
	public boolean canRenderName(EntityFairy entity) {
		return false;
	}

	@Override
	protected ResourceLocation getEntityTexture(@Nonnull EntityFairy entity) {
		return null;
	}
}
