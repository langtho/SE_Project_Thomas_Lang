package compressor;

import compressor.models.BitPacker;
import compressor.models.BitPackerFactory;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {

        BitPacker bp= BitPackerFactory.createBitPacker("overflow","src/main/resources/performance_data.jsonl");
        int[] uncompressed= {7593367,7245211,4414597,3844844,8194264,4219745,2619918,7021131,5938951,7248717,6876141,1880487,2074039,7451013,8627047,8703540,1301102,5213615,4752811,5202740,6365437,1408660,7170993,5037801,8159297,6842956,4030845,2856904,1629606,7340299,5046403,4998855,8949420,771261,4954633,4717224,904903,8164897,6419365,1081033,6370981,2735821};
        System.out.println( toBinaryString(uncompressed));
        assert bp != null;
        int[] compressed= bp.compress(uncompressed,"custom","custom");
        System.out.println( toBinaryString(compressed));
        System.out.println(Arrays.toString(compressed));
        System.out.println(bp.get(0,compressed,"custom","custom"));
        System.out.println(bp.get(1,compressed,"custom","custom"));
        System.out.println(bp.get(2,compressed,"custom","custom"));
        int[] reuncompressed= bp.decompress(compressed,"custom","custom");
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

