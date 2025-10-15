# Changelog

## [1.0.1] - 2025-10-15
### Fixed
- Compatibility with the new firmware: deviceId has grown from 8 to 24 hex characters (32 → 96 bits). It’s now stored as a string instead of a long.

## [1.0.0] - 2025-04-29
### Added
- Store settings permanently
### Changed
- Restructuring of project files
- Changes to the UI

## [0.5.1] - 2025-04-08
### Added
- Display stays on while reading the datalog
### Fixed
- Resolved problem with read buffer size

## [0.5.0] - 2025-04-07
### Fixed
- Resolved compatibility issues with FNIRSI GC-01/JOY-IT JT-RAD01.


## [0.4.0] - 2025-04-02
### Added
- Check whether the internal time of the Geiger counter is synchronized with the Android device time.
- Option to synchronize the clock of the Geiger counter with the Android device time.

### Note
- It's important that the time of the Geiger counter is set correctly to ensure accurate datalog storage.

## [0.3.1] - 2025-04-01
### Changed
- Changed from java.util.Date to the newer java.time

## [0.3.0] - 2025-03-30
### Added
- Support for importing CSV files

### Fixed
- Resolved performance issues

## [0.2.0] - 2025-03-21
### Changed
- Refactored the Room Library with a clean and organized ER model to improve query handling and future scalability

### Added
- Unified chart view displaying all downloaded data logs from a single geiger counter device
- Adaptive moving average filter for smoother and more readable chart data

## [0.1.1] - 2025-03-16
### Added
- Initial commit
