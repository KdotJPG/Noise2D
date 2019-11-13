package me.dags.noise.domain;

public class Combiner implements Domain {

    private final Domain a;
    private final Domain b;

    public Combiner(Domain a, Domain b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public float getOffsetX(float x, float y) {
        float ax = a.getX(x, y);
        float ay = a.getY(x, y);
        return b.getX(ax, ay);
    }

    @Override
    public float getOffsetY(float x, float y) {
        float ax = a.getX(x, y);
        float ay = a.getY(x, y);
        return b.getY(ax, ay);
    }
}
