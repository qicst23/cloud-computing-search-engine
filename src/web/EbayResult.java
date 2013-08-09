/**
 * @author Yayang Tian
 * http://cis.upenn.edu/~yaytian
 */
package web;

public class EbayResult{

	String title = "";
	String desc = "";
	String URL = "";
	String date = ""; 
	

	public EbayResult(String title, String desc, String URL,
			String rankScore, String date){
		this.title = title;
		this.desc = desc;
		this.URL = URL;	
		this.date = date;
	}
	
	String getTitle(){return title;}
	String getDesc(){return desc;}
	String getURL(){return URL;}
	String getDate(){return date;}
}


