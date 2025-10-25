package compressor.services;

import compressor.models.BitPacker;

public class NonSpanningBP implements BitPacker {
    public PerformanceTimer timer=null;

    public NonSpanningBP(String filePath) {
        //Initialisation of timer
        if(filePath!=null) this.timer = new PerformanceTimer(filePath,"NonSpanning");
    }

    //COMPRESS  function: Input: An Array of Integers Output: An Array of Integers
    //It compresses an array of integers to a smaller Array of Integers using bit manipulation 
    public int[] compress(int[] array, String sizeLabel,String valueLabel) {
        //Start of timetaking
        if(timer!=null)timer.start();

        if(array.length == 0) return new int[0];
        //The sum of all bits needed to represent all Integers of the Array
        int bits_needed = get_number_of_bits_needed(array);

        //Take time of the bit-needed function
        if(timer!=null)timer.stop("BitNeeded");

        //The size of a chunk of data needed
        int chunk_size = bits_needed / array.length;
        if (chunk_size==0){chunk_size=1;}
        //Number of how many chunks get in one Integer
        int chunks_per_integer = 32 / chunk_size;
        //Number of chunks needed for metadata
        int chunks_for_metadata=(int) Math.ceil((10.0/chunk_size));
        //SIze of the new array
        int new_array_size = ((array.length+ chunks_for_metadata) % (chunks_per_integer) == 0) ? ((array.length + chunks_for_metadata) / (chunks_per_integer) ): ((array.length + chunks_for_metadata) / (chunks_per_integer) + 1);
        int[] result = new int[new_array_size];
        //Number of chunks that will stay empty
        int unused_chunks=(new_array_size*chunks_per_integer)-((int)(Math.ceil(10.0/chunk_size)))-array.length;

        //Take time of the bit-needed function
        if(timer!=null)timer.stop("Setup");

        //Part that writes the bits onto the new array
        int array_cursor = 0; //points to the current treated Integer of the array
        for (int i = 0; i < result.length; i++) {
            int bit_cursor = 0; //points on the current bit
            for (int j = 0; j < chunks_per_integer; j++) {
                //The writing of the chunk size and unused_chunks at the beginning of the first Integer for each Compressed Array
                if (i == 0 && bit_cursor == 0) {
                    result[i] =insert_bits_in_result(0,bit_cursor,chunk_size,0,4);
                    result[i] =insert_bits_in_result(result[i],5,unused_chunks,0,4);
                    if(chunk_size<10){
                        bit_cursor =(10%chunk_size==0)?chunk_size*(10/chunk_size)-1 : chunk_size*(10/chunk_size);
                        j=(bit_cursor/chunk_size);
                    }

                }
                //Default case: Writing of the current Integer
                else {
                    result[i] = insert_bits_in_result(result[i],bit_cursor,array[array_cursor],0,chunk_size-1);
                    array_cursor++;
                    if(array_cursor == array.length){
                        break;
                    }
                }
                bit_cursor += chunk_size;

            }
        }

        //Stop of timetaking of writing on the compressed array
        if(timer!=null){
            timer.stop("Compressing");
            timer.saveToJson("Compress",array.length,result.length, sizeLabel,valueLabel);
        }

        return result;
    }



    public int[] decompress(int[] array, String sizeLabel,String valueLabel) {

        //Start of timetaking
        if(timer!=null)timer.start();

        if(array.length == 0) return new int[0];
        //Extraction of the size of a chunk of data needed
        int chunk_size =extractBits(array[0],0,4);
        //Extraction of the number of chunks that will stay empty
        int unused_chunks =extractBits(array[0],5,9);
        //Array size of the Array which will be returned
        int decompressed_array_size = ((array.length)*(32/chunk_size))-((int)Math.ceil(10.0/chunk_size))-unused_chunks;
        int[] result = new int[decompressed_array_size];

        //Stop of Setup time taking
        timer.stop("Setup");

        int cursor_result=0;
        for (int i = 0; i < array.length; i++) {
            if(chunk_size>16&&i==0){i++;}
            int bit_cursor = 0;
            for(int j = 0; j < 32/chunk_size; j++) {

                //At the first Integer jumping the chunks used by our data (chunk size & unused_chunks)
                if (i== 0 && bit_cursor == 0) {
                    bit_cursor=(((int)Math.ceil(10.0/chunk_size))*chunk_size);
                    j=(int)Math.ceil(10.0/chunk_size);
                }

                //Extraction of the Integer value
                result[cursor_result] = extractBits(array[i],bit_cursor,bit_cursor+chunk_size-1);
                bit_cursor+=chunk_size;
                cursor_result++;
                if(cursor_result == result.length){
                    break;
                }
            }
        }

        //Stop of timetaking of writing on the decompressed array
        if(timer!=null){
            timer.stop("Decompressing");
            timer.saveToJson("Decompress",result.length,array.length,  sizeLabel, valueLabel);
        }

        return result;
    }



    public int get(int index, int[] array, String sizeLabel,String valueLabel) {
        //Start of timetaking
        if(timer!=null)timer.start();

        //Extraction of the size of a chunk of data needed
        int chunk_size =extractBits(array[0],0,4);
        //Extraction of the number of chunks that will stay empty
        int unused_chunks =extractBits(array[0],5,9);
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
        int array_idex=(index/(chunks_per_integer));
        //Cursor on the bit in the Integer
        int cursor=(index%(chunks_per_integer))*chunk_size;
        if(32-cursor<chunk_size){cursor=0;}
        //Extraction of the value
        int result=extractBits (array[array_idex],cursor,cursor+chunk_size-1) ;

        //Stop timetaking
        if(timer!=null){
            timer.stop("get");
            timer.saveToJson("get",decompressed_array_size,array.length, sizeLabel,valueLabel);
        }
        return result;

    }


}
