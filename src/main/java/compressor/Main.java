package compressor;

import compressor.models.BitPacker;
import compressor.models.BitPackerFactory;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {

        BitPacker bp= BitPackerFactory.createBitPacker("overflow","src/main/resources/performance_data.jsonl");
        int[] uncompressed={1682227400,1574455182,780798573,1893877122,901058016,1467978036,1622447264,106847633,2086870668,916247900,826303942,1028706322,282421831,342889249,348583170,1350376317,1746067240,1582537669,569419671,884201564,1974167380,619846862,736734564,1730295098,201530116,2021124683,2095966972,1798307771,1754941743,1136618293,2011900774,2025057477,1128603860,89434608,112358254,449769948,2003330278,799681265,}
                ;
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

