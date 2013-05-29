/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler.state;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Class encapsulating a record in the URL frontier
 * 
 */
@Entity
public class frontierStore {

	@PrimaryKey
	private String fullUrl = null;

	private String host = null;
	private String resource = null;

	/**
	 * Default constructor
	 */
	public frontierStore() {
	}

	public void setPKey(String target) {
		fullUrl = target;
	}

	public String getPKey() {
		return fullUrl;
	}

	public void setHost(String input) {
		host = input;
	}

	public String getHost() {
		return host;
	}

	public void setResource(String input) {
		resource = input;
	}

	public String getResource() {
		return resource;
	}
}
