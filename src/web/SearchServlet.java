/**
 * @author Yayang Tian
 * http://cis.upenn.edu/~yaytian
 */

package web;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import storage.Result;

import com.google.gdata.util.ServiceException;

/**
 * The first page of search engine
 * localhost:8080/UI/search
 */

public class SearchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	PrintWriter out;
	ArrayList<YouTubeResult> ytResults = null;
	double duration = 0; 
	int thisPage = 1;

	boolean hasYahoo = false;
	boolean hasAmazon = false;
	boolean hasYouTube = false;

	// Cache our results in the server:  URL -> OurResult
	HashMap<String, Result> cachedResultsMap = null;
	//	ArrayList<AmazonResult> azResults = null;
	Integrator integrator = new Integrator("/Users/Alantyy/Cache");

	public SearchServlet() {
		super();
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		doGet(request, response);

	}
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String query = request.getParameter("query");
		String loc = request.getParameter("loc");
		String pagePos = request.getParameter("page");

		// APIs
		String[] apis = request.getParameterValues("api");
		hasYahoo = false;
		hasAmazon = false;
		hasYouTube = false;

		if(apis != null){
			for(String api : apis){
				if(api.equals("yahoo")) hasYahoo = true;
				if(api.equals("amazon")) hasAmazon = true;
				if(api.equals("youtube")) hasYouTube = true;
			}
		}

		if(loc != null){
			request.getSession().setAttribute("loc", "Philadelphia");
			query = query + loc;
		}

		System.out.println("[query? in doGets]" + query);
		response.setContentType("text/html");
		out = response.getWriter();

		/** START HTML */
		out.println("<!DOCTYPE html><html lang='en'>");


		/** Head */
		printHeader();

		/** Body */
		out.println("<body>");


		/** Top Navigation */
		printTopNavigationWidget(query);

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
		if(query == null || query.trim().isEmpty()) return;
		try {
			printOurResultsWidget(request, query, pagePos);
		} catch (OAuthMessageSignerException e) {
		} catch (OAuthExpectationFailedException e) {
		} catch (OAuthCommunicationException e) {
		}   

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
		out.println("<script src='js/bootstrap.js'></script>");

		/** End HTML */
		out.println("</body></html>");
	}


	/**
	 * @param all results in this page 
	 * @return
	 * @throws UnknownHostException 
	 */
	private ArrayList<Result> getThisPageResults(ArrayList<Result> allResults, String pagePos) throws UnknownHostException {
		ArrayList<Result> thisPageResults = new ArrayList<Result>();
		// get the page number
		System.out.println("[getting page number]\t");
		int pageNo = 1;
		int offset = 0;
		int max = 8;
		int capacity = allResults.size();
		if(max > capacity) 
			max = capacity;
		int end = max;

		if(pagePos != null){
			pageNo = Integer.parseInt(pagePos);
			if(pageNo == -1 && thisPage != 1){         // prev
				thisPage --;
			}else if(pageNo == 1000){  // next
				thisPage ++;
			}else{
				thisPage = pageNo;
			}
			offset=  max * (thisPage - 1);
			end = max * thisPage;
			// if the last page
			if(end > capacity) end = capacity;
		}

		System.out.println("[capacity]\t" + capacity);
		System.out.println("[pageNO]\t" + thisPage);
		System.out.println("[offset]\t" + offset);
		System.out.println("[max]\t" + max);
		System.out.println("[end]\t" + end);
		for(int i = offset; i < end; i ++){

			thisPageResults.add(allResults.get(i));
		}
		return thisPageResults;
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
				"<link rel='stylesheet' type='text/css' href='css/jquery.fancybox.css' media='screen' />" + 
				"<script src='http://code.jquery.com/jquery-latest.min.js'></script>" +
				"<script type='text/javascript' src='js/jquery.fancybox.js'></script>" + 
				"<script>" + 
				"	$(document).ready(function() { "+
				"		$('.fancybox')" + 
				"		.fancybox({" + 
				"					/*  openEffect  : 'none', " + 
				"					closeEffect : 'none', */   " + 
				"	                wrapCSS    : 'fancybox-custom', " + 
				"	                openEffect : 'elastic'," + 
				"	                openSpeed  : 150," + 
				"	                closeEffect : 'elastic'," + 
				"	                closeSpeed  : 150," + 
				"	                padding     : 5," + 
				"	                margin      : 50," + 
				"	     });" + 
				"	 });" + 
				"</script>"); 
		//		printAJAXScipt();
		out.printf("</head>");
	}


	public void printTopNavigationWidget(String query){
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
				"<input type='text' name='query' placeholder='Search...' " +
				"autocomplete='off' onkeyup='doCompletion' id='query' >" +
				"<button type='submit' class='btn btn-info'><i class='icon-search'></i></button>" +
				"</form>" +
				"</div>" +  

				// icons
				"<span class='nav pic dictation'><img src='img/dictation.jpg'/></span>" +
				"<span class='nav pic correction'><img src='img/correction.png'/></span>" +

				//advanced search
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
				"<li><a href='#'>About Globus </a></li>" +
				"</ul>" +

				"</li></ul>" + 

				"</div></div></div>" +
				"<!-- End Navbar -->");  // end top navigation bar

	}


	// --------------------Helper Functions-----------------------------------//


	public void printMainTitle(){

		out.println("\n<!-- Mainbar Title -->\n<div class='page-head'>" +
				"<h2 class='pull-left'>Results Integration" +
				"<span class='page-meta'>Powered by Michael, Angela, Krishna, Yayang </span>" +
				"</h2>" + 
				"<div class='bread-crumb pull-right'>" +
				"<a href='index'> <i class='icon-home'></i> Home </a>" +
				"<span class='divider'>/</span>" +
				"<a href='#' class='bread-current'>Web</a>" + 
				"</div>" +
				"<div class='clearfix'></div>" + 
				"</div><!-- End MainTitle -->"); // end page head

	}

	/**
	 * Main results widget
	 * @throws IOException 
	 * @throws OAuthCommunicationException 
	 * @throws OAuthExpectationFailedException 
	 * @throws OAuthMessageSignerException 
	 */
	public void printOurResultsWidget(HttpServletRequest request, String query, String pagePos) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, IOException{

		// get all results
		ArrayList<Result> allResults;
		ArrayList<Result> yResults;
		if(cachedResultsMap != null && !cachedResultsMap.isEmpty()){
			allResults =  (ArrayList<Result>) cachedResultsMap.values();
		}else{
			long startTime = System.currentTimeMillis();
			allResults = integrator.getResultsFromBackend(query);

			if(hasYahoo){
				yResults = APIHelper.searchYahoo(query);
				allResults.addAll(yResults);
				Collections.shuffle(allResults);   //#### shuffle???/
			}

			duration = ((System.currentTimeMillis() - startTime)/1000.00D);
			System.out.println("[duration]\t" + "Hi");
		}

		ArrayList<Result> results = getThisPageResults(allResults, pagePos);
		if(query == null || query.trim().isEmpty()) return;
		String location = (String) request.getSession().getAttribute("loc");
		if(location == null) 
			location = "Philadelphia, PA";

		// -------------------------------------------------------------------//
		// START main results
		out.println(
				"\n<!-- Mainbar Matter Span8 -->\n  " +
				"<div class='span8'><div class='widget wred'>" );

		// main results: head
		out.println("<div class='widget-head'>" +              
				"<div class='pull-left'>Globus Results</div>" +
				"<div class='widget-icons pull-right'>" +   
				"<a href='#' class='wminimize'><i class='icon-chevron-up'></i></a>" +
				"<a href='#' class='wclose'><i class='icon-remove'></i></a>" +
				"</div>" +
				"<div class='clearfix'></div></div>");        


		// START main results: body(statistics + sort)
		out.println("<div class='widget-content'><div class='padd'>");

		// spell checker
		System.out.println("[orig]\t" + query);
		String corrected = SpellChecker.correctQuery(query);
		System.out.println("[now]\t" + corrected);
		out.println("<h6><span class='red'>Including results for </span>" +
				"<span class='pull-right' id='feedback-hint'>Feedback changed the results. <a>Click to Reload.</a></span>" + 
				"<a href='search?query=" + corrected + "'>" + corrected + "</a>?</h6>");

		// statistics
		out.println("<div class='padd'>" +
				"<ul class='current-status pull-right'>" +
				"<li><span id='status1'></span><i class='icon-search green'></i><span class='bold'>" + allResults.size() + " results</span></li>" +
				"<li><span id='status2'></span><i class='icon-time green'></i><span class='bold'>" + duration + " seconds</span></li>" +
				"</ul></div>" +

				// sort by
				"<span> Sort by </span>" +
				"<a class='btn' href='search?q=" + query + "sort=relevence'>Relevence</a>&nbsp;" +
				"<a class='btn' href='search?q=" + query + "sort=autority'>Authority</a>&nbsp;" +
				"<span> Filter by </span>" +
				"<a class='btn' href='search?sort=nearby'>Nearby</a>&nbsp;" +

				"<div class='btn-group'>" +
				"<button class='btn dropbown-toggle' data-toggle='dropdown'>All Time<span class='caret'></span></button>" +
				"<ul class='dropdown-menu'>" +
				"<li><a href='#'>All time</a></li>" +
				"<li><a href='#'>Last 24 hours</a></li>" +
				"<li><a href='#'>Last 1 month</a></li>" +
				"<li><a href='#'>Last 1 year</a></li>" +
				"</ul>" +
				"</div>" + 

				" <hr>");


		// ----------------------------------------------------
		// main results: body ENTRIES!
		out.println("<div class='widget-content search-text'>" +
				"<div class='padd'>" +
				"<ul class='latest-news'>");

		// one normal entries

		printAmazonSlider(request, query);
		printOurResults(request, results);
		printVideoSlider(request, query);


		out.println("</ul></div></div>");
		// END main results: body ENTRIES!
		// ----------------------------------------------------

		printPagination(query, pagePos);
		// FOOTER

		out.println("\n</div></div>"); // END main results: body(statistics + sort)

		out.println("</div></div><!-- End Mainbar Matter Span8 -->");       
		// -------------------------------------------------------------------//
	}

	public void printSidebarWidget(){
		/** Sidebar */
		out.println("\n<!-- Sidebar -->\n" +
				"<div class='sidebar'><div class='sidebar-inner' >" +
				"<ul class='navi'>" +
				"<li class='nlightblue current open'><a href='search'><i class='icon-search'></i>Web</a></li>" + 
				"<li class='nviolet'><a href='image.jsp'><i class='icon-picture'></i>Flickr Image</a></li>" + 
				"<li class='nred'><a href='video'><i class='icon-facetime-video'></i>YouTube Video</a></li>" + 
				"<li class='ngreen'><a href='shopping'><i class='icon-shopping-cart'></i>eBay Shopping</a></li>" +
				"<li class='norange'><a href='map'><i class='icon-map-marker'></i>Google Map</a></li>" +
				"</ul>" +
				"</div></div>");  // end sidebar
	}


	public void printOurResults(HttpServletRequest request, ArrayList<Result> results) throws UnknownHostException{
		String url = "";
		String fullURL = "";

		String title = "";
		String yImg = "";

		for(Result mr : results){
			url = mr.getURL();
			fullURL = url;
			if(!url.toLowerCase().contains("http")) fullURL = "http://" + fullURL;
			System.out.println("[!!!!!URL full]\t" + fullURL);
			if(mr.getType() == 1){ yImg = ""; }
			if(mr.getType() == 2){ yImg = "&nbsp; <img style='width: 25px; opacity: 0.8' src='img/yahoo.jpeg'/>"; }
			out.println("<li>" +

				"<div class='btn-group pull-right'>" +
				"<button class='btn btn-mini blue'><i class='icon-save'></i></button>" +
				"<a class='btn btn-mini blue fancybox fancybox.iframe' href='" + fullURL + "'><i class='icon-zoom-in'></i></a>" +
				"</div>" +

				"<a class='title' href='" + fullURL + "'>" + mr.getTitle() + "</a>" +   
				"<span id='date'>: " + yImg + "</span>" +
				"<div class='url' id='url' class='url'> "+ url + "</div>" +
				"<p class='snippet'> " + mr.getSnippet() +  "</p>" +

				"<button class='pull-right btn btn-mini btn-success' href='search?url=" + url + "&feedback=up" + "'><i class='icon-thumbs-up'></i></button>" +
				"<button class='pull-right btn btn-mini btn-danger'><i class='icon-thumbs-down'></i></button>" +

				"<div class='clearfix'></div>" +

					"</li>");
		}
	}

	public void printVideoSlider(HttpServletRequest request, String query){
		if(query == null || query.trim() == "") return;
		//		if(!hasYouTube) return;
		int size = 6;
		try {
			ytResults = APIHelper.searchYouTube(query);
			size = ytResults.size();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}

		System.out.println("[query??]\t" + query);
		System.out.println("[size]\t" + size);
		if(size < 6){return;}
		int count = 0;
		out.println("\n<!-- Span8 AmazonSlider -->\n" +
				"<h6>Top Amazon Results</h6><li>");
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

			log("[request get!!!!]  video?q=" + query + "&videoId="+ rs.getId());
			out.println("<li class='span4'>" +
					"<div class='thumbnail'><img src='" + rs.getImageURL() + "'><span>" + rs.getLength() + "</span></div>" + 
					"<div class='desc'><a href='video?q=" + query + "&videoId="+ rs.getId() + "'>" + rs.getTitle() + "</a>" +
					"<br><span> by " + rs.getUploader() +" - " + rs.getNumViews() + " views</p></span>" +
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
					"<br><span> by " + rs.getUploader() +" - " + rs.getNumViews() + " views</p></span>" +
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


	public void printAmazonSlider(HttpServletRequest request, String query){
		if(query == null || query.trim().isEmpty()) return;
		if(!hasAmazon) return;
		ArrayList<AmazonResult> results = APIHelper.searchAmazon(query);
		if(results == null) return;
		int size = results.size();
		if(size < 8){return;}
		int count = 0;
		out.println("\n<!-- Span8 AmazonSlider -->\n" +
				"<li>" +
				"<div class='container-fluid'>" +
				"<div class='row-fluid'>" +
				"<div class='carousel slide' id='myCarousel2'>" +

				// START carousel inner
				"<div class='carousel-inner'>");

		// item active
		out.println("<div class='item active'><ul class='thumbnails'>");
		for (count = 0; count < 4; count ++){
			AmazonResult rs = results.get(count);
			out.println("<li class='span3'>" +
					"<div class='thumbnail'><img src='" + rs.getImageURL() + "'><span>" + rs.getPrice() + "</span></div>" + 
					"<div class='desc'><a src= '"+ rs.getTitle() + "'>" + rs.getTitle() + "</a>" +
					"<span>" + rs.getReviewHTML() + "</span>" +
					"</div>" + //end desc
					"</li>");
		}
		out.println( "</ul></div>");

		// item active
		out.println("<div class='item'><ul class='thumbnails'>");
		for (count = 4; count < 8; count ++){
			AmazonResult rs = results.get(count);
			out.println("<li class='span3'>" +
					"<div class='thumbnail'><img src='" + rs.getImageURL() + "'><span>" + rs.getPrice() + "</span></div>" + 
					"<div class='desc'><a src= '"+ rs.getTitle() + "'>" + rs.getTitle() + "</a>" +
					"<span>" + rs.getReviewHTML() + "</span>" +
					"</div>" + //end desc
					"</li>");
		}
		out.println( "</ul></div>");

		// END carousel inner
		out.println("</div>");

		//slider controller
		out.println("<a data-slide='prev' href='#myCarousel2' class='blue left carousel-control'> < </a>" +
				"<a data-slide='next' href='#myCarousel2' class='blue right carousel-control'> > </a>" +

				"</div></div></div>" + 
				"</li>");
	}



	public void printPagination(String query, String pagePos){
		//		int page = 1;
		//		if(pagePos != null){
		//			page = Integer.parseInt(pagePos);
		//		}

		out.println("<div class='widget-foot'>" +
				"<div class='pagination pull-right'><ul>");

		out.println("<li><a href='search?query=" + query + "&page="+ (thisPage - 1) + "'>Prev</a></li>" +
				"<li><a href='search?query=" + query + "&page="+ thisPage + "'>" + thisPage + "</a></li>" +
				"<li><a href='search?query=" + query + "&page="+ (thisPage + 1) + "'>" + (thisPage + 1) + "</a></li>" +
				"<li><a href='search?query=" + query + "&page=" + (thisPage + 2) + "'>" + (thisPage + 2) + "</a></li>" +
				"<li><a href='search?query=" + query + "&page="+ (thisPage + 1) + "'>Next</a></li>");

		out.println("</ul></div>" +
				"<div class='clearfix'></div>" +

				"</div>");	
	}

	public void printExtraResultsWidget(String query){
		if(query == null) return;


		/** START extra results */
		out.println("\n<!-- Mainbar Matter Span4 -->\n<div class='span4'>");


		/** 
		 * YELP 
		 * */
		// START Yelp widget
		ArrayList<YelpResult> yelpResults = APIHelper.searchYelp(query);
		if(yelpResults != null){

			out.println("\n<!-- Span4 Ebay -->\n<div class='widget wblue'>" );

			// main results: head
			out.println("<div class='widget-head'>" +              
					"<div class='pull-left'>Yelp Recommendation</div>" +
					"<div class='widget-icons pull-right'>" +   
					"<a href='#' class='wminimize'><i class='icon-chevron-up'></i></a>" +
					"<a href='#' class='wclose'><i class='icon-remove'></i></a>" +
					"</div>" +
					"<div class='clearfix'></div>" +
					"</div>");        

			// main results: body
			out.println("<div class='widget-content search-results'><div class='padd'>" +
					"<ul class='latest-news'>");

			for(int y = 6; y < 8; y ++){
				YelpResult rs = yelpResults.get(y);
				// one item


				out.println("<li>" +
						"<div class='user'>" +
						"<div class='user-pic'><a href='#'><img src='"+ rs.getSnippetImage() + "'/></a></div>" + 
						"<div class='user-details'>" +
						"<a href='" + rs.getURL() + "'>" + rs.getName() + "</a>" +
						"<br><img style='height: 15px;' src='"+ rs.getRatingImage() + "'/><br><strong>" + rs.getDistance()  + " M</strong>" +
						"<p>" + rs.getSnippetText() + "</p>" +
						"" + 
						"</div> <div class='clearfix'></div>" +
						"</div>" +    // end user
						"</li>"); 

			}

			out.println("</ul>" +
					"</div></div>");  

			// END Ebay widget
			out.println("</div><!-- End Span4 YouTube -->");
		}


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

			for(int y = 6; y < 8; y ++){
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




		/** EBAY */


		/** GOOGLE MAP */



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
				"<a href='#' class='btn btn-mini pull-right'><i class='icon-facebook-sign'></i></a>" +   // facebook twitter
				"<a href='#' class='btn btn-mini pull-right'><i class='icon-twitter-sign'></i></a>" +
				"</div> " +
				"<div class='clearfix'></div>" +

				"</div>" +   	        // end user
				"</li>");
	}

	public void printRecommendedAmazon(){
		out.println("");
	}

	public void printAJAXScipt(){
		out.println(
				"<script src='http://code.jquery.com/jquery-latest.min.js'></script>" + 
						"<script>" + 
						"   $(document).ready(function() {" +        
						"      $('.icon-thumbs-up').click(function() {  alert('How');" +            
						"        $.get('feedback', function(jsonList) {" +
						"           var feedbackHint = document.geDocumentById('#feedback-hint');" +
						"alert(feedbackHint);" +
						"      $.each(jsonList, function(index, amazonResult){" +
						"         $('<tr>').appendTo($table)" +
						"        .append($('<td>').text(amazonResult.ASIN))" +
						"       .append($('<td>').text(amazonResult.title))" +
						"      .append($('<td>').text(amazonResult.title))" +
						"     .append($('<td>').text(amazonResult.imageURL));" +                  
						"});" +
						"   $('#result').text(responseText);  " +
						"}); " +
						"}); " +
						"});" +
				"</script>");
	}

}
