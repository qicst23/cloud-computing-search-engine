/**
 * @author Yayang Tian - yaytian@cis.upenn.edu
 */
package storage;

import com.sleepycat.persist.model.Persistent;

@Persistent
public class Result {
	String url = "";
	String title = "";
	String snippet = "";
	Double score = 0D;
	int type = 1;

	public Result() {
	}

	public Result(String aUrl, String aTitle, String aSnippet, Double aScore, int type) {
		setUrl(aUrl);
		setTitle(aTitle);
		setSnippet(aSnippet);
		setScore(aScore);
		setType(type);
	}

	public void setUrl(String url){
		this.url = url;
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public void setSnippet(String snippet){
		this.snippet = snippet;
	}
	
	public void setScore(Double score){
		this.score = score;
	}
	
	public void setType(int type){
		this.type = type;
	}
	
	public String getURL() {
		return url;
	}

	public String getTitle() {
		return title;
	}

	public String getSnippet() {
		return snippet;
	}

	public Double getScore() {
		return score;
	}
	
	public int getType(){
		return type;
	}
}
