/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler.state;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Class encapsulating a record in the list of urls from which links still need
 * to be extracted
 * 
 */
@Entity
public class toExtractStore {

	@PrimaryKey
	private String url = null;

	/**
	 * Default constructor
	 */
	public toExtractStore() {
	}

	public void setPKey(String target) {
		url = target;
	}

	public String getPKey() {
		return url;
	}
}
