package compressor.services;
import compressor.models.BitPacker;
import org.javatuples.Triplet;

public class OverflowBP implements BitPacker {
    private PerformanceTimer timer;
    public OverflowBP(String filePath) {
        //Initialisation of timer
        if (filePath!=null)this.timer= new PerformanceTimer(filePath,"Overflow");
    }

    //COMPRESS  function: Input: An Array of Integers Output: An Array of Integers
    //It compresses an array of integers to a smaller Array of Integers using bit manipulation
    public int[] compress(int[] array, String sizeLabel,String valueLabel) {

        //Start of timetaking
        if(timer!=null)timer.start();

        if(array.length == 0) return new int[0];
        Triplet<Integer,Integer,Integer> triplet=get_ideal_chunksize(array);

        //Take time of the bit-needed function
        if(timer!=null)timer.stop("IdealChunkSize");

        //The size of a chunk of data needed
        int chunk_size = triplet.getValue0();
        //Size of the Overflow space
        int overflow_size=triplet.getValue2();

        String encoded_overflow_size=encodeEliasGamma(overflow_size+1);
        //Size of the new array
        int new_array_size = (triplet.getValue1())/32+overflow_size;

        //Number of unused bits at the end of the compressed array (before the overflow area)
        int unused_bits=32-((((chunk_size+1)*array.length)+10+encoded_overflow_size.length())%32);
        int[] result = new int[new_array_size];

        //Take time of the bit-needed function
        if(timer!=null)timer.stop("Setup");

        int result_cursor = 0; //points to the current treated Integer of the array
        int bit_cursor = 0; //points on the current bit
        int overflow_counter=0;


        //Loop that writes the bits onto the new array
        for (int i = 0; i < array.length; i++) {
            //The writing of the chunk size and unused_bits at the beginning of the first Integer for each Compressed Array
            if (i == 0 ) {
                result[result_cursor] = insert_bits_in_result(0,0,chunk_size,0,4) ;
                result[result_cursor] = insert_bits_in_result(result[result_cursor],5,unused_bits,0,4);


                int overflow_value=Integer.parseInt(encoded_overflow_size, 2);
                int numBitsToInsert = encoded_overflow_size.length();

                if (numBitsToInsert>22){
                    result[result_cursor]=insert_bits_in_result(result[result_cursor],10,overflow_value,0,21);
                    result_cursor++;

                    result[result_cursor]=insert_bits_in_result(result[result_cursor],0,overflow_value,22,numBitsToInsert);
                    bit_cursor=numBitsToInsert-22;
                }else{
                    result[result_cursor]= insert_bits_in_result(result[result_cursor],10,overflow_value,0,numBitsToInsert);
                    bit_cursor = 10+numBitsToInsert;
                }

            }

            if(chunk_size<(32-Integer.numberOfLeadingZeros(array[i]))) {
                result[result_cursor] |= 1 << bit_cursor;
                bit_cursor++;
                if(bit_cursor==32) {
                    bit_cursor=0;
                    result_cursor++;
                }
                result[result.length-overflow_counter-1]=array[i];
                array[i]=overflow_counter;
                overflow_counter++;
            }else {
                result[result_cursor] |= 0;
                bit_cursor++;
                if(bit_cursor==32) {
                    bit_cursor=0;
                    result_cursor++;
                }
            }

            //Default case: Writing of the current Integer
            if (bit_cursor + chunk_size > 32) {

                result[result_cursor]=insert_bits_in_result(result[result_cursor],bit_cursor,array[i],0,31-bit_cursor);
                result_cursor++;
                result[result_cursor]=insert_bits_in_result(0,0,array[i],32-bit_cursor,chunk_size);
                bit_cursor =chunk_size-(32-bit_cursor);

            } else if (32-bit_cursor == chunk_size ) {

                result[result_cursor] |= array[i] << bit_cursor;
                bit_cursor = 0;
                result_cursor++;

            } else {
                result[result_cursor] |= array[i] << bit_cursor;
                bit_cursor += chunk_size;
            }

        }

        //Stop of timetaking of writing on the compressed array
        if(timer!=null){
            timer.stop("Compressing");
            timer.saveToJson("Compress",array.length,result.length,  sizeLabel, valueLabel);
        }

        return result;
    }

    public int[] decompress(int[] array, String sizeLabel,String valueLabel) {

        //Start of timetaking
        if(timer!=null)timer.start();

        if(array.length == 0) return new int[0];
        //Extraction of the size of a chunk of data needed
        int chunk_size =extractBits(array[0],0,4);
        //Array size of the Array which will be returned
        int unused_bits =extractBits(array[0],5,9);
        String overflow_string=decodeEliasGamma(array);
        int overflow_size=Integer.parseInt(overflow_string,2)-1;
        int overflow_encoded_length=(overflow_string.length()-1)*2+1;
        int array_length=((((array.length-overflow_size)*32)-(10+unused_bits+overflow_encoded_length))/(chunk_size+1));
        int[] result = new int[array_length];

        //Stop of Setup time taking
        if (timer!=null)timer.stop("Setup");

        int cursor_array = 0;
        int bit_cursor = 10+((32-Integer.numberOfLeadingZeros(overflow_size+1))-1)*2+1;
        if(bit_cursor>=32) {
            bit_cursor=bit_cursor%32;
            cursor_array++;
        }
        for (int i = 0; i < array_length; i++) {
            if (32-bit_cursor == 0) {
             bit_cursor = 0;
             cursor_array++;
            }

            if(readBit(array,bit_cursor,cursor_array)==0) {
                bit_cursor++;
                if (32-bit_cursor == 0) {
                    bit_cursor = 0;
                    cursor_array++;
                }
                //Extraction of the Integer value
                if (32 - bit_cursor < chunk_size) {
                    result[i]=insert_bits_in_result(0,0,array[cursor_array],bit_cursor,bit_cursor+32-bit_cursor);
                    cursor_array++;
                    result[i] = insert_bits_in_result(result[i],32-bit_cursor,array[cursor_array],0,chunk_size-(33-bit_cursor)) ;
                    bit_cursor = chunk_size-(32-bit_cursor);
                } else if (32 - bit_cursor == 0) {
                    bit_cursor = 0;
                    cursor_array++;
                    result[i] = extractBits(array[cursor_array] , bit_cursor,bit_cursor+chunk_size-1) ;
                    bit_cursor = chunk_size;
                } else {
                    result[i] = extractBits(array[cursor_array] , bit_cursor,bit_cursor+chunk_size-1) ;
                    bit_cursor += chunk_size;
                }
            }else{
                bit_cursor++;
                if (32-bit_cursor == 0) {
                    bit_cursor = 0;
                    cursor_array++;
                }
                int overflow_index;
                //Extraction of the Integer value
                if (32 - bit_cursor < chunk_size) {
                    overflow_index=insert_bits_in_result(0,0,array[cursor_array],bit_cursor,31);
                    cursor_array++;
                    overflow_index = insert_bits_in_result(overflow_index,32-bit_cursor,array[cursor_array],0,chunk_size-(33-bit_cursor)) ;

                    bit_cursor = chunk_size-(32-bit_cursor);
                } else if (32 - bit_cursor == 0) {
                    bit_cursor = 0;
                    cursor_array++;
                    overflow_index= extractBits(0, bit_cursor,bit_cursor+chunk_size-1);
                    bit_cursor+=chunk_size;
                } else {
                   overflow_index= extractBits(array[cursor_array] , bit_cursor,bit_cursor+chunk_size-1);

                   bit_cursor += chunk_size;
                }
                if(overflow_index>=0&&overflow_size!=0&&overflow_index<array.length) {
                    overflow_index = array.length - overflow_index - 1;
                    result[i] = array[overflow_index];
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

    public int get(int index, int[] array, String sizeLabel,String valueLabel){
        //Start of timetaking
        if(timer!=null)timer.start();

        if(array.length == 0) return 0;
        //Extraction of the size of a chunk of data needed
        int chunk_size = extractBits(array[0],0,4);
        //Array size of the Array which will be returned
        int unused_bits =extractBits(array[0],5,9);
        String overflow_string=decodeEliasGamma(array);
        int overflow_size=Integer.parseInt(overflow_string,2)-1;
        int array_length=((((array.length-overflow_size)*32)-10-unused_bits)/chunk_size);

        //Test if the index is out of bounds
        if (index < 0 || index >= array_length) {
            System.err.println("index out of bounds");
            return -1;
        }

        int bit_cursor = (10+(31-Integer.numberOfLeadingZeros(overflow_size+1))*2+1)+(chunk_size+1)*index;
        int cursor_array = bit_cursor/32;
        bit_cursor = bit_cursor%32;
        int result;


        if(readBit(array,bit_cursor,cursor_array)==0) {
            bit_cursor++;
            //Extraction of the Integer value
            if (32 - bit_cursor < chunk_size) {
                result=insert_bits_in_result(0,0,array[cursor_array],bit_cursor,bit_cursor+32-bit_cursor);
                cursor_array++;
                result = insert_bits_in_result(result,32-bit_cursor,array[cursor_array],0,chunk_size-(33-bit_cursor)) ;
            } else if (32 - bit_cursor == 0) {
                bit_cursor = 0;
                cursor_array++;
                result = extractBits(array[cursor_array] , bit_cursor,bit_cursor+chunk_size-1);
            } else {
                result = extractBits(array[cursor_array] , bit_cursor,bit_cursor+chunk_size-1);
            }
        }else{
            bit_cursor++;
            int overflow_index;
            //Extraction of the Integer value
            if (32 - bit_cursor < chunk_size) {
                overflow_index=insert_bits_in_result(0,0,array[cursor_array],bit_cursor,bit_cursor+32-bit_cursor);
                cursor_array++;
                overflow_index = insert_bits_in_result(overflow_index,32-bit_cursor,array[cursor_array],0,chunk_size-(33-bit_cursor)) ;
            } else if (32 - bit_cursor == 0) {
                bit_cursor = 0;
                cursor_array++;
                overflow_index= extractBits(array[cursor_array] , bit_cursor,bit_cursor+chunk_size-1);
            } else {
                overflow_index= extractBits(array[cursor_array] , bit_cursor,bit_cursor+chunk_size-1);
            }
            overflow_index=array.length-overflow_index-1;
            result=array[overflow_index];
        }

        //Stop timetaking
        if(timer!=null){
            timer.stop("get");
            timer.saveToJson("get",array_length,array.length,  sizeLabel, valueLabel);
        }
        return result;
    }

    Triplet<Integer,Integer,Integer> get_ideal_chunksize(int[] array) {
        int[] value_distribution=new int [32];
        for (int j : array) {
            int minimal_bits_needed = 32 - Integer.numberOfLeadingZeros(j);
            if (minimal_bits_needed == 0) {
                minimal_bits_needed = 1;
            }
            value_distribution[minimal_bits_needed - 1]++;
        }

        int values_included = 0;
        int current_smallest_chunk=0;
        int overflow_size=0;
        int size_for_smallest_chunk=array.length*32+33;
        for (int i=0; i<32; i++) {

            if(value_distribution[i]!=0) {
                values_included += value_distribution[i];
                int temp_overflow_size = array.length - values_included;
                temp_overflow_size++;
                int elias_gamma_overhead = (31 - Integer.numberOfLeadingZeros(temp_overflow_size)) * 2 + 1;
                int packed_data_bits = array.length * (i + 2);
                int overflow_data_bits = (temp_overflow_size-1) * 32;
                int metadata_bits = 10;

                int temp_size_bits = packed_data_bits + elias_gamma_overhead + overflow_data_bits + metadata_bits;
                int temp_size = (int) Math.ceil(temp_size_bits / 32.0) * 32;
                if (temp_size <= size_for_smallest_chunk||current_smallest_chunk==0) {
                    if(32-Integer.numberOfLeadingZeros(temp_overflow_size)<=i+1){
                        size_for_smallest_chunk = temp_size;
                        current_smallest_chunk = i + 1;
                        overflow_size = temp_overflow_size - 1;
                    }
                }
            }
        }
        return new Triplet<>(current_smallest_chunk,size_for_smallest_chunk,overflow_size);
    }

    public static String encodeEliasGamma(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Elias-Gamma is for positive integers.");
        }

        String binaryValue = Integer.toBinaryString(value);
        int length = binaryValue.length()-1;

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < length; i++) {
            prefix.append('0');
        }

        return prefix.append(binaryValue).reverse().toString();
    }



    public static String decodeEliasGamma(int[] array) {
        int k = 0;
        int bit_cursor = 10;
        int array_index = 0;
        while (readBit(array, bit_cursor, array_index) == 0) {
            k++;
            bit_cursor++;
            if (bit_cursor >= 32) {
                bit_cursor=0;
                array_index++;
            }
        }

        StringBuilder sb=new StringBuilder();

        for (int i = 0; i <= k; i++) {
            int bit = readBit(array, bit_cursor, array_index);
            sb.append(bit);

            bit_cursor++;
            if (bit_cursor >= 32) {
                bit_cursor=0;
                bit_cursor++;
            }
        }

        return sb.toString();
    }

    private static int readBit(int[] data, int bitCursor, int arrayIndex) {
        int currentInt = data[arrayIndex];
        return (currentInt >>> bitCursor) & 1;
    }

}
