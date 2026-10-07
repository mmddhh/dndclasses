package mdh.dndclasses.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//ai生成，因为我没学会
public class Provider<T extends INBTSerializable<CompoundTag>> implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final T instance;
    private final Capability<T> capability;
    private final LazyOptional<T> lazyOptional;

    public Provider(Capability<T> capability, T instance) {
        this.capability = capability;
        this.instance = instance;
        this.lazyOptional = LazyOptional.of(() -> this.instance);
    }

    @NotNull
    @Override
    public <C> LazyOptional<C> getCapability(@NotNull Capability<C> cap, @Nullable Direction side) {
        return capability.orEmpty(cap, lazyOptional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.deserializeNBT(nbt);
    }

    public void invalidate() {
        lazyOptional.invalidate();
    }
}
