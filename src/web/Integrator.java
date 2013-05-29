package web;

import indexer.IndexerCalculator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;

import com.google.gson.Gson;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.PrimaryIndex;
import crawler.CrawlerDrone;
import crawler.state.frontierStore;
import crawler.state.visitedStore;

//import com.google.gdata.model.atom.Content;
//import com.google.gson.Gson;

import ranker.GetPRandFBScores;
import storage.BDBWrapper;
import storage.ContentClient;
import storage.Result;
import storage.ResultsCache;
import storage.UrlContent;

/**  This is the bridge for back-end and front-end.
 *
 * It computes Indexer scores, PageRank scores, and prepare results for front-end.
 * It may also mix with Yahoo results.
 * @see For Amazon and YouTube Results, see APIHelper.java.
 * @author CIS455 Team Globus
 */
public class Integrator {

	/** cache results for further usage. E.g. generating snippet
	 *  We are not using priority queue because traversing heap is expensive O(nlogn) */
	static HashMap<String, ArrayList<Result>> cachedResults = new HashMap<String, ArrayList<Result>>();

	/** Content client for updating database */
	static ContentClient client;

	/** Indexer normalization */
	static Double maxID = 0D;
	private static boolean initialized = false;
	private static BDBWrapper bdb = null;
	private static PrimaryIndex<String, ResultsCache> queryCacheIndex = null;
	private static String dbLoc = null;

	public Integrator(String dbLocation) {
		dbLoc = dbLocation;
	}

	/**  Start Pastry, and directy run main function to see results!
	 */
	public static void main(String[] args) throws IOException
	{
		String query = "ranging from voting";
		String path = "~/ResultCache";
		bdb = new BDBWrapper();
		try {
			if(args.length > 0) path = args[0];
			bdb.setupDB(path);
			bdb.setupStore(storage.BDBWrapper.DB_QUERY_CACHE);
			queryCacheIndex = bdb.getPrimaryIndex(storage.BDBWrapper.DB_QUERY_CACHE, String.class,
					ResultsCache.class);
		} catch (DatabaseException dbe) {
			//Do something here
		}

		Integrator integrator = new Integrator("~/ResultCache");
		ArrayList<Result> results = integrator.getResultsFromBackend(query);
		printResults(results);
	}


	/** This is interface to frontend: get Results from backend
	 *
	 * @param query input from frontend
	 * @return results which are computed by Indexer, PageRank, weighted and ranked by Integrator */
	public ArrayList<Result> getResultsFromBackend(String query) {
		if(!initialized) {
			bdb = new BDBWrapper();
			try {
				bdb.setupDB(dbLoc);
				bdb.setupStore(storage.BDBWrapper.DB_QUERY_CACHE);
				queryCacheIndex = bdb.getPrimaryIndex(storage.BDBWrapper.DB_QUERY_CACHE, String.class,
						ResultsCache.class);
			} catch (DatabaseException dbe) {
				//Do something here
			}
		}

		if(bdb != null && queryCacheIndex!= null && queryCacheIndex.contains(query)) {
			return queryCacheIndex.get(query).getResults();   		
		}

		// Launch client
		startClient();

		// Preprocessing
		String[] words;
		if(!query.contains("\\s")){
			words = new String[1];
			words[0] = query;
		}
		words = query.split("\\s+");

		// Compute and Weight scores
		HashMap<String, Double> URLScoreMap = computeScores(words);

		// Rank and return results
		ArrayList<Result> queryResult = getRankedResults(URLScoreMap, words);
		if(bdb != null && queryCacheIndex != null) {
			ResultsCache cacheBlock = new ResultsCache();
			cacheBlock.setQuery(query);
			cacheBlock.setResults(queryResult);
			queryCacheIndex.put(cacheBlock);
		}
		return queryResult;
	}


	/** 1. Start client to talk to DHT in Pastry ring on P2P network
	 */
	public void startClient(){
		if(client != null) return ;
		ArrayList<InetAddress> inets = new ArrayList<InetAddress>();
		try {
			inets.add(InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		client = new ContentClient(inets);
	}


	/** 2. Compute scores from Indexer and PageRank, weight them
	 * @return computed scores map
	 */
	public HashMap<String, Double> computeScores(String[] words){
		HashMap<String, Double> URLScoreMap = new HashMap<String, Double>();
		try{
			// Indexer
			System.out.println("\nCalculating Indexer...");
			IndexerCalculator ic = new IndexerCalculator(client);
			HashMap<String,Double[]> indexerScores = ic.computeScores(words);

			// PageRank
			GetPRandFBScores gpfb = new GetPRandFBScores(client);
			for(String url: indexerScores.keySet()){
				System.out.println("\nurl:" + url);
				Double[] idScores = indexerScores.get(url);
				double[] prScores = gpfb.getScores(url.trim());

				// Normalization
				if(idScores[0] > maxID) maxID = idScores[0];

				// Weight scores
				double[] scores = new double[]{idScores[0],idScores[1],
						prScores[0], prScores[1]};
				System.out.println("[Raw]\t" + idScores[0] + "\t" + idScores[1] + "\t" + prScores[0] + "\t" + prScores[1]);
				URLScoreMap.put(url, getWeighted(scores));
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return URLScoreMap;
	}


	/**  3. Weight different parameters for ranking URLs after *Normalization
	 *  score = IDScore + IDScoreFeedback + PRScore + PRScoreFeedback
	 *  @return Final weight from indexer and PageRank
	 */
	static double getWeighted(double[] scores){

		/** #########   Tune this   ################################### */

		Double scoreID = 1.3 * (scores[0] +  scores[1])/(maxID + 0.001);
		Double scorePR = scores[2];
		Double score = scoreID  + scorePR;

		/** #########   Tune this   ################################### */
		System.out.println("[Normalized]\tID: " + scoreID + "\tPR: " + scorePR);
		System.out.println("[Weighted]\t" + score);
		return score;
	}


	/** 4. Rank scores and return results
	 * @return ranked scores
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<Result> getRankedResults(HashMap<String, Double> URLScoreMap, String[] words){
		ArrayList<Result> results = new ArrayList<Result>();

		// Rank
		List list = new LinkedList(URLScoreMap.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o2)).getValue())
						.compareTo(((Map.Entry) (o1)).getValue());
			}
		});

		// Ask database for Documents
		String url;
		Double score;

		String docHash;
		UrlContent doc;
		String title;
		String content;	
		String plainText;
		String snippet;

		for(Iterator it = list.iterator(); it.hasNext();){
			Map.Entry entry = (Map.Entry)it.next();
			url = (String) entry.getKey();
			score = (Double) entry.getValue();
			docHash = client.sendGetHashForUrl(url.trim());
			doc = client.sendGetContent(docHash);
			title = doc.getTitle();
			content = doc.getContentString();
			plainText = (String) Jsoup.parse(content).body().text();
			snippet = generateSnippet(plainText, words);
			if(title.length() > 40) title = title.substring(0, 40);
			if(plainText.length() > 80) plainText = plainText.substring(0, 80);

			//          System.out.println("[url]\t" + url);
			//          System.out.println("[score]\t" + score);
			//          System.out.println("[title]\t" + doc.getTitle());
			//          System.out.println("[plainText]\t" + doc.getTitle());
			
			Result result = new Result(url, doc.getTitle(), plainText, score, 1);
			results.add(result);
		}
		return results;
	}


	/** Snippet generated based on context around words
	 * @param plainText the text extracted from body
	 * @param words the tokenized words from query
	 * @return
	 */
	static String generateSnippet(String plainText, String[] words){
		String snippet = plainText;
		if(snippet.length() > 80) snippet = plainText.substring(0, 80);

		for(String word : words){
			int pos = plainText.indexOf(word);
			if(pos == -1) continue;
			int start = pos - 20;
			int end = pos + 20;
			if(start < 0)  start = 0;
			if(end > plainText.length()) end = plainText.length();

			plainText.substring(start, pos); 
			plainText.substring(pos + word.length(), end);

			snippet = snippet + plainText.substring(start, pos) + 
					"<strong>" + word +  "</strong>" 
					+ plainText.substring(pos + word.length(), end) + "...";
		}
		// if in the title and metadata. +/-   0.1.
		return snippet;
	}
	
	
	/** Pretty print
	 */
	static void printResults(ArrayList<Result> results){
		System.out.println("\n----------------- Results --------------\n");
		for (Result rs : results){
			System.out.print(rs.getScore());
			System.out.print("\t" + rs.getURL());
			System.out.print("\t[title] " + rs.getTitle().trim());
			System.out.print("\t[snippet] " + rs.getSnippet().trim() + "\n");

		}
	}

}
