# myquery
MYA Query Web Service

Uses [jmyapi](https://github.com/JeffersonLab/jmyapi) to provide a web service for fetching data from the JLab Archiver 'MYA'.

## API    

### Multiple Event Query (Interval)   
Query for all events on the timeline between the begin (inclusive) and end (exclusive) dates.

_**Path:** myquery/interval_   

**Request URL Parameters**     

| Name  | Description                                                  | Value Format                                               | Required | Default                                    |   
|-------|--------------------------------------------------------------|------------------------------------------------------------|----------|--------------------------------------------|   
| c     | EPICS Channel name                                           | String                                                     | YES      |                                            |  
| b     | Inclusive begin date with optional time                      | String in ISO 8601 format (YYYY-MM-DD[Thh:mm:[ss]])        | YES      |                                            |  
| e     | Exclusive end date with optional time                        | String in ISO 8601 format (YYYY-MM-DD[Thh:mm:[ss]])        | YES      |                                            |
| l     | Limit by binning / sampling                                  | Integer, number of samples or bins.  No sampling if absent | NO       | No sampling is done                        | 
| t     | type of sampling                                             | String,  binned, event, or graphical (default)             | NO       | 'graphical'                                |      
| m     | MYA deployment                                               | String                                                     | NO       | 'ops'                                      |      
| f     | Fractional seconds time digits                               | Integer (0-6)                                              | NO       | 0 (ISO 8601 only)                          |    
| v     | Fractional floating value digits                             | Integer (0-9)                                              | NO       | 6 (floats only)                            |
| d     | Data update events only (ignore info events)                 | Boolean, true if parameter exits                           | NO       | All events returned (update and info)      |
| p     | Include prior point (guarantee at least one point in result) | Boolean, true if parameter exists                          | NO       | Prior point isn't included                 |   
| s     | Enumerations as strings                                      | Boolean, true if parameter exists                          | NO       | Enumerations presented as ordinal number   |   
| u     | Timestamps as milliseconds from UNIX Epoch                   | Boolean, true if parameter exists                          | NO       | Timestamps are returned in ISO 8601 format |   

**Response JSON Format**    
*On Success (HTTP 200 Response Code):*   
````json
{   
    "datatype":"<EPICS datatype>",     
    "datasize":"<data vector size; 1 for scalar>",    
    "datahost":"<MYA hostname of data home>",      
    "sampled":"<true if sampled, false otherwise>", 
    "sampleType":"<binned, event, graphical; only present if sampled = true>",
    "count":"<original count of events; only present if sampled = true>",
    "data":[   
        {   
            "d":"<DATE-TIME>",   
            "v":"<VALUE>",  
            "t":"<TYPE (only present if not update)>",  
            "x":"<DISCONNECTION-TRUE/FALSE (only present if disconnection)>"    
        },   
        ...   
    ]    
}     
````

*On Error (HTTP 400 Repsonse Code):*    
````json
{   
    "error":"<error reason>"   
}      
````

### Single Event Query (Point)
Query for a single event on the timeline closest to the specified point.  The direction to search from the point is determined by the 'w' parameter.

_**Path:** myquery/point_    

**Request Parameters**     

| Name  | Description                                                  | Value Format                                        | Required | Default                                        |   
|-------|--------------------------------------------------------------|-----------------------------------------------------|----------|------------------------------------------------|   
| c     | EPICS Channel name                                           | String                                              | YES      |                                                |
| t     | Time of interest date with optional time                     | String in ISO 8601 format (YYYY-MM-DD[Thh:mm:[ss]]) | YES      |                                                |
| m     | MYA deployment                                               | String                                              | NO       | 'ops'                                          |      
| f     | Fractional seconds time digits                               | Integer (0-6)                                       | NO       | 0 (ISO 8601 only)                              |
| v     | Fractional floating value digits                             | Integer (0-9)                                       | NO       | 6 (floats only)                                |  
| d     | Data update events only (ignore info events)                 | Boolean, true if parameter exits                    | NO       | All events returned (update and info)          |
| w     | Get closest event greater than time of interest              | Boolean, true if parameter exists                   | NO       | Get closest event less than time of interest   |
| x     | Closest event is exclusive of time of interest               | Boolean, true if parameter exists                   | NO       | Closest event is inclusive of time of interest |
| s     | Enumerations as strings                                      | Boolean, true if parameter exists                   | NO       | Enumerations presented as ordinal number       |
| u     | Timestamps as milliseconds from UNIX Epoch                   | Boolean, true if parameter exists                   | NO       | Timestamps are returned in ISO 8601 format     | 

**Response JSON Format**   
*On Success (HTTP 200 Response Code):*   
````json
{   
    "datatype":"<EPICS datatype>",     
    "datasize":"<data vector size; 1 for scalar>",    
    "datahost":"<MYA hostname of data home>",  
    "data":{   
        "d":"<DATE-TIME>",   
        "v":"<VALUE>",  
        "t":"<TYPE (only present if not update)>",  
        "x":"<DISCONNECTION-TRUE/FALSE (only present if disconnection)>"            
        }   
}    
````

*On Error (HTTP 400 Repsonse Code):*    
````json
{   
    "error":"<error reason>"   
}       
````

### JSONP
The API also supports JSONP responses.  Simply provide an additional parameter:

````
jsonp=<function name>
````
and the jsonp function name returned will be _&lt;function name&gt;_.

### Event Types
Use the 'd' parameter to limit events to updates only.  The primary event type is an 'update', which is a normal data value.  Other event types are informational and set the 't' field with one of the following strings:

Disconnection Events
   - NETWORK_DISCONNECTION
   - ARCHIVING_OF_CHANNEL_TURNED_OFF
   - ARCHIVER_SHUTDOWN
   - UNKNOWN_UNAVAILABILTY
   
Miscellaneous Events
   - ORIGIN_OF_CHANNELS_HISTORY
   - CHANNELS_PRIOR_DATA_MOVED_OFFLINE
   - CHANNELS_PRIOR_DATA_DISCARDED

Disconnection events are also flagged with the presence of the attribute 'x' for convenience. 
