package compressor.models;

import compressor.services.*;

public class BitPackerFactory {
    public static BitPacker createBitPacker(String type) {
        switch (type) {
            case "spanning":
                return new SpanningBP();
            case "nonspanning":
                return new NonSpanningBP();
            default:
                return null;
        }
    }
}
