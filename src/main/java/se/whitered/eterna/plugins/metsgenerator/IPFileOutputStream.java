package se.whitered.eterna.plugins.metsgenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IPFileOutputStream extends OutputStream {
    private final OutputStream outputStream;
    private final MessageDigest digest;
    private long size = 0;

    public IPFileOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public long getSize() {
        return size;
    }

    public byte[] getDigest() {
        return digest.digest();
    }

    @Override
    public void write(int b) throws IOException {
        size++;
        digest.update((byte) b);
        outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        size += len;
        digest.update(b, off, len);
        outputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
