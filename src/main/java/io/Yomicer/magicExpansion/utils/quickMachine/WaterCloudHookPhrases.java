package io.Yomicer.magicExpansion.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Water Cloud fishing status messages (guide-style)
 * Each state has several unique descriptions that can later be used in a fishing guide
 */
public final class WaterCloudHookPhrases {

    private static final Map<WaterCloudHookState, List<String>> PHRASES = Map.of(
            WaterCloudHookState.WAITING, List.of(
                    "The bobber sits still on calm water",
                    "The rod tip dips slightly; all is quiet",
                    "The water is still as a mirror, with only the wind to be heard",
                    "Cloud shadows drift by, but no fish can be seen"
            ),
            WaterCloudHookState.LIGHT_BITE, List.of(
                    "Tiny ripples spread across the water",
                    "The bobber gives a small shake",
                    "A fish is nibbling, then pulling away",
                    "A dark shadow slips beneath the water"
            ),
            WaterCloudHookState.FULL_BITE, List.of(
                    "The bobber suddenly sinks underwater",
                    "The rod pulls tight and water splashes everywhere",
                    "A big fish is hooked, bending the rod",
                    "A burst of splashes breaks across the water"
            )
    );

    /** Hook-escaped messages (for brief status only) */
    private static final List<String> ESCAPED_PHRASES = List.of(
            "The bobber dips and rises—the fish got away",
            "The water settles, leaving only ripples",
            "The fish slips free, as if it was never there",
            "The water and clouds remain, but this fish wasn't meant to be"
    );

    private WaterCloudHookPhrases() {
    }

    /** Get a random message for the current state */
    public static String getRandom(WaterCloudHookState state) {
        List<String> pool = PHRASES.getOrDefault(state, ESCAPED_PHRASES);
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    /** Get a random message for a fish that got away */
    public static String getRandomEscaped() {
        return ESCAPED_PHRASES.get(ThreadLocalRandom.current().nextInt(ESCAPED_PHRASES.size()));
    }
}
