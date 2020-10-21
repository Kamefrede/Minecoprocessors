package net.torocraft.minecoprocessors.util;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.gui.GuiUtils;

@OnlyIn(Dist.CLIENT)
public class GuiWidgets {
	@OnlyIn(Dist.CLIENT)
	public static class ScaledButton extends Button {
		private final double scale;

		public ScaledButton(double scale, int x, int y, int w, int h, ITextComponent text, Button.IPressable onPress) {
			super(x, y, w, h, text, onPress);
			this.scale = scale;
		}

		@Override
		public void renderButton(MatrixStack ms, int mouseX, int mouseY, float partial) {
			if (!visible) return;
			Minecraft mc = Minecraft.getInstance();
			isHovered = (mouseX >= x) && (mouseY >= y) && (mouseX < x + width) && (mouseY < y + height);
			int ist = getYImage(isHovered);
			ms.push();
			ms.scale((float) scale, (float) scale, (float) scale);
			final int bx = (int) Math.floor((double) x / scale);
			final int by = (int) Math.floor((double) y / scale);
			final int bw = (int) Math.ceil((double) width / scale);
			final int bh = (int) Math.ceil((double) height / scale);
			GuiUtils.drawContinuousTexturedBox(ms, WIDGETS_LOCATION, bx, by, 0, 46 + ist * 20, bw, bh, 200, 20, 2, 3, 2, 2, getBlitOffset());
			renderBg(ms, mc, mouseX, mouseY);
			int color = (packedFGColor != 0) ? (packedFGColor) : ((!active) ? (0xa0a0a0) : (isHovered ? 0xffffa0 : 0xe0e0e0));
			String txt = getMessage().getString();
			int txt_width = mc.fontRenderer.getStringWidth(txt);
			int ellipsis_width = mc.fontRenderer.getStringWidth("...");
			if (txt_width > bw - 6 && (txt_width > ellipsis_width)) {
				txt = mc.fontRenderer.func_238412_a_(txt, bw - 6 - ellipsis_width).trim() + "...";
			}
			drawCenteredString(ms, mc.fontRenderer, txt, bx + bw / 2, by + (bh - 8) / 2, color);
			ms.pop();
		}

	}
}