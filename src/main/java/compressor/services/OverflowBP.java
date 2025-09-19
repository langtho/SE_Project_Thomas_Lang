package compressor.services;

import compressor.models.BitPacker;

public class OverflowBP implements BitPacker {
    int[] compress(int[] array);

    int[] decompress(int[] array);

    int get(int index, int[] array);

    int get_ideal_chunksize(int[] array) {
        int bits_needed = 0;
        int[] value_distribution=new int [32];
        for (int i=0; i<array.length; i++) {
            int minimal_bits_needed = 32 - Integer.numberOfLeadingZeros(array[i]);
            value_distribution[minimal_bits_needed - 1]++;
        }

        int values_included = 0;
        int current_smallest_chunk=0;
        int size_for_smallest_chunk=array.length*32;
        for (int i=0; i<32; i++) {
            if(value_distribution[i]!=0) {
                values_included += value_distribution[i];
                int temp_size = (int) (Math.ceil((array.length * i + 10) / 32) + (array.length - values_included) * 32);
                if (temp_size < size_for_smallest_chunk) {
                    size_for_smallest_chunk = temp_size;
                    current_smallest_chunk = i;
                }
            }
        }

        return current_smallest_chunk;
    }
}
