/**
 * @author Yayang Tian - yaytian@cis.upenn.edu
 */
package web;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.ebay.services.client.ClientConfig;
import com.ebay.services.client.FindingServiceClientFactory;
import com.ebay.services.finding.FindItemsByKeywordsRequest;
import com.ebay.services.finding.FindItemsByKeywordsResponse;
import com.ebay.services.finding.FindingServicePortType;
import com.ebay.services.finding.PaginationInput;
import com.ebay.services.finding.SearchItem;
import com.google.gdata.client.youtube.YouTubeQuery;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.VideoFeed;
import com.google.gdata.data.youtube.YouTubeMediaGroup;
import com.google.gdata.data.youtube.YtStatistics;
import com.google.gdata.util.ServiceException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import storage.Result;


/**
 * APIs includes Yahoo, Amazon, YouTube, Wiki, eBay, Yelp, MaxMind, Google Map
 * @author Alantyy
 *
 */
public class APIHelper {

	public final static int YAHOO = 2;


	/**
	 * API for YouTube
	 * @return json result of youtube
	 */
	public static ArrayList<YouTubeResult> searchYouTube(String keyword) throws IOException, ServiceException{
		ArrayList<YouTubeResult> results = new ArrayList<YouTubeResult>();
		StringBuffer apiResult = new StringBuffer();		


		YouTubeService service = new YouTubeService("Yayang Tian", "AI39si4BMI10-cEQjlaTcmFxU-1AwJJW86R-Ocfra19KIbqnVG7K18vnN0_5XUrrWEObNa69LIqmkie5r4z4bo-XsfU-mfz3Kg");
		YouTubeQuery query = new YouTubeQuery(new URL("http://gdata.youtube.com/feeds/api/videos"));
		query.setFullTextQuery(keyword);
		query.setOrderBy(YouTubeQuery.OrderBy.RELEVANCE);
		query.setSafeSearch(YouTubeQuery.SafeSearch.NONE);
		query.setMaxResults(20);
		VideoFeed videoFeed = service.query(query, VideoFeed.class);

		VideoEntry topEntry = videoFeed.getEntries().get(0);
		if (videoFeed.getEntries().get(0).getRelatedVideosLink() != null) {
			String relatedURL = topEntry.getRelatedVideosLink().getHref();
			VideoFeed vf = service.getFeed(new URL(relatedURL), VideoFeed.class);
			appendYouTube(results, videoFeed, false);
		}

		appendYouTube(results, videoFeed, true);

		return results;
	}

	public static void appendYouTube(ArrayList<YouTubeResult>results, VideoFeed videoFeed, boolean isRecommended){
		String id = "";
		String title = "";
		String desc = "";
		String URL = "";

		String imageURL = "";
		String length = "";
		String numViews = "";
		String uploader = "";
		String date = "";

		log(isRecommended);

		int count = 0;
		int max = 6;
		if(isRecommended) max = 12;
		for(VideoEntry ve : videoFeed.getEntries()){
			count ++;
			if(count > max) break;
			title = ve.getTitle().getPlainText();			  // title

			// media group
			YouTubeMediaGroup mg = ve.getMediaGroup();
			id = mg.getVideoId();				              // id
			uploader = mg.getUploader();                      // uploader
			date = ve.getUpdated().toString();                // date
			if(date != null){
				date = date.split("T")[0];
			}
			YtStatistics stats = ve.getStatistics();
			if(stats != null ) {
				numViews = String.valueOf(stats.getViewCount());   // views
			}
			long duration = mg.getYouTubeContents().get(0).getDuration();// length
			imageURL = mg.getThumbnails().get(0).getUrl();
			length = duration/60 +  ":" + duration % 60;
			desc = mg.getDescription().getPlainTextContent();  // description
			URL = mg.getPlayer().getUrl();			           // URL
			YouTubeResult result = new YouTubeResult(id, title, desc, URL,
					imageURL, length, numViews, uploader, date, isRecommended);

			log(URL);
			results.add(result);
		}


	}
	/*************************  Amazon  ****************************************
	 * Searching Amazon items
	 * @param keywords
	 * @return
	 */
	public static ArrayList<AmazonResult> searchAmazon(String keywords){

		// Sign params
		String endPoint = "ecs.amazonaws.com";
		String accessKey = "AKIAIPRVUI7FO5TVULHA";
		String secretKey = "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";

		// important data to retrieve
		String requestURL = null;

		AmazonRequestsHelper helper;
		try {
			helper = AmazonRequestsHelper.getInstance(endPoint, accessKey, secretKey);				
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}

		Map<String, String> params = new HashMap<String, String>();
		params.put("Service", "AWSECommerceService");
		params.put("Operation", "ItemSearch");
		params.put("Condition", "All");
		params.put("Availability", "Available");
		params.put("SearchIndex", "All");
		params.put("Keywords", keywords);
		params.put("AssociateTag", "enersearengi-20");
		params.put("ResponseGroup", "Medium, Reviews");
		//		params.put("ResponseGroup", "Small, , OfferSummary, TopSellers");
		//		params.put("TruncateReviewsAt", "1");
		params.put("RelationshipType", "Episode");



		// sign the request 
		requestURL = helper.sign(params);      System.out.println(requestURL);

		//		return null;
		return parseAmazonResponse(requestURL);

	}


	/**
	 * Parse Amazon results
	 * @return a list of results
	 */
	public static ArrayList<AmazonResult> parseAmazonResponse(String requestURL){
		ArrayList<AmazonResult> results = new ArrayList<AmazonResult>();

		// variables for construction

		String title = "";
		String price = "";
		String company = "";
		String desc = "";
		String reviewHTML = "";
		String detailURL = "";
		String imageURL = "";
		String reviewURL = "";  // just for aided

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();	

			// Here is the doc xml tree
			Document doc = builder.parse(requestURL);
			if(doc == null)return null;

			// at most 8 results displayed
			int total = 8; 
			NodeList itemDom = doc.getElementsByTagName("Item");
			int numResults = itemDom.getLength();
			if(numResults < 8){
				total = numResults;
			}

			for(int p = 0; p < total; p ++){

				Element itemRoot = (Element) itemDom.item(p);
				reviewURL = firstChildTag(itemRoot, "IFrameURL", p);
				title = firstChildTag(itemRoot, "Title", p);
				price = lastChildTag(itemRoot, "LowestNewPrice", p);
				company = lastChildTag(itemRoot, "Manufacturer", p);
				desc = firstChildTag(itemRoot, "Content", p);
				reviewHTML = Jsoup.connect(reviewURL).get().select(".crIFrameNumCustReviews").html();
				detailURL = firstChildTag(itemRoot, "DetailPageURL", p);
				imageURL = firstChildTag(itemRoot, "LargeImage", p);


				// title
				if(title.length() > 20){
					title = title.substring(0, 19) + "..";
				}

				// add to results
				AmazonResult rs = new AmazonResult(title, price, company, desc,
						reviewHTML,detailURL, imageURL);
				results.add(rs);
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return results;
	}


	/**************************  Yahoo  **************************************** 
	 * API for Yahoo
	 * @param keyword
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static ArrayList<Result> searchYahoo(String query) throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException{

		String consumerKey = "dj0yJmk9TzZxelZHOTRhaFdaJmQ9WVdrOWIzRm5lV3N6TjJzbWNHbzlOalkzTXpNNU5EWXkmcz1jb25zdW1lcnNlY3JldCZ4PTkx";
		String consumerSecret = "7e4d7636c65033d5db770bd42da4d29e60d24c43";
		String json = null; 

		// Consumer
		OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);

		// Create the final URL
		String queryEncoded = URLEncoder.encode(query, "UTF-8");
		String url = "http://yboss.yahooapis.com/ysearch/web?q=" + queryEncoded;

		// Connect
		URL urlObject = new URL(url);
		HttpURLConnection uc = (HttpURLConnection) urlObject.openConnection();
		uc.setRequestProperty("Accept-Charset", "UTF-8");
		if (consumer != null) {
			consumer.sign(uc);
			uc.connect();  // connect
		}

		// get response
		int respCode = uc.getResponseCode();
		if (respCode == 200 || respCode == 401 || respCode == 404) {
			InputStreamReader inputStreamReader = new InputStreamReader((respCode == 200 ? uc.getInputStream() : uc.getErrorStream()),"UTF-8");
			BufferedReader rd = new BufferedReader(inputStreamReader);
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			rd.close();
			json = new String(sb.toString());
		}

		// Send the request
		if(respCode != 200)  System.out.println("Error: " + respCode);
		//		System.out.println("[debug]\t" + json);

		// Parse JSON and resturn
		return parseYahooResponse(json);

	}

	public static ArrayList<Result> parseYahooResponse(String json){
		ArrayList<Result> yResults = new ArrayList<Result>();
		String[] pieces = json.split("\\}");
		String jsonReg  = "\"(url|title|abstract)\":\"(.+?)\"";
		Pattern p = Pattern.compile(jsonReg);

		String url = "-";
		String title = "-";
		String snippet = "-";
		Double score = 1.80D;

		for(String piece : pieces){
			Matcher m = p.matcher(piece);
			while(m.find()){
				String key = m.group(1);
				String value = m.group(2);
				if(key.equals("url")) url = value.replaceAll("\\\\/", "/");
				else if(key.equals("title")) title = value.replaceAll("\\\\/", "/");
				else if(key.equals("abstract"))snippet = value.replaceAll("\\\\/", "/");
				//					score = getYahooScore(query, title, snippet);
				score = score - 0.01D;
				//					
				System.out.println("[url]\t" + url);
				System.out.println("[title]\t" + title);
				System.out.println("[abstract]\t" + snippet);
				//					System.out.println("[score]\t" + score);
				//					
				//					
				yResults.add(new Result(url, title, snippet, score, 2));
			}
		}

		System.out.println("Yahoo searched.\t");
		return yResults;
	}



	/**************************  Ebay  **************************************** 
	 * API for Ebay
	 * @param keyword
	 * @return
	 * @throws IOException 
	 * @throws UnsupportedEncodingException
//	 */
	public static String searchEbay(String keyword){

		ClientConfig config = new ClientConfig();
		config.setEndPointAddress("http://svcs.sandbox.ebay.com/services/search/FindingService/v1");
		config.setApplicationId("Universi-d2d1-4f66-bfe3-512cbb080b31");

		// Create a service client
		FindingServicePortType serviceClient = FindingServiceClientFactory.getServiceClient(config);
		FindItemsByKeywordsRequest request = new FindItemsByKeywordsRequest();
		request.setKeywords("kindle");
		PaginationInput pi = new PaginationInput();
		pi.setEntriesPerPage(4);
		request.setPaginationInput(pi);

		// Call service
		try{
			FindItemsByKeywordsResponse result = serviceClient.findItemsByKeywords(request);
			System.out.println("Result " + result.getSearchResult().getCount() + " items." );
			List<SearchItem> items = result.getSearchResult().getItem();
			for(SearchItem item : items) {
				System.out.println("title" + item.getTitle());
			}
		}
		catch(Exception e){
			return "";
		}
		return keyword;
	}


	/**
	 * API for wiki
	 * @param query
	 * @return
	 * @throws IOException
	 */
	public static String searchWiki(String query) throws IOException{
		String wikiRequest = "http://en.wikipedia.org/w/api.php?format=json&action=query&prop=revisions&rvprop=content&format=json&titles=" + query;
		System.out.println("wikiHTML");
		String json = "";
		URL urlObject = new URL(wikiRequest);
		HttpURLConnection uc = (HttpURLConnection) urlObject.openConnection();
		// get response
		int respCode = uc.getResponseCode();
		if (respCode == 200 || respCode == 401 || respCode == 404) {
			InputStreamReader inputStreamReader = new InputStreamReader((respCode == 200 ? uc.getInputStream() : uc.getErrorStream()),"UTF-8");
			BufferedReader rd = new BufferedReader(inputStreamReader);
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			rd.close();
			json = new String(sb.toString());
		}

		// Send the request
		if(respCode != 200)  System.out.println("Error: " + respCode);
		return json;
	}


	/**************************  Yelp  **************************************** 
	 * API for Yelp
	 * @param keyword
	 * @throws UnsupportedEncodingException
//	 */
	public static ArrayList<YelpResult> searchYelp(String keyword){
		// init
		String consumerKey = "7ddkWzXYkesAdtX5VJMkbg";
		String consumerSecret = "_MciwRbPwS7cLn-M4ufvTHEr1V0";
		String token = "hbyoHwN1eKeJNOeLb5s2zpxvfz1GJIns";
		String tokenSecret= "nD1f8g6LzOeJNmZ8wR2TkXm74zM";
		OAuthService service = new ServiceBuilder().provider(YelpOauth.class).apiKey(consumerKey).apiSecret(consumerSecret).build();
		Token accessToken = new Token(token, tokenSecret);

		// request
		String latitude = "39.9597";
		String longitude = "-75.1968";
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.yelp.com/v2/search");
		request.addQuerystringParameter("term", keyword);
		request.addQuerystringParameter("ll", latitude + "," + longitude);
		service.signRequest(accessToken, request);
		Response response = request.send();
		String result = response.getBody();
		return parseYelpResponse(result);
	}


	public static ArrayList<YelpResult> parseYelpResponse(String result){
		ArrayList<YelpResult> yelpResults = new ArrayList<YelpResult>();

		// argumensts
		String name = "-"; 
		String snippetText = "";
		String snippetImage = "";
		String distance = ""; 
		String ratingImage = "";
		String url = "";


		JsonElement jse = new JsonParser().parse(result);
		JsonArray jsa = jse.getAsJsonObject().getAsJsonArray("businesses");
		System.out.println(jsa.size());

		for (int i= 0; i<jsa.size(); i++ ) {

			//JsonElement elem = jsa.get(i);
			JsonObject jsonObject = jsa.get(i).getAsJsonObject();
			name = jsonObject.get("name").getAsString();
			snippetText = jsonObject.get("snippet_text").getAsString();
			distance = jsonObject.get("distance").getAsString();
			url = jsonObject.get("url").getAsString();
			ratingImage = jsonObject.get("rating_img_url").getAsString();

			try{
				snippetImage = jsonObject.get("snippet_image_url").getAsString();
			}catch(Exception e){
				e.printStackTrace();
				snippetImage = ratingImage;
			}
			if(snippetText.length() > 60) snippetText = snippetText.substring(0, 60);
			YelpResult yResult = new YelpResult(name, snippetText, snippetImage, distance, ratingImage, url);
			yelpResults.add(yResult);
			System.out.println("===========================================================");
		}
		return yelpResults;
	}



	/**************************  Helper Functions  *******************/
	public static String firstChildTag(Element doc, String tag, int i){
		NodeList tagList = doc.getElementsByTagName(tag);
		if(tagList == null || tagList.getLength() == 0) 
			return "-";

		return tagList.item(0).getFirstChild().getTextContent();
	}

	public static String lastChildTag(Element doc, String tag, int i){
		NodeList tagList = doc.getElementsByTagName(tag);
		if(tagList == null || tagList.getLength() == 0) 
			return "-";

		return tagList.item(0).getLastChild().getTextContent();
	}



	public static void main(String args[]) throws IOException, ServiceException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, JSONException{
		String query = "kindle";
		//		searchYahoo(query);
		//		searchYouTube(query);
		//		searchAmazon(query);
		//		searchWiki("happy");
		//		searchEbay(query);
		searchYelp("food");
	}

	/**
	 * The method for calculating Yahoo
	 * @param snippet
	 * @return
	 */
	public static Double getYahooScore(String query, int titleOccr, int metaOccr){
		return 1.30D - 0.1 * titleOccr + 0.2 * metaOccr;
	}


	/**
	 * Debug function
	 * equals to "out.print.ln(obj)"
	 */
	public static void log(Object value){
		System.out.println(String.valueOf(value));

	}
}
