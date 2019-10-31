# myquery
MYA Query Web Service

The myquery web service provides a simple MYA query interface.  It's primary goal is to allow users simple, programatic access to MYA data without the dependency on a specific language or wrapping a command line tool.

Currently, myquery supports querying channel history over a time interval and channel history at a specific point in time.  myquery includes simple forms that allow users to easily generate a valid query string for each type of supported query.

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
| t     | type of sampling                                             | String: 'graphical', 'simpleevent', 'myget', 'mysampler'   | NO       | 'graphical'                                |      
| m     | MYA deployment                                               | String                                                     | NO       | 'ops'                                      |      
| f     | Fractional seconds time digits                               | Integer (0-6)                                              | NO       | 0 (ISO 8601 only)                          |    
| v     | Fractional floating value digits                             | Integer (0-9)                                              | NO       | 6 (floats only)                            |
| d     | Data update events only (ignore info events)                 | Boolean, true if parameter exits                           | NO       | All events returned (update and info)      |
| p     | Include prior point (guarantee at least one point in result) | Boolean, true if parameter exists                          | NO       | Prior point isn't included                 |   
| s     | Enumerations as strings                                      | Boolean, true if parameter exists                          | NO       | Enumerations presented as ordinal number   |   
| u     | Timestamps as milliseconds from UNIX Epoch                   | Boolean, true if parameter exists                          | NO       | Timestamps are returned in ISO 8601 format |   
| i     | Integrate (float data-types only)                            | Boolean, true if parameter exists                          | NO       | No integration is performed                |

**Response JSON Format**    
*On Success (HTTP 200 Response Code):*   
````json
{   
    "datatype":"<EPICS datatype>",     
    "datasize":"<data vector size; 1 for scalar>",    
    "datahost":"<MYA hostname of data home>",      
    "sampled":"<true if sampled, false otherwise>", 
    "sampleType":"<graphical, simpleevent, myget, mysampler; only present if sampled = true>",
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
The API also supports JSONP responses (to work around browser same-origin policy).  Simply provide an additional parameter:

````
jsonp=<function name>
````
and the jsonp function name returned will be _&lt;function name&gt;_.

*Note*: JSONP should generally be avoided as it is a hack; instead setup your web server to reverse proxy the myquery server if they are not the same.

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

## Examples
A common problem for developers is that our interal web service uses an SSL certificate anchored by our internal JLab PKI.  JLab computers are configured at the system level to include th JLab root CA in the certificate trust store.  Software run onsite should refernce the system store, which many do by default.  However, some applications, modules, etc. referrence an embedded CA certificate store.  This requires some additional effort on the part of the developer to reference the system store as described below.  Alternatively, developers can download the JLab root CA certificate and reference it in place of the system store.

The JLab root certifcate authority certificates can be downloaded from http://pki.jlab.org.

### Python
The commonly used HTTP request module, requests, ships with an embedded certificate trust store and does not by default use the system trust store.  On Linux, this means that you must specify the path to the system trust store (a PEM file) as the value to the verify parameter.

````
import requests
import pandas as pd

url = "https://myaweb.acc.jlab.org/myquery/interval?c=RBM09_DsRt_LE&b=2019-01-01&e=2019-03-03&l=&t=graphical&m=&f=&v="
r = requests.get(url, verify='/etc/pki/tls/cert.pem')  # <<< This references the RHEL System CA trust anchors explicitly


df = pd.DataFrame(r.json()['data'])
df.rename(columns={'d':'Date', 'v':'JWS_Humidity'}, inplace=True)
print(df)
````

Windows is more complicated since it's trust store is in the registry.  Base Python uses this system trust store database, and the responses module lets you pass your own SSL context.  This SSL context references Python's default certificate trust store and allows your response object to verify our internal certificate.

This is a modified example pulled from https://stackoverflow.com/questions/42981429/ssl-failure-on-windows-using-python-requests

````
import requests
import pandas as pd
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.ssl_ import create_urllib3_context

class SSLContextAdapter(HTTPAdapter):
    def init_poolmanager(self, *args, **kwargs):
        context = create_urllib3_context()
        kwargs['ssl_context'] = context
        context.load_default_certs() # this loads the OS defaults on Windows
        return super(SSLContextAdapter, self).init_poolmanager(*args, **kwargs)

s = requests.Session()
adapter = SSLContextAdapter()
url = "https://myaweb.acc.jlab.org/myquery/interval?c=RBM09_DsRt_LE&b=2019-01-01&e=2019-03-03&l=&t=graphical&m=&f=&v="
s.mount(url, adapter)
r = s.get(url)

df = pd.DataFrame(r.json()['data'])
df.rename(columns={'d':'Date', 'v':'JWS_Humidity'}, inplace=True)
print(df)
````

