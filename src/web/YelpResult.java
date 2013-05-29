/**
 * 
 */
package web;

/**
 * @author Yayang Tian
 *
 */
public class YelpResult {
	String name = "-"; 
	String snippetText = "";
	String snippetImage = "";
	String distance = ""; 
	String ratingImage = "";
	String url = "";

	public YelpResult(String name, String snippetText,String snippetImage,
			String distance, 
			String image, String url){
		this.name = name;
		this.snippetText = snippetText;
		this.snippetImage = snippetImage; 
		this.distance = distance;
		this.ratingImage = image;
		this.url = url;
	}

	String getName(){return name;}
	String getSnippetText(){return snippetText;}
	String getSnippetImage(){return snippetImage;};
	String getDistance(){return distance;}
	String getRatingImage(){return ratingImage;}
	String getURL(){return url;}

}
