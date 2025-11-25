package se.whitered.eterna.plugins.metsgenerator;

import org.roda_project.commons_ip2.cli.model.enums.CSIPVersion;

public class CSIPProfile {
    public enum Profile {
        SIP, DIP, AIP
    }

    public final static CSIPProfile SIP_v2_0_4 = new CSIPProfile(Profile.SIP, CSIPVersion.V204, "https://earksip.dilcis.eu/profile/E-ARK-SIP-v2-0-4.xml", "https://DILCIS.eu/XML/METS/SIPExtensionMETS");
    public final static CSIPProfile SIP_v2_1_0 = new CSIPProfile(Profile.SIP, CSIPVersion.V210, "https://earksip.dilcis.eu/profile/E-ARK-SIP-v2-1-0.xml", "https://DILCIS.eu/XML/METS/SIPExtensionMETS");
    public final static CSIPProfile SIP_v2_2_0 = new CSIPProfile(Profile.SIP, CSIPVersion.V220, "https://earksip.dilcis.eu/profile/E-ARK-SIP-v2-2-0.xml", "https://DILCIS.eu/XML/METS/SIPExtensionMETS");

    public final static CSIPProfile AIP_v2_2_0 = new CSIPProfile(Profile.AIP, CSIPVersion.V220, "https://earksip.dilcis.eu/profile/E-ARK-AIP-v2-2-0.xml", null);

    public final static CSIPProfile DIP_v2_0_4 = new CSIPProfile(Profile.DIP, CSIPVersion.V204, "https://earksip.dilcis.eu/profile/E-ARK-DIP-v2-0-4.xml", null);
    public final static CSIPProfile DIP_v2_1_0 = new CSIPProfile(Profile.DIP, CSIPVersion.V210, "https://earksip.dilcis.eu/profile/E-ARK-DIP-v2-1-0.xml", null);
    public final static CSIPProfile DIP_v2_2_0 = new CSIPProfile(Profile.DIP, CSIPVersion.V220, "https://earkdip.dilcis.eu/profile/E-ARK-DIP-v2-2-0.xml", null);

    private final Profile profile;
    private final CSIPVersion version;
    private final String profileURI;
    private final String extensionNS;

    private CSIPProfile(Profile profile, CSIPVersion version, String profileURI, String extensionNS) {
        this.profile = profile;
        this.version = version;
        this.profileURI = profileURI;
        this.extensionNS = extensionNS;
    }

    public Profile getProfile() {
        return profile;
    }

    public CSIPVersion getVersion() {
        return version;
    }

    public String getProfileURI() {
        return profileURI;
    }

    public String getExtensionNS() {
        return extensionNS;
    }
}
