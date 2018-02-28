package me.dags.noise.modifier;

import me.dags.noise.Builder;
import me.dags.noise.Module;

/**
 * @author dags <dags@dags.me>
 */
public class Turbulence extends Modifier {

    private final Module turb0;
    private final Module turb1;
    private final Module source;
    private final float power;

    public Turbulence(Builder builder) {
        super(builder.source());
        turb0 = builder.perlin();
        turb1 = builder.seed(builder.seed() + 1).perlin();
        source = builder.source();
        power = builder.power();
    }

    public Turbulence(Module source, Module turb0, Module turb1, float power) {
        super(source);
        this.turb0 = turb0;
        this.turb1 = turb1;
        this.source = source;
        this.power = power;
    }

    @Override
    public float getValue(float x, float y) {
        float x0 = x + (12414.0F / 65536.0F);
        float y0 = y + (31337.0F / 65536.0F);
        float x1 = x + (53820.0F / 65536.0F);
        float y1 = y + (44845.0F / 65536.0F);
        x += turb0.getValue(x0, y0) * power;
        y += turb1.getValue(x1, y1) * power;
        return source.getValue(x, y);
    }

    @Override
    public float modify(float x, float y, float noiseValue) {
        float x0 = x + (12414.0F / 65536.0F);
        float y0 = y + (31337.0F / 65536.0F);
        float x1 = x + (53820.0F / 65536.0F);
        float y1 = y + (44845.0F / 65536.0F);
        x += turb0.getValue(x0, y0) * power;
        y += turb1.getValue(x1, y1) * power;
        return source.getValue(x, y);
    }
}
