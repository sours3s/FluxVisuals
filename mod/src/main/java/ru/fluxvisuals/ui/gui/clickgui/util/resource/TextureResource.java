package ru.fluxvisuals.ui.gui.clickgui.util.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public class TextureResource implements ResourceSource {
    public final AbstractTexture texture;

    @Override
    public InputStream stream() {
        if ((this.texture) instanceof ResourceTexture resourceTexture ) {
            Identifier id = resourceTexture.getId();
            try {
                System.out.println("loaded resource texture");
                return openResourceStream(id);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load ResourceTexture: " + String.valueOf(id), e);
            }
        }
        NativeImageBackedTexture nativeImageBackedTexture = (NativeImageBackedTexture) (this.texture);
        if (!(nativeImageBackedTexture instanceof NativeImageBackedTexture)) {
            return null;
        }
        NativeImageBackedTexture nativeImageBackedTexture2 = nativeImageBackedTexture;
        try {
            System.out.println("loaded native image texture");
            return encodeNativeImage((NativeImage) Objects.requireNonNull(nativeImageBackedTexture2.getImage()));
        } catch (IOException e2) {
            throw new IllegalStateException("Failed to process NativeImageBackedTexture", e2);
        }
    }

    public InputStream encodeNativeImage(NativeImage nativeImage) throws IOException {
        if (!nativeImage.getFormat().isWriteable()) {
            throw new UnsupportedOperationException("Image format is not writeable: " + String.valueOf(nativeImage.getFormat()));
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (writeImage(nativeImage, Channels.newChannel(byteArrayOutputStream))) {
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }
        throw new IOException("Failed to write NativeImage to PNG: " + String.valueOf(nativeImage));
    }

    public boolean writeImage(NativeImage nativeImage, WritableByteChannel writableByteChannel) throws IOException {
        java.nio.file.Path tempPath = java.nio.file.Files.createTempFile("fluxvisuals-texture", ".png");
        nativeImage.writeTo(tempPath);
        try (java.io.InputStream inputStream = java.nio.file.Files.newInputStream(tempPath)) {
            writableByteChannel.write(java.nio.ByteBuffer.wrap(inputStream.readAllBytes()));
        } finally {
            try {
                java.nio.file.Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
            }
        }
        return true;
    }

    public InputStream openResourceStream(Identifier identifier) throws IOException {
        return ((Resource) MinecraftClient.getInstance().getResourceManager().getResource(identifier).orElseThrow(() -> {
            return new IllegalStateException("Resource not found: " + String.valueOf(identifier));
        })).getInputStream();
    }

    public TextureResource(AbstractTexture abstractTexture) {
        this.texture = abstractTexture;
    }
}
