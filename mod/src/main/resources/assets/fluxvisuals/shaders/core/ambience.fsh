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


float p_hash(vec2 p) { p = fract(p * 0.6180339887); p *= 25.0; return fract(p.x * p.y * (p.x + p.y)); }
float p_noise(vec2 x) {
    vec2 p = floor(x); vec2 f = fract(x); f = f * f * (3.0 - 2.0 * f);
    float a = p_hash(p + vec2(0, 0)); float b = p_hash(p + vec2(1, 0));
    float c = p_hash(p + vec2(0, 1)); float d = p_hash(p + vec2(1, 1));
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

    col *= vec3(0.95, 0.97, 1.0);
    col = vec3(1.0) - col;
    col = col * col;
    col *= vec3(1.2, 1.25, 1.2);
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


vec3 fx_sun(vec3 dir, float time, float seed) {
    vec3 sunDir = normalize(vec3(0.25, 0.40, 0.85));
    float d = clamp(dot(dir, sunDir), -1.0, 1.0);


    vec3 rel = dir - sunDir * d;
    float ang = atan(rel.y, rel.x);
    float corona = 0.55 + 0.45 * sin(ang * 10.0 + time * 0.9 + seed * 6.2)
                              * sin(ang * 6.0 - time * 0.6);

    float core = smoothstep(0.9975, 0.9992, d);
    float glow = pow(max(d, 0.0), 7.0);
    float halo = pow(max(d, 0.0), 36.0) * (0.65 + 0.35 * corona);
    float rays = pow(max(d, 0.0), 14.0) * (0.5 + 0.5 * corona);

    vec3 sky = mix(vec3(0.015, 0.04, 0.11), vec3(0.10, 0.20, 0.42), clamp(dir.y * 0.5 + 0.5, 0.0, 1.0));
    vec3 col = sky;
    col += vec3(1.0, 0.45, 0.12) * glow * 0.55;
    col += vec3(1.0, 0.65, 0.25) * rays * 0.6;
    col += vec3(1.0, 0.85, 0.5) * halo;
    col += vec3(1.0, 0.97, 0.9) * core;
    return col;
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
vec3 sn_starTex(vec2 uv) {
    float n = 0.5 + 0.5 * sn_noise(vec3(uv, 0.0), 16.0);
    return vec3(n);
}

vec3 fx_cometsun(vec3 dir, float time0, float seed) {

    vec3 sunDir = normalize(vec3(0.20, 0.55, 0.82));
    float dz = dot(dir, sunDir);
    vec3 sky = mix(vec3(0.010, 0.020, 0.05), vec3(0.030, 0.060, 0.14), clamp(dir.y * 0.5 + 0.5, 0.0, 1.0));
    if (dz < 0.05) return sky;
    vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), sunDir));
    vec3 upv = cross(sunDir, right);
    vec2 p = vec2(dot(dir, right), dot(dir, upv)) / dz * 0.9;


    float dist = length(p);
    if (dist > 3.0) return sky;

    float brightness = 0.22 + 0.06 * sin(time0 * 0.6 + seed * 6.0);
    float radius = 0.24 + brightness * 0.2;
    float invRadius = 1.0 / radius;
    vec3 orange = vec3(0.8, 0.65, 0.3);
    vec3 orangeRed = vec3(0.8, 0.35, 0.1);
    float time = time0 * 0.1 + seed * 20.0;

    float fade = pow(length(2.0 * p), 0.5);
    float fVal1 = 1.0 - fade;
    float fVal2 = 1.0 - fade;
    float angle = atan(p.x, p.y) / 6.2832;
    vec3 coord = vec3(angle, dist, time * 0.1);
    float newTime1 = abs(sn_noise(coord + vec3(0.0, -time * (0.35 + brightness * 0.001), time * 0.015), 15.0));
    float newTime2 = abs(sn_noise(coord + vec3(0.0, -time * (0.15 + brightness * 0.001), time * 0.015), 45.0));
    for (int i = 1; i <= 4; i++) {
        float power = pow(2.0, float(i + 1));
        fVal1 += (0.5 / power) * sn_noise(coord + vec3(0.0, -time, time * 0.2), (power * 10.0 * (newTime1 + 1.0)));
        fVal2 += (0.5 / power) * sn_noise(coord + vec3(0.0, -time, time * 0.2), (power * 25.0 * (newTime2 + 1.0)));
    }
    float corona = pow(fVal1 * max(1.1 - fade, 0.0), 2.0) * 50.0;
    corona += pow(fVal2 * max(1.1 - fade, 0.0), 2.0) * 50.0;
    corona *= 1.2 - newTime1;

    vec2 sp = p * 2.0;
    sp *= (2.0 - brightness);
    float r = dot(sp, sp);
    float f = (1.0 - sqrt(abs(1.0 - r))) / (r + 1e-4) + brightness * 0.5;

    vec3 starSphere = vec3(0.0);
    if (dist < radius) {
        corona *= pow(dist * invRadius, 24.0);
        vec2 newUv = vec2(sp.x * f, sp.y * f) + vec2(time, 0.0);
        vec3 texSample = sn_starTex(newUv);
        float uOff = (texSample.g * brightness * 4.5 + time);
        starSphere = sn_starTex(newUv + vec2(uOff, 0.0));
    }

    float starGlow = clamp(1.0 - dist * (1.0 - brightness), 0.0, 1.0);
    vec3 col = vec3(f * (0.75 + brightness * 0.3) * orange) + starSphere + corona * orange + starGlow * orangeRed;


    float skyFade = exp(-dist * 1.5);
    return mix(sky, col, clamp(skyFade, 0.0, 1.0));
}

void main() {
    float time = ColorModulator.x;
    float opacity = ColorModulator.y;
    float seed = ColorModulator.z;
    int type = int(ColorModulator.w + 0.5);
    float scale = max(0.5, ModelOffset.z);






    vec3 cright = TextureMat[0].xyz; float thx = TextureMat[0].w;
    vec3 cup    = TextureMat[1].xyz; float thy = TextureMat[1].w;
    vec3 cfwd   = TextureMat[2].xyz;
    vec2 ndc = v_uv * 2.0 - 1.0;
    vec3 dir = normalize(cfwd + ndc.x * thx * cright + ndc.y * thy * cup);

    vec2 uv = dir.xz / max(abs(dir.y), 0.18) * scale;

    float alpha = opacity;
    vec3 col;
    if (type == 4) {
        col = fx_starnest(dir, time, seed);
    } else if (type == 5) {
        col = fx_sun(dir, time, seed);
    } else if (type == 6) {
        col = fx_cometsun(dir, time, seed);
    } else {
        if (type == 1) col = fx_plasma(uv, time, seed, ModelOffset.xy);
        else if (type == 2) col = fx_water(uv, time, seed);
        else if (type == 3) col = fx_dark(uv, time, seed);
        else col = fx_waves(uv, time, seed);


        alpha *= smoothstep(0.05, 0.34, abs(dir.y));
    }

    fragColor = vec4(col, alpha);
}
