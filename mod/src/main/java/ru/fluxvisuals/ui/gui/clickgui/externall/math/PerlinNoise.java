package ru.fluxvisuals.ui.gui.clickgui.externall.math;

import java.util.Random;

public class PerlinNoise {

    private static final int PERMUTATION_SIZE = 256;
    private final int[] permutation;

    public PerlinNoise() {
        this(System.currentTimeMillis());
    }

    public PerlinNoise(long seed) {
        Random random = new Random(seed);
        permutation = new int[PERMUTATION_SIZE * 2];

        int[] p = new int[PERMUTATION_SIZE];
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            p[i] = i;
        }

        for (int i = PERMUTATION_SIZE - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = p[index];
            p[index] = p[i];
            p[i] = temp;
        }

        for (int i = 0; i < PERMUTATION_SIZE * 2; i++) {
            permutation[i] = p[i % PERMUTATION_SIZE];
        }
    }

    public double noise(double x) {
        return noise(x, 0, 0);
    }

    public double noise(double x, double y) {
        return noise(x, y, 0);
    }

    public double noise(double x, double y, double z) {

        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A = permutation[X] + Y;
        int AA = permutation[A] + Z;
        int AB = permutation[A + 1] + Z;
        int B = permutation[X + 1] + Y;
        int BA = permutation[B] + Z;
        int BB = permutation[B + 1] + Z;

        return lerp(w,
            lerp(v,
                lerp(u, grad(permutation[AA], x, y, z),
                       grad(permutation[BA], x - 1, y, z)),
                lerp(u, grad(permutation[AB], x, y - 1, z),
                       grad(permutation[BB], x - 1, y - 1, z))),
            lerp(v,
                lerp(u, grad(permutation[AA + 1], x, y, z - 1),
                       grad(permutation[BA + 1], x - 1, y, z - 1)),
                lerp(u, grad(permutation[AB + 1], x, y - 1, z - 1),
                       grad(permutation[BB + 1], x - 1, y - 1, z - 1))));
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public double octaveNoise(double x, double y, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }
}

