package net.glad0s.bobberdetector.block.entity;

import net.glad0s.bobberdetector.block.TileEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;


import java.util.List;

import static net.glad0s.bobberdetector.BobberDetector.ClientModEvents.BobberTag;

public class BobberDetectorTileEntity extends BlockEntity /*implements MenuProvider*/ {
    public BobberDetectorTileEntity(BlockPos pos, BlockState state) {
        super(TileEntityInit.BOBBER_DETECTOR.get(), pos, state);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        BobberDetectorTileEntity tile = (BobberDetectorTileEntity) be;

        if (!level.isClientSide()) {
            tile.bobberScan();

        }
    }


    private int RANGE_UP = 5;
    private int RANGE_SIDE = 5;
    private int RANGE_FRONT = 5;
    private int catchTimer = 0;
    final int CATCHCOOLDOWN = 20; //Ticks until the next detection can take place
    private int redstoneTimer = 0;
    private final int REDSTONE_DURATION = 10; //Duration of the redstone pulse
    private int litRefreshTimer = 0;
    private final int LIT_RESET_TIME = 5; //how much time will pass before a missing bobber is noticed
    private boolean powered;
    private boolean lit;


    private void updatePower(boolean powered){
        BlockState blockstate = this.getBlockState();
        Block block = blockstate.getBlock();
        if(block instanceof BobberDetectorBlock){
            this.powered = powered;
            BobberDetectorBlock.setPowered(blockstate, this.level, this.worldPosition, powered);
        }
    }

    private void updateLit(boolean lit){
        BlockState blockstate = this.getBlockState();
        Block block = blockstate.getBlock();
        if(block instanceof BobberDetectorBlock){
            this.lit = lit;
            BobberDetectorBlock.setLit(blockstate, this.level, this.worldPosition, lit);
        }
    }

    private void bobberScan() {
        if (!level.isClientSide && catchTimer == 0) {
            //try to get the direction the block is facing
            BlockState blockstate = this.getBlockState();
            Direction facing = BobberDetectorBlock.getFacingDirection(blockstate);

            //create the search area
            BlockPos topCorner = this.worldPosition.relative(facing).relative(facing.getClockWise(), RANGE_SIDE / 2).offset(0, RANGE_UP / 2,0);
            BlockPos bottomCorner = this.worldPosition.relative(facing, RANGE_FRONT).relative(facing.getClockWise().getClockWise().getClockWise(), RANGE_SIDE / 2).offset(0, -RANGE_UP / 2,0);

            AABB box = new AABB(bottomCorner).minmax(new AABB(topCorner));

            List<net.minecraft.world.entity.Entity> entities = this.level.getEntities(null, box);

            for (Entity target : entities) {

                boolean isInTag = ForgeRegistries.ENTITY_TYPES.tags().getTag(BobberTag).contains(target.getType());

                if(isInTag) {

                    //set the block to lit if the timer is at 0
                    if(litRefreshTimer == 0){
                        updateLit(true);
                    }
                    //reset the timer each time it detects it.
                    litRefreshTimer = LIT_RESET_TIME;
                    double x = Math.round((target.getDeltaMovement().x * 100) * 10) / 10.0;
                    double y = target.getDeltaMovement().y;
                    double z = Math.round((target.getDeltaMovement().z * 100) * 10) / 10.0;
                    if (y < -0.075 && x == 0 && z == 0) {
                        catchTimer = CATCHCOOLDOWN;
                        redstoneTimer = REDSTONE_DURATION;
                        updatePower(true);
                        level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());

                    }
                }
            }
        }

        if (redstoneTimer > 0) {
            redstoneTimer--;
            if (redstoneTimer == 0) {
                updatePower(false);
            }
        }

        if (catchTimer > 0) {
            catchTimer--;
        }
        if(litRefreshTimer > 0) {
            litRefreshTimer--;
            if (litRefreshTimer == 0){
                updateLit(false);
            }
        }

    }

    /*
    @Override
    public Component getDisplayName() {
        return Component.literal("Bobber Detector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return
    }

    @Override
    protected void saveAdditional(CompoundTag nbt){
        nbt.putInt("RANGE_FRONT", RANGE_FRONT);
        nbt.putInt("RANGE_SIDE", RANGE_SIDE);
        nbt.putInt("RANGE_UP", RANGE_UP);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        nbt.getInt("RANGE_FRONT");
        nbt.getInt("RANGE_SIDE");
        nbt.getInt("RANGE_UP");
    }
    */

}
