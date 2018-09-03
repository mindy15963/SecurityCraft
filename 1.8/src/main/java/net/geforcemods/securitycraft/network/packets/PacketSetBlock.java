package net.geforcemods.securitycraft.network.packets;

import io.netty.buffer.ByteBuf;
import net.geforcemods.securitycraft.api.CustomizableSCTE;
import net.geforcemods.securitycraft.api.Owner;
import net.geforcemods.securitycraft.blocks.BlockSecurityCamera;
import net.geforcemods.securitycraft.tileentity.TileEntityKeypadChest;
import net.geforcemods.securitycraft.tileentity.TileEntityKeypadFurnace;
import net.geforcemods.securitycraft.tileentity.TileEntityOwnable;
import net.geforcemods.securitycraft.tileentity.TileEntitySecurityCamera;
import net.geforcemods.securitycraft.util.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSetBlock implements IMessage{

	private int x, y, z, meta;
	private String blockId;

	public PacketSetBlock(){

	}

	public PacketSetBlock(int x, int y, int z, String id, int meta){
		this.x = x;
		this.y = y;
		this.z = z;
		blockId = id;
		this.meta = meta;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		ByteBufUtils.writeUTF8String(buf, blockId);
		buf.writeInt(meta);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		blockId = ByteBufUtils.readUTF8String(buf);
		meta = buf.readInt();
	}

	public static class Handler extends PacketHelper implements IMessageHandler<PacketSetBlock, IMessage> {
		//TODO
		@Override
		public IMessage onMessage(PacketSetBlock message, MessageContext context) {
			int x = message.x;
			int y = message.y;
			int z = message.z;
			BlockPos pos = BlockUtils.toPos(x, y, z);
			String blockId = message.blockId;
			int meta = message.meta;
			EntityPlayer player = context.getServerHandler().playerEntity;
			World world = getWorld(player);
			TileEntity te = world.getTileEntity(pos);
			ItemStack[] modules = null;
			ItemStack[] inventory = null;
			int[] times = new int[4];
			String password = "";
			Owner owner = null;

			if(te instanceof CustomizableSCTE)
				modules = ((CustomizableSCTE) te).itemStacks;

			if(te instanceof TileEntityKeypadFurnace){
				inventory = ((TileEntityKeypadFurnace) te).furnaceItemStacks;
				times[0] = ((TileEntityKeypadFurnace) te).furnaceBurnTime;
				times[1] = ((TileEntityKeypadFurnace) te).currentItemBurnTime;
				times[2] = ((TileEntityKeypadFurnace) te).cookTime;
				times[3] = ((TileEntityKeypadFurnace) te).totalCookTime;
			}

			if(te instanceof TileEntityOwnable && ((TileEntityOwnable) te).getOwner() != null)
				owner = ((TileEntityOwnable) te).getOwner();

			if(te instanceof TileEntityKeypadChest && ((TileEntityKeypadChest) te).getPassword() != null)
				password = ((TileEntityKeypadChest) te).getPassword();

			Block block = (Block)Block.blockRegistry.getObject(blockId);
			getWorld(player).setBlockState(pos, meta >= 0 ? block.getStateFromMeta(meta) : block.getStateFromMeta(0));

			if(modules != null)
				((CustomizableSCTE) te).itemStacks = modules;

			if(inventory != null && te instanceof TileEntityKeypadFurnace){
				((TileEntityKeypadFurnace) te).furnaceItemStacks = inventory;
				((TileEntityKeypadFurnace) te).furnaceBurnTime = times[0];
				((TileEntityKeypadFurnace) te).currentItemBurnTime = times[1];
				((TileEntityKeypadFurnace) te).cookTime = times[2];
				((TileEntityKeypadFurnace) te).totalCookTime = times[3];
			}

			if(owner != null)
				((TileEntityOwnable) te).getOwner().set(owner);

			if(!password.isEmpty() && te instanceof TileEntityKeypadChest)
				((TileEntityKeypadChest) te).setPassword(password);

			if(te instanceof TileEntitySecurityCamera)
				world.notifyNeighborsOfStateChange(pos.offset((EnumFacing)world.getBlockState(pos).getValue(BlockSecurityCamera.FACING), -1), world.getBlockState(pos).getBlock());

			return null;
		}
	}
}
