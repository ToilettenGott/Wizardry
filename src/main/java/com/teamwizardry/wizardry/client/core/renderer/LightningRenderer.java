package com.teamwizardry.wizardry.client.core.renderer;

import com.teamwizardry.librarianlib.features.sprite.Sprite;
import com.teamwizardry.wizardry.Wizardry;
import com.teamwizardry.wizardry.api.util.RandUtil;
import com.teamwizardry.wizardry.api.util.TreeNode;
import kotlin.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

/**
 * Created by Demoniaque.
 */
@Mod.EventBusSubscriber(modid = Wizardry.MODID, value = Side.CLIENT)
public class LightningRenderer {

	private static Sprite streakBase = new Sprite(new ResourceLocation(Wizardry.MODID, "textures/particles/streak_base.png"));
	private static Sprite streakCorner = new Sprite(new ResourceLocation(Wizardry.MODID, "textures/particles/streak_corner.png"));
	private static ArrayList<LightningBolt> bolts = new ArrayList<>();
	private static ArrayList<LightningBolt> clear = new ArrayList<>();

	private static BufferBuilder pos(BufferBuilder vb, Vec3d pos) {
		return vb.pos(pos.x, pos.y, pos.z);
	}

	public static void addBolt(TreeNode<Vec3d> points, int maxTick) {
		bolts.add(new LightningBolt(points, maxTick));
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void render(RenderWorldLastEvent event) {
		ArrayList<LightningBolt> temp = new ArrayList<>(bolts);
		for (LightningBolt bolt : temp) {
			if (--bolt.tick <= 0) clear.add(bolt);

		}

		for (LightningBolt bolt : clear) {
			bolts.remove(bolt);
		}
		clear.clear();

		for (LightningBolt bolt : bolts) {
			ArrayList<ArrayList<Pair<Vec3d, Vec3d>>> points = bolt.points;

			if (points.isEmpty()) return;

			GlStateManager.pushMatrix();
			EntityPlayer player = Minecraft.getMinecraft().player;

			double interpPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
			double interpPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
			double interpPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

			GlStateManager.translate(-interpPosX, -interpPosY, -interpPosZ);

			GlStateManager.depthMask(false);

			GlStateManager.disableCull();
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();
			GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE);

			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder vb = tessellator.getBuffer();

			for (int i = 0; i < points.size(); i++) {

				float progress = (bolt.tick / (bolt.maxTick * 1.0f));
				float beamProgress = 1f - (i / (points.size() - 1.0f));

				if (beamProgress < progress / 2) continue;

				ArrayList<Pair<Vec3d, Vec3d>> beams = points.get(i);

				for (Pair<Vec3d, Vec3d> pair : beams) {
					Vec3d from = pair.getFirst();
					Vec3d to = pair.getSecond();

					Vec3d playerEyes = Minecraft.getMinecraft().player.getPositionEyes(event.getPartialTicks());
					Vec3d normal = (from.subtract(to)).crossProduct(playerEyes.subtract(to)).normalize(); //(b.subtract(a)).crossProduct(c.subtract(a));
					if (normal.y < 0)
						normal = normal.scale(-1);

					Color color = new Color(0xDA83FF);
					int a = (int) (color.getAlpha() * progress);
					Vec3d x = normal.scale(0.08 * RandUtil.nextDouble(1, 1.5));

					streakBase.bind();
					vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
					pos(vb, from.add(x)).tex(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, from.subtract(x)).tex(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, to.subtract(x)).tex(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, to.add(x)).tex(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					tessellator.draw();

					streakCorner.bind();
					Vec3d moreFrom = from.add(from.subtract(to).normalize().scale(0.1));
					vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
					pos(vb, from.add(x)).tex(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, from.subtract(x)).tex(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, moreFrom.subtract(x)).tex(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, moreFrom.add(x)).tex(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					tessellator.draw();

					Vec3d moreTo = to.add(to.subtract(from).normalize().scale(0.1));
					vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
					pos(vb, to.add(x)).tex(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, to.subtract(x)).tex(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, moreTo.subtract(x)).tex(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, moreTo.add(x)).tex(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					tessellator.draw();

					streakBase.bind();
					color = Color.WHITE;
					x = normal.scale(0.05 * RandUtil.nextDouble(0.3, 0.5));
					//from = from.add(normal.scale(0.001));
					//to = to.add(normal.scale(0.001));

					vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
					pos(vb, from.add(x)).tex(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, from.subtract(x)).tex(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, to.subtract(x)).tex(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, to.add(x)).tex(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					tessellator.draw();

					Vec3d moreFromCore = from.add(from.subtract(to).normalize().scale(0.01));
					vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
					pos(vb, from.add(x)).tex(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, from.subtract(x)).tex(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, moreFromCore.subtract(x)).tex(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, moreFromCore.add(x)).tex(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					tessellator.draw();

					Vec3d moreToCore = to.add(to.subtract(from).normalize().scale(0.01));
					vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
					pos(vb, to.add(x)).tex(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, to.subtract(x)).tex(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, moreToCore.subtract(x)).tex(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					pos(vb, moreToCore.add(x)).tex(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), a).endVertex();
					tessellator.draw();
				}
			}

			GlStateManager.depthMask(true);

			GlStateManager.popMatrix();
		}
	}

	public static class LightningBolt {

		public final ArrayList<ArrayList<Pair<Vec3d, Vec3d>>> points;
		public final int maxTick;
		public int tick;

		public LightningBolt(TreeNode<Vec3d> points, int maxTick) {
			this.points = new ArrayList<>();
			this.maxTick = maxTick;
			this.tick = maxTick;

			Queue<TreeNode<Vec3d>> queue = new LinkedList<>();
			Queue<TreeNode<Vec3d>> temp = new LinkedList<>();
			ArrayList<Pair<Vec3d, Vec3d>> frameList = new ArrayList<>();

			queue.add(points);

			while (!queue.isEmpty()) {
				TreeNode<Vec3d> node = queue.remove();
				temp.addAll(node.getChildren());
				for (TreeNode<Vec3d> child : node.getChildren())
					frameList.add(new Pair<>(node.getData(), child.getData()));
				
				if (queue.isEmpty()) {
					this.points.add(frameList);
					frameList = new ArrayList<>();

					queue = temp;
					temp = new LinkedList<>();
				}
			}
		}
	}
}
