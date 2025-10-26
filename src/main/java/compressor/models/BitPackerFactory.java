package compressor.models;
import compressor.logger.Logger;
import compressor.services.SpanningBP;
import compressor.services.NonSpanningBP;
import compressor.services.OverflowBP;

import java.io.File;

public class BitPackerFactory {

    /**
     * Creates and returns a concrete BitPacker implementation based on the specified type.
     * * @param type The desired compression strategy type (e.g., "spanning", "overflow").
     * @param json_file The File path for performance logging or configuration (can be null).
     * @param logger The Logger instance (injected dependency).
     * @return The configured BitPacker instance.
     * @throws IllegalArgumentException If the provided type is unknown.
     */
    public static BitPacker createBitPacker(String type, File json_file, Logger logger) {

        BitPacker packer = switch (type.toLowerCase()) {
            case "spanning" -> new SpanningBP(json_file, logger);
            case "nonspanning" -> new NonSpanningBP(json_file, logger);
            case "overflow" -> new OverflowBP(json_file, logger);

            // Fallback: Throw an exception for unknown types (better than returning null)
            default -> throw new IllegalArgumentException("Unknown BitPacker type: " + type);
        };

        return packer;
    }
}
