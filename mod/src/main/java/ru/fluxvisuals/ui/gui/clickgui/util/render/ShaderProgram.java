package ru.fluxvisuals.ui.gui.clickgui.util.render;
import com.mojang.blaze3d.opengl.GlStateManager;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.Reloadable;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ResourceSource;

import java.util.function.IntConsumer;
import org.lwjgl.opengl.GL33;

public class ShaderProgram implements Reloadable {
    public static final IntConsumer noOpCallback = i -> {
    };
    public final ResourceSource fragmentSource;
    public final ResourceSource vertexSource;
    public Integer programId;

    public IntConsumer compileCallback = noOpCallback;

    public void bind() {
        GlStateManager._glUseProgram(getProgramId());
    }

    public void unbind() {
        GlStateManager._glUseProgram(0);
    }

    @Override
    public void reload() {
        release();
    }

    public void release() {
        if (this.programId != null) {
            GL33.glDeleteProgram(this.programId.intValue());
            this.programId = null;
        }
    }

    public ShaderUniform uniform(String str) {
        ShaderUniform shaderUniform = new ShaderUniform(str);
        addCompileCallback(shaderUniform.programCompileCallback());
        return shaderUniform;
    }

    public int getProgramId() {
        if (this.programId != null) {
            return this.programId.intValue();
        }
        Integer numValueOf = Integer.valueOf(createProgram());
        this.programId = numValueOf;
        return numValueOf.intValue();
    }

    public int createProgram() {
        int iGlCreateProgram = GL33.glCreateProgram();
        int iMethod003 = compileShader(35633, this.vertexSource);
        int iMethod004 = compileShader(35632, this.fragmentSource);
        GL33.glAttachShader(iGlCreateProgram, iMethod003);
        GL33.glAttachShader(iGlCreateProgram, iMethod004);
        GL33.glLinkProgram(iGlCreateProgram);
        if (GL33.glGetProgrami(iGlCreateProgram, 35714) == 0) {
            throw new IllegalStateException("Could not link program: " + GL33.glGetProgramInfoLog(iGlCreateProgram));
        }
        GL33.glDeleteShader(iMethod003);
        GL33.glDeleteShader(iMethod004);
        notifyCompiled(iGlCreateProgram);
        return iGlCreateProgram;
    }

    public int compileShader(int i, ResourceSource resourceSource) {
        String str;
        switch (i) {
            case 35632:
                str = "FRAGMENT";
                break;
            case 35633:
                str = "VERTEX";
                break;
            default:
                str = "TYPE_" + i;
                break;
        }
        FluxVisuals.LOGGER.info("Compiling {} shader from resource: {} ({})", new Object[]{str, resourceSource, resourceSource.getClass().getName()});
        int iGlCreateShader = GL33.glCreateShader(i);
        GL33.glShaderSource(iGlCreateShader, resourceSource.utf8());
        GL33.glCompileShader(iGlCreateShader);
        if (GL33.glGetShaderi(iGlCreateShader, 35713) == 0) {
            throw new IllegalStateException("Couldn't compile shader: " + GL33.glGetShaderInfoLog(iGlCreateShader));
        }
        return iGlCreateShader;
    }

    public void notifyCompiled(int i) {
        this.compileCallback.accept(i);
    }

    public void addCompileCallback(IntConsumer intConsumer) {
        IntConsumer intConsumer2 = this.compileCallback;
        this.compileCallback = i -> {
            intConsumer2.accept(i);
            intConsumer.accept(i);
        };
    }

    public ShaderProgram(ResourceSource resourceSource, ResourceSource class178Var2) {
        this.fragmentSource = resourceSource;
        this.vertexSource = class178Var2;
    }
}
