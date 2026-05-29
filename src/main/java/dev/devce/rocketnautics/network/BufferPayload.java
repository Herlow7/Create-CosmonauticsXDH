package dev.devce.rocketnautics.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Arrays;
import java.util.function.Consumer;

public abstract class BufferPayload implements CustomPacketPayload {
    private final byte[] data;

    protected BufferPayload(FriendlyByteBuf fromStreamCodec) {
        data = fromStreamCodec.readByteArray();
    }

    protected void toStreamCodec(FriendlyByteBuf buf) {
        buf.writeByteArray(data);
    }

    protected static  <T extends BufferPayload> StreamCodec<FriendlyByteBuf, T> codec(StreamDecoder<FriendlyByteBuf, T> constructor) {
        return StreamCodec.of(
                (buf, payload) -> payload.toStreamCodec(buf),
                constructor
        );
    }

    protected BufferPayload(Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(buf);
        data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();
    }

    public final void handle() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBytes(data);
        read(buf);
        buf.release();
    }

    protected abstract void read(FriendlyByteBuf buf);

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BufferPayload) obj;
        return Arrays.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return "BufferPayload[" +
                "data=" + Arrays.toString(data) + ']';
    }

}
