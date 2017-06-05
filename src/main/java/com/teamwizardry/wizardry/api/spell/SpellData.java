package com.teamwizardry.wizardry.api.spell;

import com.teamwizardry.librarianlib.features.saving.Savable;
import com.teamwizardry.wizardry.api.capability.DefaultWizardryCapability;
import com.teamwizardry.wizardry.api.capability.IWizardryCapability;
import com.teamwizardry.wizardry.api.capability.WizardryCapabilityProvider;
import kotlin.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * Created by LordSaad.
 */
@Savable
public class SpellData implements INBTSerializable<NBTTagCompound> {

	private static HashMap<Pair, ProcessData.Process> dataProcessor = new HashMap<>();

	@Nonnull
	public World world;
	@Nonnull
	private HashMap<Pair, Object> data = new HashMap<>();

	public SpellData(@Nonnull World world) {
		this.world = world;
	}

	@SuppressWarnings("unchecked")
	@Nonnull
	public static <T, E extends NBTBase> Pair<String, Class<T>> constructPair(@Nonnull String key, @Nonnull Class<?> type, ProcessData.Process<E, T> data) {
		Pair<String, Class<T>> pair = new Pair(key, type);
		dataProcessor.put(pair, data);
		return pair;
	}

	public void addAllData(HashMap<Pair, Object> data) {
		this.data.putAll(data);
	}

	public <T> void addData(@Nonnull Pair<String, Class<T>> key, @Nullable T value) {
		this.data.put(key, value);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getData(@Nonnull Pair<String, Class<T>> pair) {
		if (data.containsKey(pair) && pair.getSecond().isInstance(data.get(pair)))
			return (T) data.get(pair);
		return null;
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public <T> T getData(@Nonnull Pair<String, Class<T>> pair, @Nonnull T def) {
		if (data.containsKey(pair) && pair.getSecond().isInstance(data.get(pair)))
			return (T) data.get(pair);
		return def;
	}

	@SuppressWarnings("unchecked")
	public <T> boolean hasData(@Nonnull Pair<String, Class<T>> pair) {
		return data.containsKey(pair) && data.get(pair) != null;
	}

	public void processEntity(@Nonnull Entity entity, boolean asCaster) {
		if (asCaster) {
			addData(DefaultKeys.ORIGIN, entity.getPositionVector().addVector(0, entity.getEyeHeight(), 0));
			addData(DefaultKeys.CASTER, entity);
			addData(DefaultKeys.YAW, entity.rotationYaw);
			addData(DefaultKeys.PITCH, entity.rotationPitch);
			addData(DefaultKeys.CAPABILITY, WizardryCapabilityProvider.getCap(entity));
		} else {
			addData(DefaultKeys.TARGET_HIT, entity.getPositionVector().addVector(0, entity.height / 2.0, 0));
			addData(DefaultKeys.ENTITY_HIT, entity);
		}
	}

	public void processBlock(@Nullable BlockPos pos, @Nullable EnumFacing facing, @Nullable Vec3d targetHit) {
		if (pos != null) addData(DefaultKeys.BLOCK_HIT, pos);
		if (targetHit != null) addData(DefaultKeys.TARGET_HIT, targetHit);
		if (facing != null) addData(DefaultKeys.FACE_HIT, facing);
	}

	public SpellData copy() {
		SpellData spell = new SpellData(world);
		spell.addAllData(data);
		spell.deserializeNBT(serializeNBT());
		return spell;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound compound = new NBTTagCompound();
		for (Pair pair : data.keySet()) {
			NBTBase nbtClass = dataProcessor.get(pair).serialize(data.get(pair));
			compound.setTag(pair.getFirst() + "", nbtClass);
		}
		return compound;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		for (String key : nbt.getKeySet()) {
			for (Pair pair : dataProcessor.keySet()) {
				if (pair.getFirst().equals(key)) {
					NBTBase nbtType = nbt.getTag(pair.getFirst() + "");
					data.put(pair, dataProcessor.get(pair).deserialize(world, nbtType));
				}
			}
		}
	}

	public static class DefaultKeys {
		public static final Pair<String, Class<Float>> STRENGTH = constructPair("strength", Float.class, new ProcessData.Process<NBTTagFloat, Float>() {
			@NotNull
			@Override
			public NBTTagFloat serialize(@Nullable Float object) {
				if (object == null) return new NBTTagFloat(1f);
				return new NBTTagFloat(object);
			}

			@Override
			public Float deserialize(@NotNull World world, @NotNull NBTTagFloat object) {
				return object.getFloat();
			}
		});

		public static final Pair<String, Class<Entity>> CASTER = constructPair("caster", Entity.class, new ProcessData.Process<NBTTagInt, Entity>() {
			@NotNull
			@Override
			public NBTTagInt serialize(Entity object) {
				return new NBTTagInt(object.getEntityId());
			}

			@Override
			public Entity deserialize(@NotNull World world, @NotNull NBTTagInt object) {
				return world.getEntityByID(object.getInt());
			}
		});

		public static final Pair<String, Class<Float>> YAW = constructPair("yaw", Float.class, new ProcessData.Process<NBTTagFloat, Float>() {
			@NotNull
			@Override
			public NBTTagFloat serialize(Float object) {
				return new NBTTagFloat(object);
			}

			@Override
			public Float deserialize(@NotNull World world, @NotNull NBTTagFloat object) {
				return object.getFloat();
			}
		});

		public static final Pair<String, Class<Float>> PITCH = constructPair("pitch", Float.class, new ProcessData.Process<NBTTagFloat, Float>() {
			@NotNull
			@Override
			public NBTTagFloat serialize(Float object) {
				return new NBTTagFloat(object);
			}

			@Override
			public Float deserialize(@NotNull World world, @NotNull NBTTagFloat object) {
				return object.getFloat();
			}
		});

		public static final Pair<String, Class<Vec3d>> ORIGIN = constructPair("origin", Vec3d.class, new ProcessData.Process<NBTTagCompound, Vec3d>() {
			@NotNull
			@Override
			public NBTTagCompound serialize(Vec3d object) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setDouble("x", object.xCoord);
				compound.setDouble("y", object.yCoord);
				compound.setDouble("z", object.zCoord);
				return compound;
			}

			@Override
			public Vec3d deserialize(@NotNull World world, @NotNull NBTTagCompound object) {
				double x = object.getDouble("x");
				double y = object.getDouble("y");
				double z = object.getDouble("z");
				return new Vec3d(x, y, z);
			}
		});

		public static final Pair<String, Class<Entity>> ENTITY_HIT = constructPair("entity_hit", Entity.class, new ProcessData.Process<NBTTagInt, Entity>() {
			@NotNull
			@Override
			public NBTTagInt serialize(Entity object) {
				if (object == null) return new NBTTagInt(-1);
				return new NBTTagInt(object.getEntityId());
			}

			@Override
			public Entity deserialize(@NotNull World world, @NotNull NBTTagInt object) {
				return world.getEntityByID(object.getInt());
			}
		});

		public static final Pair<String, Class<BlockPos>> BLOCK_HIT = constructPair("block_hit", BlockPos.class, new ProcessData.Process<NBTTagLong, BlockPos>() {
			@NotNull
			@Override
			public NBTTagLong serialize(BlockPos object) {
				if (object == null) return new NBTTagLong(-1L);
				return new NBTTagLong(object.toLong());
			}

			@Override
			public BlockPos deserialize(@NotNull World world, @NotNull NBTTagLong object) {
				return BlockPos.fromLong(object.getLong());
			}
		});

		@Nonnull
		public static final Pair<String, Class<EnumFacing>> FACE_HIT = constructPair("face_hit", BlockPos.class, new ProcessData.Process<NBTTagInt, EnumFacing>() {
			@NotNull
			@Override
			public NBTTagInt serialize(EnumFacing object) {
				if (object == null) return new NBTTagInt(-1);
				return new NBTTagInt(object.getIndex());
			}

			@Override
			public EnumFacing deserialize(@NotNull World world, @NotNull NBTTagInt object) {
				return EnumFacing.getFront(object.getInt());
			}
		});

		@Nonnull
		public static final Pair<String, Class<IWizardryCapability>> CAPABILITY = constructPair("capability", IWizardryCapability.class, new ProcessData.Process<NBTTagCompound, IWizardryCapability>() {
			@NotNull
			@Override
			public NBTTagCompound serialize(IWizardryCapability object) {
				if (object == null) return new NBTTagCompound();
				return object.saveNBTData();
			}

			@Override
			public IWizardryCapability deserialize(@NotNull World world, @NotNull NBTTagCompound object) {
				DefaultWizardryCapability cap = new DefaultWizardryCapability();
				cap.loadNBTData(object);
				return cap;
			}
		});

		@Nonnull
		public static final Pair<String, Class<Vec3d>> TARGET_HIT = constructPair("target_hit", Vec3d.class, new ProcessData.Process<NBTTagCompound, Vec3d>() {
			@NotNull
			@Override
			public NBTTagCompound serialize(Vec3d object) {
				if (object == null) return new NBTTagCompound();
				NBTTagCompound compound = new NBTTagCompound();
				compound.setDouble("x", object.xCoord);
				compound.setDouble("y", object.yCoord);
				compound.setDouble("z", object.zCoord);
				return compound;
			}

			@Override
			public Vec3d deserialize(@NotNull World world, @NotNull NBTTagCompound object) {
				if (!object.hasKey("x") || !object.hasKey("y") || !object.hasKey("z")) return Vec3d.ZERO;
				double x = object.getDouble("x");
				double y = object.getDouble("y");
				double z = object.getDouble("z");
				return new Vec3d(x, y, z);
			}
		});

		@Nonnull
		public static final Pair<String, Class<Long>> SEED = constructPair("seed", Long.class, new ProcessData.Process<NBTTagLong, Long>() {

			@NotNull
			@Override
			public NBTTagLong serialize(@Nullable Long object) {
				if (object == null) return new NBTTagLong(0);
				return new NBTTagLong(object);
			}

			@NotNull
			@Override
			public Long deserialize(@NotNull World world, @NotNull NBTTagLong object) {
				return object.getLong();
			}
		});
	}
}