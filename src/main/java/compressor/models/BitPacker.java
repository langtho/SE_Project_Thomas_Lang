package compressor.models;

public interface BitPacker {
    public int[] compress(int[] array);
    public int[] decompress(int[] array);
    public int get(int index);
}
