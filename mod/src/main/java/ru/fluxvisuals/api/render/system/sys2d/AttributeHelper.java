package ru.fluxvisuals.api.render.system.sys2d;

import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;



public class AttributeHelper {

    public static int getTotalSize() {
        return totalSize;
    }

    private static int totalSize = 0;
    private static List<Attribute> attributeOffsets = new ArrayList<>();

    public static int SIZE;

    public static void addAttribute(int size, int type, boolean normalized) {
        int typeSize = getTypeSize(type);
        int attributeSize = size * typeSize;
        int offset = totalSize;

        attributeOffsets.add(new Attribute(offset, type, size, normalized));

        totalSize += attributeSize;
        SIZE += size;
    }

    public static void createAttributes() {
        int index = 0;
        for (Attribute attribute : attributeOffsets) {
            GL30.glVertexAttribPointer(
                    index,
                    attribute.getSize(),
                    attribute.getType(),
                    attribute.isNormalized(),
                    totalSize,
                    (long) attribute.getOffset()
            );
            GL30.glEnableVertexAttribArray(index);
            index++;
        }

        attributeOffsets.clear();
        totalSize = 0;
    }


    private static int getTypeSize(int type) {
        switch (type) {
            case GL30.GL_FLOAT:
                return 4;
            case GL30.GL_INT:
            case GL30.GL_UNSIGNED_INT:
                return 4;
            case GL30.GL_UNSIGNED_BYTE:
                return 1;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    public static class Attribute {
        private final int offset;
        private final int type;
        private final int size;
        private final boolean normalized;

        public boolean isNormalized() {
            return normalized;
        }

        public Attribute(int offset, int type, int size, boolean normalized) {
            this.offset = offset;
            this.type = type;
            this.size = size;
            this.normalized = normalized;
        }

        public int getOffset() {
            return offset;
        }

        public int getType() {
            return type;
        }

        public int getSize() {
            return size;
        }
    }
}

