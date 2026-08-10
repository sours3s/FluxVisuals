#version 150

// Inline uniforms - OpenGL 3.2 compatible
uniform mat4 ModelViewMat;
uniform vec4 ColorModulator;
uniform vec3 ModelOffset;
uniform mat4 TextureMat;
uniform float LineWidth;

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator * 5.0;
}
