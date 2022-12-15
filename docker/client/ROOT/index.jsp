<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Client</title>
</head>
<body>
    <h1>Testing client-side query (with CORS)</h1>
    <hr/>
    <ul id="chan-list">
    </ul>
    <hr/>
</body>
<script>
    function toHtml(data) {
        console.log(data);

        let list = document.getElementById("chan-list");
        var fragList = document.createDocumentFragment();
        data.forEach(function (item) {
            var li = document.createElement('li');
            li.textContent = JSON.stringify(item);
            fragList.appendChild(li);
        });
        list.appendChild(fragList);
    }

    fetch('http://localhost:8080/myquery/channel?q=chan%25&m=docker')
        .then((response) => response.json())
        .then((data) => toHtml(data));
</script>
</html>