package se.whitered.eterna.plugins.metsgenerator;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.roda.core.storage.ContentPayload;
import org.roda.core.util.FileUtility;
import org.roda_project.commons_ip2.mets_v1_12.beans.Mets;
import org.roda_project.commons_ip2.model.IPConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class MetsContentPayload implements ContentPayload {
    private final Mets mets;
    private final boolean isRootMets;
    private long size = 0;
    private String checksum;

    public MetsContentPayload(final Mets mets, final boolean isRootMets) {
        this.mets = mets;
        this.isRootMets = isRootMets;
    }

    /**
     * Create a new input stream that should be explicitly closed after being
     * consumed.
     *
     * @return
     */
    @Override
    public InputStream createInputStream() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Write the current stream to the specified file path.
     *
     * @param path
     * @throws IOException
     */
    @Override
    public void writeToPath(Path path) throws IOException {
        try {
            JAXBContext context = JAXBContext.newInstance(Mets.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            final String schemasPath = isRootMets ? "schemas/" : "../../schemas/";

            m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.loc.gov/METS/ " + schemasPath + IPConstants.SCHEMA_METS_FILENAME_WITH_VERSION + " http://www.w3.org/1999/xlink " + schemasPath + IPConstants.SCHEMA_XLINK_FILENAME + " https://dilcis.eu/XML/METS/CSIPExtensionMETS " + schemasPath + IPConstants.SCHEMA_EARK_CSIP_FILENAME + " https://dilcis.eu/XML/METS/SIPExtensionMETS " + schemasPath + IPConstants.SCHEMA_EARK_SIP_FILENAME);

            try (IPFileOutputStream metsOutputStream = new IPFileOutputStream(Files.newOutputStream(path))) {
                m.marshal(mets, metsOutputStream);
                size = metsOutputStream.getSize();
                checksum = FileUtility.byteArrayToHexString(metsOutputStream.getDigest());
            }
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    /**
     * Retrieves a URI for the file (e.g. 'file:', 'http:', etc.)
     *
     * @return
     */
    @Override
    public URI getURI() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public long getSize() {
        return size;
    }

    public String getChecksum() {
        return checksum;
    }
}
