METS Generator
-----------------------

[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](LICENSE.md)

A plugin for [ETERNA](https://github.com/ETERNA-earkiv/ETERNA) providing METS Generation for E-ARK CSIP Packages.  
Updates AIPs & representations with E-ARK IP METS files.

## Features

- **METS files Generation**: The METS Generator creates and updates METS files for Intellectual Entities and Representations
- **Support for SIP, AIP & DIP**: E-ARK CSIP packages are available in different profiles for different purposes. The METS Generator supports the SIP, AIP, and DIP profiles.
  - **E-ARK SIP**: Submission Information Package is used for packages transferred from the organization to the archive
  - **E-ARK AIP**: Archival Information Package represents information as it is stored in the archive
  - **E-ARK DIP**: Dissemination Information Package is used for packages intended for export to another party
- **Optional preservation ancestor relationship**:  With the checkbox "Include Ancestor IDs", you can choose whether the Intellectual Entity’s location in the archive should be included in the created METS file

### Known Limitations

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

## Installation

Download [image-converter.zip](http://github.com/ETERNA-earkiv/eterna-image-converter/releases/latest/download/image-converter.zip) and extract it into `/.roda/config/plugins/` and restart ETERNA.

## License

See [LICENSE.md](LICENSE.md) for details.