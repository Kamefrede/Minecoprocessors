package net.torocraft.minecoprocessors.util;


import net.minecraft.nbt.CompoundNBT;

public class Label {
	public short address;
	public String name;

	public Label(short address, String name) {
		this.address = address;
		this.name = name;
	}

	public static Label fromNbt(CompoundNBT nbt) {
		return new Label(nbt.getShort("address"), nbt.getString("name"));
	}

	public CompoundNBT toNbt() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putShort("address", address);
		nbt.putString("name", name);
		return nbt;
	}

	@Override
	public String toString() {
		return name + "[" + address + "]";
	}
}