#version 150

out vec2 TexCoord;

void main() {
    vec2 verts[4] = vec2[](
        vec2(-1.0, -1.0),
        vec2( 1.0, -1.0),
        vec2( 1.0,  1.0),
        vec2(-1.0,  1.0)
    );
    int idx[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 v = verts[idx[gl_VertexID]];

    TexCoord = v * 0.5 + 0.5;

    gl_Position = vec4(v, 1.0, 1.0);
}
