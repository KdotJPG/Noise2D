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

package me.dags.noise.selector;

import me.dags.noise.Module;
import me.dags.noise.func.Interpolation;
import me.dags.noise.util.NoiseUtil;

/**
 * @Author <dags@dags.me>
 */
public class MultiBlend extends Selector {

    private final Node[] nodes;
    private final int maxIndex;
    private final float blend;
    private final float blendRange;

    public MultiBlend(float blend, Interpolation interpolation, Module control, Module... sources) {
        super(control, sources, interpolation);

        float spacing = 1F / (sources.length);
        float radius = spacing / 2F;
        float blendRange = radius * blend;
        float cellRadius = (radius - blendRange) / 2;

        this.blend = blend;
        this.nodes = new Node[sources.length];
        this.maxIndex = sources.length - 1;
        this.blendRange = blendRange;

        for (int i = 0; i < sources.length; i++) {
            float pos = i * spacing + radius;
            float min = i == 0 ? 0 : pos - cellRadius;
            float max = i == maxIndex ? 1 : pos + cellRadius;
            nodes[i] = new Node(sources[i], min, max);
        }
    }

    @Override
    public float selectValue(float x, float y, float selector) {
        int index = NoiseUtil.round(selector * maxIndex);

        Node min = nodes[index];
        Node max = min;

        if (blendRange == 0) {
            return min.source.getValue(x, y);
        }

        if (selector > min.max) {
            max = nodes[index + 1];
        } else if (selector < min.min) {
            min = nodes[index - 1];
        } else {
            return min.source.getValue(x ,y);
        }

        float alpha = (selector - min.max) / blendRange;
        alpha = NoiseUtil.clamp(alpha, 0, 1);

        return blendValues(min.source.getValue(x, y), max.source.getValue(x, y), alpha);
    }

    private static class Node {

        private final Module source;
        private final float min;
        private final float max;

        private Node(Module source, float min, float max) {
            this.source = source;
            this.min = Math.max(0, min);
            this.max = Math.min(1, max);
        }

        @Override
        public String toString() {
            return "Slot{" +
                    "min=" + min +
                    ", max=" + max +
                    '}';
        }
    }
}
