package web;

public class YahooResult{

	String title = "";
	String desc = "";
	String URL = "";
	String rankScore = "";
	String date = ""; 

	public YahooResult(String title, String desc, String URL,
			String rankScore, String date){
		this.title = title;
		this.desc = desc;
		this.URL = URL;	
		this.rankScore = rankScore;
		this.date = date;
	}

	String getTitle(){return title;}
	String getDesc(){return desc;}
	String getURL(){return URL;}
	String getRankScore(){return rankScore;}
	String getDate(){return date;}
}


