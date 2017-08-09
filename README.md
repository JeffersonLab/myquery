# wmyget
MYA Query Web Service

Uses [jmyapi](https://github.com/JeffersonLab/jmyapi) to provide a web service for fetching data from the JLab Archiver 'MYA'.

## API    

### Multiple Event Query (Interval)

**Request URL Parameters**     

| Name  | Description                                                  | Value Format                                        | Required | Default                                    |   
|-------|--------------------------------------------------------------|-----------------------------------------------------|----------|--------------------------------------------|   
| c     | EPICS Channel name                                           | String                                              | YES      |                                            |  
| b     | Inclusive begin date with optional time                      | String in ISO 8901 format (YYYY-MM-DD[Thh:mm:[ss]]) | YES      |                                            |  
| e     | Exclusive end date with optional time                        | String in ISO 8901 format (YYYY-MM-DD[Thh:mm:[ss]]) | YES      |                                            |
| l     | Limit by binning / sampling                                  | Boolean, true if parameter exists                   | NO       | No sampling is done                        | 
| m     | MYA deployment                                               | String                                              | NO       | 'ops'                                      |   
| M     | MYA master host override                                     | String                                              | NO       | Standard master used                       |   
| d     | Expression to filter events                                  | String                                              | NO       | No filter applied                          |   
| f     | Fractional seconds time digits                               | Integer (0-6)                                       | NO       | 0                                          |    
| v     | Fractional value digits                                      | Integer                                             | NO       | 2                                          |   
| p     | Include prior point (guarantee at least one point in result) | Boolean, true if parameter exists                   | NO       | Prior point isn't included                 |   
| s     | Enumerations as strings                                      | Boolean, true if parameter exists                   | NO       | Enumerations presented as ordinal number   |   
| t     | Timestamps as milliseconds from UNIX Epoch                   | Boolean, true if parameter exists                   | NO       | Timestamps are returned in ISO 8901 format |   

**Response JSON Format**    
*On Success (HTTP 200 Response Code):*   
````json
{   
    "datatype":"_EPICS datatype_",     
    "datasize":"_data vector size; 1 for scalar_",    
    "datahost":"_MYA hostname of data home_",      
    "sampled":"_true if sampled, false otherwise_",   
    "data":[   
        {   
            "d":"_DATE-TIME_",   
            "v":"_VALUE_"   
        },   
        ...   
    ]    
}     
````

*On Error (HTTP 400 Repsonse Code):*    
````json
{   
    "error":"_error reason_"   
}      
````

### Single Event Query (Point)
Query for a single event on the timeline closest to the specified point.  The direction to search from the point is determined by the 'w' parameter.

**Request Parameters**     

| Name  | Description                                                  | Value Format                                        | Required | Default                                    |   
|-------|--------------------------------------------------------------|-----------------------------------------------------|----------|--------------------------------------------|   
| c     | EPICS Channel name                                           | String                                              | YES      |                                            |
| t     | Time of interest date with optional time                     | String in ISO 8901 format (YYYY-MM-DD[Thh:mm:[ss]]) | YES      |                                            |
| m     | MYA deployment                                               | String                                              | NO       | 'ops'                                      |   
| M     | MYA master host override                                     | String                                              | NO       | Standard master used                       |
| d     | Expression to filter events                                  | String                                              | NO       | No filter applied                          |   
| f     | Fractional seconds time digits                               | Integer (0-6)                                       | NO       | 0                                          |
| w     | Get first recorded event before or equal time of interest    | Boolean, true if parameter exits                    | NO       | Get first recorded event after or equal time of interest |
| s     | Enumerations as strings                                      | Boolean, true if parameter exists                   | NO       | Enumerations presented as ordinal number   |

**Response JSON Format**   
*On Success (HTTP 200 Response Code):*   
````json
{   
    "data":{   
        "date":"_ISO 8901 DATE-TIME_",   
        "value":"_VALUE_"    
        }   
}    
````

*On Error (HTTP 400 Repsonse Code):*    
````json
{   
    "data":{}   
}       
````
