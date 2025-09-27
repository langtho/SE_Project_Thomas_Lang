package compressor.models;

import compressor.services.*;

public class BitPackerFactory {
    public static BitPacker createBitPacker(String type) {
        return switch (type) {
            case "spanning" -> new SpanningBP();
            case "nonspanning" -> new NonSpanningBP();
            case "overflow" -> new OverflowBP();
            default -> null;
        };
    }
}
