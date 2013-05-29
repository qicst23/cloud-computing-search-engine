<!DOCTYPE html>
<html>
<head>
<title>Fancy</title>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<script src="http://code.jquery.com/jquery-latest.min.js"></script>
<script type="text/javascript" src="js/jquery.fancybox.js"></script>
<link rel="stylesheet" type="text/css" href="css/jquery.fancybox.css"
	media="screen" />

<script>
    $(document).ready(function() {
        $(".fancybox")
            .fancybox({
               /*  openEffect  : 'none',
                closeEffect : 'none', */
                wrapCSS    : 'fancybox-custom',
                openEffect : 'elastic',
                openSpeed  : 150,
                closeEffect : 'elastic',
                closeSpeed  : 150,
                padding     : 5,
                margin      : 50,
            });
    });
</script>

</head>


<body>

	<a class="fancybox fancybox.iframe" href="http://www.upenn.edu">Upenn</a>
	<a class="fancybox fancybox.iframe"
		href="http://www.cis.upenn.edu/~cis455/">CIS455</a>

</body>
</html>
