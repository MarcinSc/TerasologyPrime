package com.gempukku.terasology.procedural;

public class FastMath {
    private FastMath() {
    }

    public static int floor(float val) {
        int i = (int) val;
        return (val < 0 && val != i) ? i - 1 : i;
    }
}
