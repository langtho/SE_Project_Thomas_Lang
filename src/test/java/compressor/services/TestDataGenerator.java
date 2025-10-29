package compressor.services;

import org.junit.jupiter.params.provider.Arguments;
import java.util.Random;
import java.util.stream.Stream;

public class TestDataGenerator {

    private static final Random RANDOM = new Random();

    /**
     * Generates a comprehensive stream of test arrays, covering various sizes and value ranges.
     *
     * @param tests_per_case The number of test cases per combination.
     * @return A stream of Arguments with the random int arrays.
     */
    public static Stream<Arguments> generateAllTestCases(int tests_per_case) {
        // Defines the size ranges for the arrays
        Object[][] sizeRanges = {
                {"small_s",10, 50},       // Small
                {"small_medium_s",50, 500},      // Small-Medium
                {"medium_s",500, 1000},    // Medium
                {"medium_large_s",1000, 5000},   // Medium-Large
                {"large_s",5000, 10000}   // Large
        };

        // Defines the value ranges for the array elements
        Object[][] valueRanges = {
                {"small_v",0, 1000},                      // Small
                {"small_medium_v",1000, 10000},                  // Small-Medium
                {"medium_v",10000, 100000},                // Medium
                {"medium_large_v",100000, 10000000},             // Medium-Large
                {"large_v",10000000, 200000000},          // Large
                {"mixed_v",0, 0}          // Mixed (full range)
        };

        // Generate streams for each combination of size and value ranges
        Stream<Arguments> combinedStream = Stream.empty();
        for (Object[] sizeRange : sizeRanges) {
            String sizeLabel =(String) sizeRange[0];
            int sizeMin = (int) sizeRange[1];
            int sizeMax = (int) sizeRange[2];
            int[] sizes={sizeMin, sizeMax};

            for (Object[] valueRange : valueRanges) {
                String valueLabel =(String) valueRange[0];
                int valueMin = (int) valueRange[1];
                int valueMax = (int) valueRange[2];
                int[] values = {valueMin, valueMax};

                combinedStream = Stream.concat(combinedStream, createArrayStream(tests_per_case, sizes, values,sizeLabel,valueLabel));
            }
        }
        return combinedStream;
    }


    private static Stream<Arguments> createArrayStream(int count, int[] sizeRange, int[] valueRange,String sizeLabel,String valueLabel) {
        if (count <= 0) {
            return Stream.empty();
        }
        final boolean isMixedRandom = valueLabel.startsWith("mixed_v");

        return Stream.generate(() -> {
            int arraySize = RANDOM.nextInt(sizeRange[1] - sizeRange[0] + 1) + sizeRange[0];
            int[] randomArray = new int[arraySize];

            if (arraySize > 0) {

                for (int i = 0; i < arraySize; i++) {
                    int value;

                    // 1. ZUFAELLIGE WERTGENERIERUNG
                    if (isMixedRandom) {
                        int randomBits = RANDOM.nextInt(32) + 1; // 1 to 32 bits

                        if (randomBits == 32) {
                            // KORREKTUR: Verwende die volle Range. Da der Bit-Packer positive Werte erwartet,
                            // nutzen wir direkt Math.abs(RANDOM.nextInt()).
                            value = RANDOM.nextInt();

                            // Garantiere, dass der Wert nicht 0 ist, wenn das Array groß genug ist
                            if (arraySize > 1 && value == 0) {
                                value = 1; // Setze den Wert auf 1, wenn er zufällig 0 wäre
                            }
                        } else {
                            // KORREKTUR: Verwende die Maske, aber generiere die Zahl im Bereich [1, mask]
                            long mask = (1L << randomBits) - 1;

                            // Generiere Wert im Bereich [1, mask] (mindestens 1, wenn mask > 0)
                            if (mask > 0) {
                                value = RANDOM.nextInt((int) mask) + 1; // Range: [1, mask]
                            } else {
                                value = 1; // Mindestens 1 für 1-Bit-Zahlen
                            }
                        }

                    } else if (valueRange[1] > valueRange[0]) {
                        // Standard-Bereichsgenerierung
                        // Garantiere Minimum 1, wenn der Bereich 0 enthält
                        int min = Math.max(1, valueRange[0]);
                        value = RANDOM.nextInt(valueRange[1] - min + 1) + min;

                    } else {
                        // Feste Werte
                        value = valueRange[0];
                    }

                    // 2. ZUWEISUNG (NACH Math.abs)
                    // Math.abs(value) stellt sicher, dass alle generierten Werte positiv sind
                    randomArray[i] = Math.abs(value);
                }
            }
            return Arguments.of(sizeLabel,valueLabel,(Object) randomArray);
        }).limit(count);
    }

}