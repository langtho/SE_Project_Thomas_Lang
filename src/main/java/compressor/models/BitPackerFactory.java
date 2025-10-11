package compressor.models;

import compressor.services.*;

public class BitPackerFactory {
    public static BitPacker createBitPacker(String type, String json_file) {
        if(json_file != null) {
            return switch (type) {
                case "spanning" -> new SpanningBP(json_file);
                case "nonspanning" -> new NonSpanningBP(json_file);
                case "overflow" -> new OverflowBP(json_file);
                default -> null;
            };
        }else{
            return switch (type) {
                case "spanning" -> new SpanningBP(json_file);
                case "nonspanning" -> new NonSpanningBP(json_file);
                case "overflow" -> new OverflowBP(json_file);
                default -> null;
            };
        }
    }
}
