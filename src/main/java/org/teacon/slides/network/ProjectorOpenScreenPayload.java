package org.teacon.slides.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;

public class ProjectorOpenScreenPayload {

    public final BlockPos pos;

    public ProjectorOpenScreenPayload(BlockPos pos) {
        this.pos = pos;
    }

    public ProjectorOpenScreenPayload(RegistryByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    public static void writeBuffer(ProjectorOpenScreenPayload payload, RegistryByteBuf buffer) {
        buffer.writeBlockPos(payload.pos);
    }
}
