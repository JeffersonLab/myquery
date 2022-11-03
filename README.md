# myquery [![CI](https://github.com/JeffersonLab/myquery/actions/workflows/ci.yml/badge.svg)](https://github.com/JeffersonLab/myquery/actions/workflows/ci.yml)
The myquery web service provides a simple query interface to the Jefferson Lab MYA archiver via [jmyapi](https://github.com/JeffersonLab/jmyapi). 

---
 - [Overview](https://github.com/JeffersonLab/myquery#overview)   
 - [Install](https://github.com/JeffersonLab/myquery#install)   
 - [API](https://github.com/JeffersonLab/myquery#api)    
 - [Configure](https://github.com/JeffersonLab/myquery#configure)    
 - [Build](https://github.com/JeffersonLab/myquery#build)
 - [Test](https://github.com/JeffersonLab/myquery#test)
 - [Release](https://github.com/JeffersonLab/myquery#release)
 - [See Also](https://github.com/JeffersonLab/myquery#see-also)
---

## Overview
The primary goal of myquery is to allow users simple, programmatic access to MYA data without any dependencies on a specific language or requiring wrapping a command line tool.

Supports querying channel history over a time interval and at a specific point in time, and includes simple forms that allow users to easily generate a valid query string for each type of supported query.

Access via Internet (Authentication Required): [Public MYA Web Service](https://epicsweb.jlab.org/myquery/)   
Access via Intranet: [Internal MYA Web Service](https://myaweb.acc.jlab.org/myquery/)

## Install
 1. Download [Apache Tomcat](http://tomcat.apache.org/)
 2. Download [myquery.war](https://github.com/JeffersonLab/myquery/releases) and drop it into the Tomcat webapps directory
 3. Configure Tomcat
 4. Start Tomcat and navigate your web browser to localhost:8080/myquery

## API    

[Interval](#multiple-event-query-interval)  
[Point](#single-event-query-point)  
[Channel](#wildcard-pv-lookup-query-channel)

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
| t     | type of sampling                                             | String: 'graphical', 'simpleevent', 'myget', 'mysampler'   | NO       | 'graphical'                                |      
| m     | MYA deployment                                               | String                                                     | NO       | 'ops'                                      |      
| f     | Fractional seconds time digits                               | Integer (0-6)                                              | NO       | 0 (ISO 8601 only)                          |    
| v     | Fractional floating value digits                             | Integer (0-9)                                              | NO       | 6 (floats only)                            |
| d     | Data update events only (ignore info events)                 | Boolean, true if parameter exits                           | NO       | All events returned (update and info)      |
| p     | Include prior point (guarantee at least one point in result) | Boolean, true if parameter exists                          | NO       | Prior point isn't included                 |   
| s     | Enumerations as strings                                      | Boolean, true if parameter exists                          | NO       | Enumerations presented as ordinal number   |   
| u     | Timestamps as milliseconds from UNIX Epoch                   | Boolean, true if parameter exists                          | NO       | Timestamps are returned in ISO 8601 format (local server time) |
| a     | Timestamps as milliseconds at the server zone offset*        | Boolean, true if parameter exists                          | NO       | Timestamps milliseconds in UTC             |
| i     | Integrate (float data-types only)†                          | Boolean, true if parameter exists                          | NO       | No integration is performed                |

*Some clients like web browsers have limited access to an IANA timezone database so it is convenient if the server does local timezone (America/New_York - EST and EDT) shifting of historic time series and clients can interpret time series as timezone-less UTC internally yet present it as local time.  Note: this does mean results may contain two data points for 1:00 AM where that Fall hour is duplicated due to the daylight savings boundary and an hour in the Spring that is skipped.  In other words: The data values are identical, just timestamps can differ: two 1:00 AM vs both a 1:00 AM EST and 1:00 AM EDT.

†Both integrated and non integrated values are returned.  The "raw" value has key "v" and the integrated value key is "i".

**Response JSON Format**    
*On Success (HTTP 200 Response Code):*   
````
{   
    "datatype":"<EPICS datatype>",     
    "datasize":"<data vector size; 1 for scalar>",    
    "datahost":"<MYA hostname of data home>",      
    "sampled":"<true if sampled, false otherwise>", 
    "sampleType":"<graphical, simpleevent, myget, mysampler; only present if sampled = true>",
    "count":"<original count of events; only present if sampled = true>",
    "labels":"<An array of historic labels; only present if datatype = DBR_ENUM>",
    "data":[   
        {   
            "d":"<DATE-TIME>",   
            "v":"<VALUE>",  
            "t":"<TYPE (only present if not update)>",  
            "i":"<INTEGRATED VALUE (only present if requested with i option)>",
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

*Sampling Routines Explained:*

- The **graphical** (default) application-level event-based routine returns a data set that attempts to maintain graphical fidelity.  The signal is split into approximately the number of specified bins (l), with each bin returning important characteristics such as min, max, non-update events, largest triangluar three bucket (LTTB) point.  This makes the number of returned data points somewhat unpredicatable, but generally gives very good graphical results even for heavily downsampled signals.
- The **simpleevent** application-level event-based routine returns every nth event, where n is based on the "l" parameter and the number of events in the requested channel history (n = count/l).  This gives greater detail to portions of the signal that are "busier", but may leave portions of the time domain sparsely represented.  
- The **myget** database-level time-based routine splits the time range up into the number of equally sized specified bins (l) and returns the first data point from each bin.  This tries to give equal time-representation throughout the channel history.
- The **mysampler** database-level time-based routine splits the time range up into the number of equally sized specified bins (l) and returns the previous data point before each bin with timestamps modified to start of bin.  This tries to give regularly spaced datapoints by creating them from implied values (converts MYA's delta-based data into regularly sampled data). 

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
| v     | Fractional floating value digits                             | Integer (0-38)                                      | NO       | 6 (floats only)                                |  
| d     | Data update events only (ignore info events)                 | Boolean, true if parameter exits                    | NO       | All events returned (update and info)          |
| w     | Get closest event greater than time of interest              | Boolean, true if parameter exists                   | NO       | Get closest event less than time of interest   |
| x     | Closest event is exclusive of time of interest               | Boolean, true if parameter exists                   | NO       | Closest event is inclusive of time of interest |
| s     | Enumerations as strings                                      | Boolean, true if parameter exists                   | NO       | Enumerations presented as ordinal number       |
| u     | Timestamps as milliseconds from UNIX Epoch                   | Boolean, true if parameter exists                   | NO       | Timestamps are returned in ISO 8601 format     | 
| a     | Timestamps as milliseconds at the server zone offset         | Boolean, true if parameter exists                   | NO       | Timestamps milliseconds in UTC                 |

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

### Wildcard PV lookup Query (Channel)
Query for all PVs with name like the provided wildcard query string.  Wildcard characters include % (any) and _ (one).  The results are sorted in ascending order by name.

_**Path:** myquery/channel_    

**Request Parameters**     

| Name  | Description                                                  | Value Format                                        | Required | Default                                        |   
|-------|--------------------------------------------------------------|-----------------------------------------------------|----------|------------------------------------------------|   
| q     | EPICS Channel name wildcard query                            | String                                              | YES      |                                                |
| l     | Limit results                                                | Number                                              | NO       | 10                                             |
| o     | Offset (pagination)                                          | Number                                              | NO       | 0                                              |
| m     | MYA deployment                                               | String                                              | NO       | 'ops'                                          |      

**Response JSON Format**   
*On Success (HTTP 200 Response Code):*   
````
[   
    {
    "name":"<PV name>",
    "datatype":"<EPICS datatype>",     
    "datasize":"<data vector size; 1 for scalar>",    
    "datahost":"<MYA hostname of data home>",  
    "ioc":"<name of IOC generating updates or null if unavailable>",
    "active":"<true if active, false otherwise>"
    },
    ...
]    
````

*On Error (HTTP 400 Repsonse Code):*    
````json
{   
    "error":"<error reason>"   
}       
````

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

## Configure
A [deployments.properites](https://github.com/JeffersonLab/jmyapi#deployments) file must be placed in the lib directory of Tomcat.  Download the mariadb [database driver](https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.0.8/mariadb-java-client-3.0.8.jar) and place it in the lib directory of Tomcat.  A new context.xml file is needed in the Tomcat conf directory that includes a DataSource for each host in the deployments.properties. 

## Build
This project is built with [Java 17](https://adoptium.net/) (compiled to Java 11 bytecode), and uses the [Gradle 7](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/myquery
cd myquery
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note for JLab On-Site Users**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)


## Test
Continuous Integration (CI) is setup using GitHub Actions, so on push tests are automatically run unless `[no ci]` is included in the commit message.   Tests can be manually run on a local workstation using:
```
docker compose -f build.yml up
```
Wait for containers to start then:
```
gradlew integrationTest
```

## Release
1. Bump the version number and release date in build.gradle and commit and push to GitHub (using [Semantic Versioning](https://semver.org/)).   
2. Create a new release on the GitHub [Releases](https://github.com/JeffersonLab/jaws-libj/releases) page corresponding to same version in build.gradle (Enumerate changes and link issues).  Attach war file for users to download.

## See Also
   - [Web Archive Viewer and Expositor (WAVE)](https://github.com/JeffersonLab/wave)
   - [Setup Troubleshooting](https://github.com/JeffersonLab/myquery/wiki/Setup-Troubleshooting)
   - [Usage Examples](https://github.com/JeffersonLab/myquery/wiki/Usage-Examples)
