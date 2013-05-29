/**
 * 
 */
package storage;

import java.util.HashMap;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/** @author Angela Wu */
@Entity
public class RIEntry {
	@PrimaryKey
	private String word;
	private double ndf;
	private HashMap<String, RIInfo> docRIInfoMap = new HashMap<String, RIInfo>();
	
	private static String msgDelim = "#&&#";
	private static String betweenHitsDelim = "#@@#";
	private static String withinHitsDelim = "#!!#";
	private static final String NULL_FIELD = "%&%NULL_FIELD%&%";
	
	public String convertToMessage() {
		StringBuilder s = new StringBuilder("word" + msgDelim); // #0
		if(word==null){
			s.append(NULL_FIELD);
		}
		else{
			s.append(word); // #1
		}
		s.append(msgDelim + "ndf" + msgDelim); //#2
		s.append(ndf); //#3
		s.append(msgDelim + "RIInfo" + msgDelim); // #4
		if(docRIInfoMap.isEmpty()){
			s.append(NULL_FIELD);
		}
		else{
			StringBuilder RIInfoStrings = new StringBuilder();
			for (String docHash : docRIInfoMap.keySet()) {
				if (RIInfoStrings.length() != 0) {
					RIInfoStrings.append(betweenHitsDelim);
				}
				RIInfoStrings.append(docHash);
				RIInfoStrings.append(withinHitsDelim);
				RIInfoStrings.append(docRIInfoMap.get(docHash).convertToMessage());
			}
			s.append(RIInfoStrings); //#5
		}
		return s.toString();
	}

	public static RIEntry convertFromMessage(String msg)
			throws NumberFormatException {
		String[] msgSplit = msg.split(msgDelim, 6);
		RIEntry res;
		if(msgSplit[1].equals(NULL_FIELD)){
			res = new RIEntry(null);//I don't even know...
		}
		else{
			res = new RIEntry(msgSplit[1]); // sets word
		}
		res.setNdf(Double.valueOf(msgSplit[3]));
		if(!msgSplit[5].equals(NULL_FIELD)){
			String[] hitsSplit = msgSplit[5].split(betweenHitsDelim);
			for (int i = 0; i < hitsSplit.length; i++) {
				String[] split = hitsSplit[i].split(withinHitsDelim);
				res.putHit(split[0], RIInfo.convertFromMessage(split[1]));
			}
		}
		return res;
	}

	public RIEntry() {}

	public RIEntry(String word) {
		this.word = word;
	}

	public String getPKey() {
		return word;
	}
	
	public void setPKey(String word) { //use with caution
		this.word = word;
	}

	public void setNdf(double _ndf){
		ndf = _ndf;
	}
	
	
	public double getNdf(){
		return ndf;
	}
	public boolean addUrl(String url) {
		if (docRIInfoMap.containsKey(url))
			return false;
		else {
			docRIInfoMap.put(url, new RIInfo());
			return true;
		}
	}

	public void addHit(String url, int location, String exactWord) {
		if (!docRIInfoMap.containsKey(url)) {
			addUrl(url);
		}
		RIInfo temp = docRIInfoMap.get(url);
		temp.addHit(location, exactWord);
		docRIInfoMap.put(url, temp);
	}

	public void putHit(String url, RIInfo riInfo) {
		docRIInfoMap.put(url, riInfo);
	}

	public HashMap<String, RIInfo> getDocRIInfoMap(){
		return docRIInfoMap;
	}
}
