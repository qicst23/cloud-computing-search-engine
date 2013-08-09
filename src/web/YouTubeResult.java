/**
 * @author Yayang Tian
 * http://cis.upenn.edu/~yaytian
 */

package web;

public class YouTubeResult{

	String id = "";
	String title = "";
	String desc = "";
	String URL = "";

	String imageURL = "";
	String length = "";
	String numViews = "";
	String uploader = "";
	String date = "";

	// There are two types of results: 
	// 1. normal results
	// 2. recommended results
	boolean isRecommended = false; 


	public YouTubeResult(String id, String title,  String desc, String URL,
			String imageURL, String length, String numViews, String uploader, String date,
			boolean isRecommended){

		if(title.length() > 20) title = title.substring(0, 20);
		if(desc.length() > 50) desc = desc.substring(0, 50);
		if(uploader.length() > 8) uploader = uploader.substring(0, 8);
		this.id = id;
		this.title = title;
		this.desc = desc;
		this.URL = URL;

		this.imageURL = imageURL;
		this.length = length;
		this.numViews = numViews;
		this.uploader = uploader;
		this.date = date;
		this.isRecommended = isRecommended;
	}


	String getId(){return id;}
	String getTitle(){return title;}
	String getDesc(){return desc;}
	String getURL(){return URL;}
	String getLength(){return length;}
	String getNumViews(){return numViews;}
	String getUploader(){return uploader;}
	String getDate(){return date;}
	boolean isRecommended(){return isRecommended;}


	public String getImageURL() {
		return imageURL;
	}
}
