/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.OverlayPosition;
import com.wynntils.core.features.overlays.annotations.OverlayInfo;
import com.wynntils.core.features.overlays.sizes.GuiScaledOverlaySize;
import com.wynntils.core.webapi.WebManager;
import com.wynntils.core.webapi.profiles.MapProfile;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.render.HorizontalAlignment;
import com.wynntils.mc.render.RenderUtils;
import com.wynntils.mc.render.VerticalAlignment;
import com.wynntils.mc.utils.McUtils;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class MiniMapOverlayFeature extends UserFeature {
    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    public final MiniMapOverlay miniMapOverlay = new MiniMapOverlay();

    public static class MiniMapOverlay extends Overlay {

        @Config
        public float scale = 1f;

        @Config
        public boolean followPlayerRotation = true;

        @Config
        public boolean renderUsingLinear = true;

        @Config
        public MapMaskType maskType = MapMaskType.Rectangular;

        public MiniMapOverlay() {
            super(
                    new OverlayPosition(
                            0,
                            0,
                            VerticalAlignment.Top,
                            HorizontalAlignment.Left,
                            OverlayPosition.AnchorSection.TopLeft),
                    new GuiScaledOverlaySize(150, 150));
        }

        @Override
        public void render(PoseStack poseStack, float partialTicks, Window window) {
            if (!WebManager.isMapLoaded()) return;

            MapProfile map = WebManager.getMaps().get(0);

            float width = getWidth();
            float height = getHeight();
            float renderX = getRenderX();
            float renderY = getRenderY();

            float centerX = renderX + width / 2;
            float centerZ = renderY + height / 2;
            float textureX = map.getTextureXPosition(McUtils.player().getX());
            float textureZ = map.getTextureZPosition(McUtils.player().getZ());

            // Render Minimap

            // enable mask
            switch (maskType) {
                case Rectangular -> {
                    RenderUtils.enableScissor((int) renderX, (int) renderY, (int) width, (int) height);
                }
                    // case Circle -> {
                    // TODO
                    // }
            }

            if (followPlayerRotation) {
                poseStack.pushPose();
                rotateMapToPlayer(poseStack, centerX, centerZ);
            }

            renderMapQuad(map, poseStack, centerX, centerZ, textureX, textureZ, width, height);

            if (followPlayerRotation) {
                poseStack.popPose();
            }

            // TODO minimap icons

            // TODO compass icon

            // disable mask
            switch (maskType) {
                case Rectangular -> {
                    RenderSystem.disableScissor();
                }
                    // case Circle -> {
                    // TODO
                    // }
            }

            // TODO cursor

            // TODO Border rendering

            // TODO Directional Text

            // TODO Coords

        }

        private void rotateMapToPlayer(PoseStack poseStack, float centerX, float centerZ) {
            poseStack.translate(centerX, centerZ, 0);
            // See Quaternion#fromXYZ
            poseStack.mulPose(new Quaternion(
                    0F,
                    0,
                    (float) StrictMath.sin(Math.toRadians(180 - McUtils.player().getYRot()) / 2),
                    (float) StrictMath.cos(
                            -Math.toRadians(180 - McUtils.player().getYRot()) / 2)));
            poseStack.translate(-centerX, -centerZ, 0);
        }

        private void renderMapQuad(
                MapProfile map,
                PoseStack poseStack,
                float centerX,
                float centerZ,
                float textureX,
                float textureZ,
                float width,
                float height) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, map.resource());

            // clamp map rendering
            int option = renderUsingLinear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, option);

            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);

            float uScale = 1f / map.getTextureWidth();
            float vScale = 1f / map.getTextureHeight();

            // avoid rotational overpass - This is a rather loose oversizing, if possible later
            // use trignometry, etc. to find a better one
            float extraFactor = 1F;
            if (followPlayerRotation && maskType == MapMaskType.Rectangular) {
                // 1.5 > sqrt(2);
                extraFactor = 1.5F;

                if (width > height) {
                    extraFactor *= width / height;
                } else {
                    extraFactor *= height / width;
                }
            }

            float halfRenderedWidth = width / 2 * extraFactor;
            float halfRenderedHeight = height / 2 * extraFactor;
            float halfTextureWidth = halfRenderedWidth * scale;
            float halfTextureHeight = halfRenderedHeight * scale;

            Matrix4f matrix = poseStack.last().pose();

            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferBuilder
                    .vertex(matrix, (centerX - halfRenderedWidth), (centerZ + halfRenderedHeight), 0)
                    .uv((textureX - halfTextureWidth) * uScale, (textureZ + halfTextureHeight) * vScale)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, (centerX + halfRenderedWidth), (centerZ + halfRenderedHeight), 0)
                    .uv((textureX + halfTextureWidth) * uScale, (textureZ + halfTextureHeight) * vScale)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, (centerX + halfRenderedWidth), (centerZ - halfRenderedHeight), 0)
                    .uv((textureX + halfTextureWidth) * uScale, (textureZ - halfTextureHeight) * vScale)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, (centerX - halfRenderedWidth), (centerZ - halfRenderedHeight), 0)
                    .uv((textureX - halfTextureWidth) * uScale, (textureZ - halfTextureHeight) * vScale)
                    .endVertex();
            bufferBuilder.end();
            BufferUploader.end(bufferBuilder);
        }

        @Override
        protected void onConfigUpdate(ConfigHolder configHolder) {}
    }

    public enum MapMaskType {
        Rectangular,
        // Circle;
    }
}