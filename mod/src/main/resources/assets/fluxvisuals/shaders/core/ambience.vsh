#version 150



in vec3 Position;

out vec2 v_uv;

void main() {
    gl_Position = vec4(Position.x, Position.y, 1.0, 1.0);
    v_uv = Position.xy * 0.5 + 0.5;
}
