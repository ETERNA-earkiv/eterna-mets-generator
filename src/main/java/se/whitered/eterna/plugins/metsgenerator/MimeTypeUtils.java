package se.whitered.eterna.plugins.metsgenerator;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import java.nio.file.Path;
import java.util.Map;

public class MimeTypeUtils {
    private static final MimeTypes DEFAULT_MIMETYPES = MimeTypes.getDefaultMimeTypes();
    private static Map<String, String> FILE_EXTENSION_TO_MIMETYPE_MAP;

    private MimeTypeUtils() {
    }

    public static void init() {
        if (FILE_EXTENSION_TO_MIMETYPE_MAP != null) {
            return;
        }

        FILE_EXTENSION_TO_MIMETYPE_MAP = new CaseInsensitiveMap<>();

        for (MediaType mediaType : DEFAULT_MIMETYPES.getMediaTypeRegistry().getTypes()) {
            try {
                MimeType mimeType = DEFAULT_MIMETYPES.getRegisteredMimeType(mediaType.toString());
                for (String fileExtension : mimeType.getExtensions()) {
                    FILE_EXTENSION_TO_MIMETYPE_MAP.put(fileExtension.toLowerCase(), mimeType.getName());
                }
            } catch (MimeTypeException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String getMimeType(final Path path) {
        return FILE_EXTENSION_TO_MIMETYPE_MAP
                .keySet()
                .stream()
                .filter(
                        k -> path
                                .getFileName()
                                .toString()
                                .toLowerCase()
                                .endsWith(k)
                )
                .findAny()
                .map(k -> FILE_EXTENSION_TO_MIMETYPE_MAP.get(k))
                .orElse(null);
    }
}
