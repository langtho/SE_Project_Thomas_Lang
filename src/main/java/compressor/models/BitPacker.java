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
}
