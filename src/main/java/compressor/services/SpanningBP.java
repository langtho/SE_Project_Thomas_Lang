package compressor.services;

import compressor.logger.Logger;
import compressor.models.BitPacker;
import compressor.timetaking.PerformanceTimer;
import compressor.logger.LogLevel; // Assuming LogLevel is available

import java.io.File;

public class SpanningBP implements BitPacker {

    public PerformanceTimer timer = null;
    private Logger logger;

    /**
     * Constructor for the Spanning Bit Packer (BP).
     * Initializes the logger and conditionally sets up the performance timer.
     * @param filePath File path for performance logging (null if not needed).
     * @param log The Logger instance (Dependency Injection).
     */
    public SpanningBP(File filePath, Logger log) {
        this.logger = log;
        // Initialization of timer
        if (filePath != null) {
            this.timer = new PerformanceTimer(filePath, "Spanning");
            this.logger.log(LogLevel.INFO, "PerformanceTimer initialized for SpanningBP.");
        } else {
            this.logger.log(LogLevel.DEBUG, "PerformanceTimer disabled (filePath is null).");
        }
    }

    // --- Core BitPacker Interface Methods ---

    /**
     * Compresses an array of integers using a spanning fixed-chunk-size bit manipulation strategy.
     */
    @Override
    public int[] compress(int[] array, String sizeLabel, String valueLabel) {

        // Start of timetaking
        if (timer != null) timer.start();
        this.logger.log(LogLevel.DEBUG, "Starting compress operation.");

        if (array.length == 0) return new int[0];

        // Calculate the total bits needed to represent all integers in the array
        int bits_needed = get_number_of_bits_needed(array);

        // Take time of the bit-needed function
        if (timer != null) timer.stop("BitNeeded");

        // The size of a chunk of data needed (assuming fixed size across the array)
        int chunk_size = bits_needed / array.length;
        if (chunk_size == 0) {
            chunk_size = 1;
            bits_needed = array.length;
        }

        // Calculate the required size of the new array, including space for metadata
        int new_array_size = (int) Math.ceil((10 + bits_needed) / 32.0);
        int[] result = new int[new_array_size];

        // Number of unused bits (padding at the end of the packed data)
        int nbr_unused_bit = (new_array_size * 32) - (10 + bits_needed);

        // Take time of the setup phase
        if (timer != null) timer.stop("Setup");
        this.logger.log(LogLevel.DEBUG, "Compression setup complete. Chunk size: " + chunk_size + ", New size: " + new_array_size);

        int result_cursor = 0; // Index of the current integer in the result array
        int bit_cursor = 0; // Index of the current bit within result[result_cursor]

        // Loop that writes the bits onto the new array
        for (int i = 0; i < array.length; i++) {

            // Writing the chunk size and unused_bits at the beginning of the first Integer
            if (i == 0) {
                // Write chunk_size (bits 0-4) and nbr_unused_bit (bits 5-9)
                result[result_cursor] = insert_bits_in_result(0, bit_cursor, chunk_size, 0, 4);
                result[result_cursor] = insert_bits_in_result(result[result_cursor], 5, nbr_unused_bit, 0, 4);
                bit_cursor = 10; // Metadata is 10 bits long
            }

            // Core packing logic: Spanning across integer boundaries
            if (bit_cursor + chunk_size > 32) {
                // Case 1: Spanning across two integers
                int first_part = 32 - bit_cursor;

                // Write the first part into the current integer
                result[result_cursor] = insert_bits_in_result(result[result_cursor], bit_cursor, array[i], 0, first_part - 1);

                result_cursor++; // Move to the next integer

                // Write the second part into the start of the next integer
                result[result_cursor] = insert_bits_in_result(0, 0, array[i], first_part, chunk_size - 1);
                bit_cursor = chunk_size - first_part; // Update cursor position in the new integer

            } else if (32 - bit_cursor == chunk_size) {
                // Case 2: Fills the remainder of the current integer exactly
                result[result_cursor] = insert_bits_in_result(result[result_cursor], bit_cursor, array[i], 0, chunk_size - 1);
                bit_cursor = 0;
                result_cursor++; // Move to the next integer

            } else {
                // Case 3: Fits entirely within the current integer
                result[result_cursor] = insert_bits_in_result(result[result_cursor], bit_cursor, array[i], 0, chunk_size - 1);
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
     * Decompresses the spanning-packed array back into the original integer array.
     */
    @Override
    public int[] decompress(int[] array, String sizeLabel, String valueLabel) {

        // Start of timetaking
        if (timer != null) timer.start();
        this.logger.log(LogLevel.DEBUG, "Starting decompress operation.");

        if (array.length == 0) return new int[0];

        // 1. Extract metadata from the first integer
        int chunk_size = extractBits(array[0], 0, 4);
        int unused_bits = extractBits(array[0], 5, 9);

        // Calculate the expected size of the decompressed array
        int decompressed_array_size = ((array.length * 32) - 10 - unused_bits) / chunk_size;
        int[] result = new int[decompressed_array_size];

        // Stop of Setup time taking
        if (timer != null) timer.stop("Setup");
        this.logger.log(LogLevel.DEBUG, "Decompression setup complete. Expected result size: " + decompressed_array_size);

        int cursor_array = 0; // Current integer index in the compressed array
        int bit_cursor = 10; // Start reading data after the 10 bits of metadata

        for (int i = 0; i < decompressed_array_size; i++) {

            // Core unpacking logic: Spanning across integer boundaries
            if (32 - bit_cursor < chunk_size && 32 - bit_cursor != 0) {
                // Case 1: Spanning across two integers

                // Extract the first part from the current integer
                result[i] = insert_bits_in_result(0, 0, array[cursor_array], bit_cursor, 31);
                cursor_array++; // Move to the next integer

                // Extract the second part from the new integer and combine
                result[i] = insert_bits_in_result(result[i], 32 - bit_cursor, array[cursor_array], 0, chunk_size - (33 - bit_cursor));
                bit_cursor = chunk_size - (32 - bit_cursor); // Update cursor position

            } else if (32 - bit_cursor == 0) {
                // Case 2: Cursor is exactly at the start of a new integer boundary (e.g., bit_cursor=32)
                bit_cursor = 0;
                cursor_array++;
                i--; // Decrement i to re-read the chunk that starts at the new integer boundary

            } else {
                // Case 3: Fits entirely within the current integer
                result[i] = extractBits(array[cursor_array], bit_cursor, bit_cursor + chunk_size - 1);
                bit_cursor += chunk_size;
            }
        }

        // Stop of timetaking of writing on the decompressed array
        if (timer != null) {
            timer.stop("Decompressing");
            timer.saveToJson("Decompress", result.length, array.length, sizeLabel, valueLabel);
        }
        this.logger.log(LogLevel.INFO, "Decompression finished. Result size: " + result.length);

        return result;
    }

    /**
     * Retrieves a single integer value at a specific logical index from the compressed array
     * without fully decompressing the entire array.
     */
    @Override
    public int get(int index, int[] array, String sizeLabel, String valueLabel) {

        // Start of timetaking
        if (timer != null) timer.start();
        this.logger.log(LogLevel.DEBUG, "Starting get operation for index: " + index);

        // 1. Extract metadata
        int chunk_size = extractBits(array[0], 0, 4);
        int unused_bits = extractBits(array[0], 5, 9);

        // Calculate total logical array size for bounds check
        int decompressed_array_size = ((array.length * 32)) - 10 - unused_bits;

        // 2. Test if the index is out of bounds
        if (index < 0 || index >= decompressed_array_size) {
            this.logger.log(LogLevel.WARNING, "Index " + index + " is out of bounds.");
            System.err.println("index out of bounds");
            return -1;
        }

        // 3. Calculate position of the chunk
        int bit_cursor = 10 + chunk_size * index; // Total bit offset from the start of the data

        // Calculate array index and bit position
        int array_index = bit_cursor / 32;
        int cursor = (bit_cursor % 32);
        int result;

        // 4. Core extraction logic (handling spanning across integers)
        if (32 - cursor < chunk_size) {
            // Case 1: Spanning across two integers

            // Extract the first part from the current integer
            result = insert_bits_in_result(0, 0, array[array_index], cursor, 31);
            array_index++;

            // Extract the second part from the next integer and combine
            result = insert_bits_in_result(result, 32 - cursor, array[array_index], 0, chunk_size - (33 - cursor));
        } else {
            // Case 2: Fits entirely within the current integer
            result = extractBits(array[array_index], cursor, cursor + chunk_size - 1);
        }

        // Stop timetaking
        if (timer != null) {
            timer.stop("get");
            timer.saveToJson("get", decompressed_array_size, array.length, sizeLabel, valueLabel);
        }
        this.logger.log(LogLevel.INFO, "Get operation successful. Retrieved value: " + result);

        return result;
    }
}