package ui;

import me.dags.noise.Module;
import me.dags.noise.source.fast.CellType;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author dags <dags@dags.me>
 */
public class Renderer implements Viewer.Renderer {

    private final Module module;

    public Renderer(Module module) {
        this.module = module;
    }

    @Override
    public void accept(BufferedImage img, int xOff, int zOff) {
        Viewer.clear(img);

        for (int z = 0; z < img.getWidth(); z++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int px = x + xOff;
                int pz = z + zOff;

                if (z == 256) {
                    img.setRGB(x, z, Color.GREEN.getRGB());

                    float noise = module.getValue(px, pz);
                    int height = (int) (noise * 255);
                    for (int y = 1; y <= height; y++) {
                        img.setRGB(x, img.getHeight() - y, Color.WHITE.getRGB());
                    }
                } else {
                    if ((px & 15) == 0 || (pz & 15) == 0) {
                        continue;
                    }
                    float noise = module.getValue(px, pz);
                    int gray = (int) (noise * 255);
                    Color color = new Color(gray, gray, gray);
                    img.setRGB(x, z, color.getRGB());
                }
            }
        }
    }

    public static void main(String[] args) {
        Module cell = Module.cell(2, 128, CellType.Distance2);
        Module ridge = Module.ridge(3, 128, 3);
        Module perlin = Module.perlin(1, 96, 3);
        Module blend = perlin.blend(cell, ridge); // perlin controls the blend of cell & ridge
        Renderer renderer = new Renderer(blend.norm()); // make sure output is normalized to the 0-1 range
        Viewer viewer = new Viewer(512, 512 + 256);
        viewer.setRenderer(renderer);
    }
}
