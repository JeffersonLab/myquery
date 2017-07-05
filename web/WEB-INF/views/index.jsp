<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <title>myget</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    </head>
    <body>
        <h1>myget functions</h1>
        <ul>
            <li>
                <h2><a href="/myget/span.html">span</a></h2>
            </li>
            <li>
                <h2><a href="/myget/jmyapi-span.html">jmyapi span</a></h2>
            </li>            
            <li>
                <h2><a href="/myget/event.html">event</a></h2>
            </li>            
        </ul>
        <div id="version">Version: ${initParam.releaseNumber} (${initParam.releaseDate})</div> 
    </body>
</html>
