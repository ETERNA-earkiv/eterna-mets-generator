# METS Generator - User Guide

## Overview

The METS Generator creates and updates METS files for Intellectual Entities and Representations.
This enables the export of complete E-ARK CSIP packages with the profiles SIP, AIP & DIP.

### How to use the METS Generator

1. **Run the METS Generator**: Start by selecting the Intellectual Entities you want to create METS files for, either from the Catalog page or the Search page. Then click the menu button "⋮" and choose "Start new process". On the "New process" page, select the job METS Generator under "Actions"
2. **Select IP Profile**: Choose the desired IP profile for the new METS file: SIP, AIP, or DIP
3. **Choose whether to include 'Ancestors'**: With the checkbox "Include Ancestor IDs", you can choose whether the Intellectual Entity’s location in the archive should be included in the created METS file
4. **Create the METS file**: Click the "CREATE" button in the right-hand sidebar to start the job and generate the METS file

### IP Profiles

E-ARK CSIP packages are available in different profiles for different purposes. The METS Generator supports the SIP, AIP, and DIP profiles.

- **E-ARK SIP**: Submission Information Package is used for packages transferred from the organization to the archive
- **E-ARK AIP**: Archival Information Package represents information as it is stored in the archive
- **E-ARK DIP**: Dissemination Information Package is used for packages intended for export to another party

Before exporting from the archive, you should generate accompanying METS files for the packages you plan to export, typically using the DIP profile, unless the purpose of the export is to immediately re-import into another archive, in which case using SIP can facilitate the re-import process.

### Include Ancestor IDs

With the checkbox "Include Ancestor IDs", you can choose whether the Intellectual Entity’s location in the archive should be included in the created METS file.
If this option is used, the package will be placed in the same location in the archive as the original upon re-ingest.
This is useful when exporting an entire structure, as the hierarchy is preserved in the package information.

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

### Limitations

The METS Generator currently does not handle:

- **Shallow IPs**: Packages where preservation information is not stored in the package itself but instead refers to an external location where the information is stored.
- Any other files attached in the original package besides those listed above. The job only includes the information sets defined in the E-ARK CSIP, SIP, AIP & DIP standards.

### Important Notes

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