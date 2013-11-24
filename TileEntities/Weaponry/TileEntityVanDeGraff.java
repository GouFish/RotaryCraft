/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2013
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.TileEntities.Weaponry;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.item.EnumArmorMaterial;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import Reika.DragonAPI.Libraries.ReikaAABBHelper;
import Reika.DragonAPI.Libraries.ReikaEntityHelper;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;
import Reika.RotaryCraft.API.Shockable;
import Reika.RotaryCraft.Auxiliary.EntityDischarge;
import Reika.RotaryCraft.Auxiliary.RangedEffect;
import Reika.RotaryCraft.Base.TileEntity.TileEntityPowerReceiver;
import Reika.RotaryCraft.Registry.MachineRegistry;
import Reika.RotaryCraft.Registry.SoundRegistry;

public class TileEntityVanDeGraff extends TileEntityPowerReceiver implements RangedEffect {

	//In coloumbs
	private int charge;

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		super.updateTileEntity();
		this.getPowerBelow();

		charge += Math.sqrt(power)*4;

		int r = this.getRange();

		if (r > 0) {
			for (int i = 2; i < 6; i++) {
				ForgeDirection dir = dirs[i];
				int dx = x+dir.offsetX;
				int dy = y+dir.offsetY;
				int dz = z+dir.offsetZ;
				int id = world.getBlockId(dx, dy, dz);
				int metadata = world.getBlockMetadata(dx, dy, dz);
				if (id != 0) {
					Block b = Block.blocksList[id];
					Material mat = b.blockMaterial;
					boolean flag = false;
					if (b.hasTileEntity(metadata)) {
						TileEntity te = world.getBlockTileEntity(dx, dy, dz);
						if (te instanceof Shockable) {
							flag = true;
							this.dischargeToBlock(dx, dy, dz, (Shockable)te);
						}
					}
					if (!flag) {
						if (mat == Material.iron || mat == Material.anvil) {
							this.dischargeToBlock(dx, dy, dz, null);
						}
						else if (mat == Material.water) {
							this.dischargeToBlock(dx, dy, dz, null);
						}
						else if (id == Block.tnt.blockID) {
							this.dischargeToBlock(dx, dy, dz, null);
							world.setBlock(dx, dy, dz, 0);
							EntityTNTPrimed var6 = new EntityTNTPrimed(world, dx+0.5D, dy+0.5D, dz+0.5D, null);
							if (!world.isRemote)
								world.spawnEntityInWorld(var6);
							world.playSoundAtEntity(var6, "random.fuse", 1.0F, 1.0F);
							world.spawnParticle("lava", dx+rand.nextFloat(), dy+rand.nextFloat(), dz+rand.nextFloat(), 0, 0, 0);
						}
					}
				}
			}
		}
		if (charge <= 0)
			return;

		AxisAlignedBB box = ReikaAABBHelper.getBlockAABB(x, y, z).expand(r, r, r);
		EntityLivingBase e = ReikaWorldHelper.getClosestLivingEntityNoPlayers(world, x+0.5, y+0.75, z+0.5, box, true);
		if (e != null) {
			EntityDischarge d = new EntityDischarge(world, x+0.5, y+0.75, z+0.5, charge, e.posX, e.posY+e.getEyeHeight()*0.8, e.posZ);
			if (!world.isRemote) {
				this.shock(e);
				world.spawnEntityInWorld(d);
			}
			charge = 0;
		}
		if (charge > 2097152) {
			EntityLightningBolt b = new EntityLightningBolt(world, x+0.5, y, z+0.5);
			world.spawnEntityInWorld(b);
			charge = 0;
			world.setBlock(x, y, z, 0);
			world.newExplosion(null, x+0.5, y+0.5, z+0.5, 4F, true, true);
		}

		if (world.isRaining() && world.canBlockSeeTheSky(x, y+1, z)) {
			charge *= 0.5;
		}
	}

	private void dischargeToBlock(int x, int y, int z, Shockable s) {
		if (s != null) {
			int min = s.getMinDischarge();
			if (charge < min)
				return;
			s.onDischarge(charge, ReikaMathLibrary.py3d(xCoord-x, yCoord-y, zCoord-z));
		}
		SoundRegistry.SPARK.playSoundAtBlock(worldObj, xCoord, yCoord, zCoord, 1.5F, 1F);
		EntityDischarge d = new EntityDischarge(worldObj, xCoord+0.5, yCoord+0.75, zCoord+0.5, charge, x+0.5, y+0.5, z+0.5);
		if (!worldObj.isRemote)
			worldObj.spawnEntityInWorld(d);
		charge = 0;
	}

	private void shock(EntityLivingBase e) {
		int dmg = this.getAttackDamage();

		boolean insul = ReikaEntityHelper.isEntityWearingArmorOf(e, EnumArmorMaterial.CLOTH);
		if (insul)
			dmg /= 2;

		e.attackEntityFrom(DamageSource.magic, dmg);
		//ReikaJavaLibrary.pConsole(charge+":"+this.getAttackDamage());
		if (e instanceof EntityCreeper) {
			worldObj.createExplosion(e, e.posX, e.posY, e.posZ, 3F, true);
			e.attackEntityFrom(DamageSource.magic, Integer.MAX_VALUE);
		}
		SoundRegistry.SPARK.playSoundAtBlock(worldObj, xCoord, yCoord, zCoord, 1.5F, 1F);
	}

	private int getAttackDamage() {
		return 1+(int)(Math.pow(charge, 2)/(4194304*8));
	}

	public int getCharge() {
		return charge;
	}

	@Override
	public void animateWithTick(World world, int x, int y, int z) {

	}

	@Override
	public int getMachineIndex() {
		return MachineRegistry.VANDEGRAFF.ordinal();
	}

	@Override
	public boolean hasModelTransparency() {
		return false;
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public int getRange() {
		return Math.min(charge/1024, this.getMaxRange());
	}

	@Override
	public int getMaxRange() {
		return 16;
	}

	/**
	 * Writes a tile entity to NBT.
	 */
	@Override
	public void writeToNBT(NBTTagCompound NBT)
	{
		super.writeToNBT(NBT);
		NBT.setInteger("c", charge);
	}

	/**
	 * Reads a tile entity from NBT.
	 */
	@Override
	public void readFromNBT(NBTTagCompound NBT)
	{
		super.readFromNBT(NBT);
		charge = NBT.getInteger("c");
	}

}