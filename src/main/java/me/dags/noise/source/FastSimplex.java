/*
 *
 * MIT License
 *
 * Copyright (c) 2020 TerraForged
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.dags.noise.source;

import me.dags.noise.util.Noise;
import me.dags.noise.util.NoiseUtil;

public class FastSimplex extends FastSource {

    private final float min;
    private final float max;
    private final float range;

    public FastSimplex(Builder builder) {
        super(builder);
        this.min = -max(builder.getOctaves(), builder.getGain());
        this.max = max(builder.getOctaves(), builder.getGain());
        this.range = max - min;
    }

    @Override
    public float getValue(float x, float y, int seed) {
        x *= frequency;
        y *= frequency;

        float sum = 0;
        float amp = 1;

        for (int i = 0; i < octaves; i++) {
            sum += Noise.singleSimplex(x, y, seed + i, interpolation) * amp;
            x *= lacunarity;
            y *= lacunarity;
            amp *= gain;
        }

        return NoiseUtil.map(sum, min, max, range);
    }

    private static float max(int octaves, float gain) {
        float signal = signal(octaves);

        float sum = 0;
        float amp = 1;
        for (int i = 0; i < octaves; i++) {
            sum += amp * signal;
            amp *= gain;
        }
        return sum;
    }

    private static float signal(int octaves) {
        int index = Math.min(octaves, signals.length - 1);
        return signals[index];
    }

    private static final float[] signals = {1.00F, 0.989F, 0.810F, 0.781F, 0.708F, 0.702F, 0.696F};
}
