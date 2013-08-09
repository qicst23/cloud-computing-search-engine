/**
 * @author Yayang Tian - yaytian@cis.upenn.edu
 */
package web;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import storage.Result;

import com.google.gson.Gson;

/**
 * The first page of search engine
 * localhost:8080/UI/video
 */

public class FeedbackServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	PrintWriter out;


	public FeedbackServlet() {
		super();

	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("[in feedback servlet post]\t" );
		String query = request.getParameter("q");
		response.setContentType("text/xml");
		response.setHeader("Cache-Control", "no-cache");
		response.getWriter().write("<response>" + "receive" + query + "</response>");
	}


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String query = request.getParameter("q");
		if(query.isEmpty()) return;
//		
		
		String url = "";
		String title = "";
		String snippet = "";
		Double score = 0D;
		int type = 1;
		Result r1 = new Result("www.upenn.edu", "UPenn", "IVY", 1.8D, 1);
		Result r2 = new Result("www.yahoo.com", "Yahoo", "Search", 1.5D, 2);
		Result r3 = new Result("www.amazon.com", "Amazon", "Shop", 1.2D, 1);
		List<Result> list = new ArrayList<Result>();
		list.add(r1);
		list.add(r2);
		list.add(r3);
		String json = new Gson().toJson(list);
		
		query = "philadelphia food";
//		String json = Integrator.generateResult(query);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json);
	}

	public void printHead(){
		out.println("<head>" +
				" <script type='text/javascript'" +  
				"src='http://code.jquery.com/jquery-1.9.1.js'></script>"  +
				"<script type='text/javascript' language='javascript'>"  +
				" $(document).ready(function() {" + 
				"$('#searchForm').click(function(event){ " +
				" $('#result').load('/feedback');  " +
				"});" +
				"});"+
				"</script></head>");
	}

}
