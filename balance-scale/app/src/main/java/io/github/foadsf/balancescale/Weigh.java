package io.github.foadsf.balancescale;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One Balance Scale puzzle. Plain Java (no Android types), so it can be tested on any JVM.
 *
 * Every fruit has a weight in "apples": apple 1, pear 1, banana 2, watermelon 3. The left pan is
 * fixed; the right pan may start with some fruit, and she adds fruit from the basket to it until
 * both sides weigh the same. Any combination that balances counts: two bananas balance a
 * watermelon and an apple just as well as five apples do.
 *
 * Stages, easiest first:
 *   0  left: 1-3 apples                                  basket: apple
 *   1  left: 3-6 apples                                  basket: apple
 *   2  left: 4-7 apples, right already has some          basket: apple        (5 = 2 + ?)
 *   3  left: apples and pears, 3-6 in all                basket: apple, pear  (same weight)
 *   4  left: one watermelon                              basket: apple        (fact: 🍉 = 3 🍎)
 *   5  left: watermelon + 1-3 apples                     basket: apple
 *   6  left: 1-2 bananas + 0-2 apples                    basket: banana, apple (fact: 🍌 = 2 🍎)
 *   7  both pans start with heavy fruit; right is lighter basket: banana, apple
 * Past stage 7, each puzzle picks one of stages 4-7.
 */
final class Weigh {
    static final int APPLE = 0, PEAR = 1, BANANA = 2, MELON = 3;
    static final int[] WEIGHT = {1, 1, 2, 3};
    static final String[] NAME = {"apple", "pear", "banana", "melon"};
    static final int LAST_STAGE = 7;

    final int stage, kind;
    final List<Integer> left = new ArrayList<>();
    final List<Integer> rightGiven = new ArrayList<>();
    final int[] basket;
    final int[] facts;   // fruits whose weight in apples is shown as a fact card

    private Weigh(int stage, int kind, int[] basket, int[] facts) {
        this.stage = stage;
        this.kind = kind;
        this.basket = basket;
        this.facts = facts;
    }

    static int weight(List<Integer> fruit) {
        int w = 0;
        for (int f : fruit) w += WEIGHT[f];
        return w;
    }

    /** How much more the right pan needs (in apples) before she adds anything. */
    int need() {
        return weight(left) - weight(rightGiven);
    }

    static Weigh make(int stage, Random r) {
        int k = stage > LAST_STAGE ? 4 + r.nextInt(LAST_STAGE - 3) : stage;
        Weigh w;
        switch (k) {
            case 0:
                w = new Weigh(stage, k, new int[]{APPLE}, new int[0]);
                add(w.left, APPLE, 1 + r.nextInt(3));
                break;
            case 1:
                w = new Weigh(stage, k, new int[]{APPLE}, new int[0]);
                add(w.left, APPLE, 3 + r.nextInt(4));
                break;
            case 2: {
                w = new Weigh(stage, k, new int[]{APPLE}, new int[0]);
                int n = 4 + r.nextInt(4);
                add(w.left, APPLE, n);
                add(w.rightGiven, APPLE, 1 + r.nextInt(n - 1));
                break;
            }
            case 3: {
                w = new Weigh(stage, k, new int[]{APPLE, PEAR}, new int[]{PEAR});
                int n = 3 + r.nextInt(4), pears = 1 + r.nextInt(n - 1);
                add(w.left, PEAR, pears);
                add(w.left, APPLE, n - pears);
                break;
            }
            case 4:
                w = new Weigh(stage, k, new int[]{APPLE}, new int[]{MELON});
                add(w.left, MELON, 1);
                break;
            case 5:
                w = new Weigh(stage, k, new int[]{APPLE}, new int[]{MELON});
                add(w.left, MELON, 1);
                add(w.left, APPLE, 1 + r.nextInt(3));
                break;
            case 6:
                w = new Weigh(stage, k, new int[]{BANANA, APPLE}, new int[]{BANANA});
                add(w.left, BANANA, 1 + r.nextInt(2));
                add(w.left, APPLE, r.nextInt(3));
                break;
            default: {
                w = new Weigh(stage, k, new int[]{BANANA, APPLE}, new int[]{BANANA, MELON});
                // Left: two heavy fruits. Right: one heavy fruit that is lighter than the left.
                int a = r.nextBoolean() ? MELON : BANANA, b = r.nextBoolean() ? MELON : BANANA;
                add(w.left, a, 1);
                add(w.left, b, 1);
                add(w.rightGiven, r.nextBoolean() ? MELON : BANANA, 1);
                break;
            }
        }
        return w;
    }

    private static void add(List<Integer> pan, int fruit, int n) {
        for (int i = 0; i < n; i++) pan.add(fruit);
    }
}
