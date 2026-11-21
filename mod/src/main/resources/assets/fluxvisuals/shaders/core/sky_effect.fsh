#version 150



layout(std140) uniform SkyData {
    vec4 CamForward;
    vec4 CamUp;
    vec4 CamLeft;
    vec4 FovScale;
    vec4 TintColor;
    float Time;
    int Mode;
    float Scale;
    float _pad1;
};

in vec2 TexCoord;
out vec4 fragColor;

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

vec3 worldDirection(vec2 screenUv) {
    vec2 ndc = screenUv * 2.0 - 1.0;



    return normalize(CamForward.xyz + CamLeft.xyz * (ndc.x * FovScale.x) + CamUp.xyz * (ndc.y * FovScale.y));
}

vec2 skyUv(vec3 dir) {
    vec3 n = normalize(dir);
    n /= (abs(n.x) + abs(n.y) + abs(n.z));
    if (n.y < 0.0) {
        vec2 signN = sign(n.xz);
        n.xz = (1.0 - abs(n.zx)) * signN;
    }
    return n.xz * 0.5 + 0.5;
}

vec3 skyBase(vec3 dir) {
    float horizon = smoothstep(-0.15, 0.55, dir.y);
    vec3 deep = TintColor.rgb * 0.18;
    vec3 glow = mix(TintColor.rgb, vec3(1.0), 0.35);
    return mix(deep, glow, horizon);
}

void classicWaves(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    vec2 p = (uv - 0.5) * Scale;
    float t = Time;
    float warp = fbm(p * 0.8 + vec2(t * 0.12, -t * 0.08));
    float wave = sin((p.x + warp * 1.8) * 5.0 + t * 1.7)
               + sin((p.y - warp * 1.2) * 6.0 - t * 1.25);
    float crest = pow(clamp(wave * 0.25 + 0.5, 0.0, 1.0), 2.2);
    col = mix(TintColor.rgb * 0.12, mix(TintColor.rgb, vec3(1.0), 0.55), crest);
    fill = 0.48 + crest * 0.48;
}

void classicPlasma(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    vec2 p = (uv - 0.5) * Scale;
    float t = Time * 0.8;
    float v = sin(p.x * 2.7 + t) + sin(p.y * 3.2 - t * 0.73)
            + sin((p.x + p.y) * 2.1 + t * 0.41);
    float energy = v / 6.0 + 0.5;
    vec3 spectrum = hsv2rgb(vec3(fract(energy * 0.35 + t * 0.035), 0.78, 1.0));
    col = mix(TintColor.rgb * 0.16, mix(TintColor.rgb, spectrum, 0.68), energy);
    fill = 0.5 + energy * 0.46;
}

void classicWater(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    vec2 p = (uv - 0.5) * Scale;
    float t = Time * 0.45;
    float n = fbm(p * 1.35 + vec2(t * 0.22, -t * 0.14));
    float ripples = sin((p.x + n) * 8.0 - t * 1.8) * 0.5
                  + sin((p.y - n) * 9.0 + t * 1.4) * 0.5;
    float light = ripples * 0.25 + 0.5;
    vec3 water = mix(vec3(0.01, 0.04, 0.12), mix(TintColor.rgb, vec3(0.35, 0.8, 1.0), 0.7), light);
    col = water;
    fill = 0.52 + light * 0.42;
}

void classicDark(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    vec2 p = (uv - 0.5) * Scale;
    float t = Time * 0.22;
    float smoke = fbm(p + vec2(t * 0.18, -t * 0.11));
    float veins = ridged(p * 1.3 - vec2(t * 0.08));
    float shade = clamp(smoke * 0.58 + veins * 0.25, 0.0, 1.0);
    col = mix(vec3(0.002, 0.003, 0.009), TintColor.rgb * 0.3, shade);
    fill = 0.72 + shade * 0.25;
}

void classicStars(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time * 0.04;
    vec3 domain = normalize(dir) * (150.0 + Scale * 20.0);
    float stars = pow(hash3(floor(domain)), 28.0);
    float smallStars = pow(hash3(floor(domain * 2.4 + 17.0)), 46.0);
    float clouds = fbm3(dir * (2.0 + Scale * 0.16) + vec3(t, -t * 0.7, 2.0));
    vec3 nebula = mix(TintColor.rgb * 0.08, TintColor.rgb * 0.48, pow(clouds, 2.0));
    col = vec3(0.002, 0.004, 0.015) + nebula + vec3(stars + smallStars * 0.75);
    fill = 0.82 + clamp(stars + smallStars, 0.0, 1.0) * 0.18;
}

void classicSun(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    vec3 sunDir = normalize(vec3(-0.45, 0.28, -0.85));
    float distanceToSun = 1.0 - dot(normalize(dir), sunDir);
    float disc = 1.0 - smoothstep(0.004, 0.025, distanceToSun);
    float halo = exp(-distanceToSun * 18.0);
    float rays = pow(clamp(sin(atan(dir.z, dir.x) * 14.0 + Time * 0.4) * 0.5 + 0.5, 0.0, 1.0), 4.0);
    vec3 warm = mix(TintColor.rgb, vec3(1.0, 0.72, 0.25), 0.65);
    col = skyBase(dir) * 0.3 + warm * (halo * 0.8 + rays * halo * 0.22) + vec3(1.0, 0.92, 0.68) * disc;
    fill = 0.62 + halo * 0.35;
}

void classicCometSun(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    vec3 sunDir = normalize(vec3(-0.38, 0.2, -0.9));
    float d = 1.0 - dot(normalize(dir), sunDir);
    float core = exp(-d * 90.0);
    float halo = exp(-d * 16.0);
    vec3 tangent = normalize(vec3(sunDir.z, 0.0, -sunDir.x));
    float along = dot(dir - sunDir, tangent);
    float across = dot(dir - sunDir, normalize(cross(tangent, sunDir)));
    float tail = exp(-abs(across) * 28.0) * smoothstep(0.45, -0.05, along) * exp(-abs(along) * 2.0);
    float sparks = pow(hash(floor(vec2(along, across) * 180.0 + Time * 2.0)), 24.0) * tail;
    vec3 hot = mix(TintColor.rgb, vec3(1.0, 0.58, 0.18), 0.68);
    col = skyBase(dir) * 0.22 + hot * (halo + tail * 0.75) + vec3(1.0) * (core + sparks);
    fill = 0.64 + clamp(halo + tail, 0.0, 1.0) * 0.34;
}

void modeWeb(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time;
    vec3 flow = dir * 3.0;
    vec3 drift = vec3(t * 0.12, -t * 0.08, t * 0.05);
    vec3 q = flow + (vec3(fbm3(flow + drift), fbm3(flow * 0.9 - drift), fbm3(flow * 1.1 + drift)) - 0.5) * 1.2;
    float veins = pow(ridged3(q * 1.6 + drift), 2.0);
    float mist = fbm3(q * 0.8 - drift * 0.3);
    float energy = clamp(mist * 0.35 + veins * 0.85, 0.0, 1.0);
    col = mix(TintColor.rgb * 0.25, mix(TintColor.rgb, vec3(1.0), 0.45), energy);
    fill = 0.45 + energy * 0.5;
}

void modeAurora(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time * 0.35;
    float band = smoothstep(0.05, 0.9, dir.y + 0.1);
    float wave = sin(uv.x * 8.0 + t * 1.4 + fbm3(dir * 4.0 + t * 0.2) * 3.0);
    float wave2 = sin(uv.x * -5.5 - t + fbm3(dir * 3.0) * 2.0);
    float aurora = smoothstep(-0.1, 0.95, wave * 0.45 + wave2 * 0.55) * band;
    vec3 irid = mix(TintColor.rgb, hsv2rgb(vec3(fract(t * 0.05 + uv.x * 0.2), 0.55, 1.0)), 0.35);
    col = mix(skyBase(dir), mix(TintColor.rgb, irid, aurora), 0.85);
    fill = 0.5 + aurora * 0.42;
}

void modePulse(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time;
    float azimuth = atan(dir.z, dir.x);
    float dist = length(dir.xz) * 1.6 + (1.0 - dir.y) * 0.35;
    float rings = sin(dist * 16.0 - t * 3.0 + fbm3(dir * 5.0 + t * 0.3) * 2.0 + azimuth * 2.0);
    float glow = pow(clamp(rings * 0.5 + 0.5, 0.0, 1.0), 2.2);
    float pulse = 0.55 + 0.45 * sin(t * 1.8 - dist * 8.0);
    vec3 hot = mix(TintColor.rgb, vec3(1.0), 0.6);
    col = mix(skyBase(dir), hot, glow * pulse);
    fill = 0.45 + glow * 0.45 + pulse * 0.12;
}

void modeNeon(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time;
    vec2 grid = fract(uv * vec2(12.0, 8.0));
    float lines = max(smoothstep(0.05, 0.0, abs(grid.x - 0.5)), smoothstep(0.05, 0.0, abs(grid.y - 0.5)));
    float scan = 0.5 + 0.5 * sin(uv.y * 30.0 - t * 4.0);
    vec3 neon = mix(TintColor.rgb, vec3(1.0), 0.5);
    col = skyBase(dir) * 0.35 + neon * lines * scan;
    fill = 0.4 + lines * 0.52;
}

void modeSmoke(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time * 0.25;
    vec3 flow = dir * 2.5 + vec3(t * 0.08, -t * 0.12, t * 0.04);
    float smoke = fbm3(flow);
    float smoke2 = fbm3(flow * 1.5 + vec3(-t * 0.06, t * 0.04, -t * 0.03));
    float density = smoothstep(0.2, 0.9, smoke * 0.55 + smoke2 * 0.45);
    col = mix(skyBase(dir), mix(TintColor.rgb, vec3(1.0), 0.35), density);
    fill = 0.42 + density * 0.52;
}

void modeRipple(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time;
    float dist = length(dir.xz) * 2.2 + (1.0 - dir.y) * 0.25;
    float wave = sin(dist * 20.0 - t * 3.5 + atan(dir.z, dir.x));
    float ripple = pow(clamp(wave * 0.5 + 0.5, 0.0, 1.0), 2.0);
    vec3 hot = mix(TintColor.rgb, vec3(1.0), 0.55);
    col = mix(skyBase(dir), hot, ripple);
    fill = 0.42 + ripple * 0.48;
}

void modeCrystal(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time * 0.2;
    float facets = abs(sin(uv.x * 24.0 + t) * sin(uv.y * 18.0 - t * 0.8));
    float sparkle = pow(fbm3(dir * 10.0 + t * 0.15), 4.0);
    col = mix(skyBase(dir), mix(TintColor.rgb, vec3(1.0), 0.65), facets * 0.7 + sparkle * 0.5);
    fill = 0.45 + facets * 0.32 + sparkle * 0.22;
}

void modePlasma(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time * 0.4;
    float p = sin(dir.x * 5.0 + t) + sin(dir.y * 4.0 - t * 0.8) + sin(dir.z * 3.5 + t * 0.6);
    float plasma = smoothstep(0.2, 1.6, p);
    col = mix(skyBase(dir), mix(TintColor.rgb, hsv2rgb(vec3(fract(plasma * 0.15 + t * 0.05), 0.7, 1.0)), 0.4), plasma);
    fill = 0.42 + plasma * 0.46;
}

void modeNebula(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time * 0.15;
    float clouds = fbm3(dir * 2.8 + vec3(t * 0.1, -t * 0.07, t * 0.04));
    float clouds2 = fbm3(dir * 1.8 + vec3(-t * 0.08, t * 0.05, 4.2));
    float nebula = smoothstep(0.25, 0.95, clouds * 0.6 + clouds2 * 0.4);
    vec3 cosmic = mix(TintColor.rgb, hsv2rgb(vec3(fract(uv.x * 0.3 + t * 0.03), 0.65, 1.0)), 0.45);
    col = mix(skyBase(dir) * 0.5, cosmic, nebula);
    fill = 0.45 + nebula * 0.45;
}

void modeGalaxy(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time * 0.08;
    float angle = atan(dir.z, dir.x) + (1.0 - dir.y) * 7.0 - t * 2.0;
    float spiral = smoothstep(0.15, 0.85, sin(angle * 3.0) * 0.5 + 0.5);
    float stars = pow(hash3(dir * 120.0 + floor(t * 4.0)), 18.0);
    col = mix(skyBase(dir) * 0.3, mix(TintColor.rgb, vec3(1.0), 0.4), spiral);
    col += vec3(1.0) * stars * 0.8;
    fill = 0.45 + spiral * 0.38 + stars * 0.28;
}

void modeOcean(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time * 0.3;
    float waves = sin(dir.x * 14.0 + t * 1.2) * 0.5 + sin(dir.z * 10.0 - t * 0.9) * 0.5;
    float depth = smoothstep(-0.2, 0.8, dir.y);
    vec3 water = mix(TintColor.rgb * 0.2, mix(TintColor.rgb, vec3(0.6, 0.85, 1.0), 0.5), depth);
    col = mix(water, mix(TintColor.rgb, vec3(1.0), 0.35), waves * 0.25 + 0.25);
    fill = 0.48 + depth * 0.32;
}

void modeSunset(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float horizon = smoothstep(-0.1, 0.55, dir.y);
    float t = Time * 0.12;
    float streaks = pow(fbm3(dir * 3.0 + vec3(t, t * 0.5, 0.0)), 2.0);
    vec3 warm = mix(TintColor.rgb, vec3(1.0, 0.55, 0.25), 0.55);
    col = mix(TintColor.rgb * 0.1, warm, horizon * (0.65 + streaks * 0.35));
    fill = 0.45 + horizon * 0.46;
}

void modeCloud(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time * 0.1;
    float c1 = fbm3(dir * 3.0 + vec3(t * 0.05, 0.0, -t * 0.03));
    float c2 = fbm3(dir * 2.0 + vec3(-t * 0.04, t * 0.03, t * 0.02));
    float clouds = smoothstep(0.35, 0.92, c1 * 0.55 + c2 * 0.45);
    col = mix(skyBase(dir), mix(vec3(1.0), TintColor.rgb, 0.35), clouds * 0.75);
    fill = 0.42 + clouds * 0.5;
}

void modeShimmer(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time;
    float sparkle = pow(hash3(floor(dir * 80.0 + t * 2.0)), 12.0);
    float sheen = 0.5 + 0.5 * sin(dir.x * 20.0 + dir.z * 15.0 + t * 2.5);
    col = mix(skyBase(dir), mix(TintColor.rgb, vec3(1.0), 0.7), sheen * 0.45);
    col += vec3(1.0) * sparkle * 0.9;
    fill = 0.45 + sheen * 0.32 + sparkle * 0.22;
}

void modePrism(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time * 0.2;
    float hue = uv.x * 0.6 + uv.y * 0.4 + t * 0.08 + fbm3(dir * 2.0 + t) * 0.15;
    vec3 rainbow = hsv2rgb(vec3(fract(hue), 0.75, 1.0));
    float blend = smoothstep(0.0, 0.7, dir.y + 0.1);
    col = mix(skyBase(dir), mix(TintColor.rgb, rainbow, 0.65), blend);
    fill = 0.45 + blend * 0.42;
}

void modeRadiance(vec2 uv, vec3 dir, out vec3 col, out float fill) {
    float t = Time;
    float dist = length(dir.xz) / max(dir.y + 0.4, 0.3);
    float core = exp(-dist * 3.0) * smoothstep(-0.15, 0.9, dir.y);
    float rings = pow(clamp(sin(dist * 20.0 - t * 2.5) * 0.5 + 0.5, 0.0, 1.0), 3.0);
    vec3 hot = mix(TintColor.rgb, vec3(1.0), 0.7);
    col = mix(skyBase(dir), hot, core * 0.65 + rings * 0.42);
    fill = 0.45 + core * 0.32 + rings * 0.32;
}

void main() {
    vec3 dir = worldDirection(TexCoord);
    vec2 uv = skyUv(dir);

    vec3 col;
    float fill;
    if (Mode == 0) {
        classicWaves(uv, dir, col, fill);
    } else if (Mode == 1) {
        classicPlasma(uv, dir, col, fill);
    } else if (Mode == 2) {
        classicWater(uv, dir, col, fill);
    } else if (Mode == 3) {
        classicDark(uv, dir, col, fill);
    } else if (Mode == 4) {
        classicStars(uv, dir, col, fill);
    } else if (Mode == 5) {
        classicSun(uv, dir, col, fill);
    } else if (Mode == 6) {
        classicCometSun(uv, dir, col, fill);
    } else if (Mode == 8) {
        modeAurora(uv, dir, col, fill);
    } else if (Mode == 9) {
        modePulse(uv, dir, col, fill);
    } else if (Mode == 10) {
        modeNeon(uv, dir, col, fill);
    } else if (Mode == 11) {
        modeSmoke(uv, dir, col, fill);
    } else if (Mode == 12) {
        modeRipple(uv, dir, col, fill);
    } else if (Mode == 13) {
        modeCrystal(uv, dir, col, fill);
    } else if (Mode == 14) {
        modePlasma(uv, dir, col, fill);
    } else if (Mode == 15) {
        modeNebula(uv, dir, col, fill);
    } else if (Mode == 16) {
        modeGalaxy(uv, dir, col, fill);
    } else if (Mode == 17) {
        modeOcean(uv, dir, col, fill);
    } else if (Mode == 18) {
        modeSunset(uv, dir, col, fill);
    } else if (Mode == 19) {
        modeCloud(uv, dir, col, fill);
    } else if (Mode == 20) {
        modeShimmer(uv, dir, col, fill);
    } else if (Mode == 21) {
        modePrism(uv, dir, col, fill);
    } else if (Mode == 22) {
        modeRadiance(uv, dir, col, fill);
    } else {
        modeWeb(uv, dir, col, fill);
    }

    float outA = clamp(TintColor.a * fill, 0.0, 1.0);
    fragColor = vec4(max(col, vec3(0.0)), outA);
}
