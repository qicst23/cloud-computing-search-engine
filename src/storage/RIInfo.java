/**
 * 
 */
package storage;

import java.util.TreeMap;

import com.sleepycat.persist.model.Persistent;

/**
 * @author cis455
 *
 */

@Persistent
public class RIInfo {
	private static final String NULL_FIELD = "%&%NULL_FIELD%&%";
	private double docVecLen;
	private double feedbackWeight=0;
	private TreeMap<Integer, String> hitLocs = new TreeMap<Integer, String>();
	
	//Make these unique from the delimiters in RIEntry
	private static String withinHitsDelim = "!&&!";
	private static String betweenHitLocsDelim = "!@@!";
	private static String withinHitLocsDelim = "!##!";
	
	public String convertToMessage(){
		StringBuilder s = new StringBuilder("docVecLen" + withinHitsDelim); // #0
		s.append(docVecLen); // #1
		s.append(withinHitsDelim + "feedbackWeight" + withinHitsDelim); //#2
		s.append(feedbackWeight); //#3
		s.append(withinHitsDelim + "hitLocs" + withinHitsDelim); //#4
		if(hitLocs.isEmpty()){
			s.append(NULL_FIELD); //#5
		}
		else{
			StringBuilder hitLocsString = new StringBuilder();
			for (Integer location : hitLocs.keySet()) {
				if (hitLocsString.length() != 0) {
					hitLocsString.append(betweenHitLocsDelim);
				}
				hitLocsString.append(location);
				hitLocsString.append(withinHitLocsDelim); //I got lazy, no name:value here, just value:value
				hitLocsString.append(hitLocs.get(location));
			}
			s.append(hitLocsString); //#5
		}
		
		return s.toString();
	}
	
	public static RIInfo convertFromMessage(String message) throws NumberFormatException {
		RIInfo res = new RIInfo();
		String[] docLengthSplit = message.split(withinHitsDelim, 12);
		res.setDocVecLen(Double.valueOf(docLengthSplit[1]));
		res.setFeedbackWeight(Double.valueOf(docLengthSplit[3]));
		if(!docLengthSplit[5].equals(NULL_FIELD)){
			String[] hitLocsSplit = docLengthSplit[5].split(betweenHitLocsDelim);
			for(int i=0; i<hitLocsSplit.length; i++){
				String[] hitLoc = hitLocsSplit[i].split(withinHitLocsDelim);
				res.addHit(Integer.valueOf(hitLoc[0]), hitLoc[1]);
			}
		}
		return res;
	}
	public void setDocVecLen(double length){
		docVecLen = length;
	}
	
	public double getDocVecLen(){
		return docVecLen;
	}
	
	public void addHit(int loc, String exactWord){
		hitLocs.put(loc, exactWord);
	}
	
	public TreeMap<Integer, String> getHits(){
		return hitLocs;
	}
	
	public int getTf(){
		return hitLocs.size();
	}
	
	public double getFeedbackWeight(){
		return feedbackWeight;
	}
	
	public void setFeedbackWeight(double _feedbackWeight){
		feedbackWeight = _feedbackWeight;
	}
}
