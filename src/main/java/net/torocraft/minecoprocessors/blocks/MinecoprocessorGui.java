/*
 * @file MinecoprocessorGui.java
 * @license GPL
 */
package net.torocraft.minecoprocessors.blocks;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ReadBookScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.torocraft.minecoprocessors.ModMinecoprocessors;
import net.torocraft.minecoprocessors.blocks.MinecoprocessorTileEntity.ContainerSyncFields;
import net.torocraft.minecoprocessors.processor.Processor;
import net.torocraft.minecoprocessors.processor.Register;
import net.torocraft.minecoprocessors.util.BookCreator;
import net.torocraft.minecoprocessors.util.GuiWidgets.ScaledButton;
import net.torocraft.minecoprocessors.util.InstructionUtil;
import net.torocraft.minecoprocessors.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MinecoprocessorGui extends ContainerScreen<MinecoprocessorContainer> {
	private static final ResourceLocation BACKGROUND_IMAGE = new ResourceLocation(ModMinecoprocessors.MODID, "textures/gui/minecoprocessor.png");
	private static final double GUI_SCALE = 0.5;
	private final PlayerEntity player_;
	private final List<ITextComponent> hoveredFeature = new ArrayList<>(5);
	private Button buttonSleep;
	private Button buttonStep;
	private Button buttonReset;
	private Button buttonHelp;

	public MinecoprocessorGui(MinecoprocessorContainer container, PlayerInventory player_inventory, ITextComponent title) {
		super(container, player_inventory, title);
		this.player_ = player_inventory.player;
	}

	@Override
	public void init() {
		super.init();
		final int x0 = getGuiLeft();
		final int y0 = getGuiTop();
		final int w = 49;
		final int h = 10;
		buttons.clear();
		buttons.add(buttonSleep = addButton(
				new ScaledButton(GUI_SCALE, x0 + 8, y0 + 34 + (0 * (h + 2)), w, h, (new TranslationTextComponent("minecoprocessors.gui.button.sleep")),
						(bt) -> getContainer().onGuiAction("sleep", 1))
		));
		buttons.add(buttonReset = addButton(
				new ScaledButton(GUI_SCALE, x0 + 8, y0 + 34 + (1 * (h + 2)), w, h, (new TranslationTextComponent("minecoprocessors.gui.button.reset")),
						(bt) -> getContainer().onGuiAction("reset", 1))
		));
		buttons.add(buttonStep = addButton(
				new ScaledButton(GUI_SCALE, x0 + 8, y0 + 34 + (2 * (h + 2)), w, h, (new TranslationTextComponent("minecoprocessors.gui.button.step")),
						(bt) -> getContainer().onGuiAction("step", 1))
		));
		buttons.add(buttonHelp = addButton(
				new ScaledButton(GUI_SCALE, x0 + 133, y0 + 66, 35, h, (new TranslationTextComponent("minecoprocessors.gui.button.help")),
						(bt) -> {
							Minecraft.getInstance().displayGuiScreen(new ReadBookScreen(ReadBookScreen.IBookInfo.func_216917_a(BookCreator.getManual())));
						})
		));
		buttonStep.active = false;
	}

	@Override
	public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
		renderBackground(ms);
		super.render(ms, mouseX, mouseY, partialTicks);
		renderHoveredTooltip(ms, mouseX, mouseY);
		getContainer().checkResync();
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack ms, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.color4f(1f, 1f, 1f, 1f);
		getMinecraft().getTextureManager().bindTexture(BACKGROUND_IMAGE);
		final int x0 = getGuiLeft(), y0 = getGuiTop(), w = getXSize(), h = getYSize();
		blit(ms, x0, y0, 0, 0, w, h);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack ms, int mouseX, int mouseY) {
		hoveredFeature.clear();
		ContainerSyncFields fields = getContainer().getFields();
		ms.push();
		ms.scale((float) GUI_SCALE, (float) GUI_SCALE, (float) GUI_SCALE);
		mouseX = (int) ((mouseX - getGuiLeft()) / GUI_SCALE);
		mouseY = (int) ((mouseY - getGuiTop()) / GUI_SCALE);

		int y;
		y = 50;
		drawRegister(ms, Register.A, 130 * 2, y, mouseX, mouseY);
		drawRegister(ms, Register.B, 139 * 2, y, mouseX, mouseY);
		drawRegister(ms, Register.C, 148 * 2, y, mouseX, mouseY);
		drawRegister(ms, Register.D, 157 * 2, y, mouseX, mouseY);

		y = 82;
		drawFlag(ms, "Z", fields.isZero(), 130 * 2, y, mouseX, mouseY);
		drawFlag(ms, "C", fields.isCarry() || fields.isOverflow(), 139 * 2, y, mouseX, mouseY);
		drawFlag(ms, "F", fields.isFault(), 148 * 2, y, 0xff0000, mouseX, mouseY);
		drawFlag(ms, "S", fields.isWait(), 157 * 2, y, 0x00ff00, mouseX, mouseY);

		y = 114;
		boolean mouseIsOver = drawLabeledValue(ms, "IP", StringUtil.toHex(fields.ip()), 128 * 2, y, null, mouseX, mouseY);
		if (mouseIsOver) hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Instruction Pointer"));

		drawRegister(ms, Register.ADC, 142 * 2, y, mouseX, mouseY);
		drawRegister(ms, Register.PORTS, 158 * 2, y, mouseX, mouseY);
		drawPortRegister(ms, Register.PF, 176, 47, mouseX, mouseY);
		drawPortRegister(ms, Register.PR, 216, 86, mouseX, mouseY);
		drawPortRegister(ms, Register.PL, 137, 86, mouseX, mouseY);
		drawPortRegister(ms, Register.PB, 176, 125, mouseX, mouseY);
		drawCode(ms);
		drawGuiTitle(ms);
		ms.pop();
		if (fields.isWait()) {
			buttonSleep.setMessage((new TranslationTextComponent("minecoprocessors.gui.button.wake")));
			buttonStep.active = true;
		} else {
			buttonSleep.setMessage((new TranslationTextComponent("minecoprocessors.gui.button.sleep")));
			buttonStep.active = false;
		}
	}

	@Override
	protected void renderHoveredTooltip(MatrixStack matrixStack, int mouseX, int mouseY) {
		super.renderHoveredTooltip(matrixStack, mouseX, mouseY);
		if (!hoveredFeature.isEmpty()) {
			matrixStack.push();
			matrixStack.scale(((float) GUI_SCALE), ((float) GUI_SCALE), ((float) GUI_SCALE));
			func_243308_b(matrixStack, hoveredFeature, (int) (mouseX / GUI_SCALE), (int) (mouseY / GUI_SCALE));
			matrixStack.pop();
		}
	}

	// -------------------------------------------------------------------------------------------------------------------

	private void drawRegister(MatrixStack ms, Register register, int x, int y, int mouseX, int mouseY) {
		String label = register.toString();
		byte value = getContainer().getFields().register(register.ordinal() - Register.A.ordinal());
		boolean mouseIsOver = drawLabeledValue(ms, label, StringUtil.toHex(value), x, y, null, mouseX, mouseY);
		if (mouseIsOver) {
			hoveredFeature.add(ITextComponent.getTextComponentOrEmpty(label + " Register"));
			if (Register.PORTS.equals(register)) {
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("I/O port direction"));
			} else if (Register.ADC.equals(register)) {
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("ADC/DAC switch"));
			} else {
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("General Purpose"));
			}
			hoveredFeature.add(ITextComponent.getTextComponentOrEmpty(String.format("0x%s %sb %s", StringUtil.toHex(value), StringUtil.toBinary(value), Integer.toString(value, 10))));
		}
	}

	private void drawPortRegister(MatrixStack ms, Register register, int x, int y, int mouseX, int mouseY) {
		final ContainerSyncFields fields = getContainer().getFields();
		int portIndex = register.ordinal() - Register.PF.ordinal();
		byte value = fields.port(portIndex);
		boolean mouseIsOver = centered(ms, StringUtil.toHex(value), x, y, mouseX, mouseY);
		if (mouseIsOver) {
			switch (register) {
			case PF:
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Front Port - PF"));
				break;
			case PB:
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Back Port - PB"));
				break;
			case PL:
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Left Port - PL"));
				break;
			case PR:
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Right Port - PR"));
				break;
			}
			if (MinecoprocessorTileEntity.isInOutputMode(fields.ports(), portIndex)) {
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Output Port"));
			} else if (MinecoprocessorTileEntity.isInInputMode(fields.ports(), portIndex)) {
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Input Port"));
			} else if (MinecoprocessorTileEntity.isInResetMode(fields.ports(), portIndex)) {
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Reset Port"));
			}
			if (MinecoprocessorTileEntity.isADCMode(fields.adc(), portIndex)) {
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Analog Mode"));
			} else {
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Digital Mode"));
			}
			hoveredFeature.add(ITextComponent.getTextComponentOrEmpty(String.format("0x%s %sb %s", StringUtil.toHex(value), StringUtil.toBinary(value), Integer.toString(value, 10))));
		}
	}

	private void drawFlag(MatrixStack ms, String label, Boolean flag, int x, int y, int mouseX, int mouseY) {
		drawFlag(ms, label, flag, x, y, null, mouseX, mouseY);
	}

	private void drawFlag(MatrixStack ms, String label, Boolean flag, int x, int y, Integer flashColor, int mouseX, int mouseY) {
		if (flag == null) flag = false;
		boolean mouseIsOver = drawLabeledValue(ms, label, flag ? "1" : "0", x, y, flag ? flashColor : null, mouseX, mouseY);
		if (mouseIsOver) {
			switch (label) {
			case "Z":
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Zero Flag"));
				break;
			case "C":
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Carry Flag"));
				break;
			case "F":
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Fault Indicator"));
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("STATUS 0x" + StringUtil.toHex(getContainer().getFields().fault()).toUpperCase()));
				break;
			case "S":
				hoveredFeature.add(ITextComponent.getTextComponentOrEmpty("Sleep Indicator"));
				break;
			}
			hoveredFeature.add(ITextComponent.getTextComponentOrEmpty(Boolean.toString(flag).toUpperCase()));
		}
	}

	private boolean drawLabeledValue(MatrixStack ms, String label, String value, int x, int y, Integer flashColor, int mouseX, int mouseY) {
		int wLabel = font.getStringWidth(label) / 2;
		int wValue = 0;
		if (!value.isEmpty()) {
			wValue = font.getStringWidth(value) / 2;
		}
		int color = 0xffffff;
		if ((flashColor != null) && (ModMinecoprocessors.proxy.getWorldClientSide().getGameTime() & 0xf) < 4) {
			color = flashColor;
		}
		font.drawString(ms, label, x - wLabel, y - 14, 0x404040);
		if (!value.isEmpty()) {
			font.drawString(ms, value, x - wValue, y, color);
		}
		int wMax = Math.max(wLabel, wValue);
		boolean mouseIsOver = (mouseX > (x - wMax)) && (mouseX < (x + wMax));
		mouseIsOver = mouseIsOver && mouseY > y - 14 && mouseY < y + 14;
		return mouseIsOver;
	}

	private boolean centered(MatrixStack ms, String s, float x, float y, int mouseX, int mouseY) {
		int hWidth = font.getStringWidth(s) / 2;
		int hHeight = font.FONT_HEIGHT / 2;
		int xs = (int) x - hWidth;
		int ys = (int) y - hHeight;
		font.drawString(ms, s, xs, ys, 0xffffff);
		boolean mouseIsOver = mouseX > (x - hWidth) && mouseX < (x + hWidth);
		mouseIsOver = mouseIsOver && mouseY > y - hHeight - 2 && mouseY < y + hHeight + 2;
		return mouseIsOver;
	}

	private void drawGuiTitle(MatrixStack ms) {
		String s = getContainer().getDisplayName();
		font.drawString(ms, s, getXSize() - 0.5f * font.getStringWidth(s), 10, 4210752);
	}

	private void drawCode(MatrixStack ms) {
		int x = 22;
		int y = 50;
		String label = "NEXT";
		String value = "";
		int color = 0xffffff;
		if ((!getContainer().getProcessorError().isEmpty()) || (getContainer().getFields().isFault())) {
			label = "ERROR";
			value = getContainer().getProcessorError();
			color = 0xff0000;
		} else if (getContainer().getFields().isLoaded()) {
			Processor processor = getContainer().getProcessor();
			byte[] a = null;
			if (processor != null) {
				try {
					int ip = getContainer().getFields().ip();
					List<byte[]> program = processor.getProgram();
					if (ip < program.size()) {
						a = program.get(ip);
					}
				} catch (Exception e) {
					ModMinecoprocessors.proxy.handleUnexpectedException(e);
				}
			}
			if (a != null) {
				value = InstructionUtil.compileLine(a, processor.getLabels(), (short) -1);
			}
		}
		font.drawString(ms, label, x - 4, y - 14, 0x404040);
		font.drawString(ms, value, x, y, color);
	}
}