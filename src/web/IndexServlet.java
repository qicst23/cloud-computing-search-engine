/**
 * @author Yayang Tian
 * http://cis.upenn.edu/~yaytian
 */
 
package web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The first page of search engine
 * localhost:8080/UI/index
 */

public class IndexServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public IndexServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<!DOCTYPE html><html lang='en'>");

		/** head */
		out.println("<head>" +
				"<meta charset='utf-8'/>" +
				"<link rel='stylesheet' href='css/bootstrap.min.css'/>" +
				"<link rel='stylesheet' href='css/bootstrap-responsive.min.css'/>" +
				"<link rel='stylesheet' href='css/font-awesome.css'>" +
				"<link rel='stylesheet' href='http://fonts.googleapis.com/css?family=Cantora+One' type='text/css'/>" +
				"<link rel='stylesheet' href='css/style1.css'/>" +
				"<script src='http://code.jquery.com/jquery-latest.min.js'></script>" +
				"<script>" +
				"$(document).ready(function(){" +
				"$('#tog').click(function(){$('#apis').slideToggle();});" +
				"});</script>" +
				"</head>");

		/** body */
		out.println("<body>");

		/** nav-bar */
		out.println("<div class='navbar navbar-fixed-top navbar-inverse'>" +
				"<div class='navbar-inner'>" +
				"<div class='container'>" +
				"<ul class='nav pull-right'>" +
				"<li class='pull-right'><a href='#'>Globus@Penn</a></li>" +
				"</ul>" +
				"</div></div></div>");

		/** search box */
		out.println("<div class='container main'>");  // START container

		out.println("<div class='row'>" + 			  // intro words
				"<div class='span12'>" +
				"<h1>Globus</h1>" +
				"<span class='intro'>Search engine makes global <i class='icon-globe'></i></span>" +
				"</div>" +
				"</div>");

		out.println("<div id='search-form'>" +        // search form
				"<form class='form-inline' action='search' method='GET'>" +

				"<input type='text' name='query' placeholder='Search...'>" +
				"<button type='submit' class='btn btn-info'><i class='icon-search'></i></button>" +

				"<div class='success-message'></div>" + 
				"<a id='tog' style='opacity:0.8'>Advanced &nbsp;<i class='icon-th-list'></i></a>");


		out.println("<div id='apis' class='row'>" +			  // API choices
				"<div class='span12'>" +

				"<section class='purple'>" +		  // Yahoo
				"<input type='checkbox' name='api' value='yahoo' id='yahoo'>Yahoo</input>" +
				"<label for='yahoo'></label>" +
				"</section>\n" +

				"<section class='orange'>" +		  // Amazon
				"<input type='checkbox' name='api' value='amazon' id='amazon'>Amazon</input>" +
				"<label for='amazon'></label>" +
				"</section>" +

				"<section class='red'>" +	           // YouTube
				"<input type='checkbox' name='api' value='youtube' id='youtube'>YouTube</input>" +
				"<label for='youtube'></label>" +
				"</section>" +


				"<section class='green'>" +		  	   // Green
				"<input type='checkbox' name='api' value='safe' id='safe'>Safe</input>\n" + 
				"<label for='safe'></label>" +
				"</section>" +

			    "</div>" +
			    "</form>" +
				"</div>");

		out.println("</div>");


		out.println("</div>");						 // End Container

		/** Footer */
		out.println("<footer>" +
				"<div class='container row-fluid span12'>" +
				"Copyright &copy; 2013 - <a href='http://cis555.co.nf'>" +
				"Michael, Angela, Krishna, Yayang</a>" +
				"</div>" +
				"</footer>"); 
		out.println("<div class='clearfix'></div>");

		/** Javascript */
		out.println("<script src='js/jquery.js'></script>");
		out.println("<script src='js/bootstrap.js'></script>");

		out.println("</body></html>");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

}
