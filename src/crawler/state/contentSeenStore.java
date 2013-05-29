/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler.state;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Class encapsulating a record of the hash of page content seen by the crawler
 * 
 */
@Entity
public class contentSeenStore {

	@PrimaryKey
	private String hash = null;

	/**
	 * Default constructor
	 */
	public contentSeenStore() {
	}

	public void setPKey(String input) {
		hash = input;
	}

	public String getPKey() {
		return hash;
	}
}
