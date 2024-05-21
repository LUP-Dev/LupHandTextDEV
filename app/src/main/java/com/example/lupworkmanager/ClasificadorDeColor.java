package com.example.lupworkmanager;

public class ClasificadorDeColor {

    public static String clasificador(int r, int g, int b) {
        float[] hsl = rgbToHsl(r, g, b);
        String tono = "";
        String colorPrincipal = getColorPrincipal(hsl);
        if (!colorPrincipal.equals("Negro") && !colorPrincipal.equals("Blanco")) {
            if (isLight(hsl)) {
                tono = " Claro";
            } else if (isDark(hsl)) {
                tono = " Oscuro";
            }
        }


        return colorPrincipal + tono;
    }

    private static float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0f, s, l = (max + min) / 2f;

        if (delta == 0) {
            h = 0f; // achromatic
        } else {
            if (max == rf) {
                h = ((gf - bf) / delta + (gf < bf ? 6 : 0)) * 60;
            } else if (max == gf) {
                h = ((bf - rf) / delta + 2) * 60;
            } else {
                h = ((rf - gf) / delta + 4) * 60;
            }
        }

        if (delta == 0) {
            s = 0f;
        } else {
            s = delta / (1 - Math.abs(2 * l - 1));
        }

        return new float[]{h, s, l};
    }

    private static String getColorPrincipal(float[] hsl) {
        float hue = hsl[0];
        float saturation = hsl[1];
        float lightness = hsl[2];

        if (saturation < 0.15 && lightness < 0.15) return "Negro";
        if (saturation < 0.10 && lightness > 0.85) return "Blanco";
        if (saturation < 0.15) return "Gris";

        // Rango para Marrón
        if (hue >= 20 && hue < 40 && saturation > 0.2 && saturation < 0.7 && lightness < 0.5)
            return "Marrón";

        if (hue < 15) return "Rojo";
        if (hue >= 15 && hue < 25) return "Rojo Anaranjado";
        if (hue >= 25 && hue < 35) return "Naranja";
        if (hue >= 35 && hue < 40) return "Amarillo ocre";
        if (hue >= 40 && hue < 65) return "Amarillo";
        if (hue >= 65 && hue < 75) return "Verde Pistacho";
        if (hue >= 75 && hue < 90) return "Verde Lima";
        if (hue >= 90 && hue < 135) return "Verde";
        if (hue >= 135 && hue < 165) return "Turquesa";
        if (hue >= 165 && hue < 195) return "Cián";
        if (hue >= 195 && hue < 225) return "Azul Océano";
        if (hue >= 225 && hue < 255) return "Azul";
        if (hue >= 255 && hue < 285) return "Violeta";
        if (hue >= 285 && hue < 315) return "Rosa";
        if (hue >= 315 && hue < 335) return "Fúcsia";
        if (hue >= 335 && hue < 345) return "Frambuesa";
        if (hue >= 345) return "Rojo";

        return "Color no reconocido";
    }

    private static boolean isLight(float[] hsl) {
        float lightness = hsl[2];
        return lightness > 0.6;
    }

    private static boolean isDark(float[] hsl) {
        float lightness = hsl[2];
        return lightness < 0.25;
    }
}

