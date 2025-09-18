package compressor.services;

import compressor.models.BitPacker;

public class NonSpanningBP implements BitPacker {
    
    //COMPRESS  function: Input: An Array of Integers Output: An Array of Integers
    //It compresses an array of integers to a smaller Array of Integers using bit manipulation 
    public int[] compress(int[] array) {
        
        //The sum of all bits needed to represent all Integers of the Array
        int bits_needed = get_number_of_bits_needed(array);
        //The size of a chunk of data needed
        int chunk_size = bits_needed / array.length;
        //Number of how many chunks get in one Integer
        int chunks_per_integer = 32 / chunk_size;
        //SIze of the new array
        int new_array_size = ((array.length + 1) % (chunks_per_integer) == 0) ? ((array.length + 1) / (chunks_per_integer) ): ((array.length + 1) / (chunks_per_integer) + 1);
        int[] result = new int[new_array_size];
        //Number of chunks that will stay empty
        int unused_chunks=(new_array_size*chunks_per_integer)-((int)(Math.ceil(10.0/chunk_size)))-array.length;

        //Part that writes the bits onto the new array
        int array_cursor = 0; //points to the current treated Integer of the array
        for (int i = 0; i < result.length; i++) {
            int bit_cursor = 0; //points on the current bit
            for (int j = 0; j < chunks_per_integer; j++) {
                //The writing of the chunk size and unused_chunks at the beginning of the first Integer for each Compressed Array
                if (array_cursor == 0 && bit_cursor == 0) {
                    result[i] |= chunk_size << bit_cursor;
                    result[i] |= unused_chunks << 5;
                    if(chunk_size<10){
                        bit_cursor = chunk_size*(10/chunk_size);
                    }
                }
                //Default case: Writing of the current Integer
                else {
                    result[i] |= array[array_cursor] << bit_cursor;
                    array_cursor++;
                    if(array_cursor == array.length){
                        break;
                    }
                }
                bit_cursor += chunk_size;

            }
        }
        return result;
    }



    public int[] decompress(int[] array) {
        int mask =(1<<5)-1;
        //Extraction of the size of a chunk of data needed
        int chunk_size = array[0] & mask;
        //Extraction of the number of chunks that will stay empty
        int unused_chunks = array[0]>>5 & mask;
        //Array size of the Array which will be returned
        int decompressed_array_size = ((array.length*32)/chunk_size)-((int)Math.ceil(10.0/chunk_size))-unused_chunks;
        int[] result = new int[decompressed_array_size];

        int cursor_result=0;
        for (int i = 0; i < array.length; i++) {
            int bit_cursor = 0;
            for(int j = 0; j < 32/chunk_size; j++) {

                //At the first Integer jumping the chunks used by our data (chunk size & unused_chunks)
                if (i== 0 && bit_cursor == 0) {
                    bit_cursor=(((int)Math.ceil(10.0/chunk_size))*chunk_size);
                    j=(int)Math.ceil(10/chunk_size);
                }

                mask = (1<<chunk_size) - 1;
                //Extraction of the Integer value
                result[cursor_result] = (array[i] >> bit_cursor) & mask;
                bit_cursor+=chunk_size;
                cursor_result++;
                if(cursor_result == result.length){
                    break;
                }
            }
        }
        return result;
    }



    public int get(int index, int[] array) {

        int mask =(1<<5)-1;
        //Extraction of the size of a chunk of data needed
        int chunk_size = array[0] & mask;
        //Extraction of the number of chunks that will stay empty
        int unused_chunks = array[0]>>5 & mask;
        //Array size to be able to check if the index is out of bounds
        int decompressed_array_size = ((array.length*32)/chunk_size)-((int)Math.ceil(10.0/chunk_size))-unused_chunks;

        //Test if the index is out of bounds
        if(index<0||index>=decompressed_array_size){
            System.err.println("index out of bounds");
            return -1;
        }
        index+=((int)Math.ceil(10.0/chunk_size));
        int chunks_per_integer = 32 / chunk_size;
        //Cursor on the Integer in the compressed Array
        int array_idex=(index/(chunks_per_integer+1));
        //Cursor on the bit in the Integer
        int cursor=(index%(chunks_per_integer+1))*chunk_size;
        mask= (1<<chunk_size) - 1;
        //Extraction of the value
        return (array[array_idex]>>cursor) &mask;

    }


}
