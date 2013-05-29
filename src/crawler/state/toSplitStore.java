/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler.state;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Class encapsulating a record in the list of urls processing by the URL filter
 * 
 */
@Entity
public class toSplitStore {

	@PrimaryKey
	private String url = null;

	/**
	 * Default constructor
	 */
	public toSplitStore() {
	}

	public void setPKey(String target) {
		url = target;
	}

	public String getPKey() {
		return url;
	}
}
