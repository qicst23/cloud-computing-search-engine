/**
 * 
 */
package indexer;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import org.tartarus.snowball.SnowballStemmer;

import storage.ContentClient;
import storage.RIEntry;
import storage.RIInfo;

/**
 * @author cis455
 *
 */
public class IndexerCalculator {
	
	ContentClient contentClient;
	@SuppressWarnings("rawtypes")
	Class stemClass;
    SnowballStemmer stemmer;
	//create this only once, calculating N is overhead we don't need to repeat.
    //TODO if stemmer throws error, don't stem? but this is constructor
	public IndexerCalculator(ContentClient client) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		contentClient = client;
		stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
		stemmer = (SnowballStemmer) stemClass.newInstance();
	}
	
	//returns a hashmap<url, {indexerscore, feedbackScore}
	public HashMap<String, Double[]> computeScores(String[] words){
		HashMap<String, Double[]> results = new HashMap<String, Double[]>();
		HashMap<String, Double> weights = stemCountQuery(words);
		HashMap<String, Double> docVecLens = new HashMap<String, Double>();
		for(String word: weights.keySet()){
			double wf = weights.get(word);
			RIEntry entry = contentClient.sendGetRIEntry(word);
			if(entry!=null){
				System.out.println("entry not null for " + word);
				prettyPrintRIEntry(entry);
				HashMap<String, RIInfo> infos = entry.getDocRIInfoMap();
				for(String url: infos.keySet()){
					System.out.println("url: " + url);
					RIInfo info = infos.get(url);
					double wdf = computeTfIdf(info.getTf(), entry.getNdf());
					double docVecLen = info.getDocVecLen();
					if(results.containsKey(url)){
						Double[] orig = results.get(url);
						orig[0] = orig[0] + (wdf * wf);
						results.put(url, orig);
					}
					else{
						Double[] newRes = {wdf * wf, info.getFeedbackWeight()};
						results.put(url, newRes);
						docVecLens.put(url, docVecLen);
					}
				}
				for(String url : results.keySet()){
					Double[] orig = results.get(url);
					orig[0] = orig[0]/docVecLens.get(url);
					results.put(url, orig);
				}
			}
		}
		return results;
	}
	
	public void prettyPrintRIEntry(RIEntry ri){
		System.out.println(ri.getPKey());
		System.out.println(ri.getNdf());
		System.out.println(ri.getDocRIInfoMap());
		for (String url : ri.getDocRIInfoMap().keySet()) {
			System.out.println("RIInfo url: " + url);
			RIInfo info = ri.getDocRIInfoMap().get(url);
			for (Integer loc : info.getHits().keySet()) {
				System.out.print(loc + " " + info.getHits().get(loc));
			}
			System.out.println();
			System.out.println("tf: " + info.getTf());
			System.out.println("dvl: " + info.getDocVecLen());
			System.out.println("feedback: " + info.getFeedbackWeight());
		}
	}
	public void putIndexerFeedbackScore(String[] words, String url, double feedbackscore){
		for(int i=0; i<words.length; i++){
			stemmer.setCurrent(words[i]);
			stemmer.stem();
			String res = stemmer.getCurrent();
		}
		
	}
	
	
	//stems, then merges duplicates and combines them into weights
	public HashMap<String, Double> stemCountQuery(String[] words){
		HashMap<String, Double> weights = new HashMap<String, Double>();
		for(int i=0; i<words.length; i++){
			stemmer.setCurrent(words[i]);
			stemmer.stem();
			String res = stemmer.getCurrent();
			if(weights.containsKey(res)){
				weights.put(res, weights.get(res)+1);
			}
			else{
				weights.put(res, 1.0);
			}
			for(String word: weights.keySet()){
				weights.put(word, weights.get(word)/words.length);
			}
			
		}
		return weights;
	}
	
	public double computeTfIdf(int tf, double ndf){
		return (double)tf * Math.log(ndf);
		
	}
}
