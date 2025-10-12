package compressor;

import compressor.models.BitPacker;
import compressor.models.BitPackerFactory;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {

        BitPacker bp= BitPackerFactory.createBitPacker("overflow","src/main/resources/performance_data.jsonl");
        int[] uncompressed={906, 987, 313, 886, 89, 727, 614, 112, 922, 463, 757, 146, 369, 216, 292, 72, 217, 474, 799, 8, 168, 948, 394, 114, 970, 926, 761};
        System.out.println( toBinaryString(uncompressed));
        int[] compressed= bp.compress(uncompressed);
        System.out.println( toBinaryString(compressed));
        System.out.println(Arrays.toString(compressed));
        System.out.println(bp.get(0,compressed));
        System.out.println(bp.get(1,compressed));
        System.out.println(bp.get(2,compressed));
        int[] reuncompressed= bp.decompress(compressed);
        System.out.println(Arrays.toString(reuncompressed));
    }


        public static String toBinaryString(int[] array) {
            StringBuilder sb = new StringBuilder();
            for (int j : array) {
                // Konvertiert den Integer zu einem Binärstring
                String binaryString = Integer.toBinaryString(j);

                // Fügt führende Nullen hinzu, um auf 32 Bits aufzufüllen
                String paddedString = String.format("%32s", binaryString).replace(' ', '0');

                sb.append(paddedString);
                sb.append("\n");
            }
            return sb.toString();
        }

}

