package compressor;

import compressor.models.BitPacker;
import compressor.models.BitPackerFactory;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        BitPacker bp= BitPackerFactory.createBitPacker("nonspanning");
        int[] uncompressed={1,55,12,3,12,45,23,12};
        int[] compressed= bp.compress(uncompressed);
        System.out.println(Arrays.toString(compressed));
        System.out.println(bp.get(0,compressed));
        System.out.println(bp.get(1,compressed));
        System.out.println(bp.get(2,compressed));
        System.out.println(bp.get(3,compressed));
        System.out.println(bp.get(4,compressed));
        System.out.println(bp.get(5,compressed));
        System.out.println(bp.get(6,compressed));
        System.out.println(bp.get(7,compressed));
        System.out.println(bp.get(8,compressed));
        int[] reuncompressed= bp.decompress(compressed);
        System.out.println(Arrays.toString(reuncompressed));
    }
}

