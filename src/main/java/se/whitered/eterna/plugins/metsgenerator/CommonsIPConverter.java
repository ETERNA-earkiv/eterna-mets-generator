package se.whitered.eterna.plugins.metsgenerator;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.data.v2.ip.metadata.DescriptiveMetadata;
import org.roda.core.data.v2.ip.metadata.IndexedPreservationAgent;
import org.roda.core.data.v2.ip.metadata.IndexedPreservationEvent;
import org.roda.core.data.v2.ip.metadata.OtherMetadata;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata;
import org.roda.core.index.IndexService;
import org.roda.core.model.utils.ModelUtils;
import org.roda_project.commons_ip2.model.IPDescriptiveMetadata;
import org.roda_project.commons_ip2.model.IPMetadata;
import org.roda_project.commons_ip2.model.MetadataType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.GregorianCalendar;

import static se.whitered.eterna.plugins.metsgenerator.METSUtils.createXmlGregorianCalender;


public class CommonsIPConverter {
    private static final IndexService indexService;

    static {
        indexService = RodaCoreFactory.getIndexService();
    }

    private CommonsIPConverter() {
    }

    public static IPFile convert(File file) throws RequestNotValidException, AuthorizationDeniedException, NotFoundException, IOException, NoSuchAlgorithmException, GenericException {
        final String aipId = file.getAipId();
        final String repId = file.getRepresentationId();
        final StoragePath storagePath = ModelUtils.getFileStoragePath(file);

        return METSUtils.createIPFile(storagePath, aipId, repId);
    }

    public static IPDescriptiveMetadata convert(DescriptiveMetadata descriptiveMetadata) throws RequestNotValidException, AuthorizationDeniedException, NotFoundException, IOException, NoSuchAlgorithmException, GenericException {
        final String aipId = descriptiveMetadata.getAipId();
        final String repId = descriptiveMetadata.getRepresentationId();
        final StoragePath metadataStoragePath = ModelUtils.getDescriptiveMetadataStoragePath(descriptiveMetadata);
        final IPFile ipFile = METSUtils.createIPFile(metadataStoragePath, aipId, repId);

        return new IPDescriptiveMetadata(ipFile, new MetadataType(descriptiveMetadata.getType()), descriptiveMetadata.getVersion());
    }

    public static IPMetadata convert(PreservationMetadata preservationMetadata) throws RequestNotValidException, AuthorizationDeniedException, NotFoundException, GenericException, IOException, NoSuchAlgorithmException, DatatypeConfigurationException {
        final String aipId = preservationMetadata.getAipId();
        final String repId = preservationMetadata.getRepresentationId();
        final StoragePath metadataStoragePath = ModelUtils.getPreservationMetadataStoragePath(preservationMetadata);

        final IPFile ipFile = METSUtils.createIPFile(metadataStoragePath, aipId, repId);
        IPMetadata ipMetadata = new IPMetadata(ipFile);
        ipMetadata.setId(preservationMetadata.getId());
        ipMetadata.setMetadataType(CommonsIPConverter.convert(preservationMetadata.getType()));

        switch (preservationMetadata.getType()) {
            case EVENT:
                IndexedPreservationEvent preservationEvent = indexService.retrieve(IndexedPreservationEvent.class, preservationMetadata.getId(), Collections.emptyList());
                ipMetadata.setCreateDate(createXmlGregorianCalender(preservationEvent.getEventDateTime()));
                break;

            case AGENT:
                IndexedPreservationAgent preservationAgent = indexService.retrieve(IndexedPreservationAgent.class, preservationMetadata.getId(), Collections.emptyList());
                ipMetadata.setCreateDate(createXmlGregorianCalender(preservationAgent.getCreatedOn()));
                break;

            default:
                if (ipFile.getCreationTime() != null) {
                    ipMetadata.setCreateDate(CommonsIPConverter.convert(ipFile.getCreationTime()));
                }
        }

        return ipMetadata;
    }

    public static IPMetadata convert(OtherMetadata otherMetadata) throws RequestNotValidException, AuthorizationDeniedException, NotFoundException, GenericException, IOException, NoSuchAlgorithmException {
        final String aipId = otherMetadata.getAipId();
        final String repId = otherMetadata.getRepresentationId();
        final StoragePath metadataStoragePath = ModelUtils.getRepresentationOtherMetadataFolderStoragePath(aipId, repId);

        final IPFile ipFile = METSUtils.createIPFile(metadataStoragePath, aipId, repId);
        IPMetadata ipMetadata = new IPMetadata(ipFile);
        ipMetadata.setId(otherMetadata.getId());
        ipMetadata.setMetadataType(new MetadataType(otherMetadata.getType()));

        return ipMetadata;
    }

    public static MetadataType convert(PreservationMetadata.PreservationMetadataType metadataType) {
        return switch (metadataType) {
            case AGENT -> new MetadataType(MetadataType.MetadataTypeEnum.PREMISAGENT);
            case EVENT -> new MetadataType(MetadataType.MetadataTypeEnum.PREMISEVENT);
            case RIGHTS_STATEMENT -> new MetadataType(MetadataType.MetadataTypeEnum.PREMISRIGHTS);

            // Other types are missing from MetadataType and METS MDTYPE, handles as PREMIS:OBJECT
            default -> new MetadataType(MetadataType.MetadataTypeEnum.PREMISOBJECT);
        };
    }

    public static XMLGregorianCalendar convert(FileTime fileTime)  {
        try {
            Instant instant = fileTime.toInstant();
            GregorianCalendar calendar = GregorianCalendar.from(instant.atZone(ZoneId.systemDefault()));

            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            return null;
        }
    }
}
