#version 150




layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 v_uv;
out vec4 fragColor;


vec3 fx_waves(vec2 uv0, float time, float seed) {
    const float zoomOut = 2.2;
    const float iter = 10.0;
    const float huePower = 0.7;
    const float glow = 0.5;
    const float distortScale = 0.8;
    const float distortPower = 0.45;
    const float Speed = 1.5;
    const float WaveSpeed = 11.0;
    const float Brightness = 0.3;

    vec2 uv = uv0 + seed * vec2(7.13, 3.71);
    uv *= zoomOut;
    float distortsc2 = distortScale / distortScale + distortScale / 0.5;
    for (float i = 1.0; i < iter; i++) {
        uv.x += distortPower / i * sin(i * distortScale * uv.y - time * Speed);
        uv.y += distortPower / i * sin(i * distortsc2 * uv.x + time * Speed);
    }
    vec3 col = vec3(vec3(glow) / sin(time * WaveSpeed - length(uv.yx) - uv.y));
    vec3 finalCol = vec3(col * col);
    vec3 tint = vec3(1.0) * Brightness;
    tint = tint * tint * 0.5 + 0.5 * cos(time + uv.xyx + vec3(0, 2, 4)) * huePower;
    return finalCol.rgb * tint;
}


float p_hash(vec2 p) {
    p = fract(p * 0.6180339887);
    p *= 25.0;
    return fract(p.x * p.y * (p.x + p.y));
}
float p_noise(vec2 x) {
    vec2 p = floor(x);
    vec2 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float a = p_hash(p + vec2(0, 0));
    float b = p_hash(p + vec2(1, 0));
    float c = p_hash(p + vec2(0, 1));
    float d = p_hash(p + vec2(1, 1));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}
const mat2 p_mtx = mat2(0.80, 0.60, -0.60, 0.80);
float p_fbm4(vec2 p) {
    float f = 0.0;
    f += 0.5000 * (-1.0 + 2.0 * p_noise(p)); p = p_mtx * p * 2.02;
    f += 0.2500 * (-1.0 + 2.0 * p_noise(p)); p = p_mtx * p * 2.03;
    f += 0.1250 * (-1.0 + 2.0 * p_noise(p)); p = p_mtx * p * 2.01;
    f += 0.0625 * (-1.0 + 2.0 * p_noise(p));
    return f / 0.9375;
}
float p_fbm6(vec2 p) {
    float f = 0.0;
    f += 0.500000 * p_noise(p); p = p_mtx * p * 2.02;
    f += 0.250000 * p_noise(p); p = p_mtx * p * 2.03;
    f += 0.125000 * p_noise(p); p = p_mtx * p * 2.01;
    f += 0.062500 * p_noise(p); p = p_mtx * p * 2.04;
    f += 0.031250 * p_noise(p); p = p_mtx * p * 2.01;
    f += 0.015625 * p_noise(p);
    return f / 0.96875;
}
vec2 p_fbm4_2(vec2 p) { return vec2(p_fbm4(p + vec2(1.0)), p_fbm4(p + vec2(6.2))); }
vec2 p_fbm6_2(vec2 p) { return vec2(p_fbm6(p + vec2(9.2)), p_fbm6(p + vec2(5.7))); }
float p_func(vec2 q, out vec2 o, out vec2 n, float time) {
    q += 0.05 * sin(vec2(0.11, 0.13) * time + length(q) * 4.0);
    q *= 0.7 + 0.2 * cos(0.05 * time);
    o = 0.5 + 0.5 * p_fbm4_2(q);
    o += 0.02 * sin(vec2(0.13, 0.11) * time * length(o));
    n = p_fbm6_2(4.0 * o);
    vec2 p = q + 2.0 * n + 1.0;
    float f = 0.5 + 0.5 * p_fbm4(2.0 * p);
    f = mix(f, f * f * f * 3.5, f * abs(n.x));
    f *= 1.0 - 0.5 * pow(0.5 + 0.5 * sin(8.0 * p.x) * sin(8.0 * p.y), 8.0);
    return f;
}
float p_funcs(vec2 q, float time) { vec2 t1; vec2 t2; return p_func(q, t1, t2, time); }
vec3 fx_plasma(vec2 uv0, float time, float seed, vec2 resolution) {
    vec2 uv = uv0 + seed * vec2(4.71, 8.37);
    vec2 q = 2.0 * uv - vec2(1.0);
    vec2 o; vec2 n;
    float f = p_func(q, o, n, time);
    vec3 col = vec3(0.2, 0.1, 0.4);
    col = mix(col, vec3(0.3, 0.05, 0.05), f);
    col = mix(col, vec3(0.9, 0.9, 0.9), dot(n, n));
    col = mix(col, vec3(0.5, 0.2, 0.2), 0.5 * o.y * o.y);
    col = mix(col, vec3(0.0, 0.2, 0.4), 0.5 * smoothstep(1.2, 1.3, abs(n.y) + abs(n.x)));
    col *= f * 2.0;
    vec2 ex = vec2(1.0 / max(resolution.x, 1.0), 0.0);
    vec2 ey = vec2(0.0, 1.0 / max(resolution.y, 1.0));
    vec3 nor = normalize(vec3(p_funcs(q + ex, time) - f, ex.x, p_funcs(q + ey, time) - f));
    vec3 lig = normalize(vec3(0.9, -0.2, -0.4));
    float dif = clamp(0.3 + 0.7 * dot(nor, lig), 0.0, 1.0);
    vec3 lin = vec3(0.85, 0.90, 0.95) * (nor.y * 0.5 + 0.5);
    lin += vec3(0.15, 0.10, 0.05) * dif;
    col *= lin;
    col = vec3(1.0) - col;
    col = col * col;
    col *= vec3(1.2, 1.25, 1.2);
    vec2 pp = clamp(uv, vec2(0.0), vec2(1.0));
    col *= 0.5 + 0.5 * sqrt(max(0.0, 16.0 * pp.x * pp.y * (1.0 - pp.x) * (1.0 - pp.y)));
    return col;
}


vec3 fx_water(vec2 uv0, float time, float seed) {
    const float TAU = 6.28318530718;
    const int MAX_ITER = 5;
    float t0 = time * 0.5 + 23.0;
    vec2 uv = uv0 + seed * vec2(2.17, 6.41);
    vec2 p = mod(uv * TAU, TAU) - 250.0;
    vec2 i = vec2(p);
    float c = 1.0;
    float inten = 0.005;
    for (int n = 0; n < MAX_ITER; n++) {
        float t = t0 * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
        vec2 denom = vec2(max(0.0001, abs(sin(i.x + t) / inten)), max(0.0001, abs(cos(i.y + t) / inten)));
        c += 1.0 / max(0.0001, length(vec2(p.x / denom.x, p.y / denom.y)));
    }
    c /= float(MAX_ITER);
    c = 1.17 - pow(max(c, 0.0), 1.4);
    vec3 colour = vec3(pow(abs(c), 8.0));
    colour = clamp(colour + vec3(0.0, 0.35, 0.5), 0.0, 1.0);
    return colour;
}


vec3 fx_dark(vec2 uv0, float time, float seed) {
    vec2 uv = (uv0 + seed * vec2(5.33, 1.97)) * 2.0 - 1.0;
    for (float i = 1.0; i < 10.0; i++) {
        uv.x += 0.6 / i * cos(i * 2.5 * uv.y + time);
        uv.y += 0.6 / i * cos(i * 1.5 * uv.x + time);
    }
    return vec3(0.1) / max(0.08, abs(sin(time - uv.y - uv.x)));
}

float sn_noise(vec3 uv, float res) {
    const vec3 s = vec3(1e0, 1e2, 1e4);
    uv *= res;
    vec3 uv0 = floor(mod(uv, res)) * s;
    vec3 uv1 = floor(mod(uv + vec3(1.0), res)) * s;
    vec3 f = fract(uv); f = f * f * (3.0 - 2.0 * f);
    vec4 v = vec4(uv0.x + uv0.y + uv0.z, uv1.x + uv0.y + uv0.z, uv0.x + uv1.y + uv0.z, uv1.x + uv1.y + uv0.z);
    vec4 r = fract(sin(v * 1e-3) * 1e5);
    float r0 = mix(mix(r.x, r.y, f.x), mix(r.z, r.w, f.x), f.y);
    r = fract(sin((v + uv1.z - uv0.z) * 1e-3) * 1e5);
    float r1 = mix(mix(r.x, r.y, f.x), mix(r.z, r.w, f.x), f.y);
    return mix(r0, r1, f.z) * 2.0 - 1.0;
}
vec3 fx_starnest(vec3 dir, float time, float seed) {
    const int iterations = 9;
    const float formuparam = 0.53;
    const int volsteps = 9;
    const float stepsize = 0.2;
    const float tile = 0.850;
    const float brightness = 0.0015;
    const float darkmatter = 0.300;
    const float distfading = 0.730;
    const float saturation = 0.850;
    float t = time * 0.018 + 0.25 + seed * 13.0;
    vec3 from = vec3(1.0, 0.5, 0.5) + vec3(t * 2.0, t, -2.0);
    float s = 0.1, fade = 1.0;
    vec3 v = vec3(0.0);
    for (int r = 0; r < volsteps; r++) {
        vec3 p = from + s * dir * 0.5;
        p = abs(vec3(tile) - mod(p, vec3(tile * 2.0)));
        float pa, a = pa = 0.0;
        for (int i = 0; i < iterations; i++) {
            p = abs(p) / dot(p, p) - formuparam;
            a += abs(length(p) - pa);
            pa = length(p);
        }
        float dm = max(0.0, darkmatter - a * a * 0.001);
        a *= a * a;
        if (r > 6) fade *= 1.0 - dm;
        v += fade;
        v += vec3(s, s * s, s * s * s * s) * a * brightness * fade;
        fade *= distfading;
        s += stepsize;
    }
    v = mix(vec3(length(v)), v, saturation);
    return v * 0.01;
}





vec3 gTint = vec3(1.0);
float gTime = 0.0;
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float hash3(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float vnoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash3(i);
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));
    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    return mix(mix(nx00, nx10, f.y), mix(nx01, nx11, f.y), f.z);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p = p * 2.02 + vec2(8.4, 5.7);
        a *= 0.5;
    }
    return v;
}

float fbm3(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += vnoise3(p) * a;
        p = p * 2.03 + vec3(1.7, 4.2, 9.2);
        a *= 0.5;
    }
    return v;
}

float ridged(vec2 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        float r = 1.0 - abs(noise(p) * 2.0 - 1.0);
        v += r * a;
        p = p * 2.18 + vec2(3.1, 9.2);
        a *= 0.52;
    }
    return v;
}

float ridged3(vec3 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        float r = 1.0 - abs(vnoise3(p) * 2.0 - 1.0);
        v += r * a;
        p = p * 2.18 + vec3(3.1, 9.2, 1.4);
        a *= 0.52;
    }
    return v;
}

vec3 hsv2rgb(vec3 c) {
    vec4 k = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + k.xyz) * 6.0 - k.www);
    return c.z * mix(k.xxx, clamp(p - k.xxx, 0.0, 1.0), c.y);
}


vec2 skyUv(vec3 dir) {
    vec3 n = normalize(dir);
    n /= (abs(n.x) + abs(n.y) + abs(n.z));
    if (n.y < 0.0) {
        vec2 signN = sign(n.xz);
        n.xz = (1.0 - abs(n.xz)) * signN;
    }
    return n.xz * 0.5 + 0.5;
}

vec3 skyBase(vec3 dir) {
    float horizon = smoothstep(-0.15, 0.55, dir.y);
    vec3 deep = gTint * 0.18;
    vec3 glow = mix(gTint, vec3(1.0), 0.35);
    return mix(deep, glow, horizon);
}

vec3 fx_sun_disc(vec2 uv, float time, float seed) {
    vec2 p = uv * 2.0 - 1.0;
    float d = length(p);
    float angle = atan(p.y, p.x);
    float turbulence = fbm(p * 3.4 + vec2(time * 0.16, -time * 0.11) + seed * 4.0);
    float core = 1.0 - smoothstep(0.18, 0.64, d + turbulence * 0.10);
    float halo = exp(-d * 2.8);
    float rays = pow(max(0.0, sin(angle * 12.0 + time * 1.4 + turbulence * 5.0)), 5.0)
            * exp(-d * 1.7);
    vec3 hot = mix(gTint, vec3(1.0, 0.56, 0.12), 0.62);
    return gTint * 0.08 + hot * (halo * 0.72 + rays * 0.38)
            + vec3(1.0, 0.94, 0.72) * core;
}

vec3 fx_comet_disc(vec2 uv, float time, float seed) {
    vec2 p = uv * 2.0 - 1.0;
    float orbit = time * 0.55 + seed * 6.2831853;
    vec2 head = vec2(cos(orbit), sin(orbit)) * 0.24;
    vec2 direction = normalize(vec2(cos(orbit + 1.15), sin(orbit + 1.15)));
    vec2 rel = p - head;
    float along = dot(rel, direction);
    float across = abs(dot(rel, vec2(-direction.y, direction.x)));
    float core = exp(-length(rel) * 18.0);
    float tail = exp(-across * 24.0) * smoothstep(0.72, -0.05, along) * exp(-abs(along) * 2.2);
    float sparks = pow(hash(floor((p + time * direction * 0.1) * 48.0)), 18.0) * tail;
    vec3 hot = mix(gTint, vec3(1.0, 0.52, 0.16), 0.58);
    return gTint * 0.045 + hot * tail * 0.85 + vec3(1.0, 0.92, 0.74) * core
            + vec3(1.0) * sparks * 0.65;
}

void modeWeb(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime;
    vec3 flow = dir * 3.0;
    vec3 drift = vec3(t * 0.12, -t * 0.08, t * 0.05);
    vec3 q = flow + (vec3(fbm3(flow + drift), fbm3(flow * 0.9 - drift), fbm3(flow * 1.1 + drift)) - 0.5) * 1.2;
    float veins = pow(ridged3(q * 1.6 + drift), 2.0);
    float mist = fbm3(q * 0.8 - drift * 0.3);
    float energy = clamp(mist * 0.35 + veins * 0.85, 0.0, 1.0);
    col = mix(gTint * 0.25, mix(gTint, vec3(1.0), 0.45), energy);
    fill = 0.45 + energy * 0.5;
}

void modeAurora(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime * 0.35;
    float band = smoothstep(0.05, 0.9, dir.y + 0.1);
    float wave = sin(uv.x * 8.0 + t * 1.4 + fbm3(dir * 4.0 + t * 0.2) * 3.0);
    float wave2 = sin(uv.x * -5.5 - t + fbm3(dir * 3.0) * 2.0);
    float aurora = smoothstep(-0.1, 0.95, wave * 0.45 + wave2 * 0.55) * band;
    vec3 irid = mix(gTint, hsv2rgb(vec3(fract(t * 0.05 + uv.x * 0.2), 0.55, 1.0)), 0.35);
    col = mix(skyBase(dir), mix(gTint, irid, aurora), 0.85);
    fill = 0.5 + aurora * 0.42;
}

void modePulse(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime;
    float azimuth = atan(dir.z, dir.x);
    float dist = length(dir.xz) * 1.6 + (1.0 - dir.y) * 0.35;
    float rings = sin(dist * 16.0 - t * 3.0 + fbm3(dir * 5.0 + t * 0.3) * 2.0 + azimuth * 2.0);
    float glow = pow(clamp(rings * 0.5 + 0.5, 0.0, 1.0), 2.2);
    float pulse = 0.55 + 0.45 * sin(t * 1.8 - dist * 8.0);
    vec3 hot = mix(gTint, vec3(1.0), 0.6);
    col = mix(skyBase(dir), hot, glow * pulse);
    fill = 0.45 + glow * 0.45 + pulse * 0.12;
}

void modeNeon(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime;
    vec2 grid = fract(uv * vec2(12.0, 8.0));
    float lines = max(smoothstep(0.05, 0.0, abs(grid.x - 0.5)), smoothstep(0.05, 0.0, abs(grid.y - 0.5)));
    float scan = 0.5 + 0.5 * sin(uv.y * 30.0 - t * 4.0);
    vec3 neon = mix(gTint, vec3(1.0), 0.5);
    col = skyBase(dir) * 0.35 + neon * lines * scan;
    fill = 0.4 + lines * 0.52;
}

void modeSmoke(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime * 0.25;
    vec3 flow = dir * 2.5 + vec3(t * 0.08, -t * 0.12, t * 0.04);
    float smoke = fbm3(flow);
    float smoke2 = fbm3(flow * 1.5 + vec3(-t * 0.06, t * 0.04, -t * 0.03));
    float density = smoothstep(0.2, 0.9, smoke * 0.55 + smoke2 * 0.45);
    col = mix(skyBase(dir), mix(gTint, vec3(1.0), 0.35), density);
    fill = 0.42 + density * 0.52;
}

void modeRipple(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime;
    float dist = length(dir.xz) * 2.2 + (1.0 - dir.y) * 0.25;
    float wave = sin(dist * 20.0 - t * 3.5 + atan(dir.z, dir.x));
    float ripple = pow(clamp(wave * 0.5 + 0.5, 0.0, 1.0), 2.0);
    vec3 hot = mix(gTint, vec3(1.0), 0.55);
    col = mix(skyBase(dir), hot, ripple);
    fill = 0.42 + ripple * 0.48;
}

void modeCrystal(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime * 0.2;
    float facets = abs(sin(uv.x * 24.0 + t) * sin(uv.y * 18.0 - t * 0.8));
    float sparkle = pow(fbm3(dir * 10.0 + t * 0.15), 4.0);
    col = mix(skyBase(dir), mix(gTint, vec3(1.0), 0.65), facets * 0.7 + sparkle * 0.5);
    fill = 0.45 + facets * 0.32 + sparkle * 0.22;
}

void modePlasma(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime * 0.4;
    float p = sin(dir.x * 5.0 + t) + sin(dir.y * 4.0 - t * 0.8) + sin(dir.z * 3.5 + t * 0.6);
    float plasma = smoothstep(0.2, 1.6, p);
    col = mix(skyBase(dir), mix(gTint, hsv2rgb(vec3(fract(plasma * 0.15 + t * 0.05), 0.7, 1.0)), 0.4), plasma);
    fill = 0.42 + plasma * 0.46;
}

void modeNebula(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime * 0.15;
    float clouds = fbm3(dir * 2.8 + vec3(t * 0.1, -t * 0.07, t * 0.04));
    float clouds2 = fbm3(dir * 1.8 + vec3(-t * 0.08, t * 0.05, 4.2));
    float nebula = smoothstep(0.25, 0.95, clouds * 0.6 + clouds2 * 0.4);
    vec3 cosmic = mix(gTint, hsv2rgb(vec3(fract(uv.x * 0.3 + t * 0.03), 0.65, 1.0)), 0.45);
    col = mix(skyBase(dir) * 0.5, cosmic, nebula);
    fill = 0.45 + nebula * 0.45;
}

void modeGalaxy(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime * 0.08;
    float angle = atan(dir.z, dir.x) + (1.0 - dir.y) * 7.0 - t * 2.0;
    float spiral = smoothstep(0.15, 0.85, sin(angle * 3.0) * 0.5 + 0.5);
    float stars = pow(hash3(dir * 120.0 + floor(t * 4.0)), 18.0);
    col = mix(skyBase(dir) * 0.3, mix(gTint, vec3(1.0), 0.4), spiral);
    col += vec3(1.0) * stars * 0.8;
    fill = 0.45 + spiral * 0.38 + stars * 0.28;
}

void modeOcean(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime * 0.3;
    float waves = sin(dir.x * 14.0 + t * 1.2) * 0.5 + sin(dir.z * 10.0 - t * 0.9) * 0.5;
    float depth = smoothstep(-0.2, 0.8, dir.y);
    vec3 water = mix(gTint * 0.2, mix(gTint, vec3(0.6, 0.85, 1.0), 0.5), depth);
    col = mix(water, mix(gTint, vec3(1.0), 0.35), waves * 0.25 + 0.25);
    fill = 0.48 + depth * 0.32;
}

void modeSunset(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float horizon = smoothstep(-0.1, 0.55, dir.y);
    float t = gTime * 0.12;
    float streaks = pow(fbm3(dir * 3.0 + vec3(t, t * 0.5, 0.0)), 2.0);
    vec3 warm = mix(gTint, vec3(1.0, 0.55, 0.25), 0.55);
    col = mix(gTint * 0.1, warm, horizon * (0.65 + streaks * 0.35));
    fill = 0.45 + horizon * 0.46;
}

void modeCloud(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime * 0.1;
    float c1 = fbm3(dir * 3.0 + vec3(t * 0.05, 0.0, -t * 0.03));
    float c2 = fbm3(dir * 2.0 + vec3(-t * 0.04, t * 0.03, t * 0.02));
    float clouds = smoothstep(0.35, 0.92, c1 * 0.55 + c2 * 0.45);
    col = mix(skyBase(dir), mix(vec3(1.0), gTint, 0.35), clouds * 0.75);
    fill = 0.42 + clouds * 0.5;
}

void modeShimmer(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime;
    float sparkle = pow(hash3(floor(dir * 80.0 + t * 2.0)), 12.0);
    float sheen = 0.5 + 0.5 * sin(dir.x * 20.0 + dir.z * 15.0 + t * 2.5);
    col = mix(skyBase(dir), mix(gTint, vec3(1.0), 0.7), sheen * 0.45);
    col += vec3(1.0) * sparkle * 0.9;
    fill = 0.45 + sheen * 0.32 + sparkle * 0.22;
}

void modePrism(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime * 0.2;
    float hue = uv.x * 0.6 + uv.y * 0.4 + t * 0.08 + fbm3(dir * 2.0 + t) * 0.15;
    vec3 rainbow = hsv2rgb(vec3(fract(hue), 0.75, 1.0));
    float blend = smoothstep(0.0, 0.7, dir.y + 0.1);
    col = mix(skyBase(dir), mix(gTint, rainbow, 0.65), blend);
    fill = 0.45 + blend * 0.42;
}

void modeRadiance(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = gTime;
    float dist = length(dir.xz) / max(dir.y + 0.4, 0.3);
    float core = exp(-dist * 3.0) * smoothstep(-0.15, 0.9, dir.y);
    float rings = pow(clamp(sin(dist * 20.0 - t * 2.5) * 0.5 + 0.5, 0.0, 1.0), 3.0);
    vec3 hot = mix(gTint, vec3(1.0), 0.7);
    col = mix(skyBase(dir), hot, core * 0.65 + rings * 0.42);
    fill = 0.45 + core * 0.32 + rings * 0.32;
}


vec3 fx_sky(int m, vec2 uv, vec3 dir) {
    vec3 col; float fill;
    if (m == 0) modeWeb(uv, dir, col, fill);
    else if (m == 1) modeAurora(uv, dir, col, fill);
    else if (m == 2) modePulse(uv, dir, col, fill);
    else if (m == 3) modeNeon(uv, dir, col, fill);
    else if (m == 4) modeSmoke(uv, dir, col, fill);
    else if (m == 5) modeRipple(uv, dir, col, fill);
    else if (m == 6) modeCrystal(uv, dir, col, fill);
    else if (m == 7) modePlasma(uv, dir, col, fill);
    else if (m == 8) modeNebula(uv, dir, col, fill);
    else if (m == 9) modeGalaxy(uv, dir, col, fill);
    else if (m == 10) modeOcean(uv, dir, col, fill);
    else if (m == 11) modeSunset(uv, dir, col, fill);
    else if (m == 12) modeCloud(uv, dir, col, fill);
    else if (m == 13) modeShimmer(uv, dir, col, fill);
    else if (m == 14) modePrism(uv, dir, col, fill);
    else if (m == 15) modeRadiance(uv, dir, col, fill);
    else modeWeb(uv, dir, col, fill);
    return col;
}

void main() {
    float time = ColorModulator.x;
    float opacity = ColorModulator.y;
    float seed = ColorModulator.z;
    int type = int(ColorModulator.w + 0.5);
    gTime = time;
    gTint = TextureMat[0].xyz;

    vec3 col;
    if (type == 21) {
        col = fx_sun_disc(v_uv, time, seed);
    } else if (type == 22) {
        col = fx_comet_disc(v_uv, time, seed);
    } else if (type >= 5) {
        vec2 uv = v_uv;
        vec3 dir = normalize(vec3(v_uv * 2.0 - 1.0, 0.65));
        col = fx_sky(type - 5, uv, dir);
    } else if (type == 1) {
        col = fx_plasma(v_uv, time, seed, ModelOffset.xy);
    } else if (type == 2) {
        col = fx_water(v_uv, time, seed);
    } else if (type == 3) {
        col = fx_dark(v_uv, time, seed);
    } else if (type == 4) {

        vec3 d = normalize(vec3(v_uv - floor(v_uv) - 0.5, 0.7));
        col = fx_starnest(d, time, seed);
    } else {
        col = fx_waves(v_uv, time, seed);
    }

    float alpha = opacity;
    if (ModelOffset.z > 0.5) {
        vec2 centered = v_uv * 2.0 - 1.0;
        float radius = length(centered);
        float edgeNoise = fbm(centered * 3.1 + vec2(time * 0.10, -time * 0.075));
        float detailNoise = fbm(centered * 6.4 + vec2(-time * 0.16, time * 0.12));
        float outerEdge = 0.78 + edgeNoise * 0.24 + detailNoise * 0.055;
        float innerFade = smoothstep(0.60, 0.72, radius);
        float outerFade = 1.0 - smoothstep(outerEdge - 0.15, outerEdge, radius);
        float vapor = 0.42 + 0.58 * smoothstep(0.20, 0.88, edgeNoise * 0.72 + detailNoise * 0.28);
        alpha *= innerFade * outerFade * vapor;
    }
    if (alpha <= 0.002) discard;
    fragColor = vec4(col, alpha);
}


