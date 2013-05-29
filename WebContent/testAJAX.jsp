<!DOCTYPE html>
<html>
<head>

<meta charset='utf-8'/><link rel='stylesheet' href='css/bootstrap.min.css'/><link rel='stylesheet' href='css/bootstrap-responsive.min.css'/><link rel='stylesheet' href='css/font-awesome.css'><link rel='stylesheet' href='http://fonts.googleapis.com/css?family=Cantora+One' type='text/css'/><link rel='stylesheet' href='css/prettyPhoto.css'/><link rel='stylesheet' href='css/style2.css'/><link rel='stylesheet' href='css/widgets.css'/>
  <style> img.flicker{ height: 185px;  width: 240px; overflow: hidden;
  margin: 2px;
  loat: left; }</style>
  <script src="http://code.jquery.com/jquery-1.9.1.js"></script>
</head>
<body>
  
 
 
 
 
 
 
<!-- Navbar -->
<div class='navbar navbar-fixed-top navbar-inverse'><div class='navbar-inner'><div class='container'><a href='index' class='brand'><span class='bold'>Banoogle</span></a><div class='nav search-box'><form method='GET'><input type='text' name='q' placeholder='Search...' autocomplete='off' onkeyup='doCompletion' id='query' ><button id='submit-btn' type='submit' class='btn btn-info'><i class='icon-search'></i></button></form></div><span class='nav pic dictation'><img src='img/dictation.jpg'/></span><span class='nav pic correction'><img src='img/correction.png'/></span><ul class='nav advanced'><li class='dropdown pull-right'><a data-toggle='dropdown' class='dropdown-toggle' href='#'>Advanced Search <i class='icon-eye-open'></i></a><ul class='dropdown-menu'><li><form class='choices' method='POST'><input type='checkbox' name='api'/>Yahoo<br><input type='checkbox' name='api'/>Amazon<br><input type='checkbox' name='api'/>YouTube<br><input type='checkbox' name='api'/>Safe<br><hr><button type='submit' class='btn btn-info pull-right'><i class='icon-search'></i></button></form></li></ul></li></ul><ul class='nav pull-right'><li class='dropdown pull-right'><a data-toggle='dropdown' class='dropdown-toggle' href='#'>About Team <b class='caret'></b></a><ul class='dropdown-menu'><li><a href='#'>About CIS555</a></li><li><a href='#'>About Wacky </a></li></ul></li></ul></div></div></div><!-- End Navbar -->

<!-- Content -->
<div class='content'>

<!-- Sidebar -->
<div class='sidebar'><div class='sidebar-inner'><ul class='navi'><li class='nlightblue'><a href='search'><i class='icon-search'></i>Web</a></li><li class='nviolet current open'><a href='image'><i class='icon-picture'></i>Images</a></li><li class='nred'><a href='video'><i class='icon-facetime-video'></i>Video</a></li><li class='ngreen'><a href='shopping'><i class='icon-shopping-cart'></i>Shopping</a></li><li class='norange'><a href='map'><i class='icon-map-marker'></i>Map</a></li></ul></div></div>

<!-- Mainbar -->
<div class='mainbar'>

<!-- Mainbar Title -->
<div class='page-head'><h2 class='pull-left'>Image Search<span class='page-meta'>Powered by Michael, Angela, krishna, Yayang </span></h2><div class='bread-crumb pull-right'><a href='index.html'> <i class='icon-home'></i> Home </a><span class='divider'>/</span><a href='#' class='bread-current'>Image</a></div><div class='clearfix'></div></div><!-- End MainTitle -->

<!-- Mainbar Matter -->
<div class='matter'><div class='container-fluid'><div class='row-fluid'>

<div id="images">
  adfa<br>
  <div class='flickrNav'>
  <a id='prev' href='#'>Prev</a>
  <a id='next' href='#'>Next</a>
  </div>
 
</div>
<script>
$(document).ready(function(){
        
        $("#submit-btn").click(
                
                function() {
                    var query = $(":input").val();
                      var flickerAPI = "http://api.flickr.com/services/feeds/photos_public.gne?jsoncallback=?";
                      $.getJSON( flickerAPI, {
                        tags: query,
                        tagmode: "any",
                        format: "json"
                      })
                      .done(function( data ) {
                            $.each( data.items, function( i, item ) {
                              $( "<img/>" ).attr( "src", item.media.m ).attr("class", "flicker").appendTo( "#images").wrap("<a href='" + item.link + "'></a>");
                              if ( i === 1000 ) {
                                return false;
                              }
                            });
                });
                      return false;
         });
  
});
</script>


</div></div></div><!-- End Mainbar Matter -->;
</div><!-- End Mainbar -->
</div><!-- End Content -->
<span class='totop'><a href='#'><i class='icon-chevron-up'></i></a></span>
<script src='js/jquery.js'></script>
<script src='js/bootstrap.js'></script>




</body>
</html>