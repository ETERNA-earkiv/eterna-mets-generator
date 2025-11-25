package se.whitered.eterna.plugins.metsgenerator;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.IsRODAObject;
import org.roda.core.data.v2.LiteOptionalWithCause;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.Representation;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.plugins.AbstractPlugin;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.plugins.PluginHelper;
import org.roda.core.plugins.PluginManager;
import org.roda.core.plugins.PluginOrchestrator;
import org.roda.core.plugins.RODAObjectsProcessingLogic;
import org.roda.core.plugins.orchestrate.JobPluginInfo;
import org.roda.core.plugins.orchestrate.JobsHelper;
import org.roda.core.plugins.orchestrate.pekko.PekkoBackgroundWorkerActor;
import org.roda.core.plugins.orchestrate.pekko.PekkoJobStateInfoActor;
import org.roda.core.plugins.orchestrate.pekko.PekkoWorkerActor;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.StorageService;
import org.roda_project.commons_ip.utils.IPException;
import org.roda_project.commons_ip2.mets_v1_12.beans.Mets;
import org.roda_project.commons_ip2.model.IPConstants;
import org.roda_project.commons_ip2.model.MetsWrapper;
import se.whitered.eterna.plugins.metsgenerator.exceptions.MetsGeneratorException;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MetsGeneratorPlugin extends AbstractPlugin<AIP> {
    private static final String PLUGIN_PARAMS_IP_PROFILE = "parameter.ip_profile";
    private static final List<String> PLUGIN_PARAMS_IP_PROFILE_VALUES = Arrays.asList("sip", "aip", "dip");

    private static final Properties props = new Properties();

    private static final Map<String, PluginParameter> pluginParameters = new HashMap<>();

    static {
        pluginParameters.put(
                PLUGIN_PARAMS_IP_PROFILE,
                PluginParameter.getBuilder(PLUGIN_PARAMS_IP_PROFILE, "IP Profile", PluginParameterType.DROPDOWN)
                        .isMandatory(false)
                        .withDescription("An E-ARK CSIP has an associated profile to describe whether the package is intended for submission, archival or dissemination. Please choose which profile you want to generate METS files for.")
                        .withPossibleValues(PLUGIN_PARAMS_IP_PROFILE_VALUES)
                        .withDefaultValue("sip")
                        .build()
        );

        try (InputStream in = MetsGeneratorPlugin.class.getResourceAsStream("/plugin.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MetsGeneratorPlugin() {
        super();
    }

    /**
     * <p>Returns the name of this {@link Plugin}</p>
     *
     * @return {@link String} containing the name of this {@link Plugin}
     */
    @Override
    public String getName() {
        // Get name from pom.xml
        return props.getProperty("plugin.name");
    }

    /**
     * <p>Returns the version of this {@link Plugin}.</p>
     * <p>Called by {@link AbstractPlugin#getVersion()}</p>
     *
     * @return {@link String} containing the version of this {@link Plugin}
     */
    @Override
    public String getVersionImpl() {
        // Get version from pom.xml
        return props.getProperty("plugin.version");
    }

    /**
     * <p>Returns a description of this {@link Plugin}.</p>
     *
     * @return {@link String} containing a description of this {@link Plugin}
     */
    @Override
    public String getDescription() {
        return props.getProperty("plugin.description");
    }

    /**
     * <p>Returns a {@link List} of {@link PluginParameter} containing the parameters of this {@link Plugin}</p>
     *
     * @return {@link List} of {@link PluginParameter} containing the parameters of this {@link Plugin}
     */
    @Override
    public List<PluginParameter> getParameters() {
        return new ArrayList<>(pluginParameters.values());
    }

    /**
     * <p>Used to set the values of parameters</p>
     *
     * @param parameters {@link Map Map&lt;String, String&gt;} with the parameters names as keys and their assigned values as values
     * @throws InvalidParameterException Thrown if the value of a parameter is invalid
     */
    @Override
    public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            switch (entry.getKey()) {
                case RodaConstants.PLUGIN_PARAMS_JOB_ID:
                    break;

                case PLUGIN_PARAMS_IP_PROFILE:
                    if (!PLUGIN_PARAMS_IP_PROFILE_VALUES.contains(entry.getValue())) {
                        throw new InvalidParameterException(String.format("Invalid value for parameter \"%s\"", PLUGIN_PARAMS_IP_PROFILE));
                    }
                    break;

                default:
                    throw new InvalidParameterException(String.format("Invalid parameter \"%s\"", entry.getKey()));
            }
        }

        super.setParameterValues(parameters);
    }

    /**
     * <p>Method used for inbound parameter validation.</p>
     * <p>Returns true if the values of all parameters are correct, otherwise false.</p>
     * <p>Called by {@link JobsHelper}.validateJobPluginInformation(Job job)</p>
     *
     * @return boolean - Returns true if the values of all parameters are correct, otherwise false
     */
    @Override
    public boolean areParameterValuesValid() {
        boolean valid = true;
        for (Map.Entry<String, String> entry : this.getParameterValues().entrySet()) {
            switch (entry.getKey()) {
                case PLUGIN_PARAMS_IP_PROFILE -> valid = PLUGIN_PARAMS_IP_PROFILE_VALUES.contains(entry.getValue());
                default -> valid = false;
            }

            if (!valid) {
                break;
            }
        }

        return valid;
    }

    /**
     * <p>Returns the {@link PreservationEventType} the action of this {@link Plugin} is associated with.</p>
     * <p>Called by {@link PluginHelper}.createPluginEvent</p>
     *
     * @return {@link PreservationEventType} associated with the action of this {@link Plugin}
     */
    @Override
    public PreservationEventType getPreservationEventType() {
        return PreservationEventType.UPDATE;
    }

    /**
     * <p>Returns a description describing the preservation event associated with the action of this {@link Plugin}.</p>
     * <p>Called by {@link PluginHelper}.createPluginEvent</p>
     *
     * @return {@link String} containing a description of the preservation event
     */
    @Override
    public String getPreservationEventDescription() {
        return null;
    }

    /**
     * <p>Returns the message used in the preservation event to describe a successful outcome.</p>
     * <p>Called by {@link PluginHelper}.createPluginEvent</p>
     *
     * @return {@link String} containing a message used in the preservation event to describe a successful outcome
     */
    @Override
    public String getPreservationEventSuccessMessage() {
        return null;
    }

    /**
     * <p>Returns the message used in the preservation event to describe a failed outcome.</p>
     * <p>Called by {@link PluginHelper}.createPluginEvent</p>
     *
     * @return {@link String} containing a message used in the preservation event to describe a failed outcome
     */
    @Override
    public String getPreservationEventFailureMessage() {
        return null;
    }

    /**
     * <p>Returns the message used in the preservation event to show that the plugin skipped execution.</p>
     * <p>Called by {@link PluginHelper}.createPluginEvent</p>
     *
     * @return {@link String} containing a message used in the preservation event to describe skipped execution
     */
    @Override
    public String getPreservationEventSkippedMessage() {
        return super.getPreservationEventSkippedMessage();
    }

    /**
     * <p>Returns the {@link PluginType} of this {@link Plugin}.</p>
     * <p>Used for grouping plugins based on type in ETERNAs UI.</p>
     *
     * @return {@link PluginType} describing what type of plugin this is
     */
    @Override
    public PluginType getType() {
        return PluginType.MISC;
    }

    /**
     * <p>Returns the plugin categories of this {@link Plugin}.</p>
     * <p>Used for grouping plugins based on category in ETERNAs UI.</p>
     *
     * @return {@link List<String>} containing the categories of this plugin
     */
    @Override
    public List<String> getCategories() {
        return Collections.singletonList(RodaConstants.PLUGIN_CATEGORY_MAINTENANCE);
    }

    /**
     * <p>Returns new instance of this plugin.</p>
     * <p>
     * Used to pass a new instance of the plugin to the {@link PluginOrchestrator}
     * for thread-safety in parallel execution.
     * </p>
     * <p>Called by {@link PluginManager#getPlugin(String pluginID)}</p>
     *
     * @return new instance of {@link MetsGeneratorPlugin}
     */
    @Override
    public Plugin<AIP> cloneMe() {
        return new MetsGeneratorPlugin();
    }

    /**
     * <p>Method called to initialize this {@link Plugin}.</p>
     * <p>Called by {@link PluginManager#registerPlugin(Plugin plugin)} on startup or when a new plugin is placed in the plugins directory.</p>
     *
     * @throws PluginException thrown if initialization of the plugin failed
     */
    @Override
    public void init() throws PluginException {
        MimeTypeUtils.init();
        System.out.println("Init METS Generator");
    }

    /**
     * <p>Method that returns a list of classes of objects that this plugin can be executed against.</p>
     * <p>For example: {@link AIP}, {@link Representation}, {@link File}.</p>
     *
     * @return List of classes extending {@link IsRODAObject} that this plugin can be executed against
     */
    @Override
    public List<Class<AIP>> getObjectClasses() {
        return Collections.singletonList(AIP.class);
    }

    /**
     * <p>Method called before executing the main action of the plugin.</p>
     * <p>Useful to setup the job before executing a job with many sub tasks.</p>
     * <p>Called by {@link PekkoJobStateInfoActor}.handleBeforeAllExecuteIsReady(Object msg)</p>
     *
     * @param indexService   reference to {@link IndexService}
     * @param modelService   reference to {@link ModelService}
     * @param storageService reference to {@link StorageService}
     * @return {@link Report} containing the status, progress and diagnostics for the job
     * @throws PluginException if an error occurred during execution
     */
    @Override
    public Report beforeAllExecute(IndexService indexService, ModelService modelService, StorageService storageService) throws PluginException {
        return new Report();
    }

    /**
     * <p>Method called to execute the main action of the plugin.</p>
     *
     * @param indexService   reference to {@link IndexService}
     * @param modelService   reference to {@link ModelService}
     * @param storageService reference to {@link StorageService}
     * @param liteList       list of objects to execute the plugin against
     * @return {@link Report} containing the status, progress and diagnostics for the job
     * @throws PluginException if an error occurred during execution
     */
    @Override
    public Report execute(IndexService indexService, ModelService modelService, StorageService storageService, List<LiteOptionalWithCause> liteList) throws PluginException {
        System.out.println("METS Generator execute");

        if (!this.getParameterValues().containsKey(PLUGIN_PARAMS_IP_PROFILE)) {
            throw new PluginException(String.format("Parameter: \"%s\" not defined", PLUGIN_PARAMS_IP_PROFILE));
        }

        final String ipProfileValue = this.getParameterValues().get(PLUGIN_PARAMS_IP_PROFILE);
        final CSIPProfile csipProfile = switch (ipProfileValue) {
            case "sip" -> CSIPProfile.SIP_v2_2_0;
            case "aip" -> CSIPProfile.AIP_v2_2_0;
            case "dip" -> CSIPProfile.DIP_v2_2_0;
            default -> throw new PluginException(String.format("Invalid CSIP Profile: \"%s\"", ipProfileValue));
        };

        return PluginHelper.processObjects(this, new RODAObjectsProcessingLogic<AIP>() {
            @Override
            public void process(IndexService indexService, ModelService modelService, StorageService storageService, Report report, Job cachedJob, JobPluginInfo jobPluginInfo, Plugin<AIP> plugin, List<AIP> objects) {
                processAIP(indexService, modelService, storageService, report, cachedJob, jobPluginInfo, objects, csipProfile);
            }
        }, indexService, modelService, storageService, liteList);
    }

    private void processAIP(IndexService indexService, ModelService modelService, StorageService storageService, Report report, Job job, JobPluginInfo jobPluginInfo, List<AIP> aips, CSIPProfile csipProfile) {
        for (AIP aip : aips) {
            Report reportItem = PluginHelper.initPluginReportItem(this, aip.getId(), AIP.class);
            try {
                PluginHelper.updatePartialJobReport(this, modelService, reportItem, false, job);

                final String aipId = aip.getId();
                if (aipId == null || aipId.isEmpty()) {
                    throw new MetsGeneratorException("Could not retrieve AIP ID");
                }

                final MetsWrapper metsWrapper = METSUtils.generateMETS(aip, csipProfile, reportItem);
                final Mets mets = metsWrapper.getMets();

                try {
                    final StoragePath aipStoragePath = ModelUtils.getAIPStoragePath(aip.getId());
                    final StoragePath metsStoragePath = DefaultStoragePath.parse(aipStoragePath, IPConstants.METS_FILE);

                    MetsContentPayload contentPayload = new MetsContentPayload(mets, true);
                    storageService.updateBinaryContent(metsStoragePath, contentPayload, false, true);
                } catch (RequestNotValidException | GenericException | NotFoundException |
                         AuthorizationDeniedException e) {
                    throw new MetsGeneratorException("Could not create new IP level METS file");
                }

                jobPluginInfo.incrementObjectsProcessedWithSuccess();
                reportItem.setPluginState(PluginState.SUCCESS);
                reportItem.setHtmlPluginDetails(true).setPluginDetails(String.format("Created E-ARK CSIP version '%s' METS file(s) with a '%s' profile.", csipProfile.getVersion(), csipProfile.getProfile().toString()));

            } catch (MetsGeneratorException | AuthorizationDeniedException e) {
                reportItem.setPluginState(PluginState.FAILURE).setPluginDetails(e.getMessage());
                jobPluginInfo.incrementObjectsProcessedWithFailure();
            } finally {
                report.addReport(reportItem);
                PluginHelper.updatePartialJobReport(this, modelService, reportItem, true, job);
            }
        }
    }

    /**
     * <p>Method called after executing the main action of the plugin.</p>
     * <p>Useful to perform required cleanup after executing a job with many sub-tasks.</p>
     * <p>Called by {@link PekkoWorkerActor}.handlePluginAfterAllExecuteIsReady(Object msg) & {@link PekkoBackgroundWorkerActor}.handlePluginAfterAllExecuteIsReady(Object msg)</p>
     *
     * @param indexService   reference to {@link IndexService}
     * @param modelService   reference to {@link ModelService}
     * @param storageService reference to {@link StorageService}
     * @return {@link Report} containing the status, progress and diagnostics for the job
     * @throws PluginException if an error occurred during execution
     */
    @Override
    public Report afterAllExecute(IndexService indexService, ModelService modelService, StorageService storageService) throws PluginException {
        return new Report();
    }

    /**
     * <p>Used to shut down the plugin and perform cleanup.</p>
     * <p>Called by {@link PluginManager}
     *
     * @see PluginManager#shutdown()
     */
    @Override
    public void shutdown() {
        System.out.println("MetsGenerator shutdown");
    }
}
