package compressor.services;

import org.junit.jupiter.params.provider.Arguments;
import java.util.Random;
import java.util.stream.Stream;

public class TestDataGenerator {

    private static final Random RANDOM = new Random();

    /**
     * Generiert einen umfassenden Stream von Test-Arrays, der verschiedene Größen- und Wertebereiche abdeckt.
     *
     * @param tests_per_case Die Anzahl der Testfälle pro Kombination.
     * @return Ein Stream von Arguments mit den zufälligen int-Arrays.
     */
    public static Stream<Arguments> generateAllTestCases(int tests_per_case) {
        // Defines the size ranges for the arrays
        int[][] sizeRanges = {
                {10, 50},       // Small
                {50, 500},      // Small-Medium
                {500, 1000},    // Medium
                {1000, 5000},   // Medium-Large
                {5000, 10000}   // Large
        };

        // Defines the value ranges for the array elements
        int[][] valueRanges = {
                {0, 1000},                      // Small
                {1000, 10000},                  // Small-Medium
                {10000, 100000},                // Medium
                {100000, 10000000},             // Medium-Large
                {10000000, 200000000},          // Large
                {0, Integer.MAX_VALUE}          // Mixed (full range)
        };

        // Generate streams for each combination of size and value ranges
        Stream<Arguments> combinedStream = Stream.empty();
        for (int[] sizeRange : sizeRanges) {
            for (int[] valueRange : valueRanges) {
                combinedStream = Stream.concat(combinedStream, createArrayStream(tests_per_case, sizeRange, valueRange));
            }
        }
        return combinedStream;
    }

    /**
     * Hilfsmethode zur Erstellung eines Streams von zufälligen int-Arrays.
     */
    private static Stream<Arguments> createArrayStream(int count, int[] sizeRange, int[] valueRange) {
        if (count <= 0) {
            return Stream.empty();
        }

        return Stream.generate(() -> {
            int arraySize = RANDOM.nextInt(sizeRange[1] - sizeRange[0] + 1) + sizeRange[0];
            int[] randomArray = new int[arraySize];
            if (arraySize > 0) {
                for (int i = 0; i < arraySize; i++) {
                    if (valueRange[1] > valueRange[0]) {
                        randomArray[i] = RANDOM.nextInt(valueRange[1] - valueRange[0]) + valueRange[0];
                    } else {
                        randomArray[i] = valueRange[0];
                    }
                }
            }
            return Arguments.of((Object) randomArray);
        }).limit(count);
    }
}