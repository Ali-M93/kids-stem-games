package io.github.foadsf.patternmachine;

/**
 * A token is a shape and a colour packed into one int: {@code shape * 8 + colour}.
 * Plain Java on purpose (no Android types), so the pattern logic can be tested on any JVM.
 */
final class Tokens {
    static final int CIRCLE = 0, TRIANGLE = 1, SQUARE = 2, STAR = 3, HEART = 4, DIAMOND = 5;
    static final int SHAPES = 6, COLOURS = 6;
    static final String[] SHAPE_NAME = {"circle", "triangle", "square", "star", "heart", "diamond"};
    static final String[] COLOUR_NAME = {"pink", "yellow", "blue", "green", "purple", "orange"};

    static int token(int shape, int colour) {
        return shape * 8 + colour;
    }

    static int shapeOf(int t) {
        return t / 8;
    }

    static int colourOf(int t) {
        return t % 8;
    }

    static String name(int t) {
        return SHAPE_NAME[shapeOf(t)] + "-" + COLOUR_NAME[colourOf(t)];
    }

    private Tokens() {
    }
}
