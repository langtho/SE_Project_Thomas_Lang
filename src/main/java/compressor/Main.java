package compressor;

import compressor.models.BitPacker;
import compressor.models.BitPackerFactory;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        BitPacker bp= BitPackerFactory.createBitPacker("spanning");
        int[] uncompressed={1,55,12,3,12,45,23,12};
        System.out.println( toBinaryString(uncompressed));
        int[] compressed= bp.compress(uncompressed);
        System.out.println( toBinaryString(compressed));
        System.out.println(Arrays.toString(compressed));
        System.out.println(bp.get(0,compressed));
        System.out.println(bp.get(1,compressed));
        System.out.println(bp.get(2,compressed));
        System.out.println(bp.get(3,compressed));
        System.out.println(bp.get(4,compressed));
        System.out.println(bp.get(5,compressed));
        System.out.println(bp.get(6,compressed));
        System.out.println(bp.get(7,compressed));

        int[] reuncompressed= bp.decompress(compressed);
        System.out.println(Arrays.toString(reuncompressed));
    }


        public static String toBinaryString(int[] array) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                // Konvertiert den Integer zu einem Binärstring
                String binaryString = Integer.toBinaryString(array[i]);

                // Fügt führende Nullen hinzu, um auf 32 Bits aufzufüllen
                String paddedString = String.format("%32s", binaryString).replace(' ', '0');

                sb.append(paddedString);
                sb.append("\n");
            }
            return sb.toString();
        }

}

