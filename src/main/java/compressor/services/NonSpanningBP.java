package compressor.services;

import compressor.models.BitPacker;

public class NonSpanningBP implements BitPacker {

    public int[] compress(int[] array) {
        //Part that calculates the size of the compressed array
        int bits_needed = get_number_of_bits_needed(array);
        int compressed_int_size = bits_needed / array.length;
        int nbr_int_for_compressed_int = 32 / compressed_int_size;
        int new_array_size = ((array.length + 1) % (nbr_int_for_compressed_int) == 0) ? ((array.length + 1) / (nbr_int_for_compressed_int) ): ((array.length + 1) / (nbr_int_for_compressed_int) + 1);
        int[] result = new int[new_array_size];

        //Part that writes the bits onto the new array
        int array_curser = 0;
        for (int i = 0; i < result.length; i++) {
            int bit_curser = 0;
            for (int j = 0; j < nbr_int_for_compressed_int; j++) {
                if (array_curser == 0 && bit_curser == 0) {
                    result[i] |= compressed_int_size << bit_curser;
                } else {
                    result[i] |= array[array_curser] << bit_curser;
                    array_curser++;
                    if(array_curser == array.length){
                        break;
                    }
                }
                bit_curser += compressed_int_size;

            }
        }
        return result;
    }

    ;

    public int[] decompress(int[] array) {
        int mask =(1<<5)-1;
        int compressed_int_size = array[0] & mask;
        int max_decompressed_int_size = ((array.length*32)/compressed_int_size);
        int[] result = new int[max_decompressed_int_size];
        int curser_result=0;
        for (int i = 0; i < array.length; i++) {
            int bit_curser = 0;
            for(int j = 0; j < 32/compressed_int_size; j++) {
                if (i== 0 && bit_curser == 0) {
                    bit_curser=compressed_int_size;
                    j++;
                }

                mask = (1<<compressed_int_size) - 1;
                result[curser_result] = (array[i] >> bit_curser) & mask;
                bit_curser+=compressed_int_size;
                curser_result++;
            }
        }
        return result;
    }

    ;

    public int get(int index, int[] array) {
        index++;
        int mask =(1<<5)-1;
        int compressed_int_size = array[0] & mask;
        int nbr_int_for_compressed_int = 32 / compressed_int_size;
        int array_idex=(index/nbr_int_for_compressed_int);
        int curser=(index%nbr_int_for_compressed_int)*compressed_int_size;
        mask= (1<<compressed_int_size) - 1;
        return (array[array_idex]>>curser) &mask;

    }

    ;
}
