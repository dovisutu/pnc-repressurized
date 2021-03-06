package me.desht.pneumaticcraft.client.render.pneumatic_armor;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class RenderCoordWireframe {
    public final BlockPos pos;
    public final World world;
    public int ticksExisted;

    public RenderCoordWireframe(World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    public static void addInfo(List<String> tooltip, World world, BlockPos pos) {
        RenderCoordWireframe coordHandler = new RenderCoordWireframe(world, pos);
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).contains("Coordinate Tracker")) {
                tooltip.set(i, tooltip.get(i) + " (tracking " + coordHandler.pos.getX() + ", " + coordHandler.pos.getY() + ", " + coordHandler.pos.getZ() + " in " + coordHandler.world.provider.getDimensionType() + ")");
                break;
            }
        }
    }

    public void render(float partialTicks) {
        double minX = 0;
        double minY = 0;
        double minZ = 0;
        double maxX = 1;
        double maxY = 1;
        double maxZ = 1;
        float progress = (ticksExisted % 20 + partialTicks) / 20;
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.glLineWidth(1.0F);
        // GlStateManager.color(0, 1, 1, progress < 0.5F ? progress + 0.5F : 1.5 - progress);
        GlStateManager.color(0, progress < 0.5F ? progress + 0.5F : 1.5F - progress, 1, 1);
        GlStateManager.pushMatrix();
        // GlStateManager.translate(-0.5D, -0.5D, -0.5D);
        GlStateManager.translate(pos.getX(), pos.getY(), pos.getZ());
        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        wr.pos(minX, minY, minZ).endVertex();
        wr.pos(minX, maxY, minZ).endVertex();
        wr.pos(minX, minY, maxZ).endVertex();
        wr.pos(minX, maxY, maxZ).endVertex();

        wr.pos(maxX, minY, minZ).endVertex();
        wr.pos(maxX, maxY, minZ).endVertex();
        wr.pos(maxX, minY, maxZ).endVertex();
        wr.pos(maxX, maxY, maxZ).endVertex();

        wr.pos(minX, minY, minZ).endVertex();
        wr.pos(maxX, minY, minZ).endVertex();
        wr.pos(minX, minY, maxZ).endVertex();
        wr.pos(maxX, minY, maxZ).endVertex();

        wr.pos(minX, maxY, minZ).endVertex();
        wr.pos(maxX, maxY, minZ).endVertex();
        wr.pos(minX, maxY, maxZ).endVertex();
        wr.pos(maxX, maxY, maxZ).endVertex();

        wr.pos(minX, minY, minZ).endVertex();
        wr.pos(minX, minY, maxZ).endVertex();
        wr.pos(maxX, minY, minZ).endVertex();
        wr.pos(maxX, minY, maxZ).endVertex();

        wr.pos(minX, maxY, minZ).endVertex();
        wr.pos(minX, maxY, maxZ).endVertex();
        wr.pos(maxX, maxY, minZ).endVertex();
        wr.pos(maxX, maxY, maxZ).endVertex();

        Tessellator.getInstance().draw();

        GlStateManager.popMatrix();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
    }
}
