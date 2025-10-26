package compressor.models;


public interface BitPacker {

    /**
     * Compresses the input array of integers.
     * * @param array The array of integers to compress.
     * @param sizeLabel A custom label for the performance Logging "arraysize".
     * @param valueLabel A custom label for the performance Logging "valuesize".
     * @return The resulting compressed array.
     */
    int[] compress(int[] array, String sizeLabel, String valueLabel);

    /**
     * Decompresses the input array of integers.
     * * @param array The compressed array of integers.
     * @param sizeLabel A custom label for the performance Logging "arraysize".
     * @param valueLabel A custom label for the performance Logging "valuesize".
     * @return The resulting decompressed array.
     */
    int[] decompress(int[] array, String sizeLabel, String valueLabel);

    /**
     * Extracts a single integer value at a specific index from the packed array.
     * * @param index The index of the integer to retrieve.
     * @param array The packed or unpacked array containing the data.
     * @param sizeLabel A custom label for the performance Logging "arraysize".
     * @param valueLabel A custom label for the performance Logging "valuesize".
     * @return The integer value at the specified index.
     */
    int get(int index, int[] array, String sizeLabel, String valueLabel);

    /**
     * Calculates the number of bits needed to represent the largest element in the array,
     * then multiplies that by the array's length (based on original logic).
     * * @param array Array of Integers.
     * @return The total bit capacity calculated.
     */
    default int get_number_of_bits_needed(int[] array) {
        int bits_needed = 0;
        for (int j : array) {
            // Find the maximum number of bits needed for any single integer
            bits_needed = Math.max(bits_needed, 32 - Integer.numberOfLeadingZeros(j));
        }
        // Apply the multiplication based on the original logic
        bits_needed = bits_needed * array.length;
        return bits_needed;
    }

    /**
     * Inserts a sequence of bits from a source integer into a destination integer.
     * * @param dest The target integer where bits will be inserted.
     * @param start_bit_dest The starting bit index in the destination.
     * @param source The integer providing the bits.
     * @param start_bit_source The starting bit index in the source.
     * @param end_bit_source The ending bit index in the source.
     * @return The destination integer with the new bits inserted.
     */
    default int insert_bits_in_result(int dest, int start_bit_dest, int source, int start_bit_source, int end_bit_source) {
        int numBitsToCopy = end_bit_source - start_bit_source + 1;

        if (numBitsToCopy <= 0 || start_bit_source < 0 || start_bit_dest < 0) {
            throw new IllegalArgumentException("Illegal Index");
        }

        long extractionMask = (1L << numBitsToCopy) - 1;
        extractionMask <<= start_bit_source;

        // 1. Extract the bits from the source and shift them to the rightmost position
        int extractedBits = (int) ((source & extractionMask) >>> start_bit_source);

        // 2. Create a mask to clear the destination area
        long clearMask = (1L << numBitsToCopy) - 1;
        clearMask <<= start_bit_dest;

        // 3. Clear the area in the destination
        int clearedTargetInt = dest & (int) ~clearMask;

        // 4. Shift the extracted bits to the target position and combine
        int shiftedBits = extractedBits << start_bit_dest;

        return clearedTargetInt | shiftedBits;
    }

    /**
     * Extracts a sequence of bits from a source integer.
     * * @param sourceInt The integer to extract bits from.
     * @param startIndex The starting bit index (inclusive).
     * @param endIndex The ending bit index (inclusive).
     * @return The extracted bits, shifted to the rightmost position.
     */
    default int extractBits(int sourceInt, int startIndex, int endIndex) {
        if (startIndex < 0 || endIndex > 31 || startIndex > endIndex) {
            throw new IllegalArgumentException("Illegal Index");
        }
        int numBits = endIndex - startIndex + 1;

        // 1. Create a mask for the required bits
        long mask = (1L << numBits) - 1;
        mask <<= startIndex;

        // 2. Extract the bits and shift them to the rightmost position
        int extractedBits = (int)(sourceInt & mask);
        return extractedBits >>> startIndex;
    }
}