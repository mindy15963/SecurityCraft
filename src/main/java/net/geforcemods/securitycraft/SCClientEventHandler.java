package net.geforcemods.securitycraft;

import com.mojang.blaze3d.systems.RenderSystem;

import net.geforcemods.securitycraft.api.IExplosive;
import net.geforcemods.securitycraft.blocks.SecurityCameraBlock;
import net.geforcemods.securitycraft.entity.SecurityCameraEntity;
import net.geforcemods.securitycraft.entity.SentryEntity;
import net.geforcemods.securitycraft.tileentity.SecurityCameraTileEntity;
import net.geforcemods.securitycraft.util.BlockUtils;
import net.geforcemods.securitycraft.util.GuiUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.DrawHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.GuiScreenEvent.MouseClickedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid=SecurityCraft.MODID, value=Dist.CLIENT)
public class SCClientEventHandler
{
	@SubscribeEvent
	public static void onPlayerRendered(RenderPlayerEvent.Pre event) {
		if(event.getEntity() instanceof LivingEntity && PlayerUtils.isPlayerMountedOnCamera((LivingEntity)event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onDrawBlockHighlight(DrawHighlightEvent.HighlightBlock event)
	{
		if(PlayerUtils.isPlayerMountedOnCamera(Minecraft.getInstance().player) && Minecraft.getInstance().player.getRidingEntity().getPosition().equals(event.getTarget().getPos()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void renderGameOverlay(RenderGameOverlayEvent.Post event) {
		if(event.getType() == ElementType.EXPERIENCE && Minecraft.getInstance().player != null && PlayerUtils.isPlayerMountedOnCamera(Minecraft.getInstance().player)){
			if(((BlockUtils.getBlock(Minecraft.getInstance().world, BlockUtils.toPos((int)Math.floor(Minecraft.getInstance().player.getRidingEntity().getPosX()), (int)Minecraft.getInstance().player.getRidingEntity().getPosY(), (int)Math.floor(Minecraft.getInstance().player.getRidingEntity().getPosZ()))) instanceof SecurityCameraBlock)))
				GuiUtils.drawCameraOverlay(Minecraft.getInstance(), Minecraft.getInstance().ingameGUI, Minecraft.getInstance().getMainWindow(), Minecraft.getInstance().player, Minecraft.getInstance().world, BlockUtils.toPos((int)Math.floor(Minecraft.getInstance().player.getRidingEntity().getPosX()), (int)Minecraft.getInstance().player.getRidingEntity().getPosY(), (int)Math.floor(Minecraft.getInstance().player.getRidingEntity().getPosZ())));
		}
		else if(event.getType() == ElementType.ALL)
		{
			Minecraft mc = Minecraft.getInstance();
			ClientPlayerEntity player = mc.player;
			World world = player.getEntityWorld();
			int held = player.inventory.currentItem;

			if(held < 0 || held >= player.inventory.mainInventory.size())
				return;

			ItemStack stack = player.inventory.mainInventory.get(held);

			if(!stack.isEmpty() && stack.getItem() == SCContent.CAMERA_MONITOR.get())
			{
				String textureToUse = "item_not_bound";
				double eyeHeight = player.getEyeHeight();
				Vec3d lookVec = new Vec3d((player.getPosX() + (player.getLookVec().x * 5)), ((eyeHeight + player.getPosY()) + (player.getLookVec().y * 5)), (player.getPosZ() + (player.getLookVec().z * 5)));
				RayTraceResult mop = world.rayTraceBlocks(new RayTraceContext(new Vec3d(player.getPosX(), player.getPosY() + player.getEyeHeight(), player.getPosZ()), lookVec, BlockMode.OUTLINE, FluidMode.NONE, player));

				if(mop != null && mop.getType() == Type.BLOCK && world.getTileEntity(((BlockRayTraceResult)mop).getPos()) instanceof SecurityCameraTileEntity)
				{
					CompoundNBT cameras = stack.getTag();

					if(cameras != null)
						for(int i = 1; i < 31; i++)
						{
							if(!cameras.contains("Camera" + i))
								continue;

							String[] coords = cameras.getString("Camera" + i).split(" ");

							if(Integer.parseInt(coords[0]) == ((BlockRayTraceResult)mop).getPos().getX() && Integer.parseInt(coords[1]) == ((BlockRayTraceResult)mop).getPos().getY() && Integer.parseInt(coords[2]) == ((BlockRayTraceResult)mop).getPos().getZ())
							{
								textureToUse = "item_bound";
								break;
							}
						}

					RenderSystem.enableAlphaTest();
					Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation(SecurityCraft.MODID, "textures/gui/" + textureToUse + ".png"));
					AbstractGui.blit(Minecraft.getInstance().getMainWindow().getScaledWidth() / 2 - 90 + held * 20 + 2, Minecraft.getInstance().getMainWindow().getScaledHeight() - 16 - 3, 0, 0, 16, 16, 16, 16);
					RenderSystem.disableAlphaTest();
				}
			}
			else if(!stack.isEmpty() && stack.getItem() == SCContent.REMOTE_ACCESS_MINE.get())
			{
				String textureToUse = "item_not_bound";
				double eyeHeight = player.getEyeHeight();
				Vec3d lookVec = new Vec3d((player.getPosX() + (player.getLookVec().x * 5)), ((eyeHeight + player.getPosY()) + (player.getLookVec().y * 5)), (player.getPosZ() + (player.getLookVec().z * 5)));
				RayTraceResult mop = world.rayTraceBlocks(new RayTraceContext(new Vec3d(player.getPosX(), player.getPosY() + player.getEyeHeight(), player.getPosZ()), lookVec, BlockMode.OUTLINE, FluidMode.NONE, player));

				if(mop != null && mop.getType() == Type.BLOCK && world.getBlockState(((BlockRayTraceResult)mop).getPos()).getBlock() instanceof IExplosive)
				{
					CompoundNBT mines = stack.getTag();

					if(mines != null)
						for(int i = 1; i <= 6; i++)
						{
							if(stack.getTag().getIntArray("mine" + i).length > 0)
							{
								int[] coords = mines.getIntArray("mine" + i);

								if(coords[0] == ((BlockRayTraceResult)mop).getPos().getX() && coords[1] == ((BlockRayTraceResult)mop).getPos().getY() && coords[2] == ((BlockRayTraceResult)mop).getPos().getZ())
								{
									textureToUse = "item_bound";
									break;
								}
							}
						}

					RenderSystem.enableAlphaTest();
					Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation(SecurityCraft.MODID, "textures/gui/" + textureToUse + ".png"));
					AbstractGui.blit(Minecraft.getInstance().getMainWindow().getScaledWidth() / 2 - 90 + held * 20 + 2, Minecraft.getInstance().getMainWindow().getScaledHeight() - 16 - 3, 0, 0, 16, 16, 16, 16);
					RenderSystem.disableAlphaTest();
				}
			}
			else if(!stack.isEmpty() && stack.getItem() == SCContent.REMOTE_ACCESS_SENTRY.get())
			{
				String textureToUse = "item_not_bound";
				Entity hitEntity = Minecraft.getInstance().pointedEntity;

				if(hitEntity != null && hitEntity instanceof SentryEntity)
				{
					CompoundNBT sentries = stack.getTag();

					if(sentries != null)
						for(int i = 1; i <= 12; i++)
						{
							if(stack.getTag().getIntArray("sentry" + i).length > 0)
							{
								int[] coords = sentries.getIntArray("sentry" + i);
								if(coords[0] == hitEntity.getPosition().getX() && coords[1] == hitEntity.getPosition().getY() && coords[2] == hitEntity.getPosition().getZ())
								{
									textureToUse = "item_bound";
									break;
								}
							}
						}

					RenderSystem.enableAlphaTest();
					Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation(SecurityCraft.MODID, "textures/gui/" + textureToUse + ".png"));
					AbstractGui.blit(Minecraft.getInstance().getMainWindow().getScaledWidth() / 2 - 90 + held * 20 + 2, Minecraft.getInstance().getMainWindow().getScaledHeight() - 16 - 3, 0, 0, 16, 16, 16, 16);
					RenderSystem.disableAlphaTest();
				}
			}
		}
	}

	@SubscribeEvent
	public static void fovUpdateEvent(FOVUpdateEvent event){
		if(PlayerUtils.isPlayerMountedOnCamera(event.getEntity()))
			event.setNewfov(((SecurityCameraEntity) event.getEntity().getRidingEntity()).getZoomAmount());
	}

	@SubscribeEvent
	public static void renderHandEvent(RenderHandEvent event){
		if(PlayerUtils.isPlayerMountedOnCamera(Minecraft.getInstance().player))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onMouseClicked(MouseClickedEvent.Pre event) {
		if(Minecraft.getInstance().world != null)
		{
			if(event.getButton() != 1 && Minecraft.getInstance().player.openContainer == null) //anything other than rightclick and only if no gui is open)
			{
				if(PlayerUtils.isPlayerMountedOnCamera(Minecraft.getInstance().player) && Minecraft.getInstance().player.inventory.getCurrentItem().getItem() != SCContent.CAMERA_MONITOR.get())
					event.setCanceled(true);
			}
		}
	}
}
