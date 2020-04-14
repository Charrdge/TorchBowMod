package mod.torchbowmod;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

//import static mod.torchbowmod.TorchBowMod.MODID;
//import static mod.torchbowmod.TorchBowMod.glowstonetorch;

public class TorchBow extends Item 
{
    public TorchBow() 
    {
        this.maxStackSize = 1;
        this.setMaxDamage(384);
        this.setCreativeTab(CreativeTabs.COMBAT);
        this.setUnlocalizedName("torchbow");
        this.setRegistryName(new ResourceLocation(TorchBowMod.MODID, "torchbow"));
        this.addPropertyOverride(new ResourceLocation("pull"), new IItemPropertyGetter() 
        {
            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) 
            {
                if (entityIn == null) 
                {
                    return 0.0F;
                } 
                else 
                {
                    return entityIn.getActiveItemStack().getItem() != Items.BOW ? 0.0F : (float) (stack.getMaxItemUseDuration() - entityIn.getItemInUseCount()) / 20.0F;
                }
            }
        });
        this.addPropertyOverride(new ResourceLocation("pulling"), new IItemPropertyGetter() 
        {
            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) 
            {
                return entityIn != null && entityIn.isHandActive() && entityIn.getActiveItemStack() == stack ? 1.0F : 0.0F;
            }
        });
    }

    /**
     * Return player ItemStack with needing item/items
     */
    private ItemStack GetPlayerAmmo(EntityPlayer player)
    {        
        if (IsAmmo(player.getHeldItemOffhand())) return player.getHeldItemOffhand();
        else if (IsAmmo(player.getHeldItemMainhand())) return player.getHeldItemMainhand();
        else
        {
            for (int i = 0; i < player.inventory.getSizeInventory(); ++i)
            {
                if (IsAmmo(player.inventory.getStackInSlot(i))) return player.inventory.getStackInSlot(i);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Return stack to ammo compatibility
     */
    private boolean IsAmmo(ItemStack stack)
    {
        Item item = stack.getItem();

        if (item == new ItemStack(Blocks.TORCH).getItem()) return true; //Default torch
        else if (item == new ItemStack(Blocks.REDSTONE_TORCH).getItem()) return true; //Redstone torch
        else if (TorchBowMod.glowstonetorch != null && item == new ItemStack(TorchBowMod.glowstonetorch).getItem()) return true; //Glowtorch from galacticraft
        else if (TorchBowMod.torchbandolier != null && item == new ItemStack(TorchBowMod.torchbandolier).getItem()) //Torch container from Torchbandolier mod
        {
            NBTTagCompound nbt = stack.getTagCompound();

            nbt = nbt.getCompoundTag("TorchBandolier");

            if (nbt.hasKey("Count") && nbt.getInteger("Count") > 0) return true;
        }
        else if (TorchBowMod.torchbinder != null && item == new ItemStack(TorchBowMod.torchbinder).getItem()) //Torch container from silentGems mod
        {
            if (stack.getTagCompound().getInteger("BlockCount") > 0) return true;
        }
        return false;
    }

    /**
     * Return standing block for this ItemStack
     */
    private Block GetAmmoBlock(ItemStack stack)
    {
        if (stack.getItem() == new ItemStack(Blocks.TORCH).getItem()) return Blocks.TORCH;
        if (stack.getItem() == new ItemStack(Blocks.REDSTONE_TORCH).getItem()) return Blocks.REDSTONE_TORCH;
        if (TorchBowMod.glowstonetorch != null && stack.getItem() == new ItemStack(TorchBowMod.glowstonetorch).getItem()) return TorchBowMod.glowstonetorch;
        if (TorchBowMod.torchbandolier != null && stack.getItem() == new ItemStack(TorchBowMod.torchbandolier).getItem()) return Blocks.TORCH;
        if (TorchBowMod.torchbinder != null && stack.getItem() == new ItemStack(TorchBowMod.torchbinder).getItem()) return Blocks.TORCH;
        throw new java.lang.Error("Ammo not converted to block");
    }

    /**
     * Delete one arrow from ammo
     */
    private void RemoveOneAmmo(EntityPlayer player, ItemStack stack, World worldIn)
    {
        if (TorchBowMod.torchbandolier != null && stack.getItem() == new ItemStack(TorchBowMod.torchbandolier).getItem())
        {
            if (!worldIn.isRemote) {
                NBTTagCompound nbt = stack.getTagCompound().getCompoundTag("TorchBandolier");
                int size = nbt.getInteger("Count");
                nbt.setInteger("Count", --size);
            }
        }
        else if (TorchBowMod.torchbinder != null && stack.getItem() == new ItemStack(TorchBowMod.torchbinder).getItem())
        {
            if (!worldIn.isRemote) {
                int size = stack.getTagCompound().getInteger("BlockCount");
                stack.getTagCompound().setInteger("BlockCount", --size);
            }
        }
        else
        {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.inventory.deleteStack(stack);
            }
        }
    }

    /**
     * Called when the player stops using an Item (stops holding the right mouse button).
     */
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft) {
        if (entityLiving instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer) entityLiving;
            
            boolean needammo = !entityplayer.capabilities.isCreativeMode && !(EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, stack) > 0);
            
            ItemStack itemstack = this.GetPlayerAmmo(entityplayer);

            Block block;
            if (itemstack != ItemStack.EMPTY) block = GetAmmoBlock(itemstack);
            else if (!needammo) block = Blocks.TORCH;
            else return;

            int i = this.getMaxItemUseDuration(stack) - timeLeft;
            i = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(stack, worldIn, entityplayer, i, true);

            float velocity = getArrowVelocity(i);

            if ((double) velocity >= 0.1D) {
                EntityTorch entityarrow = new EntityTorch(worldIn, entityplayer);
                entityarrow.setSetingBlock(block);

                if (!worldIn.isRemote) {
                    entityarrow.shoot(entityplayer, entityplayer.rotationPitch, entityplayer.rotationYaw, 0.0F, velocity * 3.0F, 1.0F);

                    if (velocity == 1.0F) entityarrow.setIsCritical(true);

                    int enchpower = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);

                    if (enchpower > 0) entityarrow.setDamage(entityarrow.getDamage() + (double) enchpower * 0.5D + 0.5D);

                    int k = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, stack);

                    if (k > 0) entityarrow.setKnockbackStrength(k);

                    if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, stack) > 0) entityarrow.setFire(100);

                    if (needammo) RemoveOneAmmo(entityplayer, itemstack, worldIn);

                    if (entityplayer.capabilities.isCreativeMode) entityarrow.pickupStatus = EntityTorch.PickupStatus.CREATIVE_ONLY;

                    worldIn.spawnEntity(entityarrow);
                }

                worldIn.playSound((EntityPlayer) null, entityplayer.posX, entityplayer.posY, entityplayer.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + velocity * 0.5F);


                entityplayer.addStat(StatList.getObjectUseStats(this));
            }
        }
    }

    /**
     * Gets the velocity of the arrow entity from the bow's charge
     */
    private static float getArrowVelocity(int charge) {
        float f = (float) charge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;

        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    /**
     * How long it takes to use or consume an item
     */
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    /**
     * returns the action that specifies what animation to play when the item is being used
     */
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    /**
     * Called when the equipped item is right clicked.
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        boolean hasammo = !this.GetPlayerAmmo(playerIn).isEmpty();

        ActionResult<ItemStack> ret = net.minecraftforge.event.ForgeEventFactory.onArrowNock(itemstack, worldIn, playerIn, handIn, hasammo);
        if (ret != null) return ret;

        if (!playerIn.capabilities.isCreativeMode && !hasammo) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, itemstack);
        } else {
            playerIn.setActiveHand(handIn);
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemstack);
        }
    }

    /**
     * Return the enchantability factor of the item, most of the time is based on material.
     */
    public int getItemEnchantability() {
        return 1;
    }
}