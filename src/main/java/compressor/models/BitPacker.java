package compressor.models;

public interface BitPacker {
    int[] compress(int[] array);

    int[] decompress(int[] array);

    int get(int index, int[] array);

    //GET_NUMBER_OF_BITS_NEEDED function: Input: Array of Integers Output: Integer
    //Returns an Integer of how many bits needed to represent the most of the Input Array
    default int get_number_of_bits_needed(int[] array) {
        int bits_needed = 0;
        for (int j : array) {
            bits_needed =Math.max (bits_needed, 32- Integer.numberOfLeadingZeros(j) );
        }
        bits_needed = bits_needed * array.length;
        return bits_needed;
    }

    default int insert_bits_in_result(int dest,int start_bit_dest,int source,int start_bit_source,int end_bit_source){
        int numBitsToCopy = end_bit_source - start_bit_source + 1;

        if (numBitsToCopy <= 0 || start_bit_source < 0 || start_bit_dest < 0) {
            throw new IllegalArgumentException("Illegal Idex");
        }

        long extractionMask = (1L << numBitsToCopy) - 1;
        extractionMask <<= start_bit_source;

        int extractedBits=(int) ((source&extractionMask)>>start_bit_source);

        long clearMask = (1L << numBitsToCopy) - 1;
        clearMask <<= start_bit_dest;


        int clearedTargetInt = dest & (int)~clearMask;

        int shiftedBits= extractedBits<<start_bit_dest;

        return clearedTargetInt|shiftedBits;
    }

    default int extractBits(int sourceInt, int startIndex, int endIndex) {
        if (startIndex < 0 || endIndex > 31 || startIndex > endIndex) {
            throw new IllegalArgumentException("Illegal Index");
        }
        int numBits = endIndex - startIndex + 1;
        long mask = (1L << numBits) - 1;
        mask <<= startIndex;
        int extractedBits = (int)(sourceInt & mask);
        return extractedBits >> startIndex;
    }
}
