package gregicadditions.renderer;

import codechicken.lib.texture.TextureUtils;
import codechicken.lib.vec.Matrix4;
import gregtech.api.gui.resources.TextureArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

@SideOnly(Side.CLIENT)
public class RenderHelper {

    @SideOnly(Side.CLIENT)
    public static void renderFluidOverLay(float x, float y, float width, float height, float z, FluidStack fluidStack, float alpha) {
        if (fluidStack != null) {
            int color = fluidStack.getFluid().getColor(fluidStack);
            float r = (color >> 16 & 255) / 255.0f;
            float g = (color >> 8 & 255) / 255.0f;
            float b = (color & 255) / 255.0f;
            TextureAtlasSprite sprite = TextureUtils.getTexture(fluidStack.getFluid().getStill(fluidStack));
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableAlpha();
            //GlStateManager.disableLighting();
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();

            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            double uMin = sprite.getInterpolatedU(16D - width * 16D), uMax = sprite.getInterpolatedU(width * 16D);
            double vMin = sprite.getMinV(), vMax = sprite.getInterpolatedV(height * 16D);
            buf.pos(x, y, z).tex(uMin, vMin).color(r, g, b, alpha).endVertex();
            buf.pos(x, y + height, z).tex(uMin, vMax).color(r, g, b, alpha).endVertex();
            buf.pos(x + width, y + height, z).tex(uMax, vMax).color(r, g, b, alpha).endVertex();
            buf.pos(x + width, y, z).tex(uMax, vMin).color(r, g, b, alpha).endVertex();
            tess.draw();

            //GlStateManager.enableLighting();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    @SideOnly(Side.CLIENT)
    public static void renderRect(float x, float y, float width, float height, float z, int color) {
        renderGradientRect(x, y, width, height, z, color, color, false);
    }

    @SideOnly(Side.CLIENT)
    public static void renderGradientRect(float x, float y, float width, float height, float z, int startColor, int endColor, boolean horizontal) {
        float startAlpha = (float) (startColor >> 24 & 255) / 255.0F;
        float startRed = (float) (startColor >> 16 & 255) / 255.0F;
        float startGreen = (float) (startColor >> 8 & 255) / 255.0F;
        float startBlue = (float) (startColor & 255) / 255.0F;
        float endAlpha = (float) (endColor >> 24 & 255) / 255.0F;
        float endRed = (float) (endColor >> 16 & 255) / 255.0F;
        float endGreen = (float) (endColor >> 8 & 255) / 255.0F;
        float endBlue = (float) (endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        if (horizontal) {
            buffer.pos(x + width, y, z).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            buffer.pos(x, y, z).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            buffer.pos(x, y + height, z).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            buffer.pos(x + width, y + height, z).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            tessellator.draw();
        } else {
            buffer.pos(x + width, y, z).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            buffer.pos(x, y, z).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            buffer.pos(x, y + height, z).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            buffer.pos(x + width, y + height, z).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            tessellator.draw();
        }
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    @SideOnly(Side.CLIENT)
    public static void renderTextureArea(TextureArea textureArea, float x, float y, float width, float height, float z) {
        double imageU = textureArea.offsetX;
        double imageV = textureArea.offsetY;
        double imageWidth = textureArea.imageWidth;
        double imageHeight = textureArea.imageHeight;
        Minecraft.getMinecraft().renderEngine.bindTexture(textureArea.imageLocation);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, z).tex(imageU, imageV + imageHeight).endVertex();
        bufferbuilder.pos(x + width, y + height, z).tex(imageU + imageWidth, imageV + imageHeight).endVertex();
        bufferbuilder.pos(x + width, y, z).tex(imageU + imageWidth, imageV).endVertex();
        bufferbuilder.pos(x, y, z).tex(imageU, imageV).endVertex();
        tessellator.draw();
    }

    @SideOnly(Side.CLIENT)
    public static void renderItemOverLay(float x, float y, float z, float scale, ItemStack itemStack) {
        net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 0.0001f);
        GlStateManager.translate(x * 16, y * 16, z * 16);
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0 );
        GlStateManager.popMatrix();
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
    }

    @SideOnly(Side.CLIENT)
    public static void renderText(float x, float y, float z, float scale, int color, final String renderedText, boolean centered) {
        GlStateManager.pushMatrix();
        final FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        final int width = fr.getStringWidth( renderedText );
        GlStateManager.translate(x, y, z);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate(-0.5f * (centered? 1:0)*width, 0.0f, 0.5f );

        fr.drawString(renderedText, 0, 0, color);
        GlStateManager.popMatrix();
    }

    @SideOnly(Side.CLIENT)
    public static void renderLineChart(List<Long> data, float x, float y, float width, float height, float lineWidth, int color) {
        if (data.isEmpty()) return;
        long max = Long.MIN_VALUE;
        long min = 0;
        for (long d : data) {
            max = Math.max(max, d);
        }
        float durX = data.size() > 1 ? width / (data.size() - 1) : 0;
        float hY = max - min > 0 ? height / (max - min) : 0;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA);
        GlStateManager.color(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, ((color >> 24) & 0xFF) / 255f);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        float last_x = x + 0 * durX;
        float last_y = y - (data.get(0) - min) * hY;
        for (int i = 0; i < data.size(); i++) {
            float _x = x + i * durX;
            float _y = y - (data.get(i) - min) * hY;
            // draw lines
            if (i != 0) {
                bufferbuilder.pos(last_x, last_y - lineWidth, 0.01D).endVertex();
                bufferbuilder.pos(last_x, last_y + lineWidth, 0.01D).endVertex();
                bufferbuilder.pos(_x, _y + lineWidth, 0.01D).endVertex();
                bufferbuilder.pos(_x, _y - lineWidth, 0.01D).endVertex();
                last_x = _x;
                last_y = _y;
            }
            // draw points
            bufferbuilder.pos(_x - 3 * lineWidth, _y, 0.01D).endVertex();
            bufferbuilder.pos(_x, _y + 3 * lineWidth, 0.01D).endVertex();
            bufferbuilder.pos(_x + 3 * lineWidth, _y, 0.01D).endVertex();
            bufferbuilder.pos(_x, _y - 3 * lineWidth, 0.01D).endVertex();
        }
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    @SideOnly(Side.CLIENT)
    public static Matrix4 adjustTrans(Matrix4 translation, EnumFacing side, int layer) {
        Matrix4 trans = translation.copy();
        switch (side){
            case DOWN:
                trans.translate(0 , -0.001D * layer,0);
                break;
            case UP:
                trans.translate(0 , 0.001D * layer,0);
                break;
            case NORTH:
                trans.translate(0 , 0,-0.001D * layer);
                break;
            case SOUTH:
                trans.translate(0 , 0,0.001D * layer);
                break;
            case EAST:
                trans.translate(0.001D * layer, 0,0);
                break;
            case WEST:
                trans.translate(-0.001D * layer, 0,0);
                break;
        }
        return trans;
    }

    @SideOnly(Side.CLIENT)
    public static void moveToFace(double x, double y, double z, EnumFacing face) {
        GlStateManager.translate( x + 0.5 + face.getXOffset() * 0.5, y + 0.5 + face.getYOffset() * 0.5, z + 0.5 + face.getZOffset() * 0.5);
    }

    @SideOnly(Side.CLIENT)
    public static void rotateToFace(EnumFacing face, EnumFacing spin) {
        switch( face ) {
            case UP:
                GlStateManager.scale( 1.0f, -1.0f, 1.0f );
                GlStateManager.rotate( 90.0f, 1.0f, 0.0f, 0.0f );
                GlStateManager.rotate(spin == EnumFacing.EAST ? 90 : spin == EnumFacing.SOUTH ? 180 : spin == EnumFacing.WEST ? -90 : 0, 0, 0, 1);
                break;
            case DOWN:
                GlStateManager.scale( 1.0f, -1.0f, 1.0f );
                GlStateManager.rotate( -90.0f, 1.0f, 0.0f, 0.0f );
                GlStateManager.rotate(spin == EnumFacing.EAST ? 90 : spin == EnumFacing.NORTH ? 180 : spin == EnumFacing.WEST ? -90 : 0, 0, 0, 1);
                break;
            case EAST:
                GlStateManager.scale( -1.0f, -1.0f, -1.0f );
                GlStateManager.rotate( -90.0f, 0.0f, 1.0f, 0.0f );
                break;
            case WEST:
                GlStateManager.scale( -1.0f, -1.0f, -1.0f );
                GlStateManager.rotate( 90.0f, 0.0f, 1.0f, 0.0f );
                break;
            case NORTH:
                GlStateManager.scale( -1.0f, -1.0f, -1.0f );
                break;
            case SOUTH:
                GlStateManager.scale( -1.0f, -1.0f, -1.0f );
                GlStateManager.rotate( 180.0f, 0.0f, 1.0f, 0.0f );
                break;
            default:
                break;
        }
    }
}
