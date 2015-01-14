package com.cricketcraft.chisel.carving;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class CarvingVariation implements Comparable<CarvingVariation> {

	public CarvingVariation(Block block, int metadata, int ord) {
		this.order = ord;
		this.block = block;
		this.meta = metadata;
		this.damage = metadata;
	}

	@Override
	public int compareTo(CarvingVariation a) {
		return order - a.order;
	}

	public int order;
	public Block block;
	public int meta;
	public int damage;

	public ItemStack getStack() {
		return new ItemStack(block, 1, damage);
	}

}
