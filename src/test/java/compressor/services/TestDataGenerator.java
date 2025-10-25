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
                {"mixed_v",0, Integer.MAX_VALUE}          // Mixed (full range)
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
            return Arguments.of(sizeLabel,valueLabel,(Object) randomArray);
        }).limit(count);
    }
}