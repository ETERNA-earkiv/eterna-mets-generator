# METS Generator - User Guide

## Overview

The METS Generator creates and updates METS files for Intellectual Entities and Representations.
This enables the export of complete E-ARK CSIP packages with the profiles SIP, AIP & DIP.

---

### How to use the METS Generator

1. **Run the METS Generator**: Start by selecting the Intellectual Entities you want to create METS files for, either from the Catalog page or the Search page. Then click the menu button "⋮" and choose "Start new process". On the "New process" page, select the job METS Generator under "Actions"
2. **Select IP Profile**: Choose the desired IP profile for the new METS file: SIP, AIP, or DIP
3. **Choose whether to include 'Ancestors'**: With the checkbox "Include Ancestor IDs", you can choose whether the Intellectual Entity’s location in the archive should be included in the created METS file
4. **Create the METS file**: Click the "CREATE" button in the right-hand sidebar to start the job and generate the METS file

---

### IP Profiles

E-ARK CSIP packages are available in different profiles for different purposes. The METS Generator supports the SIP, AIP, and DIP profiles.

- **E-ARK SIP**: Submission Information Package is used for packages transferred from the organization to the archive
- **E-ARK AIP**: Archival Information Package represents information as it is stored in the archive
- **E-ARK DIP**: Dissemination Information Package is used for packages intended for export to another party

Before exporting from the archive, you should generate accompanying METS files for the packages you plan to export, typically using the DIP profile, unless the purpose of the export is to immediately re-import into another archive, in which case using SIP can facilitate the re-import process.

---

### Include Ancestor IDs

With the checkbox "Include Ancestor IDs", you can choose whether the Intellectual Entity’s location in the archive should be included in the created METS file.
If this option is used, the package will be placed in the same location in the archive as the original upon re-ingest.
This is useful when exporting an entire structure, as the hierarchy is preserved in the package information.

---

### What does the METS Generator handle?

The METS Generator includes the following package information in the METS files:

- AIP level:
    - Creation date
    - Creator
    - Location in the archive (Ancestors)
    - Descriptive metadata
    - Preservation metadata
    - Technical metadata
    - Source metadata
    - Rights metadata
    - Other metadata
    - Schemas
    - Documentation
- Representation level:
    - Preservation data
    - Descriptive metadata
    - Preservation metadata
    - Technical metadata
    - Source metadata
    - Rights metadata
    - Other metadata
    - Schemas
    - Documentation

---

### Limitations

The METS Generator currently does not handle:

- **Shallow IPs**: Packages where preservation information is not stored in the package itself but instead refers to an external location where the information is stored.
- **Representation Type**:
    The representation type in the METS file is divided into two fields: `TYPE` and `CONTENTINFORMATIONTYPE` (and optionally `OTHERTYPE` and `OTHERCONTENTINFORMATIONTYPE` if the main fields have the value `OTHER`).
    When *commons-ip* was used as a command-line tool for packaging prior to version **v2.11.0**, the selected representation type was stored in the `TYPE` field (and possibly in `OTHERTYPE`), but during ingest, the value was read from `CONTENTINFORMATIONTYPE` (and possibly `OTHERCONTENTINFORMATIONTYPE`).
    This means that packages created with those versions may have stored an incorrect representation type upon ingest.
    In newer versions of *commons-ip*, both values can be set independently, but `CONTENTINFORMATIONTYPE` is still the field that will be read during ingest — the other value will be lost.
    Consequently, when reimporting packages with the new METS files, the original representation type may be missing or incorrect.
    In future versions of **ETERNA**, we intend to extend the data models for AIP & Representation with the field `contentInformationType` in order to preserve both types as specified in the METS file.

- **Representation Status**:
  The status of representations is currently not preserved in the new METS file.

- Any other files attached in the original package besides those listed above. The job only includes the information sets defined in the E-ARK CSIP, SIP, AIP & DIP standards.

---

### Important Notes

### Ancestor IDs

If the option "Include Ancestor IDs" is used, the package’s location in the archive is included as “Ancestor” information.
This means that when the package with the new METS file is later re-imported, it will by default be restored to the same place in the archive from which it was exported.
If this location does not exist in the target archive, ETERNA will attempt to recreate the hierarchy when importing the package.

For example, if the package was stored in the archive at:
`/ Level1 / Level2 / PackageToExport`
it will be imported back into the same location and placed next to the original package.

If `/ Level1 / Level2` is missing, ETERNA will create empty Intellectual Entities to represent Level1 and Level2. However, these will not have any name or metadata, as only the ID is included in the “Ancestor” information.

In the archive, it will appear as follows:
`/ 👻 ghost / 👻 ghost / PackageToExport`.

If you try to import the package into another location in the original archive, for example `/ AnotherUnit /`, which does not contain the original “Level1” & “Level2”, the system will recreate the Intellectual Entities in the same way, and the package will then be imported as:
`/ AnotherUnit / 👻 ghost / 👻 ghost / PackageToExport`.

To handle this during import, you can check the option "Force parent node", which tells the import process to ignore the packaged “Ancestor” information.
The package will then be imported either to the root of the catalog or to the selected location if you have used the “Select Parent Node” function.

---

### Preservation Metadata

The METS Generator does not create any preservation events or PREMIS:EVENTs, as it is our interpretation that PREMIS:EVENT describes events concerning the *content* of a package, not changes to the package itself.

See: [E-ARK CSIP | Common Specification for Information Packages — 5.4. Use of PREMIS](https://earkcsip.dilcis.eu/#54-use-of-premis)
> 5.4. Use of PREMIS
>
> The CSIP recommends and advocates the use of the PREservation Metadata Implementation Strategies (PREMIS, [https://www.loc.gov/standards/premis/](https://www.loc.gov/standards/premis/)) metadata standard for recording preservation and technical metadata about digital objects contained within CSIP Information Packages. The use of PREMIS is described in the “E-ARK Common Specification for Preservation Metadata using PREMIS” (CS PREMIS) found at [https://citspremis.dilcis.eu/specification/](https://citspremis.dilcis.eu/specification/) .

If changes to the METS file were to be included as PREMIS:EVENTs, this would create a circular dependency where the modification of the METS file would lead to the creation of a new PREMIS:EVENT file, which in turn would require the METS file to be modified again to include the new file, which would then require yet another PREMIS:EVENT file, and so on.

---

### Compatibility with Packages Created in RODA-In

When packages are created in RODA-In using the default configuration, EAD metadata files are given filenames that do not match RODA / ETERNA’s expected metadata naming convention — specifically: `ead2002.xml` and `ead3.xml`, depending on which metadata format is used. RODA & ETERNA expect the filenames `ead_2002.xml` and `ead_3.xml` in order to understand that the metadata type is EAD and the metadata version is “2002” or “3”.

When a new METS file is generated using the METS Generator from a logical unit loaded from such a package, the information about type and version is missing in ETERNA, and the metadata type is instead interpreted as “ead2002” without a version, or “ead3” without a version. In the METS file, this is reflected by the attributes `MDTYPE="OTHER"` and `MDOTHERTYPE="ead2002"`, and by the absence of an `MDVERSION` attribute. When this package is later reimported, there is no metadata of type “ead2002” or “ead3” in ETERNA, so the metadata is therefore interpreted as an unknown XML file.

This issue is known in RODA-In and has been reported in the following issue with Keep Solution:
[https://github.com/keeps/roda-in/issues/440](https://github.com/keeps/roda-in/issues/440)
