package se.whitered.eterna.plugins.metsgenerator;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.roda.core.RodaCoreFactory;
import org.roda.core.common.iterables.CloseableIterable;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.common.OptionalWithCause;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.core.data.v2.ip.Representation;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.data.v2.ip.metadata.DescriptiveMetadata;
import org.roda.core.data.v2.ip.metadata.OtherMetadata;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.storage.Binary;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.Resource;
import org.roda.core.storage.StorageService;
import org.roda.core.util.FileUtility;
import org.roda_project.commons_ip.utils.IPException;
import org.roda_project.commons_ip.utils.METSEnums;
import org.roda_project.commons_ip2.mets_v1_12.beans.DivType;
import org.roda_project.commons_ip2.mets_v1_12.beans.FileType;
import org.roda_project.commons_ip2.mets_v1_12.beans.MdSecType;
import org.roda_project.commons_ip2.mets_v1_12.beans.Mets;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType;
import org.roda_project.commons_ip2.model.IPAgent;
import org.roda_project.commons_ip2.model.IPAgentNoteTypeEnum;
import org.roda_project.commons_ip2.model.IPConstants;
import org.roda_project.commons_ip2.model.IPContentInformationType;
import org.roda_project.commons_ip2.model.IPContentType;
import org.roda_project.commons_ip2.model.IPDescriptiveMetadata;
import org.roda_project.commons_ip2.model.IPHeader;
import org.roda_project.commons_ip2.model.IPMetadata;
import org.roda_project.commons_ip2.model.MetadataType;
import org.roda_project.commons_ip2.model.MetsWrapper;
import org.roda_project.commons_ip2.model.impl.eark.EARKMETSCreator;
import org.roda_project.commons_ip2.model.impl.eark.METSGeneratorFactory;
import org.roda_project.commons_ip2.utils.Utils;
import se.whitered.eterna.plugins.metsgenerator.exceptions.MetsGeneratorException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.roda_project.commons_ip2.utils.Utils.generateRandomAndPrefixedUUID;

public class METSUtils {
    private static final String SIP_VERSION = "2.2.0";

    private static final IndexService indexService;
    private static final ModelService modelService;
    private static final StorageService storageService;

    private static final boolean useFileContentMimeTypeDetection = RodaCoreFactory.getRodaConfiguration().getBoolean(
            "core.tools.mets-generator.useFileContentMimeTypeDetection", false);


    static {
        indexService = RodaCoreFactory.getIndexService();
        modelService = RodaCoreFactory.getModelService();
        storageService = RodaCoreFactory.getStorageService();
    }

    public static MetsWrapper generateMETS(AIP aip, CSIPProfile profile, Report reportItem) throws AuthorizationDeniedException, MetsGeneratorException {
        return generateMETS(aip, null, profile, reportItem);
    }

    public static MetsWrapper generateMETS(AIP aip, Representation representation, CSIPProfile profile, final Report reportItem) throws AuthorizationDeniedException, MetsGeneratorException {
        final String aipId = aip.getId();
        final String repId = representation != null ? representation.getId() : null;
        final IndexedAIP indexedAIP = getIndexedAIP(aipId, reportItem);
        final IndexedRepresentation indexedRepresentation = getIndexedRepresentation(aipId, repId, reportItem);

        final METSGeneratorFactory factory = new METSGeneratorFactory();
        final EARKMETSCreator metsCreator = factory.getGenerator(SIP_VERSION);

        final IPHeader ipHeader = getIpHeader(indexedAIP);

        boolean isMetadata;
        boolean isMetadataOther;
        boolean isSchemas;
        boolean isDocumentation;
        boolean isSubmission;
        boolean isRepresentations;
        boolean isRepresentationsData;

        try {
            isMetadata = containsMetadata(indexedAIP, indexedRepresentation);
            isMetadataOther = containsMetadataOther(indexedAIP, indexedRepresentation);
            isSchemas = containsSchemas(indexedAIP, indexedRepresentation);
            isDocumentation = containsDocumentation(indexedAIP, indexedRepresentation);
            isSubmission = containsSubmissions(indexedAIP, indexedRepresentation);
            isRepresentations = containsRepresentations(indexedAIP, indexedRepresentation);
            isRepresentationsData = containsRepresentationData(indexedAIP, indexedRepresentation);
        } catch (AuthorizationDeniedException e) {
            throw new AuthorizationDeniedException("Not authorized to read source AIP");
        } catch (GenericException e) {
            throw new MetsGeneratorException("Could not read the content of the source AIP");
        }

        MetsWrapper metsWrapper;
        try {
            metsWrapper = metsCreator.generateMETS(
                    representation == null ? aipId : repId,
                    representation == null ? indexedAIP.getDescription() : null,
                    profile.getProfileURI(),
                    representation == null,
                    representation == null ? Optional.ofNullable(indexedAIP.getAncestors()) : Optional.empty(),
                    null, // Path MetsPath
                    ipHeader,
                    profile.getProfile().toString(),
                    new IPContentType(representation == null ? aip.getType() : representation.getType()),
                    IPContentInformationType.getMIXED(), // TODO: Handle other ContentInformationType such as SIARD, ERMS, etc.
                    isMetadata,
                    isMetadataOther,
                    isSchemas,
                    isDocumentation,
                    isSubmission,
                    isRepresentations,
                    isRepresentationsData
            );
        } catch (IPException exception) {
            if (representation == null) {
                throw new MetsGeneratorException("Could not create new IP level METS file");
            } else {
                throw new MetsGeneratorException("Could not create new representation level METS file");
            }
        }

        // EARKMETSCreator#generateMETS sets the creation date to the current date / time
        setCreationDate(metsWrapper, aip);

        if (repId != null) {
            processDataFiles(metsWrapper, aipId, repId, reportItem);
        }

        processDescriptiveMetadata(metsWrapper, aipId, repId, reportItem);
        processPreservationMetadata(metsWrapper, aipId, repId, reportItem);
        processTechnicalMetadata(metsWrapper, aipId, repId, reportItem);
        processSourceMetadata(metsWrapper, aipId, repId, reportItem);
        processRightsMetadata(metsWrapper, aipId, repId, reportItem);
        processOtherMetadata(metsWrapper, aipId, repId, reportItem);
        processSchemas(metsWrapper, aipId, repId, reportItem);
        processDocumentation(metsWrapper, aipId, repId, reportItem);

        if (repId == null) {
            processRepresentations(metsWrapper, aip, profile, reportItem);
        }

        return metsWrapper;
    }

    public static XMLGregorianCalendar createXmlGregorianCalender(final Date date) {
        try {
            final GregorianCalendar gregorianCalendarCreatedOn = new GregorianCalendar();
            gregorianCalendarCreatedOn.setTime(date);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendarCreatedOn);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setCreationDate(MetsWrapper metsWrapper, AIP aip) {
        metsWrapper.getMets().getMetsHdr().setCREATEDATE(createXmlGregorianCalender(aip.getCreatedOn()));
    }

    private static IndexedAIP getIndexedAIP(final String aipId, final Report reportItem) {
        try {
            return indexService.retrieve(
                    IndexedAIP.class,
                    aipId,
                    Arrays.asList(
                            RodaConstants.INDEX_UUID,
                            RodaConstants.AIP_ANCESTORS,
                            RodaConstants.AIP_CREATED_BY,
                            RodaConstants.AIP_CREATED_ON,
                            RodaConstants.AIP_DESCRIPTION,
                            RodaConstants.AIP_DESCRIPTIVE_METADATA_ID,
                            RodaConstants.AIP_HAS_REPRESENTATIONS,
                            RodaConstants.AIP_ID,
                            RodaConstants.AIP_NUMBER_OF_DOCUMENTATION_FILES,
                            RodaConstants.AIP_NUMBER_OF_SCHEMA_FILES,
                            RodaConstants.AIP_NUMBER_OF_SUBMISSION_FILES,
                            RodaConstants.AIP_PARENT_ID,
                            RodaConstants.AIP_REPRESENTATION_ID,
                            RodaConstants.AIP_STATE,
                            RodaConstants.AIP_TITLE,
                            RodaConstants.AIP_TYPE
                    )
            );
        } catch (NotFoundException | GenericException e) {
            new MetsGeneratorException(e).warn(reportItem);
        }

        return null;
    }

    private static IndexedRepresentation getIndexedRepresentation(final String aipId, final String representationId, final Report reportItem) {
        if (representationId == null) {
            return null;
        }

        try {

            SimpleFilterParameter aipIdFilterParameter = new SimpleFilterParameter("aipId", aipId);
            SimpleFilterParameter repIdFilterParameter = new SimpleFilterParameter("id", representationId);

            Filter filter = new Filter(aipIdFilterParameter, repIdFilterParameter);

            Sublist sublist = new Sublist(0, 1);

            List<IndexedRepresentation> result = indexService.find(IndexedRepresentation.class, filter, null, sublist,
                    Arrays.asList(
                            RodaConstants.INDEX_UUID,
                            RodaConstants.REPRESENTATION_ANCESTORS,
                            RodaConstants.REPRESENTATION_CREATED_BY,
                            RodaConstants.REPRESENTATION_CREATED_ON,
                            RodaConstants.AIP_DESCRIPTION,
                            RodaConstants.AIP_DESCRIPTIVE_METADATA_ID,
                            RodaConstants.REPRESENTATION_ID,
                            RodaConstants.REPRESENTATION_NUMBER_OF_DATA_FILES,
                            RodaConstants.REPRESENTATION_NUMBER_OF_DATA_FOLDERS,
                            RodaConstants.REPRESENTATION_NUMBER_OF_DOCUMENTATION_FILES,
                            RodaConstants.REPRESENTATION_NUMBER_OF_SCHEMA_FILES,
                            RodaConstants.REPRESENTATION_STATES,
                            RodaConstants.REPRESENTATION_TITLE,
                            RodaConstants.REPRESENTATION_TYPE
                    )
            ).getResults();

            if (result.size() == 1) {
                return result.getFirst();
            }
        } catch (RequestNotValidException | GenericException e) {
            new MetsGeneratorException(e).warn(reportItem);
        }

        return null;
    }

    private static boolean containsResources(final StoragePath storagePath) throws AuthorizationDeniedException, GenericException {
        if (!storageService.hasDirectory(storagePath)) {
            return false;
        }

        try {
            return storageService.countResourcesUnderDirectory(storagePath, true) > 0;
        } catch (RequestNotValidException | NotFoundException e) {
            return false;
        }
    }

    private static boolean containsMetadata(final IndexedAIP aip, final IndexedRepresentation representation) throws AuthorizationDeniedException, GenericException {
        if (representation == null && aip.getFields().containsKey("descriptiveMetadataId")) {
            Object descriptiveMetadataIdField = aip.getFields().get("descriptiveMetadataId");
            if (descriptiveMetadataIdField instanceof List<?> list && !list.isEmpty()) {
                return true;
            }
        }

        if (representation != null && representation.getFields().containsKey("descriptiveMetadataId")) {
            Object descriptiveMetadataIdField = representation.getFields().get("descriptiveMetadataId");
            if (descriptiveMetadataIdField instanceof List<?> list && !list.isEmpty()) {
                return true;
            }
        }

        final String aipId = aip.getId();
        final String repId = representation != null ? representation.getId() : null;

        try {
            if (containsResources(ModelUtils.getDescriptiveMetadataDirectoryStoragePath(aipId, repId))) {
                return true;
            }
        } catch (RequestNotValidException ignored) {
        }

        try {
            if (containsResources(ModelUtils.getRepresentationPreservationMetadataStoragePath(aipId, repId))) {
                return true;
            }
        } catch (RequestNotValidException ignored) {
        }

        try {
            if (containsResources(ModelUtils.getRepresentationTechnicalMetadataContainerPath(aipId, repId))) {
                return true;
            }
        } catch (RequestNotValidException ignored) {
        }

        try {
            if (containsResources(getRepresentationSourceMetadataContainerPath(aipId, repId))) {
                return true;
            }
        } catch (RequestNotValidException ignored) {
        }

        try {
            return containsResources(getRepresentationRightsMetadataContainerPath(aipId, repId));
        } catch (RequestNotValidException ignored) {
            return false;
        }
    }

    private static boolean containsMetadataOther(final IndexedAIP aip, final IndexedRepresentation representation) throws AuthorizationDeniedException, GenericException {
        final String aipId = aip.getId();
        final String repId = representation != null ? representation.getId() : null;

        try {
            return containsResources(ModelUtils.getRepresentationOtherMetadataFolderStoragePath(aipId, repId));
        } catch (RequestNotValidException ignored) {
            return false;
        }
    }

    private static boolean containsSchemas(final IndexedAIP aip, final IndexedRepresentation representation) throws AuthorizationDeniedException, GenericException {
        if ((representation == null && aip.getNumberOfSchemaFiles() > 0) || (representation != null) && representation.getNumberOfSchemaFiles() > 0) {
            return true;
        }

        final String aipId = aip.getId();
        final String repId = representation != null ? representation.getId() : null;

        try {
            return containsResources(ModelUtils.getSchemasStoragePath(aipId, repId));
        } catch (RequestNotValidException ignored) {
            return false;
        }
    }

    private static boolean containsDocumentation(final IndexedAIP aip, final IndexedRepresentation representation) throws AuthorizationDeniedException, GenericException {
        if ((representation == null && aip.getNumberOfDocumentationFiles() > 0) || (representation != null) && representation.getNumberOfDocumentationFiles() > 0) {
            return true;
        }

        final String aipId = aip.getId();
        final String repId = representation != null ? representation.getId() : null;

        try {
            return containsResources(ModelUtils.getDocumentationStoragePath(aipId, repId));
        } catch (RequestNotValidException ignored) {
            return false;
        }
    }

    private static boolean containsSubmissions(final IndexedAIP aip, final IndexedRepresentation representation) throws AuthorizationDeniedException, GenericException {
        if (representation == null) {
            if (aip.getNumberOfSubmissionFiles() > 0) {
                return true;
            }

            try {
                return containsResources(ModelUtils.getSubmissionStoragePath(aip.getId()));
            } catch (RequestNotValidException ignored) {
                return false;
            }
        }

        return false;
    }

    private static boolean containsRepresentations(IndexedAIP aip, IndexedRepresentation representation) throws AuthorizationDeniedException, GenericException {
        if (representation == null) {
            if (aip.getFields().containsKey("hasRepresentations")) {
                Object hasRepresentationsField = aip.getFields().get("hasRepresentations");
                if (hasRepresentationsField instanceof Boolean hasRepresentationsValue) {
                    return hasRepresentationsValue;
                }
            }

            try {
                return containsResources(ModelUtils.getRepresentationsContainerPath(aip.getId()));
            } catch (RequestNotValidException ignored) {
                return false;
            }
        }

        return false;
    }

    private static boolean containsRepresentationData(IndexedAIP aip, IndexedRepresentation representation) throws AuthorizationDeniedException, GenericException {
        if (representation != null) {
            if (representation.getNumberOfDataFiles() > 0 || representation.getNumberOfDataFolders() > 0) {
                return true;
            }

            try {
                return containsResources(ModelUtils.getRepresentationDataStoragePath(aip.getId(), representation.getId()));
            } catch (RequestNotValidException ignored) {
                return false;
            }
        }

        return false;
    }

    private static IPHeader getIpHeader(IndexedAIP aip) {
        final IPHeader ipHeader = new IPHeader();

        IPAgent agentSoftware = new IPAgent("ETERNA", "CREATOR", null, METSEnums.CreatorType.OTHER, "SOFTWARE", "0.5.0", IPAgentNoteTypeEnum.SOFTWARE_VERSION);
        ipHeader.addAgent(agentSoftware);

        if (aip != null) {
            IPAgent agentCreator = new IPAgent(aip.getCreatedBy(), "CREATOR", null, METSEnums.CreatorType.INDIVIDUAL, null, aip.getCreatedBy(), IPAgentNoteTypeEnum.IDENTIFICATIONCODE);
            ipHeader.addAgent(agentCreator);
        }

        return ipHeader;
    }

    private static void processDataFiles(MetsWrapper metsWrapper, String aipId, String representationId, Report reportItem) throws AuthorizationDeniedException {
        try (CloseableIterable<OptionalWithCause<File>> oFileIterable = modelService.listFilesUnder(aipId, representationId, true)) {
            for (OptionalWithCause<File> oFile : oFileIterable) {
                if (!oFile.isPresent()) {
                    System.out.println("Could not retrieve representation data file, cause: " + oFile.getCause());
                    continue;
                }

                addDataFile(metsWrapper, oFile.get(), reportItem);
            }
        } catch (NotFoundException | IOException | RequestNotValidException | GenericException e) {
            new MetsGeneratorException("Could not process representation data files").warn(reportItem);
        }
    }

    private static void processDescriptiveMetadata(MetsWrapper metsWrapper, String aipId, String representationId, Report reportItem) throws AuthorizationDeniedException {
        try (CloseableIterable<OptionalWithCause<DescriptiveMetadata>> odmIterable = modelService.listDescriptiveMetadata(aipId, representationId)) {
            for (OptionalWithCause<DescriptiveMetadata> odm : odmIterable) {
                if (!odm.isPresent()) {
                    System.out.println("Could not retrieve descriptive metadata, cause: " + odm.getCause());
                    continue;
                }

                addDescriptiveMetadata(metsWrapper, odm.get(), reportItem);
            }
        } catch (NotFoundException | IOException | RequestNotValidException | GenericException e) {
            new MetsGeneratorException("Could not process descriptive metadata").warn(reportItem);
        }
    }

    private static void processPreservationMetadata(MetsWrapper metsWrapper, String aipId, String representationId, Report reportItem) throws AuthorizationDeniedException {
        try (CloseableIterable<OptionalWithCause<PreservationMetadata>> opmIterable = modelService.listPreservationMetadata(aipId, representationId)) {
            for (OptionalWithCause<PreservationMetadata> opm : opmIterable) {
                if (!opm.isPresent()) {
                    System.out.println("Could not retrieve preservation metadata, cause: " + opm.getCause());
                    continue;
                }

                addPreservationMetadata(metsWrapper, opm.get(), reportItem);
            }
        } catch (NotFoundException | IOException | RequestNotValidException | GenericException e) {
            new MetsGeneratorException("Could not process preservation metadata").warn(reportItem);
        }
    }

    private static void processTechnicalMetadata(MetsWrapper metsWrapper, String aipId, String representationId, Report reportItem) throws AuthorizationDeniedException {
        try {
            final StoragePath path = ModelUtils.getRepresentationTechnicalMetadataContainerPath(aipId, representationId);

            if (!storageService.exists(path)) {
                return;
            }

            try (CloseableIterable<Resource> resourceIterable = storageService.listResourcesUnderContainer(path, true)) {
                for (Resource resource : resourceIterable) {
                    addTechnicalMetadata(metsWrapper, aipId, representationId, resource, reportItem);
                }
            }
        } catch (NotFoundException | RequestNotValidException | IOException | GenericException e) {
            new MetsGeneratorException("Could not process technical metadata").warn(reportItem);
        }
    }

    private static void processSourceMetadata(MetsWrapper metsWrapper, String aipId, String representationId, Report reportItem) throws AuthorizationDeniedException {
        try {
            final StoragePath path = getRepresentationSourceMetadataContainerPath(aipId, representationId);

            if (!storageService.exists(path)) {
                return;
            }

            try (CloseableIterable<Resource> resourceIterable = storageService.listResourcesUnderContainer(path, true)) {
                for (Resource resource : resourceIterable) {
                    addSourceMetadata(metsWrapper, aipId, representationId, resource, reportItem);
                }
            }
        } catch (NotFoundException | RequestNotValidException | IOException | GenericException e) {
            new MetsGeneratorException("Could not process source metadata").warn(reportItem);
        }
    }

    private static void processRightsMetadata(MetsWrapper metsWrapper, String aipId, String representationId, Report reportItem) throws AuthorizationDeniedException {
        try {
            final StoragePath path = getRepresentationRightsMetadataContainerPath(aipId, representationId);

            if (!storageService.exists(path)) {
                return;
            }

            try (CloseableIterable<Resource> resourceIterable = storageService.listResourcesUnderContainer(path, true)) {
                for (Resource resource : resourceIterable) {
                    addRightsMetadata(metsWrapper, aipId, representationId, resource, reportItem);
                }
            }
        } catch (NotFoundException | RequestNotValidException | IOException | GenericException e) {
            new MetsGeneratorException("Could not process rights metadata").warn(reportItem);
        }
    }

    private static void processOtherMetadata(final MetsWrapper metsWrapper, final String aipId, final String representationId, final Report reportItem) throws AuthorizationDeniedException {
        try (CloseableIterable<OptionalWithCause<OtherMetadata>> opmIterable = modelService.listOtherMetadata(aipId, representationId)) {
            for (OptionalWithCause<OtherMetadata> opm : opmIterable) {
                if (!opm.isPresent()) {
                    System.out.println("Could not retrieve other metadata, cause: " + opm.getCause());
                    continue;
                }

                OtherMetadata otherMetadata = opm.get();
                addOtherMetadata(metsWrapper, otherMetadata, reportItem);
            }
        } catch (NotFoundException | IOException | RequestNotValidException | GenericException e) {
            new MetsGeneratorException("Could not process other metadata").warn(reportItem);
        }
    }

    private static void processSchemas(final MetsWrapper metsWrapper, final String aipId, final String representationId, final Report reportItem) throws AuthorizationDeniedException {
        try {
            final StoragePath path = ModelUtils.getSchemasStoragePath(aipId, representationId);

            if (!storageService.exists(path)) {
                return;
            }

            try (CloseableIterable<Resource> resourceIterable = storageService.listResourcesUnderContainer(path, true)) {
                for (Resource resource : resourceIterable) {
                    addSchema(metsWrapper, aipId, representationId, resource, reportItem);
                }
            }
        } catch (NotFoundException | RequestNotValidException | IOException | GenericException e) {
            new MetsGeneratorException("Could not process schemas").warn(reportItem);
        }
    }

    private static void processDocumentation(final MetsWrapper metsWrapper, final String aipId, final String representationId, final Report reportItem) throws AuthorizationDeniedException {
        try {
            final StoragePath path = ModelUtils.getDocumentationStoragePath(aipId, representationId);

            if (!storageService.exists(path)) {
                return;
            }

            try (CloseableIterable<Resource> resourceIterable = storageService.listResourcesUnderContainer(path, true)) {
                for (Resource resource : resourceIterable) {
                    addDocumentation(metsWrapper, aipId, representationId, resource, reportItem);
                }
            }
        } catch (NotFoundException | RequestNotValidException | IOException | GenericException e) {
            new MetsGeneratorException("Could not process documentation").warn(reportItem);
        }
    }

    private static void processRepresentations(final MetsWrapper metsWrapper, final AIP aip, final CSIPProfile profile, final Report reportItem) throws AuthorizationDeniedException, MetsGeneratorException {
        final String aipId = aip.getId();

        for (Representation rep : aip.getRepresentations()) {
            try {
                final String repId = rep.getId();
                final MetsWrapper repMetsWrapper = METSUtils.generateMETS(aip, rep, profile, reportItem);
                repMetsWrapper.getMainDiv().setTYPE(rep.getType()); // TODO: Check if rep.getType is the same as representation.getStatus in commons ip: https://github.com/keeps/commons-ip/blob/9f2ead8a297d6515d2018c4351de9cf89a1efaff/src/main/java/org/roda_project/commons_ip2/model/impl/eark/EARKUtils.java#L265

                final Mets mets = repMetsWrapper.getMets();

                final StoragePath repStoragePath = ModelUtils.getRepresentationStoragePath(aipId, repId);
                final StoragePath metsStoragePath = DefaultStoragePath.parse(repStoragePath, IPConstants.METS_FILE);

                MetsContentPayload contentPayload = new MetsContentPayload(mets, false);
                storageService.updateBinaryContent(metsStoragePath, contentPayload, false, true);

                final Path aipPath = METSUtils.storagePathToPath(ModelUtils.getAIPStoragePath(aipId)).normalize();
                final Path repPath = METSUtils.storagePathToPath(ModelUtils.getRepresentationStoragePath(aipId, repId)).normalize();

                final Path filePath = METSUtils.storagePathToPath(metsStoragePath).normalize();

                final Path aipRelativized = aipPath.relativize(filePath);
                final Path repRelativized = repPath.relativize(filePath);

                final String mimeType = "application/xml";

                final IPFile ipFile = new IPFile(aipRelativized, repRelativized, contentPayload.getSize(), contentPayload.getChecksum(), mimeType, FileTime.from(Instant.now()));

                addRepresentation(metsWrapper, rep, ipFile);
            } catch (RequestNotValidException | NotFoundException | GenericException e) {
                new MetsGeneratorException("Could not process representations").warn(reportItem);
            }
        }
    }

    private static void addDataFile(final MetsWrapper metsWrapper, final File file, final Report reportItem) throws AuthorizationDeniedException {
        IPFile ipFile;
        try {
            ipFile = CommonsIPConverter.convert(file);
        } catch (RequestNotValidException | NotFoundException | IOException | NoSuchAlgorithmException |
                 GenericException e) {
            new MetsGeneratorException("Could not read representation data file").warn(reportItem);
            return;
        }

        final String filePath = ipFile.getRelativePath().toString();

        final FileType fileType = new FileType();
        fileType.setID(Utils.generateRandomAndPrefixedFileID());

        fileType.setMIMETYPE(ipFile.getMimeType());
        if (ipFile.getCreationTime() != null) {
            fileType.setCREATED(CommonsIPConverter.convert(ipFile.getCreationTime()));
        }

        fileType.setSIZE(ipFile.getSize());
        fileType.setCHECKSUM(ipFile.getChecksum());
        fileType.setCHECKSUMTYPE(ipFile.getChecksumAlgorithm());

        // add to file section
        final FileType.FLocat fileLocation = new FileType.FLocat();
        fileLocation.setType(IPConstants.METS_TYPE_SIMPLE);
        fileLocation.setLOCTYPE(METSEnums.LocType.URL.toString());
        fileLocation.setHref(org.roda_project.commons_ip2.utils.METSUtils.encodeHref(filePath));

        fileType.getFLocat().add(fileLocation);
        metsWrapper.getDataFileGroup().getFile().add(fileType);

        // add to struct map
        if (metsWrapper.getDataDiv().getFptr().isEmpty()) {
            final DivType.Fptr fptr = new DivType.Fptr();
            fptr.setFILEID(metsWrapper.getDataFileGroup());
            metsWrapper.getDataDiv().getFptr().add(fptr);
        }
    }

    private static void addDescriptiveMetadata(final MetsWrapper metsWrapper, final DescriptiveMetadata descriptiveMetadata, final Report reportItem) throws AuthorizationDeniedException {
        IPDescriptiveMetadata metadata;
        try {
            metadata = CommonsIPConverter.convert(descriptiveMetadata);
        } catch (RequestNotValidException | NotFoundException | IOException | NoSuchAlgorithmException |
                 GenericException e) {
            new MetsGeneratorException("Could not read descriptive metadata").warn(reportItem);
            return;
        }

        final IPFile ipFile = (IPFile) metadata.getMetadata();

        final MdSecType dmdSec = new MdSecType();
        dmdSec.setSTATUS(metadata.getMetadataStatus().toString());
        dmdSec.setID(generateRandomAndPrefixedUUID());

        final MdSecType.MdRef mdRef = createMdRef(metadata.getId(), ipFile.getRelativePath().toString());

        final String mdType = metadata.getMetadataType().getType().getType();
        final String mdOtherType = metadata.getMetadataType().getOtherType();

        mdRef.setMDTYPE(mdType);
        if (StringUtils.isNotBlank(mdOtherType)) {
            mdRef.setOTHERMDTYPE(mdOtherType);
        }
        mdRef.setMDTYPEVERSION(metadata.getMetadataVersion());

        mdRef.setMIMETYPE(ipFile.getMimeType());
        if (ipFile.getCreationTime() != null) {
            mdRef.setCREATED(CommonsIPConverter.convert(ipFile.getCreationTime()));
        }

        mdRef.setSIZE(ipFile.getSize());
        mdRef.setCHECKSUM(ipFile.getChecksum());
        mdRef.setCHECKSUMTYPE(ipFile.getChecksumAlgorithm());

        dmdSec.setCREATED(mdRef.getCREATED());

        metsWrapper.getMetadataDiv().getDMDID().add(dmdSec);

        dmdSec.setMdRef(mdRef);
        metsWrapper.getMets().getDmdSec().add(dmdSec);
    }

    private static void addPreservationMetadata(final MetsWrapper metsWrapper, final PreservationMetadata preservationMetadata, final Report reportItem) throws AuthorizationDeniedException {
        IPMetadata ipMetadata;
        try {
            ipMetadata = CommonsIPConverter.convert(preservationMetadata);
        } catch (RequestNotValidException | NotFoundException | IOException | NoSuchAlgorithmException |
                 GenericException | DatatypeConfigurationException e) {
            new MetsGeneratorException("Could not read preservation metadata").warn(reportItem);
            return;
        }

        final IPFile ipFile = (IPFile) ipMetadata.getMetadata();
        final String preservationMetadataPath = ipFile.getRelativePath().toString();

        final MdSecType digiprovMD = new MdSecType();
        digiprovMD.setSTATUS(ipMetadata.getMetadataStatus().toString());
        digiprovMD.setID(Utils.generateRandomAndPrefixedUUID());
        final MdSecType.MdRef mdRef = createMdRef(preservationMetadata.getId(), preservationMetadataPath);
        mdRef.setMDTYPE(ipMetadata.getMetadataType().getType().getType());
        if (StringUtils.isNotBlank(ipMetadata.getMetadataType().getOtherType())) {
            mdRef.setOTHERMDTYPE(ipMetadata.getMetadataType().getOtherType());
        }

        // set mimetype, date creation, etc.
        mdRef.setMIMETYPE(ipFile.getMimeType());
        if (ipFile.getCreationTime() != null) {
            mdRef.setCREATED(CommonsIPConverter.convert(ipFile.getCreationTime()));
        }

        mdRef.setSIZE(ipFile.getSize());
        mdRef.setCHECKSUM(ipFile.getChecksum());
        mdRef.setCHECKSUMTYPE(ipFile.getChecksumAlgorithm());

        // structural map info.
        metsWrapper.getMetadataDiv().getADMID().add(digiprovMD);

        digiprovMD.setMdRef(mdRef);
        metsWrapper.getMets().getAmdSec().getFirst().getDigiprovMD().add(digiprovMD);
    }

    private static void addTechnicalMetadata(final MetsWrapper metsWrapper, final String aipId, final String representationId, final Resource metadataResource, final Report reportItem) throws AuthorizationDeniedException {
        IPFile ipFile;
        try {
            ipFile = createIPFile(metadataResource.getStoragePath(), aipId, representationId);
        } catch (RequestNotValidException | NotFoundException | IOException | NoSuchAlgorithmException |
                 GenericException e) {
            new MetsGeneratorException("Could not read technical metadata").warn(reportItem);
            return;
        }

        final IPMetadata metadata = new IPMetadata(ipFile, new MetadataType(MetadataType.MetadataTypeEnum.OTHER));

        final MdSecType techMD = new MdSecType();
        techMD.setSTATUS(metadata.getMetadataStatus().toString());
        techMD.setID(Utils.generateRandomAndPrefixedUUID());

        final MdSecType.MdRef mdRef = createMdRef(metadata.getId(), ipFile.getRelativePath().toString());
        mdRef.setMDTYPE(metadata.getMetadataType().getType().getType());
        if (StringUtils.isNotBlank(metadata.getMetadataType().getOtherType())) {
            mdRef.setOTHERMDTYPE(metadata.getMetadataType().getOtherType());
        }

        // set mimetype, date creation, etc.
        mdRef.setMIMETYPE(ipFile.getMimeType());
        if (ipFile.getCreationTime() != null) {
            mdRef.setCREATED(CommonsIPConverter.convert(ipFile.getCreationTime()));
        }

        mdRef.setSIZE(ipFile.getSize());
        mdRef.setCHECKSUM(ipFile.getChecksum());
        mdRef.setCHECKSUMTYPE(ipFile.getChecksumAlgorithm());

        // structural map info.
        if (metsWrapper.getMetadataDiv() != null) {
            metsWrapper.getMetadataDiv().getADMID().add(techMD);
        }

        techMD.setMdRef(mdRef);
        metsWrapper.getMets().getAmdSec().getFirst().getTechMD().add(techMD);
    }

    private static void addSourceMetadata(final MetsWrapper metsWrapper, final String aipId, final String representationId, final Resource metadataResource, final Report reportItem) throws AuthorizationDeniedException {
        IPFile ipFile;
        try {
            ipFile = createIPFile(metadataResource.getStoragePath(), aipId, representationId);
        } catch (RequestNotValidException | NotFoundException | IOException | NoSuchAlgorithmException |
                 GenericException e) {
            new MetsGeneratorException("Could not read source metadata").warn(reportItem);
            return;
        }

        final IPMetadata metadata = new IPMetadata(ipFile, new MetadataType(MetadataType.MetadataTypeEnum.OTHER));

        final MdSecType sourceMD = new MdSecType();
        sourceMD.setSTATUS(metadata.getMetadataStatus().toString());
        sourceMD.setID(Utils.generateRandomAndPrefixedUUID());

        final MdSecType.MdRef mdRef = createMdRef(metadata.getId(), ipFile.getRelativePath().toString());
        mdRef.setMDTYPE(metadata.getMetadataType().getType().getType());
        if (StringUtils.isNotBlank(metadata.getMetadataType().getOtherType())) {
            mdRef.setOTHERMDTYPE(metadata.getMetadataType().getOtherType());
        }

        // set mimetype, date creation, etc.
        mdRef.setMIMETYPE(ipFile.getMimeType());
        if (ipFile.getCreationTime() != null) {
            mdRef.setCREATED(CommonsIPConverter.convert(ipFile.getCreationTime()));
        }

        mdRef.setSIZE(ipFile.getSize());
        mdRef.setCHECKSUM(ipFile.getChecksum());
        mdRef.setCHECKSUMTYPE(ipFile.getChecksumAlgorithm());

        // structural map info.
        if (metsWrapper.getMetadataDiv() != null) {
            metsWrapper.getMetadataDiv().getADMID().add(sourceMD);
        }

        sourceMD.setMdRef(mdRef);
        metsWrapper.getMets().getAmdSec().getFirst().getSourceMD().add(sourceMD);
    }

    private static void addRightsMetadata(final MetsWrapper metsWrapper, final String aipId, final String representationId, final Resource metadataResource, final Report reportItem) throws AuthorizationDeniedException {
        IPFile ipFile;
        try {
            ipFile = createIPFile(metadataResource.getStoragePath(), aipId, representationId);
        } catch (RequestNotValidException | NotFoundException | IOException | NoSuchAlgorithmException |
                 GenericException e) {
            new MetsGeneratorException("Could not read source metadata").warn(reportItem);
            return;
        }

        final IPMetadata metadata = new IPMetadata(ipFile, new MetadataType(MetadataType.MetadataTypeEnum.OTHER));

        final MdSecType rightsMD = new MdSecType();
        rightsMD.setSTATUS(metadata.getMetadataStatus().toString());
        rightsMD.setID(Utils.generateRandomAndPrefixedUUID());

        final MdSecType.MdRef mdRef = createMdRef(metadata.getId(), ipFile.getRelativePath().toString());
        mdRef.setMDTYPE(metadata.getMetadataType().getType().getType());
        if (StringUtils.isNotBlank(metadata.getMetadataType().getOtherType())) {
            mdRef.setOTHERMDTYPE(metadata.getMetadataType().getOtherType());
        }

        // set mimetype, date creation, etc.
        mdRef.setMIMETYPE(ipFile.getMimeType());
        if (ipFile.getCreationTime() != null) {
            mdRef.setCREATED(CommonsIPConverter.convert(ipFile.getCreationTime()));
        }

        mdRef.setSIZE(ipFile.getSize());
        mdRef.setCHECKSUM(ipFile.getChecksum());
        mdRef.setCHECKSUMTYPE(ipFile.getChecksumAlgorithm());

        // structural map info.
        if (metsWrapper.getMetadataDiv() != null) {
            metsWrapper.getMetadataDiv().getADMID().add(rightsMD);
        }

        rightsMD.setMdRef(mdRef);
        metsWrapper.getMets().getAmdSec().getFirst().getRightsMD().add(rightsMD);
    }

    private static void addOtherMetadata(final MetsWrapper metsWrapper, final OtherMetadata otherMetadata, final Report reportItem) throws AuthorizationDeniedException {
        IPMetadata metadata;
        try {
            metadata = CommonsIPConverter.convert(otherMetadata);
        } catch (RequestNotValidException | NotFoundException | IOException | NoSuchAlgorithmException |
                 GenericException e) {
            new MetsGeneratorException("Could not read other metadata").warn(reportItem);
            return;
        }

        final IPFile ipFile = (IPFile) metadata.getMetadata();
        final String otherMetadataPath = ipFile.getRelativePath().toString();

        final MdSecType dmdSec = new MdSecType();
        dmdSec.setID(Utils.generateRandomAndPrefixedUUID());

        final MdSecType.MdRef mdRef = createMdRef(metadata.getId(), otherMetadataPath);
        mdRef.setMDTYPE("OTHER");

        // set mimetype, date creation, etc.
        mdRef.setMIMETYPE(ipFile.getMimeType());
        if (ipFile.getCreationTime() != null) {
            mdRef.setCREATED(CommonsIPConverter.convert(ipFile.getCreationTime()));
        }

        mdRef.setSIZE(ipFile.getSize());
        mdRef.setCHECKSUM(ipFile.getChecksum());
        mdRef.setCHECKSUMTYPE(ipFile.getChecksumAlgorithm());

        // also set date created in dmdSec elem
        dmdSec.setCREATED(mdRef.getCREATED());

        // structural map info.
        if (otherMetadata.getRepresentationId() != null) {
            metsWrapper.getMetadataDiv().getDMDID().add(dmdSec);
        } else {
            metsWrapper.getOtherMetadataDiv().getDMDID().add(dmdSec);
        }

        dmdSec.setMdRef(mdRef);
        metsWrapper.getMets().getDmdSec().add(dmdSec);
    }

    private static void addSchema(final MetsWrapper metsWrapper, final String aipId, final String representationId, final Resource resource, final Report reportItem) throws AuthorizationDeniedException {
        IPFile ipFile;
        try {
            ipFile = createIPFile(resource.getStoragePath(), aipId, representationId);
        } catch (RequestNotValidException | NotFoundException | IOException | NoSuchAlgorithmException |
                 GenericException e) {
            new MetsGeneratorException("Could not read schema").warn(reportItem);
            return;
        }

        final String schemaFilePath = ipFile.getRelativePath().toString();

        final FileType file = new FileType();
        file.setID(Utils.generateRandomAndPrefixedUUID());

        file.setMIMETYPE(ipFile.getMimeType());
        if (ipFile.getCreationTime() != null) {
            file.setCREATED(CommonsIPConverter.convert(ipFile.getCreationTime()));
        }

        file.setSIZE(ipFile.getSize());
        file.setCHECKSUM(ipFile.getChecksum());
        file.setCHECKSUMTYPE(ipFile.getChecksumAlgorithm());

        // add to file section
        final FileType.FLocat fileLocation = org.roda_project.commons_ip2.utils.METSUtils.createFileLocation(schemaFilePath);
        file.getFLocat().add(fileLocation);
        if (metsWrapper.getSchemasFileGroup() != null) {
            metsWrapper.getSchemasFileGroup().getFile().add(file);
        }

        // add to struct map
        if (metsWrapper.getSchemasDiv() != null && metsWrapper.getSchemasDiv().getFptr().isEmpty()) {
            final DivType.Fptr fptr = new DivType.Fptr();
            fptr.setFILEID(metsWrapper.getSchemasFileGroup());
            metsWrapper.getSchemasDiv().getFptr().add(fptr);
        }
    }

    private static void addDocumentation(final MetsWrapper metsWrapper, final String aipId, final String representationId, final Resource resource, final Report reportItem) throws AuthorizationDeniedException {
        IPFile ipFile;
        try {
            ipFile = createIPFile(resource.getStoragePath(), aipId, representationId);
        } catch (RequestNotValidException | NotFoundException | IOException | NoSuchAlgorithmException |
                 GenericException e) {
            new MetsGeneratorException("Could not read documentation").warn(reportItem);
            return;
        }

        final String schemaFilePath = ipFile.getRelativePath().toString();

        final FileType file = new FileType();
        file.setID(Utils.generateRandomAndPrefixedUUID());

        file.setMIMETYPE(ipFile.getMimeType());
        if (ipFile.getCreationTime() != null) {
            file.setCREATED(CommonsIPConverter.convert(ipFile.getCreationTime()));
        }

        file.setSIZE(ipFile.getSize());
        file.setCHECKSUM(ipFile.getChecksum());
        file.setCHECKSUMTYPE(ipFile.getChecksumAlgorithm());

        // add to file section
        final FileType.FLocat fileLocation = org.roda_project.commons_ip2.utils.METSUtils.createFileLocation(schemaFilePath);
        file.getFLocat().add(fileLocation);
        metsWrapper.getDocumentationFileGroup().getFile().add(file);

        // add to struct map
        if (metsWrapper.getDocumentationDiv().getFptr().isEmpty()) {
            final DivType.Fptr fptr = new DivType.Fptr();
            fptr.setFILEID(metsWrapper.getDocumentationFileGroup());
            metsWrapper.getDocumentationDiv().getFptr().add(fptr);
        }
    }

    private static void addRepresentation(final MetsWrapper metsWrapper, final Representation representation, final IPFile ipFile) {
        final String href = org.roda_project.commons_ip2.utils.METSUtils.encodeHref(ipFile.getPath().toString());

        // create mets pointer
        final DivType.Mptr mptr = new DivType.Mptr();
        mptr.setLOCTYPE(METSEnums.LocType.URL.toString());
        mptr.setType(IPConstants.METS_TYPE_SIMPLE);
        mptr.setHref(href);

        // create file
        final FileType fileType = new FileType();
        fileType.setID(Utils.generateRandomAndPrefixedFileID());

        fileType.setMIMETYPE(ipFile.getMimeType());
        if (ipFile.getCreationTime() != null) {
            fileType.setCREATED(CommonsIPConverter.convert(ipFile.getCreationTime()));
        }

        fileType.setSIZE(ipFile.getSize());
        fileType.setCHECKSUM(ipFile.getChecksum());
        fileType.setCHECKSUMTYPE(ipFile.getChecksumAlgorithm());

        // add to file group and then to file section
        final MetsType.FileSec.FileGrp fileGroup = new MetsType.FileSec.FileGrp();
        fileGroup.setID(Utils.generateRandomAndPrefixedUUID());
        fileGroup.setUSE(IPConstants.REPRESENTATIONS_WITH_FIRST_LETTER_CAPITAL + "/" + representation.getId());

        final FileType.FLocat fileLocation = new FileType.FLocat();
        fileLocation.setType(IPConstants.METS_TYPE_SIMPLE);
        fileLocation.setLOCTYPE(METSEnums.LocType.URL.toString());
        fileLocation.setHref(href);

        fileType.getFLocat().add(fileLocation);
        fileGroup.getFile().add(fileType);
        metsWrapper.getMets().getFileSec().getFileGrp().add(fileGroup);

        // set mets pointer
        final DivType representationDiv = new DivType();
        representationDiv.setID(Utils.generateRandomAndPrefixedUUID());
        representationDiv.setLABEL(IPConstants.REPRESENTATIONS_WITH_FIRST_LETTER_CAPITAL + "/" + representation.getId());
        representationDiv.getMptr().add(mptr);

        mptr.setTitle(fileGroup.getID());
        metsWrapper.getMainDiv().getDiv().add(representationDiv);
    }

    private static MdSecType.MdRef createMdRef(final String id, final String metadataPath) {
        final MdSecType.MdRef mdRef = new MdSecType.MdRef();
        mdRef.setID(METSEnums.FILE_ID_PREFIX + escapeNCName(id));
        mdRef.setType(IPConstants.METS_TYPE_SIMPLE);
        mdRef.setLOCTYPE(METSEnums.LocType.URL.toString());
        mdRef.setHref(org.roda_project.commons_ip2.utils.METSUtils.encodeHref(metadataPath));
        return mdRef;
    }

    private static String escapeNCName(final String id) {
        return id.replaceAll("[:@$%&/+,;\\s]", "_");
    }

    private static StoragePath getRepresentationSourceMetadataContainerPath(final String aipId, final String representationId) throws RequestNotValidException {
        List<String> path = new ArrayList<>(ModelUtils.getRepresentationStoragePath(aipId, representationId).asList());
        path.addAll(List.of(RodaConstants.STORAGE_DIRECTORY_METADATA, "source"));
        return DefaultStoragePath.parse(path);
    }

    private static StoragePath getRepresentationRightsMetadataContainerPath(final String aipId, final String representationId) throws RequestNotValidException {
        List<String> path = new ArrayList<>(ModelUtils.getRepresentationStoragePath(aipId, representationId).asList());
        path.addAll(List.of(RodaConstants.STORAGE_DIRECTORY_METADATA, "rights"));
        return DefaultStoragePath.parse(path);
    }

    public static Path storagePathToPath(final StoragePath storagePath) {
        final List<String> storagePathList = storagePath.asList();
        final Iterator<String> iterator = storagePathList.iterator();
        Path path = Path.of(iterator.next());
        while (iterator.hasNext()) {
            path = path.resolve(iterator.next());
        }

        return path;
    }

    public static IPFile createIPFile(final StoragePath storagePath, final String aipId, final String representationId) throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException, IOException, NoSuchAlgorithmException {
        final Path aipPath = METSUtils.storagePathToPath(ModelUtils.getAIPStoragePath(aipId)).normalize();
        final Path repPath = METSUtils.storagePathToPath(ModelUtils.getRepresentationStoragePath(aipId, representationId)).normalize();

        final Path basePath = representationId == null ? aipPath : repPath;

        final Path filePath = METSUtils.storagePathToPath(storagePath).normalize();

        final Path aipRelativized = aipPath.relativize(filePath);
        final Path basePathRelativized = basePath.relativize(filePath);

        Binary binary = storageService.getBinary(storagePath);

        final FileTime creationTime = storageService.getCreationTime(storagePath);

        String mimeType;
        try {
            if (useFileContentMimeTypeDetection) {
                try (InputStream inputStream = binary.getContent().createInputStream()) {
                    final Metadata tikaMetadata = new Metadata();
                    tikaMetadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filePath.getFileName().toString());
                    final TikaConfig tika = new TikaConfig();
                    mimeType = tika.getDetector().detect(TikaInputStream.get(inputStream), tikaMetadata).toString();
                }
            } else {
                mimeType = MimeTypeUtils.getMimeType(filePath);
            }
        } catch (TikaException e) {
            throw new GenericException(e);
        }

        try (InputStream inputStream = binary.getContent().createInputStream()) {
            final String checksum = FileUtility.checksum(inputStream, IPConstants.CHECKSUM_SHA_256_ALGORITHM);
            return new IPFile(aipRelativized, basePathRelativized, binary.getSizeInBytes(), checksum, mimeType, creationTime);
        }
    }
}
