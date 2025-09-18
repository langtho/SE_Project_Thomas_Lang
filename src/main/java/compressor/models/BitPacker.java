package compressor.models;

public interface BitPacker {
    public int[] compress(int[] array);
    public int[] decompress(int[] array);
    public int get(int index, int[] array);

    default int get_number_of_bits_needed(int[] array){
        int bits_needed = 0;
        for(int i = 0; i < array.length; i++){
            bits_needed = (32-Integer.numberOfLeadingZeros(array[i])>bits_needed)? 32-Integer.numberOfLeadingZeros(array[i]) : bits_needed ;
        }
        bits_needed=bits_needed* array.length;
        return bits_needed;
    }
}
