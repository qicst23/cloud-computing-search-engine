<!DOCTYPE html>
<html>
<head>
<title>search engine for geo</title>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<script src="http://code.jquery.com/jquery-latest.min.js"></script>
<script type="text/javascript" src="js/jquery.fancybox.js"></script>
<link rel="stylesheet" type="text/css" href="css/jquery.fancybox.css"
	media="screen" />


    <script src="http://j.maxmind.com/js/apis/geoip2/v2.0/geoip2.js" type="text/javascript"></script>
    
    
<script type="text/javascript">

var fillInPage = (function () {
    var updateCityText = function (geoipResponse) {
        var link = "/shelter?lat=" + geoipResponse.location.latitude;
        link += "&lon=" +geoipResponse.location.longitude;
        var cityName = geoipResponse.city.names.en || 'Your city';
        var cityHTML = '<a href="' + link + '">' + cityName + '</a>.';
        $("#city").html(cityHTML);
    };

    var onSuccess = function (geoipResponse) {
        updateCityText(geoipResponse);
    };

    /* If we get an error we will */
    var onError = function (error) {
        return;
    };

    return function () {
        geoip2.city( onSuccess, onError );
    };
}());

fillInPage();




</script>


</head>

<p> City:?</p>
<p id="city"></p>


</body>
</html>
