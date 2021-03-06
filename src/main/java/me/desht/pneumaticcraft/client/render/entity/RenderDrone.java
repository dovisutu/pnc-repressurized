package me.desht.pneumaticcraft.client.render.entity;

import me.desht.pneumaticcraft.client.model.entity.ModelDrone;
import me.desht.pneumaticcraft.common.entity.living.EntityDroneBase;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class RenderDrone extends RenderLiving<EntityDroneBase> {
    public static final IRenderFactory<EntityDroneBase> REGULAR_FACTORY = RenderDrone::new;
    public static final IRenderFactory<EntityDroneBase> LOGISTICS_FACTORY = manager -> new RenderDrone(manager, 0xFFFF0000);
    public static final IRenderFactory<EntityDroneBase> HARVESTING_FACTORY = manager -> new RenderDrone(manager, 0xFF006102);

    public RenderDrone(RenderManager manager) {
        super(manager, new ModelDrone(), 0);
    }
    
    public RenderDrone(RenderManager manager, int frameColor) {
        super(manager, new ModelDrone(frameColor), 0);
    }

    private void renderDrone(EntityDroneBase drone, double x, double y, double z, float yaw, float partialTicks) {
        if (drone.getHealth() <= 0) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0.76F, 0);
        GlStateManager.scale(0.5F, -0.5F, -0.5F);
        bindEntityTexture(drone);
        mainModel.setLivingAnimations(drone, 0, 0, partialTicks);
        mainModel.render(drone, 0, 0, 0, 0, partialTicks, 1 / 16F);
        GlStateManager.popMatrix();

        drone.renderExtras(x, y, z, partialTicks);
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityDroneBase par1Entity) {
        return Textures.MODEL_DRONE;
    }

    @Override
    public void doRender(EntityDroneBase drone, double par2, double par4, double par6, float par8, float par9) {
        renderDrone(drone, par2, par4, par6, par8, par9);
        renderName(drone, par2, par4, par6); //TODO 1.8 test (renaming)
    }

    @Override
    protected boolean canRenderName(EntityDroneBase drone) {
        return super.canRenderName(drone) && (drone.getAlwaysRenderNameTagForRender() || drone.hasCustomName() && drone == renderManager.pointedEntity);
    }
}
