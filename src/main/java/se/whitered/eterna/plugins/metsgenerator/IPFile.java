package se.whitered.eterna.plugins.metsgenerator;

import org.roda_project.commons_ip2.model.IPConstants;
import org.roda_project.commons_ip2.model.IPFileInterface;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

public class IPFile implements IPFileInterface {
    private final Path path;
    private final Path relativePath;
    private final long size;
    private final String checksum;
    private final String mimeType;
    private final FileTime creationTime;

    public IPFile(final Path path, Path relativePath, final long size, final String checksum, final String mimeType, final FileTime creationTime) {
        this.size = size;
        this.checksum = checksum;
        this.path = path;
        this.relativePath = relativePath;
        this.mimeType = mimeType;
        this.creationTime = creationTime;
    }

    @Override
    public List<String> getRelativeFolders() {
        final int folderCount = relativePath.getNameCount() > 0 ? relativePath.getNameCount() - 1 : 0;
        final List<String> relativeFolders = new ArrayList<>(folderCount);
        for (int i = 0; i < relativePath.getNameCount(); i++) {
            relativeFolders.add(relativePath.getName(i).toString());
        }

        return relativeFolders;
    }

    @Override
    public String getFileName() {
        return path.getFileName().toString();
    }

    @Override
    public Path getPath() {
        return path;
    }

    public Path getRelativePath() {
        return relativePath;
    }

    public long getSize() {
        return size;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getChecksumAlgorithm() {
        return IPConstants.CHECKSUM_SHA_256_ALGORITHM;
    }

    public String getMimeType() {
        return mimeType;
    }

    public FileTime getCreationTime() {
        return creationTime;
    }
}
