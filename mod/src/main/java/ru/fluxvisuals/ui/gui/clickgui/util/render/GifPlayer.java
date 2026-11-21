package ru.fluxvisuals.ui.gui.clickgui.util.render;
import ru.fluxvisuals.ui.gui.clickgui.util.ImageBufferUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ResourceSource;
import ru.fluxvisuals.ui.gui.clickgui.TextureAnimationFrame;

import java.util.List;
import java.util.stream.Collectors;

public class GifPlayer {
    public final List<TextureAnimationFrame> frames;
    public int currentFrame = 0;
    public long lastFrameTime = System.currentTimeMillis();

    public GifPlayer(ResourceSource resourceSource) {
        this.frames = loadFrames(resourceSource);
    }

    public List<TextureAnimationFrame> loadFrames(ResourceSource resourceSource) {
        return (List) new GifDecoder(resourceSource.stream()).frames().stream().map(gifFrame -> {
            return new TextureAnimationFrame(gifFrame, new GlTexture(ImageBufferUtil.convertToByteBuffer(gifFrame.image())).setDimensions(gifFrame.image().getWidth(), gifFrame.image().getHeight()));
        }).collect(Collectors.toList());
    }

    public void updateFrame() {
        if (System.currentTimeMillis() - this.lastFrameTime >= this.frames.get(this.currentFrame).delay()) {
            this.currentFrame = (this.currentFrame + 1) % this.frames.size();
            this.lastFrameTime = System.currentTimeMillis();
        }
    }

    public GlTexture currentImage() {
        if (this.frames.isEmpty()) {
            throw new IllegalStateException("No frames loaded");
        }
        return this.frames.get(this.currentFrame).texture();
    }

    public int getCurrentFrame() {
        return this.currentFrame;
    }
}
