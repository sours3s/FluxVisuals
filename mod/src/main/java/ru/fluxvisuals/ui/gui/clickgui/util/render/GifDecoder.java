package ru.fluxvisuals.ui.gui.clickgui.util.render;
import ru.fluxvisuals.ui.gui.clickgui.GifFrame;
import ru.fluxvisuals.ui.gui.clickgui.util.StencilBufferUtil;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GifDecoder {
    public static final int MAX_STACK_SIZE = 4096;
    public static final int STATUS_OK = 0;
    public static final int STATUS_FORMAT_ERROR = 1;
    public static final int STATUS_OPEN_ERROR = 2;
    public BufferedInputStream inputStream;
    public int status;
    public int width;
    public int height;
    public boolean gctFlag;
    public int gctSize;
    public int[] gct;
    public int[] lct;
    public int[] act;
    public int bgIndex;
    public int bgColor;
    public int lastBgColor;
    public int pixelAspect;
    public boolean lctFlag;
    public boolean interlace;
    public int lctSize;
    public int ix;
    public int iy;
    public int iw;
    public int ih;

    public Rectangle lastRect;

    public BufferedImage currentImage;

    public BufferedImage lastImage;
    public int dispose;
    public int lastDispose;
    public boolean transparency;
    public int delay;
    public int transIndex;

    public short[] prefix;

    public byte[] suffix;

    public byte[] pixelStack;

    public byte[] pixels;

    public List<GifFrame> frames;
    public int frameCount;
    public int loopCount = 0;

    public final byte[] block = new byte[256];
    public int blockSize = 0;

    public GifDecoder(InputStream inputStream) {
        this.status = 0;
        try {
            if (inputStream != null) {
                inputStream = inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream);
                this.inputStream = (BufferedInputStream) inputStream;
                this.frames = new ArrayList();
                readHeader();
                if (!hasError()) {
                    readContents();
                    if (this.frameCount < 0) {
                        this.status = 1;
                    }
                }
            } else {
                this.status = 2;
            }
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
            this.status = 2;
        }
    }

    public int frameDelay(int i) {
        this.delay = -1;
        if (i >= 0 && i < this.frameCount) {
            this.delay = this.frames.get(i).delay();
        }
        return this.delay;
    }

    public void setPixels() {
        int[] data = ((DataBufferInt) this.currentImage.getRaster().getDataBuffer()).getData();
        if (this.lastDispose > 0) {
            if (this.lastDispose == 3) {
                int i = this.frameCount - 2;
                if (i > 0) {
                    this.lastImage = frameImage(i - 1);
                } else {
                    this.lastImage = null;
                }
            }
            if (this.lastImage != null) {
                System.arraycopy(((DataBufferInt) this.lastImage.getRaster().getDataBuffer()).getData(), 0, data, 0, this.width * this.height);
                if (this.lastDispose == 2) {
                    Graphics2D graphics2DCreateGraphics = this.currentImage.createGraphics();
                    graphics2DCreateGraphics.setColor(this.transparency ? new Color(0, 0, 0, 0) : new Color(this.lastBgColor));
                    graphics2DCreateGraphics.setComposite(AlphaComposite.Src);
                    graphics2DCreateGraphics.fill(this.lastRect);
                    graphics2DCreateGraphics.dispose();
                }
            }
        }
        int i2 = 1;
        int i3 = 8;
        int i4 = 0;
        for (int i5 = 0; i5 < this.ih; i5++) {
            int i6 = i5;
            if (this.interlace) {
                if (i4 >= this.ih) {
                    i2++;
                    switch (i2) {
                        case STATUS_OPEN_ERROR:
                            i4 = 4;
                            break;
                        case 3:
                            i4 = 2;
                            i3 = 4;
                            break;
                        case 4:
                            i4 = 1;
                            i3 = 2;
                            break;
                    }
                }
                i6 = i4;
                i4 += i3;
            }
            int i7 = i6 + this.iy;
            if (i7 < this.height) {
                int i8 = i7 * this.width;
                int i9 = i8 + this.ix;
                int i10 = i9 + this.iw;
                if (i8 + this.width < i10) {
                    i10 = i8 + this.width;
                }
                int i11 = i5 * this.iw;
                while (i9 < i10) {
                    int i12 = i11;
                    i11++;
                    int i13 = this.act[this.pixels[i12] & 255];
                    if (i13 != 0) {
                        data[i9] = i13;
                    }
                    i9++;
                }
            }
        }
    }

    public BufferedImage frameImage(int i) {
        BufferedImage bufferedImageImage = null;
        if (i >= 0 && i < this.frameCount) {
            bufferedImageImage = this.frames.get(i).image();
        }
        return bufferedImageImage;
    }

    public boolean hasError() {
        return this.status != 0;
    }

    public void decodeImageData() {
        int i = this.iw * this.ih;
        if (this.pixels == null || this.pixels.length < i) {
            this.pixels = new byte[i];
        }
        if (this.prefix == null) {
            this.prefix = new short[MAX_STACK_SIZE];
        }
        if (this.suffix == null) {
            this.suffix = new byte[MAX_STACK_SIZE];
        }
        if (this.pixelStack == null) {
            this.pixelStack = new byte[4097];
        }
        int i2 = read();
        int i3 = 1 << i2;
        int i4 = i3 + 1;
        int i5 = i3 + 2;
        int i6 = -1;
        int i7 = i2 + 1;
        int i8 = (1 << i7) - 1;
        for (int i9 = 0; i9 < i3; i9++) {
            this.prefix[i9] = 0;
            this.suffix[i9] = (byte) i9;
        }
        int i10 = 0;
        int i11 = 0;
        int i12 = 0;
        int i13 = 0;
        int iMethod006 = 0;
        int i14 = 0;
        int i15 = 0;
        int i16 = 0;
        while (i16 < i) {
            if (i12 == 0) {
                if (i14 < i7) {
                    if (iMethod006 == 0) {
                        iMethod006 = readBlock();
                        if (iMethod006 <= 0) {
                            break;
                        } else {
                            i10 = 0;
                        }
                    }
                    i15 += (this.block[i10] & 255) << i14;
                    i14 += 8;
                    i10++;
                    iMethod006--;
                } else {
                    int i17 = i15 & i8;
                    i15 >>= i7;
                    i14 -= i7;
                    if (i17 > i5 || i17 == i4) {
                        break;
                    }
                    if (i17 == i3) {
                        i7 = i2 + 1;
                        i8 = (1 << i7) - 1;
                        i5 = i3 + 2;
                        i6 = -1;
                    } else if (i6 == -1) {
                        int i18 = i12;
                        i12++;
                        this.pixelStack[i18] = this.suffix[i17];
                        i6 = i17;
                        i13 = i17;
                    } else {
                        if (i17 == i5) {
                            int i19 = i12;
                            i12++;
                            this.pixelStack[i19] = (byte) i13;
                            i17 = i6;
                        }
                        while (i17 > i3) {
                            int i20 = i12;
                            i12++;
                            this.pixelStack[i20] = this.suffix[i17];
                            i17 = this.prefix[i17];
                        }
                        i13 = this.suffix[i17] & 255;
                        if (i5 >= 4096) {
                            int i21 = i12;
                            i12++;
                            this.pixelStack[i21] = (byte) i13;
                        } else {
                            int i22 = i12;
                            i12++;
                            this.pixelStack[i22] = (byte) i13;
                            this.prefix[i5] = (short) i6;
                            this.suffix[i5] = (byte) i13;
                            i5++;
                            if ((i5 & i8) == 0 && i5 < 4096) {
                                i7++;
                                i8 += i5;
                            }
                            i6 = i17;
                        }
                    }
                }
            }
            i12--;
            int i23 = i11;
            i11++;
            this.pixels[i23] = this.pixelStack[i12];
            i16++;
        }
        for (int i24 = i11; i24 < i; i24++) {
            this.pixels[i24] = 0;
        }
    }

    public int read() {
        int i = 0;
        try {
            i = this.inputStream.read();
        } catch (Exception e) {
            this.status = 1;
        }
        return i;
    }

    public int readBlock() {
        int i;
        this.blockSize = read();
        int i2 = 0;
        if (this.blockSize > 0) {
            try {
                while (i2 < this.blockSize && (i = this.inputStream.read(this.block, i2, this.blockSize - i2)) != -1) {
                    i2 += i;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (i2 < this.blockSize) {
                this.status = 1;
            }
        }
        return i2;
    }

    public int[] readColorTable(int i) {
        int i2 = 3 * i;
        int[] iArr = null;
        byte[] bArr = new byte[i2];
        try {
            if (this.inputStream.read(bArr) < i2) {
                this.status = 1;
            } else {
                iArr = new int[256];
                int i3 = 0;
                int i4 = 0;
                while (i3 < i) {
                    int i5 = i4;
                    int i6 = i4 + 1;
                    int i7 = bArr[i5] & 255;
                    int i8 = i6 + 1;
                    int i9 = bArr[i6] & 255;
                    i4 = i8 + 1;
                    int i10 = i3;
                    i3++;
                    iArr[i10] = (-16777216) | (i7 << 16) | (i9 << 8) | (bArr[i8] & 255);
                }
            }
            return iArr;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void readContents() {
        boolean z = false;
        while (!z && !hasError()) {
            switch (read()) {
                case 0:
                    break;
                case 33:
                    switch (read()) {
                        case 249:
                            readGraphicControlExt();
                            break;
                        case StencilBufferUtil.STENCIL_MASK:
                            readBlock();
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < 11; i++) {
                                sb.append((char) this.block[i]);
                            }
                            if (sb.toString().equals("NETSCAPE2.0")) {
                                readNetscapeExt();
                            } else {
                                skipBlocks();
                            }
                            break;
                        default:
                            skipBlocks();
                            break;
                    }
                    break;
                case 44:
                    readImage();
                    break;
                case 59:
                    z = true;
                    break;
                default:
                    this.status = 1;
                    break;
            }
        }
    }

    public void readGraphicControlExt() {
        read();
        int i = read();
        this.dispose = (i & 28) >> 2;
        if (this.dispose == 0) {
            this.dispose = 1;
        }
        this.transparency = (i & 1) != 0;
        this.delay = readShort() * 10;
        this.transIndex = read();
        read();
    }

    public void readHeader() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append((char) read());
        }
        if (!sb.toString().startsWith("GIF")) {
            this.status = 1;
            return;
        }
        readLogicalScreenDescriptor();
        if (!this.gctFlag || hasError()) {
            return;
        }
        this.gct = readColorTable(this.gctSize);
        this.bgColor = this.gct[this.bgIndex];
    }

    public void readImage() {
        this.ix = readShort();
        this.iy = readShort();
        this.iw = readShort();
        this.ih = readShort();
        int i = read();
        this.lctFlag = (i & 128) != 0;
        this.interlace = (i & 64) != 0;
        this.lctSize = 2 << (i & 7);
        if (this.lctFlag) {
            this.lct = readColorTable(this.lctSize);
            this.act = this.lct;
        } else {
            this.act = this.gct;
            if (this.bgIndex == this.transIndex) {
                this.bgColor = 0;
            }
        }
        int i2 = 0;
        if (this.transparency) {
            i2 = this.act[this.transIndex];
            this.act[this.transIndex] = 0;
        }
        if (this.act == null) {
            this.status = 1;
        }
        if (hasError()) {
            return;
        }
        decodeImageData();
        skipBlocks();
        if (hasError()) {
            return;
        }
        this.frameCount++;
        this.currentImage = new BufferedImage(this.width, this.height, 3);
        setPixels();
        this.frames.add(new GifFrame(this.currentImage, this.delay));
        if (this.transparency) {
            this.act[this.transIndex] = i2;
        }
        resetFrame();
    }

    public void readLogicalScreenDescriptor() {
        this.width = readShort();
        this.height = readShort();
        int i = read();
        this.gctFlag = (i & 128) != 0;
        this.gctSize = 2 << (i & 7);
        this.bgIndex = read();
        this.pixelAspect = read();
    }

    public void readNetscapeExt() {
        do {
            readBlock();
            if (this.block[0] == 1) {
                this.loopCount = ((this.block[2] & 255) << 8) | (this.block[1] & 255);
            }
            if (this.blockSize <= 0) {
                return;
            }
        } while (!hasError());
    }

    public int readShort() {
        return read() | (read() << 8);
    }

    public void resetFrame() {
        this.lastDispose = this.dispose;
        this.lastRect = new Rectangle(this.ix, this.iy, this.iw, this.ih);
        this.lastImage = this.currentImage;
        this.lastBgColor = this.bgColor;
        this.lct = null;
    }

    public void skipBlocks() {
        do {
            readBlock();
            if (this.blockSize <= 0) {
                return;
            }
        } while (!hasError());
    }

    public GifDecoder loopCount(int i) {
        this.loopCount = i;
        return this;
    }

    public List<GifFrame> frames() {
        return this.frames;
    }

    public int frameCount() {
        return this.frameCount;
    }
}
