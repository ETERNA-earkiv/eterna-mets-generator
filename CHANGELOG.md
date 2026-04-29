# Changelog
## [2.0.0]
### Changes
- Update parent POM dependency to 1.0.0-alpha-SNAPSHOT
- Refactor internal storage handling and plugin interface for improved maintainability
- Fix binary content update for more stable save behavior
- Localize plugin name, description, and parameter labels to Swedish

### Bug Fixes
- METS generator now skips generation if the file already exists, preventing unintended overwrites

## [1.0.1]
### Changes
- Update plugin compatibility to ETERNA 0.6.0

## [1.0.0]
### First release
- Initial release of the METS Generator plugin
- Creates E-ARK CSIP 2.2.0 METS files from E-ARK AIPs
- Supports the profiles SIP, AIP & DIP

### Limitations
- Does not support Shallow E-ARK packages
