package gregicadditions.renderer.onlinepictexture;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

public abstract class PictureTexture {
    public int width;
    public int height;

    public PictureTexture(int width, int height) {
        this.width = width;
        this.height = height;

    }

    public void beforeRender() {

    }

    public void render(float x, float y, float width, float height, float rotation, float sizeX, float sizeY, boolean flippedX, boolean flippedY) {
        this.beforeRender();
        GlStateManager.enableBlend();
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GlStateManager.bindTexture(this.getTextureID());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager.pushMatrix();
        GL11.glRotated(rotation, 0, 0, 1);
        GlStateManager.enableRescaleNormal();
        GL11.glScaled(sizeX, sizeY, 1);
        GL11.glBegin(GL11.GL_POLYGON);
        GL11.glTexCoord3f(flippedY ? 1 : 0, flippedX ? 1 : 0, 0);
        GL11.glVertex3f(x, y, 0.01f);
        GL11.glTexCoord3f(flippedY ? 1 : 0, flippedX ? 0 : 1, 0);
        GL11.glVertex3f(x, y + height, 0.01f);
        GL11.glTexCoord3f(flippedY ? 0 : 1, flippedX ? 0 : 1, 0);
        GL11.glVertex3f(x + width, y + height, 0.01f);
        GL11.glTexCoord3f(flippedY ? 0 : 1, flippedX ? 1 : 0, 0);
        GL11.glVertex3f(x + width, y, 0.01f);
        GL11.glEnd();
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
    }

    public abstract void tick();

    public abstract int getTextureID();

    public boolean hasTexture() {
        return getTextureID() != -1;
    }

    public void release() {
        GlStateManager.deleteTexture(getTextureID());
    }
}
