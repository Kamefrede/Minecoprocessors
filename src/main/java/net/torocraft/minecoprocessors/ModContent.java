/*
 * @file ModContent.java
 * @license GPL
 *
 * Definition and initialisation of blocks of this
 * module, along with their tile entities if applicable.
 */
package net.torocraft.minecoprocessors;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.torocraft.minecoprocessors.blocks.MinecoprocessorBlock;
import net.torocraft.minecoprocessors.blocks.MinecoprocessorContainer;
import net.torocraft.minecoprocessors.blocks.MinecoprocessorGui;
import net.torocraft.minecoprocessors.blocks.MinecoprocessorTileEntity;
import net.torocraft.minecoprocessors.items.CodeBookContainer;
import net.torocraft.minecoprocessors.items.CodeBookGui;
import net.torocraft.minecoprocessors.items.CodeBookItem;

@SuppressWarnings("unused")
public class ModContent {
	//--------------------------------------------------------------------------------------------------------------------
	// Blocks
	//--------------------------------------------------------------------------------------------------------------------

	public static final MinecoprocessorBlock MINECOPROCESSOR = (MinecoprocessorBlock) (new MinecoprocessorBlock(
			MinecoprocessorBlock.CONFIG_DEFAULT,
			AbstractBlock.Properties.create(Material.MISCELLANEOUS).hardnessAndResistance(0f).sound(SoundType.STONE).notSolid().noDrops()
	)).setRegistryName(new ResourceLocation(ModMinecoprocessors.MODID, "processor"));

	public static final MinecoprocessorBlock MINECOPROCESSOR_OVERCLOCKED = (MinecoprocessorBlock) (new MinecoprocessorBlock(
			MinecoprocessorBlock.CONFIG_OVERCLOCKED,
			AbstractBlock.Properties.create(Material.MISCELLANEOUS).hardnessAndResistance(0f).sound(SoundType.STONE).notSolid().noDrops()
	)).setRegistryName(new ResourceLocation(ModMinecoprocessors.MODID, "overclocked_processor"));
	public static final CodeBookItem CODE_BOOK = (CodeBookItem) (new CodeBookItem(
			(new Item.Properties()).group(ModMinecoprocessors.ITEMGROUP).setNoRepair().maxStackSize(1)
	).setRegistryName(ModMinecoprocessors.MODID, "code_book"));

	//--------------------------------------------------------------------------------------------------------------------
	// Items
	//--------------------------------------------------------------------------------------------------------------------
	public static final ContainerType<MinecoprocessorContainer> CT_MINECOPROCESSOR = register(MinecoprocessorContainer::new, "ct_minecoprocessor");
	public static final ContainerType<CodeBookContainer> CT_CODEBOOK = register(CodeBookContainer::new, "ct_codebook");

	//--------------------------------------------------------------------------------------------------------------------
	// Tile entities types
	//--------------------------------------------------------------------------------------------------------------------
	private static final Block[] MOD_BLOCKS = {
			MINECOPROCESSOR,
			MINECOPROCESSOR_OVERCLOCKED
	};
	public static final TileEntityType<?> TET_MINECOPROCESSOR = TileEntityType.Builder
			.create(MinecoprocessorTileEntity::new, MOD_BLOCKS)
			.build(null)
			.setRegistryName(ModMinecoprocessors.MODID, "te_processor");

	//--------------------------------------------------------------------------------------------------------------------
	// Container registration
	//--------------------------------------------------------------------------------------------------------------------
	private static final Item[] MOD_ITEMS = {
			CODE_BOOK
	};
	private static final TileEntityType<?>[] TILE_ENTITY_TYPES = {
			TET_MINECOPROCESSOR
	};
	// DON'T FORGET TO REGISTER THE GUI in registerContainerGuis(), no list/map format found yet for that.
	private static final ContainerType<?>[] CONTAINER_TYPES = {
			CT_MINECOPROCESSOR,
			CT_CODEBOOK
	};

	private static <T extends Container> ContainerType<T> register(ContainerType.IFactory<T> factory, String regname) {
		ContainerType<T> container_type = new ContainerType<T>(factory);
		container_type.setRegistryName(new ResourceLocation(ModMinecoprocessors.MODID, regname));
		return container_type;
	}

	//--------------------------------------------------------------------------------------------------------------------
	// Initialisation events
	//--------------------------------------------------------------------------------------------------------------------

	public static void registerBlocks(final RegistryEvent.Register<Block> event) {
		for (Block e : MOD_BLOCKS) event.getRegistry().register(e);
	}

	public static void registerBlockItems(final RegistryEvent.Register<Item> event) {
		for (Block e : MOD_BLOCKS) {
			ResourceLocation rl = e.getRegistryName();
			if (rl == null) continue;
			event.getRegistry().register(new BlockItem(e, (new Item.Properties().group(ModMinecoprocessors.ITEMGROUP))).setRegistryName(rl));
		}
	}

	public static void registerItems(final RegistryEvent.Register<Item> event) {
		for (Item e : MOD_ITEMS) event.getRegistry().register(e);
	}

	public static void registerTileEntities(final RegistryEvent.Register<TileEntityType<?>> event) {
		for (final TileEntityType<?> e : TILE_ENTITY_TYPES) event.getRegistry().register(e);
	}

	public static void registerContainers(final RegistryEvent.Register<ContainerType<?>> event) {
		for (final ContainerType<?> e : CONTAINER_TYPES) event.getRegistry().register(e);
	}

	@OnlyIn(Dist.CLIENT)
	public static void registerGuis(final FMLClientSetupEvent event) {
		ScreenManager.registerFactory(CT_MINECOPROCESSOR, MinecoprocessorGui::new);
		ScreenManager.registerFactory(CT_CODEBOOK, CodeBookGui::new);
	}

	@OnlyIn(Dist.CLIENT)
	public static final void processContentClientSide(final FMLClientSetupEvent event) {
		RenderTypeLookup.setRenderLayer(MINECOPROCESSOR, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(MINECOPROCESSOR_OVERCLOCKED, RenderType.getCutout());
	}

}