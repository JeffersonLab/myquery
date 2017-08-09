# wmyget
MYA Query Web Service

Uses [jmyapi](https://github.com/JeffersonLab/jmyapi) to provide a web service for fetching data from the JLab Archiver 'MYA'.

## API    

**Request Parameters**     

| Name  | Description                                                  | Required | Default                                    |   
|-------|--------------------------------------------------------------|----------|--------------------------------------------|   
| c     | EPICS Channel name                                           | YES      |                                            |  
| b     | Inclusive begin date in ISO 8901 format with optional time (YYYY-MM-DD[Thh:mm:[ss]]) | YES |  |  
| e     | Exclusive end date in ISO 8901 format with optional time (YYYY-MM-DD[Thh:mm:[ss]]) | YES |   |
| l     | Limit by binning (sampling)                                  | NO       | No sampling is done                        | 
| m     | MYA deployment (default opts                                 | NO       | 'ops'                                      |   
| M     | MYA master host override                                     | NO       | Standard master used                       |   
| d     | Expression to filter events                                  | NO       | No filter applied                          |   
| f     | Fractional seconds time digits                               | NO       | No fractional seconds                      |    
| v     | Fractional value digits                                      | NO       | 2                                          |   
| p     | Include prior point (guarantee at least one point in result) | NO       | Prior point isn't included                 |   
| s     | Enumerations as strings                                      | NO       | Enumerations presented as ordinal number   |   
| t     | Timestamps as milliseconds from Epoch (1970)                 | NO       | Timestamps are returned in ISO 8901 format |   
