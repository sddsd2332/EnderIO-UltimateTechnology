package crazypants.enderio.machine;

import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.api.client.gui.IResourceTooltipProvider;

import crazypants.enderio.BlockEio;
import crazypants.enderio.EnderIO;
import crazypants.enderio.IModObject;
import crazypants.enderio.network.PacketHandler;
import crazypants.enderio.paint.IPaintable;
import crazypants.enderio.paint.PainterUtil2;
import crazypants.enderio.render.EnumRenderMode;
import crazypants.enderio.render.IBlockStateWrapper;
import crazypants.enderio.render.IOMode;
import crazypants.enderio.render.IRenderMapper;
import crazypants.enderio.render.ISmartRenderAwareBlock;
import crazypants.enderio.render.SmartModelAttacher;
import crazypants.enderio.render.TextureRegistry;
import crazypants.enderio.render.TextureRegistry.TextureSupplier;
import crazypants.enderio.render.pipeline.BlockStateWrapperBase;
import crazypants.enderio.waila.IWailaInfoProvider;
import crazypants.util.NullHelper;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AbstractMachineBlock<T extends AbstractMachineEntity> extends BlockEio<T> implements IGuiHandler, IResourceTooltipProvider,
    IWailaInfoProvider, ISmartRenderAwareBlock, IBlockColor {
  
  public static final TextureSupplier selectedFaceIcon = TextureRegistry.registerTexture("blocks/overlays/selectedFace");

  protected final Random random;

  protected final IModObject modObject;

  static {
    PacketHandler.INSTANCE.registerMessage(PacketIoMode.class, PacketIoMode.class, PacketHandler.nextID(), Side.SERVER);
    PacketHandler.INSTANCE.registerMessage(PacketItemBuffer.class, PacketItemBuffer.class, PacketHandler.nextID(), Side.SERVER);
    PacketHandler.INSTANCE.registerMessage(PacketPowerStorage.class, PacketPowerStorage.class, PacketHandler.nextID(), Side.CLIENT);
  }

  protected AbstractMachineBlock(@Nonnull IModObject mo, @Nullable Class<T> teClass, @Nonnull Material mat) {
    super(mo.getUnlocalisedName(), teClass, mat);
    modObject = mo;
    setHardness(2.0F);
    setSoundType(SoundType.METAL);
    setHarvestLevel("pickaxe", 0);
    random = new Random();    
    initDefaultState();
  }

  protected void initDefaultState() {
    setDefaultState(this.blockState.getBaseState().withProperty(EnumRenderMode.RENDER, EnumRenderMode.AUTO));
  }

  protected AbstractMachineBlock(@Nonnull IModObject mo, @Nullable Class<T> teClass) {
    this(mo, teClass, new Material(MapColor.IRON));
  }

  @Override
  protected void init() {
    super.init();
    EnderIO.guiHandler.registerGuiHandler(getGuiId(), this);
    registerInSmartModelAttacher();
  }
  
  protected void registerInSmartModelAttacher() {
    SmartModelAttacher.register(this);
  }
  
  @Override
  protected @Nonnull BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, new IProperty[] { EnumRenderMode.RENDER });
  }

  @Override
  public @Nonnull IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(@Nonnull IBlockState state) {
    return 0;
  }

  @Override
  public @Nonnull IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
    return getDefaultState();
  }

  @Override
  @SideOnly(Side.CLIENT)
  public final @Nonnull IBlockState getExtendedState(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
    NullHelper.untrusted(state, "Parameter IBlockState to Block.getExtendedState()");
    NullHelper.untrusted(world, "Parameter IBlockAccess to Block.getExtendedState()");
    NullHelper.untrusted(pos, "Parameter BlockPos to Block.getExtendedState()");

    IBlockStateWrapper blockStateWrapper = createBlockStateWrapper(state, world, pos);
    T tileEntity = getTileEntity(world, pos);
    if (tileEntity != null) {
      setBlockStateWrapperCache(blockStateWrapper, world, pos, tileEntity);
    }
    blockStateWrapper.bakeModel();
    return blockStateWrapper;
  }

  protected abstract void setBlockStateWrapperCache(@Nonnull IBlockStateWrapper blockStateWrapper, @Nonnull IBlockAccess world, @Nonnull BlockPos pos,
      @Nonnull T tileEntity);

  protected @Nonnull BlockStateWrapperBase createBlockStateWrapper(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
    return new BlockStateWrapperBase(state, world, pos, getBlockRenderMapper());
  }

  @Override
  @SideOnly(Side.CLIENT)
  public @Nonnull BlockRenderLayer getBlockLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  @Override
  protected boolean openGui(World world, BlockPos pos, EntityPlayer entityPlayer, EnumFacing side) {   
    if(!world.isRemote) {
      final EnderIO instance2 = EnderIO.instance;
      if (instance2 != null) {
        entityPlayer.openGui(instance2, getGuiId(), world, pos.getX(), pos.getY(), pos.getZ());
      } else {
        // TODO handle null value
      }
    }
    return true;
  }
  
  @Override
  public boolean canSilkHarvest(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer player) {
    return false;
  }  

  @Override
  public boolean doNormalDrops(IBlockAccess world, BlockPos pos) { 
    return false;
  }

  @Override
  protected void processDrop(IBlockAccess world, BlockPos pos, @Nullable AbstractMachineEntity te, ItemStack drop) {
    if(te != null) {
      te.writeToItemStack(drop);
    }
  }

  @Override
  public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase player,
      @Nonnull ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, player, stack);
    int heading = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
    AbstractMachineEntity te = getTileEntity(world, pos);
    if (te != null) {
      te.setFacing(getFacingForHeading(heading));
      te.readFromItemStack(stack);
    }
    if(world.isRemote) {
      return;
    }        
    world.notifyBlockUpdate(pos, state, state, 3);    
  }

  protected EnumFacing getFacingForHeading(int heading) {
    switch (heading) {
    case 0:
      return EnumFacing.NORTH;
    case 1:
      return EnumFacing.EAST;
    case 2:
      return EnumFacing.SOUTH;
    case 3:
    default:
      return EnumFacing.WEST;
    }
  }

  @Override
  public void onBlockAdded(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    super.onBlockAdded(world, pos,state);
    world.notifyBlockUpdate(pos, state, state, 3);
  }
  
  @Override
  public void neighborChanged(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Block neighborBlock) {
    TileEntity ent = world.getTileEntity(pos);
    if(ent instanceof AbstractMachineEntity) {
      AbstractMachineEntity te = (AbstractMachineEntity) ent;
      te.onNeighborBlockChange(neighborBlock);
//      randomDisplayTick(worldIn, pos, state, rand);
    }    
  }

  
  
  
  @SideOnly(Side.CLIENT)
  @Override
  public void randomDisplayTick(@Nonnull IBlockState bs, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Random rand) {
    // If active, randomly throw some smoke around
    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();
    if(isActive(world, pos)) {
      float startX = x + 1.0F;
      float startY = y + 1.0F;
      float startZ = z + 1.0F;
      for (int i = 0; i < 4; i++) {
        float xOffset = -0.2F - rand.nextFloat() * 0.6F;
        float yOffset = -0.1F + rand.nextFloat() * 0.2F;
        float zOffset = -0.2F - rand.nextFloat() * 0.6F;
        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, startX + xOffset, startY + yOffset, startZ + zOffset, 0.0D, 0.0D, 0.0D);
      }
    }
  }

  protected abstract int getGuiId();

  protected boolean isActive(@Nonnull IBlockAccess blockAccess, @Nonnull BlockPos pos) {
    AbstractMachineEntity te = getTileEntity(blockAccess, pos);
    if (te != null) {
      return te.isActive();
    }
    return false;
  }
  
  @Deprecated
  protected boolean isActive(@Nonnull IBlockAccess blockAccess, int x, int y, int z) {
    return isActive(blockAccess, new BlockPos(x,y,z));
  }

  @Override
  public String getUnlocalizedNameForTooltip(ItemStack stack) {
    return getUnlocalizedName();
  }

  @Override
  public void getWailaInfo(List<String> tooltip, EntityPlayer player, World world, int x, int y, int z) {
  }

  @Override
  public int getDefaultDisplayMask(World world, int x, int y, int z) {
    return IWailaInfoProvider.ALL_BITS;
  }

  @SideOnly(Side.CLIENT)
  public IOMode.EnumIOMode mapIOMode(IoMode mode, EnumFacing side) {
    switch (mode) {
    case NONE:
      return IOMode.EnumIOMode.NONE;
    case PULL:
      return IOMode.EnumIOMode.PULL;
    case PUSH:
      return IOMode.EnumIOMode.PUSH;
    case PUSH_PULL:
      return IOMode.EnumIOMode.PUSHPULL;
    case DISABLED:
      return IOMode.EnumIOMode.DISABLED;
    }
    throw new RuntimeException("Hey, leave our enums alone!");
  }

  @Override
  @SideOnly(Side.CLIENT)
  public IRenderMapper.IItemRenderMapper getItemRenderMapper() {
    return RenderMappers.BODY_MAPPER;
  }

  @SideOnly(Side.CLIENT)
  public IRenderMapper.IBlockRenderMapper getBlockRenderMapper() {
    return RenderMappers.BODY_MAPPER;
  }

  // ///////////////////////////////////////////////////////////////////////
  // PAINT START
  // ///////////////////////////////////////////////////////////////////////

  public IBlockState getFacade(IBlockAccess world, BlockPos pos, EnumFacing side) {
    if (world == null || pos == null) {
      return null;
    }
    return getPaintSource(getDefaultState(), world, pos);
  }

  public void setPaintSource(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nullable IBlockState paintSource) {
    if (this instanceof IPaintable) {
      T te = getTileEntity(world, pos);
      if (te instanceof IPaintable.IPaintableTileEntity) {
        ((IPaintable.IPaintableTileEntity) te).setPaintSource(paintSource);
      }
    }
  }

  public void setPaintSource(@Nonnull Block block, @Nonnull ItemStack stack, @Nullable IBlockState paintSource) {
    if (this instanceof IPaintable) {
      PainterUtil2.setSourceBlock(stack, paintSource);
    }
  }

  public @Nullable IBlockState getPaintSource(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
    if (this instanceof IPaintable) {
      T te = getTileEntity(world, pos);
      if (te instanceof IPaintable.IPaintableTileEntity) {
        return ((IPaintable.IPaintableTileEntity) te).getPaintSource();
      }
    }
    return null;
  }

  public @Nullable IBlockState getPaintSource(@Nonnull Block block, @Nonnull ItemStack stack) {
    if (this instanceof IPaintable) {
      return PainterUtil2.getSourceBlock(stack);
    }
    return null;
  }

  @Override
  public boolean canRenderInLayer(@Nonnull IBlockState state, @Nonnull BlockRenderLayer layer) {
    return this instanceof IPaintable ? true : super.canRenderInLayer(state, layer);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public int colorMultiplier(@Nonnull IBlockState state, @Nullable IBlockAccess worldIn, @Nullable BlockPos pos, int renderPass) {
    if (this instanceof IPaintable && worldIn != null && pos != null) {
      IBlockState paintSource = getPaintSource(worldIn.getBlockState(pos), worldIn, pos);
      if (paintSource != null && paintSource.getBlock() instanceof IBlockColor) {
        try {
          return ((IBlockColor)paintSource.getBlock()).colorMultiplier(state, worldIn, pos, renderPass);
        } catch (Throwable e) {
        }
      }
    }
    return -1;
  }

  // ///////////////////////////////////////////////////////////////////////
  // PAINT END
  // ///////////////////////////////////////////////////////////////////////

}
