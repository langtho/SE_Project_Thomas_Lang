package compressor.services;

import org.junit.jupiter.params.provider.Arguments;
import java.util.Random;
import java.util.stream.Stream;

public class DataGenerator {

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


                    if (isMixedRandom) {
                        int randomBits = RANDOM.nextInt(32) + 1; // 1 to 32 bits

                        if (randomBits == 32) {

                            value = RANDOM.nextInt();


                            if (arraySize > 1 && value == 0) {
                                value = 1;
                            }
                        } else {

                            long mask = (1L << randomBits) - 1;


                            if (mask > 0) {
                                value = RANDOM.nextInt((int) mask) + 1; // Range: [1, mask]
                            } else {
                                value = 1;
                            }
                        }

                    } else if (valueRange[1] > valueRange[0]) {

                        int min = Math.max(1, valueRange[0]);
                        value = RANDOM.nextInt(valueRange[1] - min + 1) + min;

                    } else {

                        value = valueRange[0];
                    }


                    randomArray[i] = Math.abs(value);
                }
            }
            return Arguments.of(sizeLabel,valueLabel, randomArray);
        }).limit(count);
    }

}