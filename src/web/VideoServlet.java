package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gdata.util.ServiceException;

/**
 * The page for searching video
 * localhost:8080/UI/video
 */

public class VideoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	PrintWriter out;
	ArrayList<YouTubeResult> ytResults = null;

	public VideoServlet() {
		super();
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String query = request.getParameter("q");
		String videoId = request.getParameter("videoId");
		response.setContentType("text/html");
		out = response.getWriter();

		/** START HTML */
		out.println("<!DOCTYPE html><html lang='en'>");


		/** Head */
		printHeader();

		/** Body */
		out.println("<body>");

		/** Top Navigation */
		printTopNavigationWidget();

		/** START Sidebar + Main Content */
		out.println("\n<!-- Content -->\n" +
				"<div class='content'>");

		/** Sidebar */
		printSidebarWidget();     


		/** START Mainbar */
		out.println("\n<!-- Mainbar -->\n" +
				"<div class='mainbar'>");

		// content head
		printMainTitle();

		// START Mainbar Matter 
		out.println("\n<!-- Mainbar Matter -->\n" +
				"<div class='matter'><div class='container-fluid'><div class='row-fluid'>"); 

		// Main results returned by our search engine 
		printOurResultsWidget(request, query, videoId);   

		// Addition results: YouTube + Amazon
		printExtraResultsWidget(query);

		// END Mainbar Matter
		out.println("</div></div></div><!-- End Mainbar Matter -->;");

		// END Mainbar
		out.println("</div><!-- End Mainbar -->");

		// END Content
		out.println("</div><!-- End Content -->");

		// Scoll to top & JavaScript 
		out.println("<span class='totop'><a href='#'><i class='icon-chevron-up'></i></a></span>");
		out.println("<script src='js/jquery.js'></script>");
		out.println("<script src='js/bootstrap.js'></script>");

		/** End HTML */
		out.println("</body></html>");
	}


	// --------------------Helper Functions-----------------------------------//

	public void printHeader(){
		out.println("<head>" +
				"<meta charset='utf-8'/>" +
				"<link rel='stylesheet' href='css/bootstrap.min.css'/>" +
				"<link rel='stylesheet' href='css/bootstrap-responsive.min.css'/>" +
				"<link rel='stylesheet' href='css/font-awesome.css'>" +
				"<link rel='stylesheet' href='http://fonts.googleapis.com/css?family=Cantora+One' type='text/css'/>" +
				"<link rel='stylesheet' href='css/prettyPhoto.css'/>" +
				"<link rel='stylesheet' href='css/style2.css'/>" +
				"<link rel='stylesheet' href='css/widgets.css'/>" +
				"</head>");
	}


	public void printTopNavigationWidget(){
		/**
		 *  Top Navigation Bar 
		 */
		out.println("\n<!-- Navbar -->\n" +
				"<div class='navbar navbar-fixed-top navbar-inverse'>" +
				"<div class='navbar-inner'>" +
				"<div class='container'>" +

				// our brand name
				"<a href='index' class='brand'><span class='bold'>Globus</span></a>" + 

				// search form ##
				"<div class='nav search-box'>" +
				"<form method='GET'>" +
				"<input type='text' name='q' placeholder='Search...' " +
				"autocomplete='off' onkeyup='doCompletion' id='query' >" +
				"<button type='submit' class='btn btn-info'><i class='icon-search'></i></button>" +
				"</form>" +
				"</div>" +  

				// icons
				"<span class='nav pic dictation'><img src='img/dictation.jpg'/></span>" +
				"<span class='nav pic correction'><img src='img/correction.png'/></span>" +

				// advanced search
				"<ul class='nav advanced'>" +

				"<li class='dropdown pull-right'>" +
				"<a data-toggle='dropdown' class='dropdown-toggle' href='#'>" +
				"Advanced Search <i class='icon-eye-open'></i></a>" +

				"<ul class='dropdown-menu'><li>" +
				"<form class='choices' method='POST'>" +
				"<input type='checkbox' name='api'/>Yahoo<br>" +
				"<input type='checkbox' name='api'/>Amazon<br>" +
				"<input type='checkbox' name='api'/>YouTube<br>" +
				"<input type='checkbox' name='api'/>Safe<br>" +
				"<hr>" +
				"<button type='submit' class='btn btn-info pull-right'><i class='icon-search'></i></button>" +
				"</form></li></ul>" + 

				"</li></ul>" +  // end advanced search

				// About our team
				"<ul class='nav pull-right'><li class='dropdown pull-right'>" +
				"<a data-toggle='dropdown' class='dropdown-toggle' href='#'>" +
				"About Team <b class='caret'></b></a>" +

				"<ul class='dropdown-menu'>" +
				"<li><a href='#'>About CIS555</a></li>" +
				"<li><a href='#'>About Wacky </a></li>" +
				"</ul>" +

				"</li></ul>" + 

				"</div></div></div>" +
				"<!-- End Navbar -->");  // end top navigation bar

	}


	// --------------------Helper Functions-----------------------------------//


	public void printMainTitle(){

		out.println("\n<!-- Mainbar Title -->\n<div class='page-head'>" +
				"<h2 class='pull-left'>Video Search" +
				"<span class='page-meta'>Powered by Michael, Angela, krishna, Yayang </span>" +
				"</h2>" + 
				"<div class='bread-crumb pull-right'>" +
				"<a href='index'> <i class='icon-home'></i> Home </a>" +
				"<span class='divider'>/</span>" +
				"<a href='video' class='bread-current'>Video</a>" + 
				"</div>" +
				"<div class='clearfix'></div>" + 
				"</div><!-- End MainTitle -->"); // end page head

	}

	/**
	 * Main results widget
	 */
	public void printOurResultsWidget(HttpServletRequest request, String query, String videoId){
		if(query == null) return;
		String location = (String) request.getSession().getAttribute("loc");
		if(location == null) 
			location = "Philadelphia, PA";

		// videoId
		if(videoId == null) videoId = "videoId";

		// -------------------------------------------------------------------//
		// START main results
		out.println(
				"\n<!-- Mainbar Matter Span8 -->\n  " +
				"<div class='span8'><div class='widget wred'>" );

		// main results: head
		out.println("<div class='widget-head'>" +              
				"<div class='pull-left'>Top Videos on YouTube</div>" +
				"<div class='widget-icons pull-right'>" +   
				"<a href='#' class='wminimize'><i class='icon-chevron-up'></i></a>" +
				"<a href='#' class='wclose'><i class='icon-remove'></i></a>" +
				"</div>" +
				"<div class='clearfix'></div></div>");        

		// ----------------------------------------------------
		// main results: body ENTRIES!
		out.println("<div class='widget-content search-text'>" +
				"<div class='padd'>" +
				"<ul class='latest-news'>");

		// one normal entries
		printVideoSlider(query);

		// main player
		printPlayer(videoId);


		out.println("</ul></div></div>");
		// END main results: body ENTRIES!
		// ----------------------------------------------------

		printPagination();
		// FOOTER


		out.println("</div></div><!-- End Mainbar Matter Span8 -->");       
		// -------------------------------------------------------------------//
	}

	public void printSidebarWidget(){
		/** Sidebar */
		out.println("\n<!-- Sidebar -->\n" +
				"<div class='sidebar'><div class='sidebar-inner'>" +
				"<ul class='navi'>" +
				"<li class='nlightblue'><a href='search'><i class='icon-search'></i>Web</a></li>" + 
				"<li class='nviolet'><a href='image.jsp'><i class='icon-picture'></i>Images</a></li>" + 
				"<li class='nred current open'><a href='video'><i class='icon-facetime-video'></i>Video</a></li>" + 
				"<li class='ngreen'><a href='shopping'><i class='icon-shopping-cart'></i>Shopping</a></li>" +
				"<li class='norange'><a href='map'><i class='icon-map-marker'></i>Map</a></li>" +
				"</ul>" +
				"</div></div>");  // end sidebar
	}


	public void printOurResults(String query){
		out.println("<li>" +

				"<div class='btn-group pull-right'>" +
				"<button class='btn btn-mini blue'><i class='icon-save'></i></button>" +
				"<button class='btn btn-mini blue'><i class='icon-zoom-in'></i></button>" +
				"</div>" +

				"<h6><a href='#'>Title1</a> - <span id='date'>Jan 1, 2012</span></h6>" +
				"<a id='url' class='url'> http://facebook.com </a>" +
				"<p> This is the content !!!<br> The second </p>" +

				"<div class='btn-group pull-right'>" +
				"<button class='btn btn-mini btn-success'><i class='icon-thumbs-up'></i></button>" +
				"<button class='btn btn-mini btn-danger'><i class='icon-thumbs-down'></i></button>" +
				"</div><div class='clearfix'></div>" +

				"</li>");

		out.println("<li>" +

				"<div class='btn-group pull-right'>" +
				"<button class='btn btn-mini blue'><i class='icon-save'></i></button>" +
				"<button class='btn btn-mini blue'><i class='icon-zoom-in'></i></button>" +
				"</div>" +

				"<h6><a href='#'>Title3</a> - <span id='date'>Jan 1, 2013</span></h6>" +
				"<a id='url' class='url'> http://facebook.com </a>" +
				"<p> This is the content !!!<br> The second </p>" +

				"<div class='btn-group pull-right'>" +
				"<button class='btn btn-mini btn-success'><i class='icon-thumbs-up'></i></button>" +
				"<button class='btn btn-mini btn-danger'><i class='icon-thumbs-down'></i></button>" +
				"</div><div class='clearfix'></div>" +

				"</li>");
	}

	public void printVideoSlider(String query){

		int size = 6;
		try {
			ytResults = APIHelper.searchYouTube(query);
			size = ytResults.size();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}

		if(size < 6){return;}
		int count = 0;
		out.println("\n<!-- Span8 AmazonSlider -->\n  <li>");
		out.println("<h6>Top YouTube Results</h6>");

		out.println("<div class='container-fluid'>" +
				"<div class='row-fluid'>" +
				"<div class='carousel slide' id='myCarousel1'>" +

				// START carousel inner
				"<div class='carousel-inner'>");


		// item active
		out.println("<div class='item active'><ul class='thumbnails'>");
		for (count = 0; count < 3; count ++){
			YouTubeResult rs = ytResults.get(count);
			out.println("<li class='span4'>" +
					"<div class='thumbnail'><img src='" + rs.getImageURL() + "'><span>" + rs.getLength() + "</span></div>" + 
					"<div class='desc'><a href='video?q=" + query + "&videoId="+ rs.getId() + "'>" + rs.getTitle() + "</a>" +
					"<span> by " + rs.getUploader() +"<br>" + rs.getNumViews() + " views</span>" +
					"</div>" + //end desc
					"</li>");
		}
		out.println( "</ul></div>");

		// item
		out.println("<div class='item'><ul class='thumbnails'>");
		for (count = 3; count < 6; count ++){
			YouTubeResult rs = ytResults.get(count);
			out.println("<li class='span4'>" +
					"<div class='thumbnail'><img src='" + rs.getImageURL() + "'><span>" + rs.getLength() + "</span></div>" + 
					"<div class='desc'><a href='video?q=" + query + "&videoId="+ rs.getId() + "'>" + rs.getTitle() + "</a>" +
					"<span> by " + rs.getUploader() +"<br>" + rs.getNumViews() + " views</span>" +
					"</div>" + //end desc
					"</li>");
		}
		out.println( "</ul></div>");

		// END carousel inner
		out.println("</div>");

		//slider controller
		out.println("<a data-slide='prev' href='#myCarousel1' class='blue left carousel-control'> < </a>" +
				"<a data-slide='next' href='#myCarousel1' class='blue right carousel-control'> > </a>" +

				"</div></div></div>" + 
				"</li>");
	}


	public void printPagination(){
		out.println("<div class='widget-foot'>" +

				"<div class='pagination pull-right'><ul>" +
				"<li><a href='#'>Prev</a></li>" +
				"<li><a href='#'>1</a></li>" +
				"<li><a href='#'>2</a></li>" +
				"<li><a href='#'>3</a></li>" +
				"<li><a href='#'>4</a></li>" +
				"<li><a href='#'>Next</a></li>" +
				"</ul></div>" +
				"<div class='clearfix'></div>" +

				"</div>");	
	}

	public void printExtraResultsWidget(String query){
		if(query == null) return;


		/** START extra results */
		out.println("\n<!-- Mainbar Matter Span4 -->\n<div class='span4'>");

		/** 
		 * YOUTUBE 
		 * */
		// START YouTube widget
		if(ytResults != null){

			out.println("\n<!-- Span4 YouTube -->\n<div class='widget wviolet'>" );

			// main results: head
			out.println("<div class='widget-head'>" +              
					"<div class='pull-left'>YouTube Recommendation</div>" +
					"<div class='widget-icons pull-right'>" +   
					"<a href='#' class='wminimize'><i class='icon-chevron-up'></i></a>" +
					"<a href='#' class='wclose'><i class='icon-remove'></i></a>" +
					"</div>" +
					"<div class='clearfix'></div>" +
					"</div>");        

			// main results: body
			out.println("<div class='widget-content search-results'><div class='padd'>" +
					"<ul class='latest-news'>");

			for(int y = 6; y < 11; y ++){
				YouTubeResult rs = ytResults.get(y);
				// one item


				out.println("<li>" +
						"<div class='user'>" +
						"<div class='user-pic'><a href='#'><img src='"+ rs.getImageURL() + "'/></a></div>" + 
						"<div class='user-details'>" +
						"<a href='video?q=" + query + "&videoId=" + rs.getId() + "'>" + rs.getTitle() + "</a>" +
						"<br>by + <strong>" + rs.getUploader() + " - " + rs.getNumViews() + " views </strong><p>" + rs.getDesc() + "</p>" +
						"</div> <div class='clearfix'></div>" +
						"</div>" +    // end user
						"</li>"); 

			}

			out.println("</ul>" +
					"</div></div>");  

			// END YouTube widget
			out.println("</div><!-- End Span4 YouTube -->");
		}

		/** END extra results */
		out.println("</div>" +
				"<!-- End Mainbar Matter Span4 -->" );       
	}


	public void printRecommendedYouTubes(){
		// one item
		out.println(
				"<li>" +

				"<div class='user'>" +  // start user
				"<div class='user-pic'><a href='#'><img src='http://img.youtube.com/vi/vFD2gu007dc/default.jpg'/></a></div>" + 
				"<div class='user-details'><h6>YouTube</h6><p>ABSTRACT!!! of youtube</p>" +
				"</div> " +
				"<a href='#' class='btn btn-mini pull-right'><i class='icon-facebook-sign'></i></a>" +   // facebook twitter
				"<a href='#' class='btn btn-mini pull-right'><i class='icon-twitter-sign'></i></a>" + 
				"<div class='clearfix'></div>" +

				"</div>" +   	        // end user
				"</li>");
	}

	public void printPlayer(String id){
		String  URL = "http://www.youtube.com/embed/" + id +"?enablejsapi=1&origin=http://example.com";
		out.println(
				"<iframe id='player' width='640' height='390'" +
						"src="+ URL + 
				"'frameborder='0'></iframe>");
	}


	public void printAJAXScript(){

		//ajax
		out.println("<script>" +

				"var xmlhttp;" +
				"//initialization" +
				"function sendAJAXReq(URL, function){" +
				"if(window.XMLHttpRequest){xmlhttp=new XMLHttpRequest();}"+
				"else{xmlhttp=new ActiveXObject(\"Microsoft.XMLHTTP\";}" +
				"xmlhttp.onreadystatechange=function;" +
				"xmlhttp.open(\"GET\", URL, true);" +
				"xmlhttp.send();" +
				"}//end init" +

				"// auto completion" +
				"function doCompletion(){" +
				"alert(\"H!!!!\");" +
				"sendAJAXReq(\"search?q=happy\", function(){" +

				"if(xmlhttp.readyState==4 && xmlhttp.readyState==200){" +
				"document.getElementById(\"query\").innerHTML=xmlhttp.responseText;}" +

				"});" +
				"} // end auto complete" +

				"</script>"

				);
	}

}
