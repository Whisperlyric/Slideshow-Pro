package org.teacon.slides.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class ProjectorOpenScreenPayload {

    public final BlockPos pos;

    public ProjectorOpenScreenPayload(BlockPos pos) {
        this.pos = pos;
    }

    public ProjectorOpenScreenPayload(RegistryFriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    public static void writeBuffer(ProjectorOpenScreenPayload payload, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.pos);
    }
}
