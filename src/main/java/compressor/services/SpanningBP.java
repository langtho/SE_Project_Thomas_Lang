package compressor.services;

import compressor.models.BitPacker;

public class SpanningBP implements BitPacker {

    public PerformanceTimer timer=null;

    public SpanningBP(String filePath) {
        //Initialisation of timer
        if(filePath!=null) this.timer = new PerformanceTimer(filePath,"Spanning");
    }

    //COMPRESS  function: Input: An Array of Integers Output: An Array of Integers
    //It compresses an array of integers to a smaller Array of Integers using bit manipulation
    public int[] compress(int[] array) {

        //Start of timetaking
        if(timer!=null)timer.start();

        if(array.length == 0) return new int[0];
        //The sum of all bits needed to represent all Integers of the Array
        int bits_needed = get_number_of_bits_needed(array);

        //Take time of the bit-needed function
        if(timer!=null)timer.stop("BitNeeded");

        //The size of a chunk of data needed
        int chunk_size = bits_needed / array.length;
        if(chunk_size==0){chunk_size=1;bits_needed=array.length;}
        //Size of the new array
        int new_array_size = (int) Math.ceil((10 + bits_needed) / 32.0);
        int[] result = new int[new_array_size];
        //Number of unused bits
        int nbr_unused_bit = (new_array_size * 32) - (10 + bits_needed);

        //Take time of the bit-needed function
        if(timer!=null)timer.stop("Setup");

        int result_cursor = 0; //points to the current treated Integer of the array
        int bit_cursor = 0; //points on the current bit
        //Loop that writes the bits onto the new array
        for (int i = 0; i < array.length; i++) {
            //The writing of the chunk size and unused_bits at the beginning of the first Integer for each Compressed Array
            if (i == 0) {
                result[result_cursor] = insert_bits_in_result(0,bit_cursor,chunk_size,0,4);
                result[result_cursor] = insert_bits_in_result(result[result_cursor],5,nbr_unused_bit,0,4);
                bit_cursor = 10;

            }
            //Default case: Writing of the current Integer
            if (bit_cursor + chunk_size > 32) {
                int first_part=32-bit_cursor;
                result[result_cursor] =insert_bits_in_result(result[result_cursor],bit_cursor,array[i],0,first_part-1);
                result_cursor++;
                result[result_cursor]= insert_bits_in_result(0,0,array[i],first_part,chunk_size-1);
                bit_cursor=chunk_size-first_part;
            } else if (32-bit_cursor == chunk_size ) {
                result[result_cursor] = insert_bits_in_result(result[result_cursor],bit_cursor,array[i],0,chunk_size-1);
                bit_cursor = 0;
                result_cursor++;
            } else {
                result[result_cursor]= insert_bits_in_result(result[result_cursor],bit_cursor,array[i],0,chunk_size-1);
                bit_cursor += chunk_size;
            }

        }

        //Stop of timetaking of writing on the compressed array
        if(timer!=null){
            timer.stop("Compressing");
            timer.saveToJson("Compress",array.length,result.length);
        }

        return result;
    }


    public int[] decompress(int[] array) {

        //Start of timetaking
        if(timer!=null)timer.start();

        if(array.length == 0) return new int[0];
        //Extraction of the size of a chunk of data needed
        int chunk_size = extractBits(array[0],0,4);
        //Extraction of the number of chunks that will stay empty
        int unused_bits =extractBits(array[0] ,5 ,9);
        //Array size of the Array which will be returned
        int decompressed_array_size = ((array.length * 32) - 10 - unused_bits)/chunk_size;
        int[] result = new int[decompressed_array_size];

        //Stop of Setup time taking
        timer.stop("Setup");

        int cursor_array = 0;
        int bit_cursor = 10;

        for (int i = 0; i < decompressed_array_size; i++) {
            //Extraction of the Integer value
            if (32 - bit_cursor < chunk_size) {
                result[i]=insert_bits_in_result(0,0,array[cursor_array],bit_cursor,bit_cursor+31-bit_cursor);
                cursor_array++;
                result[i] = insert_bits_in_result(result[i],32-bit_cursor,array[cursor_array],0,chunk_size-(33-bit_cursor)) ;
                bit_cursor = chunk_size-(32-bit_cursor);
            } else if (32 - bit_cursor == 0) {
                bit_cursor = 0;
                cursor_array++;
            } else {
                result[i] =extractBits (array[cursor_array] , bit_cursor,bit_cursor+chunk_size-1);
                bit_cursor += chunk_size;
            }
        }

        //Stop of timetaking of writing on the decompressed array
        if(timer!=null){
            timer.stop("Decompressing");
            timer.saveToJson("Decompress",result.length,array.length);
        }

        return result;
    }


    public int get(int index, int[] array) {

        //Start of timetaking
        if(timer!=null)timer.start();

        //Extraction of the size of a chunk of data needed
        int chunk_size = extractBits(array[0],0,4);
        //Extraction of the number of chunks that will stay empty
        int unused_bits = extractBits(array[0],5,9);
        //Array size to be able to check if the index is out of bounds
        int decompressed_array_size = ((array.length * 32) ) - 10 - unused_bits;

        //Test if the index is out of bounds
        if (index < 0 || index >= decompressed_array_size) {
            System.err.println("index out of bounds");
            return -1;
        }
        int bit_cursor = 10 + chunk_size * (index );
        //Cursor on the Integer in the compressed Array
        int array_index = bit_cursor / 32;
        //Cursor on the bit in the Integer
        int cursor = (bit_cursor % 32);
        int result;
        if(32-cursor<chunk_size) {
            result=insert_bits_in_result(0,0,array[array_index],cursor,31);
            array_index++;
            result = insert_bits_in_result(result,32-cursor,array[array_index],0,chunk_size-(33-cursor)) ;
        }else{
            //Extraction of the value
            result= extractBits(array[array_index] , cursor,cursor+chunk_size-1) ;
        }

        //Stop timetaking
        if(timer!=null){
            timer.stop("get");
            timer.saveToJson("get",decompressed_array_size,array.length);

        }

        return result;
    }
}
