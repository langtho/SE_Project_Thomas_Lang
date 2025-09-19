package compressor.services;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BitPackerTest {

    private final SpanningBP spanningBP=new SpanningBP();
    private final NonSpanningBP nonSpanningBP=new NonSpanningBP();

    static Stream<Arguments> provideTestArrays() {
        return Stream.of(
                // --- Normal Cases ---
                Arguments.of(new int[]{1, 2, 3, 4, 5, 6}),
                Arguments.of(new int[]{10, 20, 30, 40, 50}),
                Arguments.of(new int[]{100, 200, 300}),

                // --- Smaller values (fit in 4 bits) ---
                Arguments.of(new int[]{1, 5, 12, 8, 3, 15}),
                Arguments.of(new int[]{0, 0, 0, 0, 0}),

                // --- Bit Boundary Cases ---
                // Requires 10 bits (up to 1023)
                Arguments.of(new int[]{500, 1000, 750, 250}),
                // Requires 12 bits (up to 4095)
                Arguments.of(new int[]{2048, 4095, 1024}),

                // --- Edge Cases ---
                Arguments.of(new int[]{}), // Empty array
                Arguments.of(new int[]{1}), // Single element
                Arguments.of(new int[]{123456789}), // Single large value

                // --- Tests for Integer.MAX_VALUE ---
                Arguments.of(new int[]{Integer.MAX_VALUE, 0, Integer.MAX_VALUE}),
                Arguments.of(new int[]{Integer.MAX_VALUE, 0, Integer.MAX_VALUE}),

                // --- Mixed large and small values (overflow relevant) ---
                Arguments.of(new int[]{1, 2, 1024, 3, 4, 2048}),
                Arguments.of(new int[]{1, 2, 1024, 3, 4, 2048})

                // --- Negative Numbers Not solvable for the moment---
                /*
                Arguments.of(new int[]{-1, -100, -1000, 1000, 100}),
                Arguments.of(new int[]{-5, -10, -15})
                 */
        );
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

}