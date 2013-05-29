/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler.state;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Class encapsulating a record in the visited table
 * 
 */
@Entity
public class visitedStore {

	@PrimaryKey
	private String url = null;

	private Long timestamp = null;

	/**
	 * Default constructor
	 */
	public visitedStore() {
	}

	public void setPKey(String target) {
		url = target;
	}

	public String getPKey() {
		return url;
	}

	public void setTimestamp(Long time) {
		timestamp = time;
	}

	public Long getTimestamp() {
		return timestamp;
	}
}
