package gregtech.common.metatileentities.multi.electric;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IBatteryBlockPart;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.common.blocks.BlockGlassCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

import static gregtech.api.util.RelativeDirection.*;

public class MetaTileEntityPowerSubstation extends MultiblockWithDisplayBase {

    // Structure Constants
    private static final int MAX_BATTERY_LAYERS = 18;
    private static final int MIN_CASINGS = 14;

    // NBT Keys
    private static final String NBT_ENERGY_BANK = "EnergyBank";

    private PowerStationEnergyStorage energyBank;

    public MetaTileEntityPowerSubstation(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityPowerSubstation(metaTileEntityId);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        // todo create the storage bank from context
    }

    @Override
    protected void updateFormedValid() {

    }

    @Nonnull
    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start(RIGHT, FRONT, UP)
                .aisle("XXXXX", "XXXXX", "XXSXX", "XXXXX", "XXXXX")
                .aisle("XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX")
                .aisle("GGGGG", "GBBBG", "GBBBG", "GBBBG", "GGGGG").setRepeatable(1, MAX_BATTERY_LAYERS)
                .aisle("GGGGG", "GGGGG", "GGGGG", "GGGGG", "GGGGG")
                .where('S', selfPredicate())
                .where('X' ,states(getCasingState()).setMinGlobalLimited(MIN_CASINGS)
                        .or(abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1))
                        .or(abilities(MultiblockAbility.OUTPUT_ENERGY).setMinGlobalLimited(1)))
                .where('G', states(getGlassState()))
                .where('B', states(getBatteryStates()))
                .build();
    }

    protected IBlockState getCasingState() {
        return null; // todo
    }

    protected IBlockState getGlassState() {
        return MetaBlocks.TRANSPARENT_CASING.getState(BlockGlassCasing.CasingType.LAMINATED_GLASS);
    }

    protected IBlockState[] getBatteryStates() {
        return null; // todo
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        if (energyBank != null) {
            data.setTag(NBT_ENERGY_BANK, energyBank.writeToNBT(new NBTTagCompound()));
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey(NBT_ENERGY_BANK)) {
            energyBank = new PowerStationEnergyStorage(data.getCompoundTag(NBT_ENERGY_BANK));
        }
    }

    public static class PowerStationEnergyStorage {

        private static final String NBT_SIZE = "Size";
        private static final String NBT_STORED = "Stored";
        private static final String NBT_MAX = "Max";

        private final long[] storage;
        private final long[] maximums;
        private int index;

        // list is expected to be sorted
        public PowerStationEnergyStorage(List<IBatteryBlockPart> batteries) {
            storage = new long[batteries.size()];
            maximums = new long[batteries.size()];
            for (int i = 0; i < batteries.size(); i++) {
                maximums[i] = batteries.get(i).getCapacity();
            }
        }

        public PowerStationEnergyStorage(NBTTagCompound storageTag) {
            int size = storageTag.getInteger(NBT_SIZE);
            storage = new long[size];
            maximums = new long[size];
            for (int i = 0; i < size; i++) {
                NBTTagCompound subtag = storageTag.getCompoundTag(String.valueOf(i));
                if (subtag.hasKey(NBT_STORED)) {
                    storage[i] = subtag.getLong(NBT_STORED);
                }
                maximums[i] = subtag.getLong(NBT_MAX);
            }
        }

        private NBTTagCompound writeToNBT(NBTTagCompound compound) {
            compound.setInteger(NBT_SIZE, storage.length);
            for (int i = 0; i < storage.length; i++) {
                NBTTagCompound subtag = new NBTTagCompound();
                if (storage[i] > 0) {
                    subtag.setLong(NBT_STORED, storage[i]);
                }
                subtag.setLong(NBT_MAX, maximums[i]);
                compound.setTag(String.valueOf(i), subtag);
            }
            return compound;
        }

        /**
         * Rebuild the power storage with a new list of batteries.
         * Will use existing stored power and try to map it onto new batteries.
         * If there was more power before the rebuild operation, it will be lost.
         */
        public PowerStationEnergyStorage rebuild(@Nonnull List<IBatteryBlockPart> batteries) {
            if (batteries.size() == 0) {
                throw new IllegalArgumentException("Cannot rebuild Power Substation power bank with no batteries!");
            }
            batteries.sort(Comparator.comparingLong(IBatteryBlockPart::getCapacity));
            PowerStationEnergyStorage newStorage = new PowerStationEnergyStorage(batteries);
            for (long stored : storage) {
                newStorage.fill(stored);
            }
            return newStorage;
        }

        /** @return Amount filled into storage */
        public long fill(long amount) {
            if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative!");

            // ensure index
            if (index != storage.length - 1 && storage[index] == maximums[index]) {
                index++;
            }

            long maxFill = Math.min(maximums[index] - storage[index], amount);

            // storage is completely full
            if (maxFill == 0 && index == storage.length - 1) {
                return 0;
            }

            // fill this "battery" as much as possible
            storage[index] += maxFill;
            amount -= maxFill;

            // try to fill other "batteries" if necessary
            if (amount > 0 && index != storage.length - 1) {
                return maxFill + fill(amount);
            }

            // other fill not necessary, either because the storage is now completely full,
            // or we were able to consume all the energy in this "battery"
            return maxFill;
        }

        /** @return Amount drained from storage */
        public long drain(long amount) {
            if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative!");

            // ensure index
            if (index != 0 && storage[index] == 0) {
                index--;
            }

            long maxDrain = Math.min(storage[index], amount);

            // storage is completely empty
            if (maxDrain == 0 && index == 0) {
                return 0;
            }

            // drain this "battery" as much as possible
            storage[index] -= maxDrain;
            amount -= maxDrain;

            // try to drain other "batteries" if necessary
            if (amount > 0 && index != 0) {
                index--;
                return maxDrain + drain(amount);
            }

            // other drain not necessary, either because the storage is now completely empty,
            // or we were able to drain all the energy from this "battery"
            return maxDrain;
        }

        public BigInteger getCapacity() {
            return summarize(maximums);
        }

        public BigInteger getStored() {
            return summarize(storage);
        }

        private static BigInteger summarize(long[] values) {
            BigInteger retVal = BigInteger.ZERO;
            long currentSum = 0;
            for (long value : values) {
                if (currentSum != 0 && value > Long.MAX_VALUE - currentSum) {
                    // will overflow if added
                    retVal = retVal.add(BigInteger.valueOf(currentSum));
                    currentSum = 0;
                }
                currentSum += value;
            }
            if (currentSum != 0) {
                retVal = retVal.add(BigInteger.valueOf(currentSum));
            }
            return retVal;
        }
    }
}
