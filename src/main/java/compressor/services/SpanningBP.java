package compressor.services;

import compressor.models.BitPacker;

public class SpanningBP implements BitPacker {

    //COMPRESS  function: Input: An Array of Integers Output: An Array of Integers
    //It compresses an array of integers to a smaller Array of Integers using bit manipulation
    public int[] compress(int[] array) {

        //The sum of all bits needed to represent all Integers of the Array
        int bits_needed = get_number_of_bits_needed(array);
        //The size of a chunk of data needed
        int chunk_size = bits_needed / array.length;
        //Size of the new array
        int new_array_size = (int) Math.ceil((10 + bits_needed) / 32.0);
        int[] result = new int[new_array_size];
        //Number of unused bits
        int nbr_unused_bit = (new_array_size * 32) - (10 + bits_needed);


        int result_cursor = 0; //points to the current treated Integer of the array
        int bit_cursor = 0; //points on the current bit
        //Loop that writes the bits onto the new array
        for (int i = 0; i < array.length; i++) {

            //The writing of the chunk size and unused_bits at the beginning of the first Integer for each Compressed Array
            if (i == 0 && bit_cursor == 0) {
                result[result_cursor] |= chunk_size << bit_cursor;
                result[result_cursor] |= nbr_unused_bit << 5;

                bit_cursor = 10;

            }
            //Default case: Writing of the current Integer

            if (bit_cursor + chunk_size > 32) {
                int cut_point = 32 - bit_cursor;
                int mask = (1 << cut_point) - 1;
                int value = (array[i] >> 0) & mask;
                result[result_cursor] |= value << bit_cursor;
                result_cursor++;
                bit_cursor = 0;
                mask = (1 << (chunk_size - cut_point)) - 1;
                value = (array[i] >> cut_point) & mask;
                result[result_cursor] |= value << bit_cursor;
                bit_cursor += chunk_size - cut_point;
            } else if (bit_cursor - chunk_size == 0) {
                result[result_cursor] |= array[i] << bit_cursor;
                bit_cursor = 0;
                result_cursor++;
            } else {
                result[result_cursor] |= array[i] << bit_cursor;
                bit_cursor += chunk_size;
            }

        }


        return result;
    }


    public int[] decompress(int[] array) {
        int mask = (1 << 5) - 1;
        //Extraction of the size of a chunk of data needed
        int chunk_size = array[0] & mask;
        //Extraction of the number of chunks that will stay empty
        int unused_bits = array[0] >> 5 & mask;
        //Array size of the Array which will be returned
        int decompressed_array_size = (array.length * 32) - 10 - unused_bits;
        int[] result = new int[decompressed_array_size];

        int cursor_array = 0;
        int bit_cursor = 10;
        mask = (1 << chunk_size) - 1;
        for (int i = 0; i < decompressed_array_size; i++) {
            //Extraction of the Integer value
            if (32 - bit_cursor < chunk_size) {
                int cut_point = 32 - bit_cursor;
                int temp_value1 = array[cursor_array] >> cut_point;
                int temp_mask = (1 << (chunk_size - cut_point)) - 1;
                cursor_array++;
                int temp_value2 = array[cursor_array] & temp_mask;
                result[i] = temp_value1 << 3;
                result[i] |= temp_value2;
                bit_cursor = chunk_size - cut_point;
            } else if (32 - bit_cursor == 0) {
                bit_cursor = 0;
                cursor_array++;
            } else {
                result[i] = (array[cursor_array] >> bit_cursor) & mask;
                bit_cursor += chunk_size;
            }
        }
        return result;
    }


    public int get(int index, int[] array) {

        int mask = (1 << 5) - 1;
        //Extraction of the size of a chunk of data needed
        int chunk_size = array[0] & mask;
        //Extraction of the number of chunks that will stay empty
        int unused_bits = array[0] >> 5 & mask;
        //Array size to be able to check if the index is out of bounds
        int decompressed_array_size = ((array.length * 32) ) - 10 - unused_bits;

        //Test if the index is out of bounds
        if (index < 0 || index >= decompressed_array_size) {
            System.err.println("index out of bounds");
            return -1;
        }
        int bit_cursor = 10 + chunk_size * (index );
        //Cursor on the Integer in the compressed Array
        int array_idex = bit_cursor / 32;
        //Cursor on the bit in the Integer
        int cursor = (bit_cursor % 32);
        mask = (1 << chunk_size) - 1;
        //Extraction of the value
        return (array[array_idex] >> cursor) & mask;

    }
}
