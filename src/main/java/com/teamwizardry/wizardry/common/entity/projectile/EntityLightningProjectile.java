package com.teamwizardry.wizardry.common.entity.projectile;

import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.SEED;
import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.LOOK;

import com.teamwizardry.librarianlib.features.utilities.client.ClientRunnable;
import com.teamwizardry.wizardry.Wizardry;
import com.teamwizardry.wizardry.api.spell.SpellData;
import com.teamwizardry.wizardry.api.spell.SpellRing;
import com.teamwizardry.wizardry.api.spell.attribute.AttributeRange;
import com.teamwizardry.wizardry.api.spell.attribute.AttributeRegistry;
import com.teamwizardry.wizardry.api.util.PosUtils;
import com.teamwizardry.wizardry.api.util.RandUtil;
import com.teamwizardry.wizardry.api.util.RandUtilSeed;
import com.teamwizardry.wizardry.common.module.effects.ModuleEffectLightning;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class EntityLightningProjectile extends EntitySpellProjectile {
	public static final DataParameter<NBTTagCompound> CHILD_RING = EntityDataManager.createKey(EntitySpellProjectile.class, DataSerializers.COMPOUND_TAG);
	
	public EntityLightningProjectile(World world) {
		super(world);
	}

	public EntityLightningProjectile(World world, SpellRing spellRing, SpellRing childRing, SpellData spellData, float dist, float speed, float gravity) {
		super(world, spellRing, spellData, dist, speed, gravity, true);
		setChildRing(childRing);
	}

	protected SpellRing getChildRing() {
		NBTTagCompound compound = getDataManager().get(CHILD_RING);
		return SpellRing.deserializeRing(compound);
	}

	protected void setChildRing(SpellRing ring) {
		getDataManager().set(CHILD_RING, ring.serializeNBT());
		getDataManager().setDirty(CHILD_RING);
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();
		if (isDead) return;
		
		SpellData data = getSpellData();
		SpellRing spellRing = getSpellRing();
		SpellRing childRing = getChildRing();
		
		double range = childRing.getAttributeValue(AttributeRegistry.RANGE, data);
		double potency = childRing.getAttributeValue(AttributeRegistry.POTENCY, data);
		double duration = childRing.getAttributeValue(AttributeRegistry.DURATION, data);
		double maxPotency = childRing.getModule().getAttributeRanges().get(AttributeRegistry.POTENCY).max;
		
		if (data == null || spellRing == null || childRing == null)
		{
			setDead();
			world.removeEntity(this);
			return;
		}

		Vec3d dir = data.getData(LOOK);
		
		if (!world.isRemote)
		{
			long seed = RandUtil.nextLong(100, 100000);
			data.addData(SEED, seed);
			
			RandUtilSeed rand = new RandUtilSeed(seed);
			if (rand.nextDouble(maxPotency) < potency)
			{
				float u = rand.nextFloat();
				float v = rand.nextFloat();
				float pitch = (float) (180 * Math.acos(2*u - 1) / Math.PI);
				float yaw = (float) (2 * Math.PI * v);
				
				Vec3d to = dir.rotatePitch(pitch).rotateYaw(yaw).normalize().scale(range).add(getPositionVector());
				
				rand = new RandUtilSeed(seed);
				ModuleEffectLightning.doLightning(rand, world, data.getCaster(), getPositionVector(), to, range, potency, duration);
			}
		}
		else if (doesRender())
		{
			ClientRunnable.run(new ClientRunnable() {
				@Override
				public void runIfClient()
				{
					Long seed = data.getData(SEED);
					if (seed == null) return;
					
					RandUtilSeed rand = new RandUtilSeed(seed);
					if (rand.nextDouble(maxPotency) < potency)
					{
						float u = rand.nextFloat();
						float v = rand.nextFloat();
						float pitch = (float) (180 * Math.acos(2*u - 1) / Math.PI);
						float yaw = (float) (2 * Math.PI * v);
						
						Vec3d to = dir.rotatePitch(pitch).rotateYaw(yaw).normalize().scale(range).add(getPositionVector());
						
						ModuleEffectLightning.doLightningRender(seed, world, getPositionVector(), to, range);
					}
				}
			});
			return;
		}
	}
	
	@Override
	protected void goBoom(SpellRing spellRing, SpellData data) {
		SpellRing childRing = getChildRing();
		if (childRing == null || childRing.getModule() == null) {
			return;
		}

		double range = childRing.getAttributeValue(AttributeRegistry.RANGE, data);
		double potency = childRing.getAttributeValue(AttributeRegistry.POTENCY, data);
		double duration = childRing.getAttributeValue(AttributeRegistry.DURATION, data);
		AttributeRange potencyRange = childRing.getModule().getAttributeRanges().get(AttributeRegistry.POTENCY);
		Vec3d origin = data.getOriginWithFallback();
		Entity caster = data.getCaster();

		long seed = RandUtil.nextLong(100, 100000);
		data.addData(SEED, seed);
		
		if (origin != null) {
			for (int i = 0; i < potency; i += ((int) potencyRange.min >> 2)) {
				RandUtilSeed rand = new RandUtilSeed(RandUtil.nextLong(100, 100000));
				Vec3d dir = PosUtils.vecFromRotations(rand.nextFloat(0, 180), rand.nextFloat(0, 360));
				Vec3d pos = dir.scale(range).add(origin);

				ModuleEffectLightning.doLightning(rand, world, caster, origin, pos, range, potency, duration);
			}
		}

		super.goBoom(spellRing, data);
	}

	@Override
	public void readCustomNBT(@Nonnull NBTTagCompound compound) {
		super.readCustomNBT(compound);

		if (compound.hasKey("child_ring")) {
			setChildRing(SpellRing.deserializeRing(compound.getCompoundTag("child_ring")));
		}
	}

	@Override
	public void writeCustomNBT(@Nonnull NBTTagCompound compound) {
		super.writeCustomNBT(compound);

		// Stupid Wawla, refusing to fix their problems...
		// https://github.com/micdoodle8/Galacticraft/commit/543e6afad64e51b02252a07489d0832fb93faa8d
		// https://github.com/Darkhax-Minecraft/WAWLA/issues/75
		if (world.isRemote) return;

		compound.setTag("child_ring", getChildRing().serializeNBT());
	}
}
