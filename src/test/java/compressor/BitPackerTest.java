package compressor;

import compressor.logger.LoggerFactory;
import compressor.services.NonSpanningBP;
import compressor.services.OverflowBP;
import compressor.services.SpanningBP;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;

import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Random;
import java.util.Arrays;


class BitPackerTest {

    File jsonFile=new File("src/main/resources/performance_data.jsonl");
    private static final int tests_per_case = 100;
    private static final Random RANDOM = new Random();

    private final SpanningBP spanningBP=new SpanningBP(jsonFile, LoggerFactory.createLogger("NONE"));
    private final NonSpanningBP nonSpanningBP=new NonSpanningBP(jsonFile, LoggerFactory.createLogger("NONE"));
    private final OverflowBP overflow=new OverflowBP(jsonFile, LoggerFactory.createLogger("NONE"));


    static Stream<Arguments> provideTestArrays() {
        return Stream.of(
                // --- Normal Cases ---
                Arguments.of("small_s","small_v", new int[]{1, 2, 3, 4, 5, 6}),
                Arguments.of("small_s","small_v", new int[]{10, 20, 30, 40, 50}),
                Arguments.of("small_s","small_v", new int[]{100, 200, 300}),

                // --- Smaller values (fit in 4 bits) ---
                Arguments.of("small_s","small_v", new int[]{1, 5, 12, 8, 3, 15}),
                Arguments.of("small_s","small_v", new int[]{0, 0, 0, 0, 0}),

                // --- Bit Boundary Cases ---
                // Requires 10 bits (up to 1023)
                Arguments.of("small_s","small_v", new int[]{500, 1000, 750, 250}),
                // Requires 12 bits (up to 4095)
                Arguments.of("small_s","small_v", new int[]{2048, 4095, 1024}),

                // --- Edge Cases ---
                Arguments.of("small_s","small_v", new int[]{}), // Empty array
                Arguments.of("small_s","small_v", new int[]{1}), // Single element
                Arguments.of("small_s","large_v", new int[]{123456789}), // Single large value

                // --- Tests for Integer.MAX_VALUE ---
                Arguments.of("small_s","large_v", new int[]{Integer.MAX_VALUE, 0, Integer.MAX_VALUE}),
                Arguments.of("small_s","large_v", new int[]{Integer.MAX_VALUE, 0, Integer.MAX_VALUE}),

                // --- Mixed large and small values (overflow relevant) ---
                Arguments.of("small_s","small_medium_v", new int[]{1, 2, 1024, 3, 4, 2048}),
                Arguments.of("small_s","small_medium_v", new int[]{1, 2, 1024, 3, 4, 2048})


        );
    }

    public static Stream<Arguments> generateRandomArrays() {
        return DataGenerator.generateAllTestCases(tests_per_case);
    }

    // Methode, die die Testdaten liefert (verwende deinen TestDataGenerator)
    @ParameterizedTest
    @MethodSource("generateRandomArrays")
    void testNonSpanning(String sizeLabel, String valueLabel,int[] array) {
        int[] compressed = nonSpanningBP.compress(array,  sizeLabel, valueLabel);
        int[] decompressed = nonSpanningBP.decompress(compressed,  sizeLabel, valueLabel);
        assertArrayEquals(array, decompressed, "NonSpanning: The decompressed array should match the original.");
        if (array.length > 0) {
            int i = RANDOM.nextInt(array.length);
            int retrievedValue = nonSpanningBP.get(i, compressed,  sizeLabel, valueLabel);
            assertEquals(retrievedValue, array[i], "NonSpanning: Retrieved value should match original at index " + i);
        }
    }

    @MethodSource("generateRandomArrays")
    @ParameterizedTest
    void testSpanning(String sizeLabel, String valueLabel,int[] array) {
        int[] compressed = spanningBP.compress(array,  sizeLabel, valueLabel);
        int[] decompressed = spanningBP.decompress(compressed,  sizeLabel, valueLabel);
        assertArrayEquals(array, decompressed, "Spanning: The decompressed array should match the original.");
        if (array.length > 0) {
            int i = RANDOM.nextInt(array.length);
            int retrievedValue = spanningBP.get(i, compressed,  sizeLabel, valueLabel);
            assertEquals(retrievedValue, array[i], "Spanning: Retrieved value should match original at index " + i);
        }
    }

    @MethodSource("generateRandomArrays")
    @ParameterizedTest
    void testOverflow(String sizeLabel, String valueLabel,int[] array) {
        int[] original_for_comparison = Arrays.copyOf(array, array.length);
        int[] mutable_array_for_compress = array.clone();

        int[] compressed = overflow.compress(mutable_array_for_compress,  sizeLabel, valueLabel);
        int[] decompressed = overflow.decompress(compressed,  sizeLabel, valueLabel);
        assertArrayEquals(original_for_comparison, decompressed, "Overflow: The decompressed array should match the original.");
        if (array.length > 0) {
            int i = RANDOM.nextInt(array.length);
            int retrievedValue = overflow.get(i, compressed,  sizeLabel, valueLabel);
            assertEquals(retrievedValue, original_for_comparison[i], "Overflow: Retrieved value should match original at index " + i);
        }
    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testCompressionAndDecompressionNonSpanning(String sizeLabel, String valueLabel,int[] originalArray) {

        int[] compressed = nonSpanningBP.compress(originalArray,  sizeLabel, valueLabel);
        int[] decompressed = nonSpanningBP.decompress(compressed,  sizeLabel, valueLabel);

        assertArrayEquals(originalArray, decompressed, "The decompressed array should match the original.");
    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testDirectAccessNonSpanning(String sizeLabel, String valueLabel,int[] originalArray) {

        int[] compressed = nonSpanningBP.compress(originalArray,  sizeLabel, valueLabel);


        for (int i=0; i<originalArray.length; i++) {
            int retrievedValue = nonSpanningBP.get(i,compressed,  sizeLabel, valueLabel);
            assertEquals(originalArray[i], retrievedValue, "The value retrieved via get() should match the original value.");
        }

    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testCompressionAndDecompressionSpanning(String sizeLabel, String valueLabel,int[] originalArray) {

        int[] compressed = spanningBP.compress(originalArray,  sizeLabel, valueLabel);
        int[] decompressed = spanningBP.decompress(compressed,  sizeLabel, valueLabel);

        assertArrayEquals(originalArray, decompressed, "The decompressed array should match the original.");
    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testDirectAccessSpanning(String sizeLabel, String valueLabel,int[] originalArray) {

        int[] compressed = spanningBP.compress(originalArray,  sizeLabel, valueLabel);


        for (int i=0; i<originalArray.length; i++) {
            int retrievedValue = spanningBP.get(i,compressed,  sizeLabel, valueLabel);
            assertEquals(originalArray[i], retrievedValue, "The value retrieved via get() should match the original value.");
        }

    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testCompressionAndDecompressionOverflow(String sizeLabel, String valueLabel,int[] originalArray) {

        int[] compressed = overflow.compress(originalArray,  sizeLabel, valueLabel);
        int[] decompressed = overflow.decompress(compressed,  sizeLabel, valueLabel);

        assertArrayEquals(originalArray, decompressed, "The decompressed array should match the original.");
    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testDirectAccessOverflow(String sizeLabel, String valueLabel,int[] originalArray) {

        int[] compressed = overflow.compress(originalArray,  sizeLabel, valueLabel);


        for (int i=0; i<originalArray.length; i++) {
            int retrievedValue = overflow.get(i,compressed,  sizeLabel, valueLabel);
            assertEquals(originalArray[i], retrievedValue, "The value retrieved via get() should match the original value.");
        }

    }

}