/*
 * @file MinecoprocessorTileEntity.java
 * @license GPL
 */
package net.torocraft.minecoprocessors.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WritableBookItem;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.ForgeEventFactory;
import net.torocraft.minecoprocessors.ModContent;
import net.torocraft.minecoprocessors.ModMinecoprocessors;
import net.torocraft.minecoprocessors.items.CodeBookItem;
import net.torocraft.minecoprocessors.processor.Processor;
import net.torocraft.minecoprocessors.processor.Register;
import net.torocraft.minecoprocessors.util.ByteUtil;
import net.torocraft.minecoprocessors.util.InstructionUtil;
import net.torocraft.minecoprocessors.util.RedstoneUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


public class MinecoprocessorTileEntity extends TileEntity implements ITickableTileEntity, INameable, IInventory, INamedContainerProvider
{
  public static final int NUM_OF_SLOTS = 1;

  private NonNullList<ItemStack> inventory = NonNullList.withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
  private final Processor processor = new Processor();
  private final byte[] prevPortValues = new byte[4];
  private byte prevPortsRegister = -1;
  private boolean inventoryChanged = false;
  private boolean inputsChanged = false;
  private boolean outputsChanged = false;
  private String customName = new String();
  private int loadTime;
  private int tickTimer = 0;

  public MinecoprocessorTileEntity()
  { super(ModContent.TET_MINECOPROCESSOR); }

  public MinecoprocessorTileEntity(TileEntityType<?> te_type)
  { super(te_type); }

  // TileEntity --------------------------------------------------------------------------------------------------------

  @Override
  public void read(CompoundNBT nbt)
  { super.read(nbt); readnbt(nbt); }

  private void readnbt(CompoundNBT nbt)
  {
    processor.setNBT(nbt.getCompound("processor"));
    inventory = NonNullList.<ItemStack>withSize(1, ItemStack.EMPTY);  // <<-- this.getSizeInventory() -> 1
    ItemStackHelper.loadAllItems(nbt, inventory);
    loadTime = nbt.getShort("loadTime");
    customName = nbt.getString("CustomName");
  }

  @Override
  public CompoundNBT write(CompoundNBT nbt)
  { return writennbt(super.write(nbt)); }

  private CompoundNBT writennbt(CompoundNBT nbt)
  {
    ItemStackHelper.saveAllItems(nbt, inventory);
    if(hasCustomName()) nbt.putString("CustomName", customName);
    nbt.put("processor", processor.getNBT());
    nbt.putShort("loadTime", (short)loadTime);
    return nbt;
  }

  // INameable ---------------------------------------------------------------------------

  @Override
  public ITextComponent getName()
  {
    if(hasCustomName()) return new StringTextComponent(customName);
    final BlockState state = getBlockState();
    return new StringTextComponent((state!=null) ? (state.getBlock().getTranslationKey()) : ("Minecoprocessor"));
  }

  @Override
  public boolean hasCustomName()
  { return (customName != null) && (!customName.isEmpty()); }

  @Override
  public ITextComponent getCustomName()
  { return getName(); }

  @Override
  public ITextComponent getDisplayName()
  { return INameable.super.getDisplayName(); }

  // INamedContainerProvider ------------------------------------------------------------------------------

  @Override
  public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player )
  { return new MinecoprocessorContainer(id, inventory, this, IWorldPosCallable.of(world, pos), fields); }

  // Container/GUI synchronization fields
  public static final class ContainerSyncFields extends IntArray
  {
    public static int NUM_OF_FIELDS = 4;  //  [0,1,2]: (sizeof(regs)/sizeof(int))+1 and flags. [3]: ip,sp,fault

    public ContainerSyncFields()
    { super(NUM_OF_FIELDS); }

    public void writeServerSide(Processor processor)
    {
      final byte[] regs = processor.getRegisters();
      set(0, 0 // IP SP FAULT
        | ((((int)processor.getIp()) & 0xffff)<< 0)
        | ((((int)processor.getSp()) & 0xff)<<16)
        | ((((int)processor.getFaultCode()) & 0xff)<<24)
      );
      set(1, (((int)regs[0]) & 0xff) | ((((int)regs[1]) & 0xff)<<8) | ((((int)regs[2]) & 0xff)<<16) | ((((int)regs[3]) & 0xff)<<24)); // A B C D
      set(2, (((int)regs[4]) & 0xff) | ((((int)regs[5]) & 0xff)<<8) | ((((int)regs[6]) & 0xff)<<16) | ((((int)regs[7]) & 0xff)<<24)); // PF PB PL PR
      set(3, (((int)regs[8]) & 0xff) | ((((int)regs[9]) & 0xff)<<8) | (0 // PORTS ADC FLAGS
        | (processor.isFault()    ? 0x0100 : 0)
        | (processor.isZero()     ? 0x0200 : 0)
        | (processor.isOverflow() ? 0x0400 : 0)
        | (processor.isCarry()    ? 0x0800 : 0)
        | (processor.isWait()     ? 0x1000 : 0)
        | (processor.isStep()     ? 0x2000 : 0)
      ));
    }

    // Client side getters
    public short ip()             { return (short)((get(0)>> 0) & 0xffff); }
    public byte sp()              { return (byte)((get(0)>>16) & 0xff); }
    public byte fault()           { return (byte)((get(0)>>24) & 0xff); }
    public byte register(int i)   { return (byte)((get(1+(i/4)) >> (8*(i&0x3))) & 0xff); }
    public byte port(int i)       { return register(i+4); }
    public byte ports()           { return register(8); }
    public byte adc()             { return register(9); }
    public boolean isFault()      { return (get(3) & 0x0100) != 0; }
    public boolean isZero()       { return (get(3) & 0x0200) != 0; }
    public boolean isOverflow()   { return (get(3) & 0x0400) != 0; }
    public boolean isCarry()      { return (get(3) & 0x0800) != 0; }
    public boolean isWait()       { return (get(3) & 0x1000) != 0; }
    public boolean isStep()       { return (get(3) & 0x2000) != 0; }
  }

  protected final ContainerSyncFields fields = new ContainerSyncFields();

  // IInventory -------------------------------------------------------------------------------------------

  @Override
  public int getSizeInventory()
  { return inventory.size(); }

  @Override
  public boolean isEmpty()
  { for(ItemStack stack: inventory) { if(!stack.isEmpty()) return false; } return true; }

  @Override
  public ItemStack getStackInSlot(int index)
  { return (index < getSizeInventory()) ? inventory.get(index) : ItemStack.EMPTY; }

  @Override
  public ItemStack decrStackSize(int index, int count)
  { onInventoryChanged(); return ItemStackHelper.getAndSplit(inventory, index, count); }

  @Override
  public ItemStack removeStackFromSlot(int index)
  { onInventoryChanged(); return ItemStackHelper.getAndRemove(inventory, index); }

  @Override
  public void setInventorySlotContents(int index, ItemStack stack)
  {
    onInventoryChanged();
    inventory.set(index, stack);
    if(stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
  }

  @Override
  public int getInventoryStackLimit()
  { return NUM_OF_SLOTS; }

  @Override
  public void markDirty()
  { super.markDirty(); }

  @Override
  public boolean isUsableByPlayer(PlayerEntity player)
  { return getPos().distanceSq(player.getPosition()) < 36; }

  @Override
  public void openInventory(PlayerEntity player)
  {}

  @Override
  public void closeInventory(PlayerEntity player)
  { markDirty(); }

  @Override
  public boolean isItemValidForSlot(int index, ItemStack stack)
  { return isValidBook(stack); }

  @Override
  public void clear()
  { onInventoryChanged(); inventory.clear(); markDirty(); }

  // ITickable ---------------------------------------------------------------------------------------------------------

  @Override
  public void tick()
  {
    if((world.isRemote()) || (--tickTimer > 0)) return;
    final BlockState blockState = world.getBlockState(pos); // @todo --> not sure if the caches getBlockState() would be good here.
    if(!(blockState.getBlock() instanceof MinecoprocessorBlock)) { tickTimer = 20; return; }
    final MinecoprocessorBlock block = (MinecoprocessorBlock)blockState.getBlock();
    tickTimer = ((block.config & MinecoprocessorBlock.CONFIG_OVERCLOCKED)!=0) ? 1 : 2;
    boolean dirty = false;
    final boolean hasBook = !inventory.get(0).isEmpty();

    // "Firmware update"
    if(inventoryChanged) {
      // @todo: ---> moved book loading/unloading to here to have it in sync with the tick.
      inventoryChanged = false;
      loadBook(inventory.get(0));
      prevPortsRegister = 0x0f;
      for(int i=0; i<prevPortValues.length; i++) prevPortValues[i] = 0;
      inputsChanged = true;
      outputsChanged = true;
      dirty = true;
    } else if(hasBook) {
      // Processor run
      if(processor.tick()) {
        outputsChanged = true;
      }
      // Port config update
      if(prevPortsRegister != processor.getRegister(Register.PORTS)) {
        prevPortsRegister = processor.getRegister(Register.PORTS);
        inputsChanged = true;
        outputsChanged = true;
      }
      // Input
      if(inputsChanged) {
        inputsChanged = false;
        updateInputPorts(blockState);
      }
    }
    if(outputsChanged) {
      outputsChanged = false;
      updateOutputs(blockState, block);
    }
    BlockState state = blockState.with(MinecoprocessorBlock.ACTIVE, hasBook && (!processor.isWait()) && (!processor.isFault()));
    if(state != blockState) {
      world.setBlockState(pos, state, 1|2|16);
    }
    fields.writeServerSide(processor);
    if(dirty) markDirty();
  }

  // Class specific methods --------------------------------------------------------------------------------------------

  public static boolean isValidBook(ItemStack stack)
  { return (!stack.isEmpty()) && ((stack.getItem() == ModContent.CODE_BOOK) || (stack.getItem() == Items.WRITTEN_BOOK) || (stack.getItem() == Items.WRITABLE_BOOK)); }

  public static boolean isInInputMode(byte ports, int portIndex)
  { return (ByteUtil.getBit(ports, portIndex)) && (!ByteUtil.getBit(ports, portIndex + 4)); }

  public static boolean isInOutputMode(byte ports, int portIndex)
  { return (!ByteUtil.getBit(ports, portIndex)) && (!ByteUtil.getBit(ports, portIndex + 4)); }

  public static boolean isADCMode(byte adc, int portIndex)
  { return ByteUtil.getBit(adc, portIndex); }

  public static boolean isInResetMode(byte ports, int portIndex)
  { return ByteUtil.getBit(ports, portIndex) && ByteUtil.getBit(ports, portIndex + 4); }

  public Processor getProcessor()
  { return processor; }

  public void resetProcessor()
  { inventoryChanged = true; }

  public void setCustomName(ITextComponent name) // Invoked from block
  { this.customName = name.getString(); }

  public void neighborChanged(BlockPos fromPos) // Invoked from block
  { inputsChanged = true; } // Update redstone input state

  public int getPower(BlockState state, Direction side, boolean strong) //  Invoked from block
  {
    // @todo: Considering making a mapping here, where we have in here only a
    //        array with N,E,S,W -> p(N), p(E), p(S), p(W). Calculation of this
    //        cache values on port change?
    //        Does this make sense for you?
    // @review: getPortSignal() already clamps 0..15, so removed RedstoneUtil.portToPower()
    int p = (RedstoneUtil.isFrontPort(state, side)) ? getPortSignal(0) : (
            (RedstoneUtil.isBackPort(state, side))  ? getPortSignal(1) : (
            (RedstoneUtil.isLeftPort(state, side))  ? getPortSignal(2) : (
            (RedstoneUtil.isRightPort(state, side)) ? getPortSignal(3) : (
            0))));
System.out.println("getPower("+side+", "+(strong?"strong":"weak")+") = "+p);
    return p;
  }

  private void onInventoryChanged() // Invoked from inventory methods
  { inventoryChanged = true; tickTimer = 0; }

  private byte getPortSignal(int portIndex)
  {
    if(!isInOutputMode(processor.getRegister(Register.PORTS), portIndex)) {
      return 0;
    }
    byte signal = processor.getRegisters()[Register.PF.ordinal() + portIndex];
    return (byte)(isADCMode(processor.getRegister(Register.ADC), portIndex)
         ? MathHelper.clamp(signal, 0, 15)
         : ((signal == 0) ? (0) : (15)));
  }

  //
  // @review: Combined block methods and detectOutputChange() (were only a few lines in summary after porting).
  //
  private boolean updateOutputs(final BlockState state, final Block block)
  {
    final Direction front = state.get(MinecoprocessorBlock.HORIZONTAL_FACING).getOpposite();
    final byte[] registers = processor.getRegisters();
    final byte ports = processor.getRegister(Register.PORTS);
    boolean updated = false;
    for(int portIndex=0; portIndex<4; ++portIndex) {
      byte curVal = registers[Register.PF.ordinal() + portIndex];
      if(prevPortValues[portIndex] == curVal) continue;
      if(!isInOutputMode(ports, portIndex)) continue;
      updated = true;
      prevPortValues[portIndex] = curVal;
      Direction side = RedstoneUtil.convertPortIndexToFacing(front, portIndex);
      BlockPos neighborPos = pos.offset(side);
System.out.println("out["+portIndex+"]=" + (((int)curVal)&0xff) + " -> " + side + " -> " + neighborPos);
      if(ForgeEventFactory.onNeighborNotify(world, pos, state, java.util.EnumSet.of(side), false).isCanceled()) continue;
      world.neighborChanged(neighborPos, block, pos);
      world.notifyNeighborsOfStateExcept(neighborPos, block, side.getOpposite());
    }
    return updated;
  }

  private boolean updateInputPorts(BlockState state)
  {
    //
    // @review: included updateInputPort() here to have for reusing local vars on the stack.
    //
    final Direction front = state.get(MinecoprocessorBlock.HORIZONTAL_FACING).getOpposite();
    final byte[] registers = processor.getRegisters();
    final byte ports = processor.getRegister(Register.PORTS);
    final byte adc = processor.getRegister(Register.ADC);
    boolean updated = false;
    boolean reset = false;
    for(int portIndex=0; portIndex<4; ++portIndex) {
      Direction side = RedstoneUtil.convertPortIndexToFacing(front, portIndex);
      int power = calculateInputStrength(pos.offset(side), side);
      byte value = (byte)((isADCMode(adc, portIndex)) ? RedstoneUtil.powerToPort(power) : ((power == 0) ? (0) : (0xff)));
      if(prevPortValues[portIndex] == value) continue; // not changed
      prevPortValues[portIndex] = value;
      if(isInInputMode(ports, portIndex)) {
        registers[Register.PF.ordinal() + portIndex] = value;
        updated = true;
      } else if((value != 0) && isInResetMode(ports, portIndex)) {
        updated = true;
        reset = true;
      }
System.out.println("inp["+portIndex+"]=" + value + " -> " + side);
    }
    if(reset) processor.reset();
    if(updated) processor.wake();
    return updated;
  }

  /**
   * Loads or unloads a book. Returns true if a book was loaded, false otherwise.
   */
  private boolean loadBook(ItemStack stack)
  {
    processor.reset();
    if(stack.isEmpty()) {
      processor.load(null);
      customName = "";
      return false;
    } else {
      if(!stack.hasTag()) return false;
      List<String> code = new ArrayList<>();
      if(stack.getItem() instanceof CodeBookItem) {
        code = CodeBookItem.Data.loadFromStack(stack).getContinuousProgram();
      } else if (stack.getItem() instanceof WritableBookItem) {
        ListNBT pages = stack.getTag().getList("pages", 8);
        for(int i = 0; i < pages.size(); ++i) {
          Collections.addAll(code, pages.getString(i).split("\\r?\\n"));
        }
      } else if (stack.getItem() instanceof WrittenBookItem) {
        ListNBT pages = stack.getTag().getList("pages", 8);
        for(int i = 0; i < pages.size(); ++i) {
          try {
            ITextComponent tc = ITextComponent.Serializer.fromJsonLenient(pages.getString(i));
            Collections.addAll(code, tc.getUnformattedComponentText().split("\\r?\\n"));
          } catch(Exception e) {
            // don't load, will result in a processor fault flag displayed.
          }
        }
      }
      if(customName.isEmpty()) customName = readNameFromHeader(code);
      return processor.load(code);
    }
  }

  private static String readNameFromHeader(List<String> code)
  {
    try {
      List<String> nameSearch = InstructionUtil.regex("^\\s*;\\s*(.*)", code.get(0), Pattern.CASE_INSENSITIVE);
      if (nameSearch.size() != 1) {
        return "";
      }
      String name = nameSearch.get(0);
      if((name != null) && (!name.isEmpty())) {
        return name;
      }
    } catch (Exception e) {
      ModMinecoprocessors.proxy.handleUnexpectedException(e);
    }
    return "";
  }

  private int calculateInputStrength(BlockPos pos, Direction side)
  {
    BlockState adjacent = world.getBlockState(pos);
    Block block = adjacent.getBlock();
    int p = world.getRedstonePower(pos, side);
    if (p >= 15) return 15;
    // @todo: check if for modded compat something like "instanceof RedstoneWireBlock" is good. Would be less performance however.
    return Math.max(p, ((block == Blocks.REDSTONE_WIRE) ? adjacent.get(RedstoneWireBlock.POWER) : 0));
  }

}


// ---------------------------------------------------------------------------------------------------------------------
// PORTING: LEFT CODE FROM 1.12 TILE ENTITY
// ---------------------------------------------------------------------------------------------------------------------
//  private Set<ServerPlayerEntity> playersToUpdate = new HashSet<ServerPlayerEntity>();
//
//  public void updatePlayers() {
//    for (EntityPlayerMP player : playersToUpdate) {
//      Minecoprocessors.NETWORK.sendTo(new MessageProcessorUpdate(processor.writeToNBT(), pos, getName()), player);
//    }
//  }
//  public void enablePlayerGuiUpdates(EntityPlayerMP player, boolean enable) {
//    if (enable) {
//      playersToUpdate.add(player);
//      updatePlayers();
//    } else {
//      playersToUpdate.remove(player);
//    }
//  }
