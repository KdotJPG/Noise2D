package me.dags.noise.source;

import me.dags.noise.Builder;
import me.dags.noise.Module;
import me.dags.noise.Source;

/**
 * @author dags <dags@dags.me>
 */
public class Constant implements Source {

    private final float value;

    public Constant(float value) {
        this.value = value;
    }

    @Override
    public float minValue() {
        return value;
    }

    @Override
    public float maxValue() {
        return value;
    }

    @Override
    public Builder toBuilder() {
        return Module.builder();
    }

    @Override
    public float getValue(float x, float y) {
        return value;
    }
}
