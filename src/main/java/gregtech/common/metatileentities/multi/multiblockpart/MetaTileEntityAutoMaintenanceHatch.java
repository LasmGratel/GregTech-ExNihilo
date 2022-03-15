package gregtech.common.metatileentities.multi.multiblockpart;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.capability.IMaintenanceHatch;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.IMetaTileEntity.IMTEGetSubBlocks;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.ConfigHolder;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;

import java.util.List;

public class MetaTileEntityAutoMaintenanceHatch extends MetaTileEntityMultiblockPart implements IMultiblockAbilityPart<IMaintenanceHatch>, IMaintenanceHatch, IMTEGetSubBlocks {

    public MetaTileEntityAutoMaintenanceHatch(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, 3);
    }

    @Override
    public IMetaTileEntity createMetaTileEntity(IGregTechTileEntity metaTileEntityHolder) {
        return new MetaTileEntityAutoMaintenanceHatch(metaTileEntityId);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (shouldRenderOverlay()) {
            Textures.MAINTENANCE_OVERLAY_FULL_AUTO.renderSided(getFrontFacing(), renderState, translation, pipeline);
        }
    }

    @Override
    public void setTaped(boolean ignored) {
    }

    @Override
    public void storeMaintenanceData(byte ignored1, int ignored2) {
    }

    @Override
    public boolean hasMaintenanceData() {
        return true;
    }

    @Override
    public Tuple<Byte, Integer> readMaintenanceData() {
        return new Tuple<>((byte) 0b111111, 0);
    }

    @Override
    public boolean isFullAuto() {
        return true;
    }

    @Override
    public double getDurationMultiplier() {
        return 1.0;
    }

    @Override
    public double getTimeMultiplier() {
        return 1.0;
    }

    @Override
    public boolean startWithoutProblems() {
        return true;
    }

    @Override
    public ModularUI createUI(EntityPlayer entityPlayer) {
        return null;
    }

    @Override
    protected boolean openGUIOnRightClick() {
        return false;
    }

    @Override
    public MultiblockAbility<IMaintenanceHatch> getAbility() {
        return MultiblockAbility.MAINTENANCE_HATCH;
    }

    @Override
    public void registerAbilities(List<IMaintenanceHatch> abilityList) {
        abilityList.add(this);
    }

    @Override
    public boolean canPartShare() {
        return false;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> blocks) {
        if (ConfigHolder.machines.enableMaintenance) {
            blocks.add(getStackForm());
        }
    }
}
