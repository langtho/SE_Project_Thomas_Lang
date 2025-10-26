package compressor.services;

import compressor.logger.Logger;
import compressor.models.BitPacker;
import compressor.timetaking.PerformanceTimer;
import compressor.logger.LogLevel; // Assuming LogLevel is available
import org.javatuples.Triplet; // Assuming you are using the javatuples library

import java.io.File;

public class OverflowBP implements BitPacker {

    private PerformanceTimer timer;
    private Logger logger;

    /**
     * Constructor for the Overflow Bit Packer (BP).
     * Initializes the logger and conditionally sets up the performance timer.
     * @param filePath File path for performance logging (null if not needed).
     * @param log The Logger instance (Dependency Injection).
     */
    public OverflowBP(File filePath, Logger log) {
        this.logger = log;
        // Initialization of timer
        if (filePath != null) {
            this.timer = new PerformanceTimer(filePath, "Overflow");
            this.logger.log(LogLevel.INFO, "PerformanceTimer initialized for OverflowBP.");
        } else {
            this.logger.log(LogLevel.DEBUG, "PerformanceTimer disabled (filePath is null).");
        }
    }

    // --- Core BitPacker Interface Methods ---

    /**
     * Compresses an array of integers using the Overflow strategy.
     * This strategy reserves a separate area for large 'overflow' values.
     */
    @Override
    public int[] compress(int[] array, String sizeLabel, String valueLabel) {

        // Start of timetaking
        if (timer != null) {
            timer.start();
        }
        this.logger.log(LogLevel.DEBUG, "Starting compress operation.");

        if (array.length == 0) return new int[0];

        // Determine the optimal chunk size and associated metadata
        Triplet<Integer, Integer, Integer> triplet = get_ideal_chunksize(array);

        // Take time of the bit-needed function (IdealChunkSize calculation)
        if (timer != null) {
            timer.stop("IdealChunkSize");
        }
        this.logger.log(LogLevel.DEBUG, "Ideal chunk size determined: " + triplet.getValue0());

        // The size of a chunk of data needed
        int chunk_size = triplet.getValue0();
        // Size of the Overflow space (number of overflowed items)
        int overflow_size = triplet.getValue2();

        // Encode overflow size for metadata storage (Elias Gamma)
        String encoded_overflow_size = encodeEliasGamma(overflow_size + 1);

        // Calculate the size of the new compressed array (in bits, then converted to array size)
        int new_array_size = (triplet.getValue1()) / 32 + overflow_size;

        // Number of unused bits at the end of the compressed array (before the overflow area)
        int unused_bits = 32 - ((((chunk_size + 1) * array.length) + 10 + encoded_overflow_size.length()) % 32);
        int[] result = new int[new_array_size];

        // Take time of the setup phase
        if (timer != null) {
            timer.stop("Setup");
        }
        this.logger.log(LogLevel.DEBUG, "Setup complete. New array size: " + new_array_size);

        int result_cursor = 0; // Points to the current integer in the result array
        int bit_cursor = 0; // Points to the current bit in the result integer
        int overflow_counter = 0;


        // Loop that writes the bits onto the new array
        for (int i = 0; i < array.length; i++) {

            // Writing the metadata (chunk size, unused_bits, and encoded overflow size) into the first integer
            if (i == 0) {
                // Write chunk_size (bits 0-4) and unused_bits (bits 5-9)
                result[result_cursor] = insert_bits_in_result(0, 0, chunk_size, 0, 4);
                result[result_cursor] = insert_bits_in_result(result[result_cursor], 5, unused_bits, 0, 4);

                // Write the encoded overflow size (Elias Gamma)
                int overflow_value = Integer.parseInt(encoded_overflow_size, 2);
                int numBitsToInsert = encoded_overflow_size.length();

                if (numBitsToInsert > 22) { // Logic for writing overflow size that spans two integers
                    result[result_cursor] = insert_bits_in_result(result[result_cursor], 10, overflow_value, 0, 21);
                    result_cursor++;
                    result[result_cursor] = insert_bits_in_result(0, 0, overflow_value, 22, numBitsToInsert);
                    bit_cursor = numBitsToInsert - 22;
                } else { // Logic for writing overflow size within the first integer
                    result[result_cursor] = insert_bits_in_result(result[result_cursor], 10, overflow_value, 0, numBitsToInsert);
                    bit_cursor = 10 + numBitsToInsert;
                }
            }

            // Check if the current value is an overflow value
            if (chunk_size < (32 - Integer.numberOfLeadingZeros(array[i]))) {
                // Case: Overflow value -> Write '1' marker bit, store value in overflow area
                result[result_cursor] |= 1 << bit_cursor; // Set marker bit to 1
                bit_cursor++;
                if (bit_cursor == 32) {
                    bit_cursor = 0;
                    result_cursor++;
                }

                // Store original value in the overflow section (from the end of the result array)
                result[result.length - overflow_counter - 1] = array[i];

                // Store the index of the overflowed value in the main data stream
                array[i] = overflow_counter;
                overflow_counter++;
            } else {
                // Case: Normal value -> Write '0' marker bit
                result[result_cursor] |= 0; // Set marker bit to 0
                bit_cursor++;
                if (bit_cursor == 32) {
                    bit_cursor = 0;
                    result_cursor++;
                }
            }

            // Write the current integer (either the original value or the overflow index), handling spanning
            if (bit_cursor + chunk_size > 32) {
                // Spanning across two integers
                result[result_cursor] = insert_bits_in_result(result[result_cursor], bit_cursor, array[i], 0, 31 - bit_cursor);
                result_cursor++;
                result[result_cursor] = insert_bits_in_result(0, 0, array[i], 32 - bit_cursor, chunk_size);
                bit_cursor = chunk_size - (32 - bit_cursor);

            } else if (32 - bit_cursor == chunk_size) {
                // Fills the rest of the current integer exactly
                result[result_cursor] |= array[i] << bit_cursor;
                bit_cursor = 0;
                result_cursor++;

            } else {
                // Fits entirely within the current integer
                result[result_cursor] |= array[i] << bit_cursor;
                bit_cursor += chunk_size;
            }

        }

        // Stop of timetaking of writing on the compressed array
        if (timer != null) {
            timer.stop("Compressing");
            timer.saveToJson("Compress", array.length, result.length, sizeLabel, valueLabel);
        }
        this.logger.log(LogLevel.INFO, "Compression completed. Input size: " + array.length + ", Result size: " + result.length);

        return result;
    }

    /**
     * Decompresses the overflow-packed array back into the original integer array.
     */
    @Override
    public int[] decompress(int[] array, String sizeLabel, String valueLabel) {

        // Start of timetaking
        if (timer != null) {
            timer.start();
        }
        this.logger.log(LogLevel.DEBUG, "Starting decompress operation.");

        if (array.length == 0) return new int[0];

        // 1. Extract metadata
        int chunk_size = extractBits(array[0], 0, 4);
        // Extraction of the number of unused bits
        int unused_bits = extractBits(array[0], 5, 9);

        // Decode Elias Gamma to get overflow size
        String overflow_string = decodeEliasGamma(array);
        int overflow_size = Integer.parseInt(overflow_string, 2) - 1;
        int overflow_encoded_length = (overflow_string.length() - 1) * 2 + 1;

        // Calculate the original array length
        int array_length = ((((array.length - overflow_size) * 32) - (10 + unused_bits + overflow_encoded_length)) / (chunk_size + 1));
        int[] result = new int[array_length];

        // Stop of Setup time taking
        if (timer != null) { // CRITICAL FIX: Ensure timer is checked before stopping
            timer.stop("Setup");
        }
        this.logger.log(LogLevel.DEBUG, "Decompression setup complete. Expected result size: " + array_length);

        int cursor_array = 0;

        // Calculate starting bit position after metadata
        int bit_cursor = 10 + ((32 - Integer.numberOfLeadingZeros(overflow_size + 1)) - 1) * 2 + 1;
        if (bit_cursor >= 32) {
            bit_cursor = bit_cursor % 32;
            cursor_array++;
        }

        for (int i = 0; i < array_length; i++) {

            // Adjust cursor for integer boundary
            if (32 - bit_cursor == 0) {
                bit_cursor = 0;
                cursor_array++;
            }

            // Check the marker bit (0 for normal value, 1 for overflow index)
            if (readBit(array, bit_cursor, cursor_array) == 0) {
                // Case: Normal value (0 marker)
                bit_cursor++;
                if (32 - bit_cursor == 0) {
                    bit_cursor = 0;
                    cursor_array++;
                }

                // Extract the value, handling spanning across integers
                if (32 - bit_cursor < chunk_size) {
                    result[i] = insert_bits_in_result(0, 0, array[cursor_array], bit_cursor, bit_cursor + 32 - bit_cursor);
                    cursor_array++;
                    result[i] = insert_bits_in_result(result[i], 32 - bit_cursor, array[cursor_array], 0, chunk_size - (33 - bit_cursor));
                    bit_cursor = chunk_size - (32 - bit_cursor);
                } else if (32 - bit_cursor == 0) {
                    bit_cursor = 0;
                    cursor_array++;
                    result[i] = extractBits(array[cursor_array], bit_cursor, bit_cursor + chunk_size - 1);
                    bit_cursor = chunk_size;
                } else {
                    result[i] = extractBits(array[cursor_array], bit_cursor, bit_cursor + chunk_size - 1);
                    bit_cursor += chunk_size;
                }
            } else {
                // Case: Overflow value (1 marker)
                bit_cursor++;
                if (32 - bit_cursor == 0) {
                    bit_cursor = 0;
                    cursor_array++;
                }

                int overflow_index;
                // Extraction of the overflow index, handling spanning
                if (32 - bit_cursor < chunk_size) {
                    overflow_index = insert_bits_in_result(0, 0, array[cursor_array], bit_cursor, 31);
                    cursor_array++;
                    overflow_index = insert_bits_in_result(overflow_index, 32 - bit_cursor, array[cursor_array], 0, chunk_size - (33 - bit_cursor));
                    bit_cursor = chunk_size - (32 - bit_cursor);
                } else if (32 - bit_cursor == 0) {
                    bit_cursor = 0;
                    cursor_array++;
                    overflow_index = extractBits(0, bit_cursor, bit_cursor + chunk_size - 1);
                    bit_cursor += chunk_size;
                } else {
                    overflow_index = extractBits(array[cursor_array], bit_cursor, bit_cursor + chunk_size - 1);
                    bit_cursor += chunk_size;
                }

                // Retrieve the original value from the overflow area
                if (overflow_index >= 0 && overflow_size != 0 && overflow_index < array.length) {
                    overflow_index = array.length - overflow_index - 1;
                    result[i] = array[overflow_index];
                }
            }
        }

        // Stop of timetaking
        if (timer != null) {
            timer.stop("Decompressing");
            timer.saveToJson("Decompress", result.length, array.length, sizeLabel, valueLabel);
        }
        this.logger.log(LogLevel.INFO, "Decompression finished. Result size: " + result.length);

        return result;
    }

    /**
     * Retrieves a single integer value at a specific logical index from the compressed array.
     */
    @Override
    public int get(int index, int[] array, String sizeLabel, String valueLabel) {

        // Start of timetaking
        if (timer != null) {
            timer.start();
        }
        this.logger.log(LogLevel.DEBUG, "Starting get operation for index: " + index);

        if (array.length == 0) return 0;

        // 1. Extract metadata
        int chunk_size = extractBits(array[0], 0, 4);
        int unused_bits = extractBits(array[0], 5, 9);

        // Decode Elias Gamma to get overflow size
        String overflow_string = decodeEliasGamma(array);
        int overflow_size = Integer.parseInt(overflow_string, 2) - 1;

        // Calculate the original array length for bounds check
        int array_length = ((((array.length - overflow_size) * 32) - 10 - unused_bits) / chunk_size);

        // Test if the index is out of bounds
        if (index < 0 || index >= array_length) {
            this.logger.log(LogLevel.WARNING, "Index " + index + " is out of bounds (Max: " + (array_length - 1) + ")");
            System.err.println("index out of bounds");
            return -1;
        }

        // Calculate bit position of the required chunk
        int bit_cursor = (10 + (31 - Integer.numberOfLeadingZeros(overflow_size + 1)) * 2 + 1) + (chunk_size + 1) * index;
        int cursor_array = bit_cursor / 32;
        bit_cursor = bit_cursor % 32;
        int result;

        // Read marker bit
        if (readBit(array, bit_cursor, cursor_array) == 0) {
            // Case: Normal value (0 marker)
            bit_cursor++;
            // Extraction of the Integer value, handling spanning
            if (32 - bit_cursor < chunk_size) {
                result = insert_bits_in_result(0, 0, array[cursor_array], bit_cursor, bit_cursor + 32 - bit_cursor);
                cursor_array++;
                result = insert_bits_in_result(result, 32 - bit_cursor, array[cursor_array], 0, chunk_size - (33 - bit_cursor));
            } else if (32 - bit_cursor == 0) {
                bit_cursor = 0;
                cursor_array++;
                result = extractBits(array[cursor_array], bit_cursor, bit_cursor + chunk_size - 1);
            } else {
                result = extractBits(array[cursor_array], bit_cursor, bit_cursor + chunk_size - 1);
            }
        } else {
            // Case: Overflow value (1 marker)
            bit_cursor++;
            int overflow_index;
            // Extraction of the overflow index, handling spanning
            if (32 - bit_cursor < chunk_size) {
                overflow_index = insert_bits_in_result(0, 0, array[cursor_array], bit_cursor, bit_cursor + 32 - bit_cursor);
                cursor_array++;
                overflow_index = insert_bits_in_result(overflow_index, 32 - bit_cursor, array[cursor_array], 0, chunk_size - (33 - bit_cursor));
            } else if (32 - bit_cursor == 0) {
                bit_cursor = 0;
                cursor_array++;
                overflow_index = extractBits(array[cursor_array], bit_cursor, bit_cursor + chunk_size - 1);
            } else {
                overflow_index = extractBits(array[cursor_array], bit_cursor, bit_cursor + chunk_size - 1);
            }
            // Retrieve original value from the overflow area
            overflow_index = array.length - overflow_index - 1;
            result = array[overflow_index];
        }

        // Stop timetaking
        if (timer != null) { // CRITICAL FIX: Ensure timer is checked before stopping
            timer.stop("get");
            timer.saveToJson("get", array_length, array.length, sizeLabel, valueLabel);
        }
        this.logger.log(LogLevel.INFO, "Get operation successful. Retrieved value: " + result);

        return result;
    }

    // --- Utility Methods (Provided by User) ---

    /**
     * Calculates the ideal chunk size for the Overflow strategy based on the distribution of values.
     */
    public Triplet<Integer,Integer,Integer> get_ideal_chunksize(int[] array) {
        int[] value_distribution = new int[32];
        for (int j : array) {
            int minimal_bits_needed = 32 - Integer.numberOfLeadingZeros(j);
            if (minimal_bits_needed == 0) {
                minimal_bits_needed = 1;
            }
            value_distribution[minimal_bits_needed - 1]++;
        }

        int values_included = 0;
        int current_smallest_chunk = 0;
        int overflow_size = 0;
        int size_for_smallest_chunk = array.length * 32 + 33;
        for (int i = 0; i < 32; i++) {

            if (value_distribution[i] != 0) {
                values_included += value_distribution[i];
                int temp_overflow_size = array.length - values_included;
                temp_overflow_size++;
                int elias_gamma_overhead = (31 - Integer.numberOfLeadingZeros(temp_overflow_size)) * 2 + 1;
                int packed_data_bits = array.length * (i + 2);
                int overflow_data_bits = (temp_overflow_size - 1) * 32;
                int metadata_bits = 10;

                int temp_size_bits = packed_data_bits + elias_gamma_overhead + overflow_data_bits + metadata_bits;
                int temp_size = (int) Math.ceil(temp_size_bits / 32.0) * 32;
                if (temp_size <= size_for_smallest_chunk || current_smallest_chunk == 0) {
                    if (32 - Integer.numberOfLeadingZeros(temp_overflow_size) <= i + 1) {
                        size_for_smallest_chunk = temp_size;
                        current_smallest_chunk = i + 1;
                        overflow_size = temp_overflow_size - 1;
                    }
                }
            }
        }
        return new Triplet<>(current_smallest_chunk, size_for_smallest_chunk, overflow_size);
    }

    /**
     * Encodes an integer value using the Elias Gamma encoding scheme.
     */
    public static String encodeEliasGamma(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Elias-Gamma is for positive integers.");
        }

        String binaryValue = Integer.toBinaryString(value);
        int length = binaryValue.length() - 1;

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < length; i++) {
            prefix.append('0');
        }

        return prefix.append(binaryValue).reverse().toString();
    }

    /**
     * Decodes the Elias Gamma encoded overflow size metadata from the compressed array.
     */
    public static String decodeEliasGamma(int[] array) {
        int k = 0;
        int bit_cursor = 10;
        int array_index = 0;

        // Find the length of the prefix (number of leading zeros, 'k')
        while (readBit(array, bit_cursor, array_index) == 0) {
            k++;
            bit_cursor++;
            if (bit_cursor >= 32) {
                bit_cursor = 0;
                array_index++;
            }
        }

        StringBuilder sb = new StringBuilder();

        // Read the k+1 bits that represent the value
        for (int i = 0; i <= k; i++) {
            int bit = readBit(array, bit_cursor, array_index);
            sb.append(bit);

            bit_cursor++;
            if (bit_cursor >= 32) {
                bit_cursor = 0;
                bit_cursor++; // Original logic has double increment, kept for compatibility
            }
        }

        return sb.toString();
    }

    /**
     * Reads a single bit from the array at a specific bit position.
     */
    private static int readBit(int[] data, int bitCursor, int arrayIndex) {
        int currentInt = data[arrayIndex];
        return (currentInt >>> bitCursor) & 1;
    }


}