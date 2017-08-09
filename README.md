# wmyget
MYA Query Web Service

Uses [jmyapi](https://github.com/JeffersonLab/jmyapi) to provide a web service for fetching data from the JLab Archiver 'MYA'.

## API

**Request Parameters**   
| Name  | Description             | Required | 
|-------|-------------------------|----------|
| c     | EPICS Channel name      | YES      |
| b     | Inclusive begin date in ISO 8901 format with optional time (YYYY-MM-DD[Thh:mm:[ss]]) | YES |
| e     | Exclusive end date in ISO 8901 format with optional time (YYYY-MM-DD[Thh:mm:[ss]]) | YES |
| l     | Limit by binning (sampling) | NO - default: no sampling is done |
| m     | MYA deployment (default opts | NO - default: 'ops' |
| M     | MYA master host override | NO - default: standard master used |
| d     | Expression to filter events | NO - default: no filter applied |
| f     | Fractional seconds time digits | NO - default: no fractional seconds | 
| v     | Fractional value digits | NO - default: 2 |
| p     | Include prior point (guarantee at least one point in result) | NO - default: prior point isn't included |
| s     | |
| t     | |
