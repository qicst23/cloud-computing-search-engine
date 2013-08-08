/**
 * @author Yayang Tian
 * http://cis.upenn.edu/~yaytian
 */
package web;

public class AmazonResult {

	String title = "";
	String price = "";
	String company = "";
	String desc = "";
	String reviewHTML = "";
	String detailURL = "";
	String imageURL = "";

	public AmazonResult(String title, String price, String company, String desc, 
			String reviewHTML, String detailURL, String imageURL){	
		if(title.length() > 30){
			title = title.substring(0, 29);
		}

		this.title = title;
		this.price = price;
		this.company = company;
		this.desc = desc;
		this.reviewHTML = reviewHTML;
		this.detailURL = detailURL;
		this.imageURL = imageURL;
	}

	public String getTitle(){return title;}
	public String getPrice(){return price;}
	public String getCompany(){return company;}
	public String getDesc(){return desc;}
	public String getReviewHTML(){return reviewHTML;}
	public String getDetailURL(){return detailURL;}
	public String getImageURL(){return imageURL;}
}
