#version 150

// Отдельный вершинный шейдер для финального blit-прохода Shader Fog.
// z = 1.0 обязателен: пайплайн использует EQUAL_DEPTH_TEST и должен рисовать
// только туда, где глубина кадра ещё не записана (небо).
out vec2 texCoord;

void main() {
    vec2 grid[6] = vec2[](
        vec2(-1.0, -1.0),
        vec2( 1.0, -1.0),
        vec2(-1.0,  1.0),
        vec2(-1.0,  1.0),
        vec2( 1.0, -1.0),
        vec2( 1.0,  1.0)
    );

    vec2 pos = grid[gl_VertexID];
    texCoord = pos * 0.5 + 0.5;

    gl_Position = vec4(pos, 1.0, 1.0);
}
