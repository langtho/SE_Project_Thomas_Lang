package compressor.services;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Random;


class BitPackerTest {

    String jsonFile="src/main/resources/performance_data.jsonl";
    private static final int tests_per_case = 100;
    private static final Random RANDOM = new Random();

    private final SpanningBP spanningBP=new SpanningBP(jsonFile);
    private final NonSpanningBP nonSpanningBP=new NonSpanningBP(jsonFile);
    private final OverflowBP overflow=new OverflowBP(jsonFile);


    static Stream<Arguments> provideTestArrays() {
        return Stream.of(
                // --- Normal Cases ---
                Arguments.of((Object) new int[]{1, 2, 3, 4, 5, 6}),
                Arguments.of((Object) new int[]{10, 20, 30, 40, 50}),
                Arguments.of((Object) new int[]{100, 200, 300}),

                // --- Smaller values (fit in 4 bits) ---
                Arguments.of((Object) new int[]{1, 5, 12, 8, 3, 15}),
                Arguments.of((Object) new int[]{0, 0, 0, 0, 0}),

                // --- Bit Boundary Cases ---
                // Requires 10 bits (up to 1023)
                Arguments.of((Object) new int[]{500, 1000, 750, 250}),
                // Requires 12 bits (up to 4095)
                Arguments.of((Object) new int[]{2048, 4095, 1024}),

                // --- Edge Cases ---
                Arguments.of((Object) new int[]{}), // Empty array
                Arguments.of((Object) new int[]{1}), // Single element
                Arguments.of((Object) new int[]{123456789}), // Single large value

                // --- Tests for Integer.MAX_VALUE ---
                Arguments.of((Object) new int[]{Integer.MAX_VALUE, 0, Integer.MAX_VALUE}),
                Arguments.of((Object) new int[]{Integer.MAX_VALUE, 0, Integer.MAX_VALUE}),

                // --- Mixed large and small values (overflow relevant) ---
                Arguments.of((Object) new int[]{1, 2, 1024, 3, 4, 2048}),
                Arguments.of((Object) new int[]{1, 2, 1024, 3, 4, 2048})

                // --- Negative Numbers Not solvable for the moment---
                /*
                Arguments.of(new int[]{-1, -100, -1000, 1000, 100}),
                Arguments.of(new int[]{-5, -10, -15})
                 */
        );
    }

    public static Stream<Arguments> generateRandomArrays() {
        return TestDataGenerator.generateAllTestCases(tests_per_case); // Generiert 100 Arrays für jede Kategorie
    }

    // Methode, die die Testdaten liefert (verwende deinen TestDataGenerator)
    @MethodSource("generateRandomArrays")
    @ParameterizedTest
    void testNonSpanning(int[] array) {
        int[] compressed = nonSpanningBP.compress(array);
        int[] decompressed = nonSpanningBP.decompress(compressed);
        assertArrayEquals(array, decompressed, "NonSpanning: The decompressed array should match the original.");
        if (array.length > 0) {
            int i = RANDOM.nextInt(array.length);
            int retrievedValue = nonSpanningBP.get(i, compressed);
            assertEquals(retrievedValue, array[i], "NonSpanning: Retrieved value should match original at index " + i);
        }
    }

    @MethodSource("generateRandomArrays")
    @ParameterizedTest
    void testSpanning(int[] array) {
        int[] compressed = spanningBP.compress(array);
        int[] decompressed = spanningBP.decompress(compressed);
        assertArrayEquals(array, decompressed, "Spanning: The decompressed array should match the original.");
        if (array.length > 0) {
            int i = RANDOM.nextInt(array.length);
            int retrievedValue = spanningBP.get(i, compressed);
            assertEquals(retrievedValue, array[i], "Spanning: Retrieved value should match original at index " + i);
        }
    }

    @MethodSource("generateRandomArrays")
    @ParameterizedTest
    void testOverflow(int[] array) {
        int[] compressed = overflow.compress(array);
        int[] decompressed = overflow.decompress(compressed);
        assertArrayEquals(array, decompressed, "Overflow: The decompressed array should match the original.");
        if (array.length > 0) {
            int i = RANDOM.nextInt(array.length);
            int retrievedValue = overflow.get(i, compressed);
            assertEquals(retrievedValue, array[i], "Overflow: Retrieved value should match original at index " + i);
        }
    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testCompressionAndDecompressionNonSpanning(int[] originalArray) {

        int[] compressed = nonSpanningBP.compress(originalArray);
        int[] decompressed = nonSpanningBP.decompress(compressed);

        assertArrayEquals(originalArray, decompressed, "The decompressed array should match the original.");
    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testDirectAccessNonSpanning(int[] originalArray) {

        int[] compressed = nonSpanningBP.compress(originalArray);


        for (int i=0; i<originalArray.length; i++) {
            int retrievedValue = nonSpanningBP.get(i,compressed);
            assertEquals(originalArray[i], retrievedValue, "The value retrieved via get() should match the original value.");
        }

    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testCompressionAndDecompressionSpanning(int[] originalArray) {

        int[] compressed = spanningBP.compress(originalArray);
        int[] decompressed = spanningBP.decompress(compressed);

        assertArrayEquals(originalArray, decompressed, "The decompressed array should match the original.");
    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testDirectAccessSpanning(int[] originalArray) {

        int[] compressed = spanningBP.compress(originalArray);


        for (int i=0; i<originalArray.length; i++) {
            int retrievedValue = spanningBP.get(i,compressed);
            assertEquals(originalArray[i], retrievedValue, "The value retrieved via get() should match the original value.");
        }

    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testCompressionAndDecompressionOverflow(int[] originalArray) {

        int[] compressed = overflow.compress(originalArray);
        int[] decompressed = overflow.decompress(compressed);

        assertArrayEquals(originalArray, decompressed, "The decompressed array should match the original.");
    }

    @ParameterizedTest
    @MethodSource("provideTestArrays")
    void testDirectAccessOverflow(int[] originalArray) {

        int[] compressed = overflow.compress(originalArray);


        for (int i=0; i<originalArray.length; i++) {
            int retrievedValue = overflow.get(i,compressed);
            assertEquals(originalArray[i], retrievedValue, "The value retrieved via get() should match the original value.");
        }

    }

}