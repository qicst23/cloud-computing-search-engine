package storage;

import java.io.Serializable;
import java.util.ArrayList;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/** @author cis455 */
@Entity
public class UrlLinks implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String NULL_FIELD = "%&%NULL_FIELD%&%";

	/** url of the webpage */
	@PrimaryKey
	private String url = null;

	/** outlinks of the webpage */
	private ArrayList<String> outlinks = new ArrayList<String>();

	/** page rank score of the webpage */
	private double pagerankscore;

	/** feedback score of the webpage */
	private double feedbackscore;

	private static String msgDelim = "#&&#";
	private static String betweenOutlinksDelim = "#@@#";

	public UrlLinks() {
		pagerankscore = 0.15;
		feedbackscore = 0.00;
	}

	public UrlLinks(String url) {
		this.url = url;
		pagerankscore = 0.15;
		feedbackscore = 0.00;
	}

	public UrlLinks(String url, ArrayList<String> outlinks) {
		this.url = url;
		this.outlinks = outlinks;
		pagerankscore = 0.15;
		feedbackscore = 0.00;
	}

	public String getPkey() {
		return url;
	}

	public void setPkey(String inputKey) {
		url = inputKey;
	}

	public String convertToMessage() {
		StringBuilder s = new StringBuilder("url" + msgDelim); // #0
		s.append(url + msgDelim); // #1
		s.append("pagerankscore" + msgDelim); // #2
		s.append(Double.toString(pagerankscore) + msgDelim); // #3
		s.append("feedbackscore" + msgDelim); // #4
		s.append(Double.toString(feedbackscore) + msgDelim); // #5

		s.append("outlinks" + msgDelim); // #6
		if (outlinks.size() == 0) {
			s.append(NULL_FIELD + msgDelim);
		} else {
			for (int i = 0; i < outlinks.size(); i++) {
				if (i == outlinks.size() - 1) {
					s.append(outlinks.get(i) + msgDelim);
				} else {
					s.append(outlinks.get(i) + betweenOutlinksDelim);
				}
			}
		}

		return s.toString();
	}

	public static UrlLinks convertFromMessage(String msg) {
		String[] msgSplit = msg.split(msgDelim, 8);
		System.out.println(msgSplit[0]);
		UrlLinks urllinks = new UrlLinks(msgSplit[1]); // sets url
		urllinks.setPagerankscore(Double.parseDouble(msgSplit[3])); // sets
																																// pagerankscore
		urllinks.setFeedbackscore(Double.parseDouble(msgSplit[5])); // sets
																																// feedbackscore
		if (msgSplit.length == 8) {
			String outlinkspattern = msgSplit[7];
			String[] outlinks = outlinkspattern.split(betweenOutlinksDelim);
			if (outlinks.length > 0) {
				ArrayList<String> parsedOutlinks = new ArrayList<String>();
				for (String ol : outlinks) {
					parsedOutlinks.add(ol);
				}
				urllinks.setOutlinks(parsedOutlinks); // sets outlinks
			}
		}
		return urllinks;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public ArrayList<String> getOutlinks() {
		return outlinks;
	}

	public void setOutlinks(ArrayList<String> outlinks) {
		this.outlinks = outlinks;
	}

	public double getPagerankscore() {
		return pagerankscore;
	}

	public void setPagerankscore(double pagerankscore) {
		this.pagerankscore = pagerankscore;
	}

	public double getFeedbackscore() {
		return feedbackscore;
	}

	public void setFeedbackscore(double feedbackscore) {
		this.feedbackscore = feedbackscore;
	}
}
