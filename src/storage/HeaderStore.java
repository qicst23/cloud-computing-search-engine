/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package storage;

import java.io.Serializable;
import java.util.TreeSet;
import com.sleepycat.persist.model.Persistent;

/** Persistent class encapsulating hashtable of HTTP headers (made serializable
 * for use with Berkeley DB
 * 
 * @author Michael Collis (mcollis@seas.upenn.edu)
 * @version 20130410 */
@Persistent
public class HeaderStore implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** Serializable storage for the headers */
	private TreeSet<String> headers = null;

	/** Default constructor */
	protected HeaderStore() {}

	/** Initializes the data structure to store the headers */
	protected void setup() {
		headers = new TreeSet<String>();
	}

	/** Returns the TreeSet of header key-value pairs as Strings (generally not
	 * used). Instead @see {@link #get(String)}.
	 * 
	 * @return Set of header strings if initialized; otherwise, null */
	protected TreeSet<String> getHeaders() {
		return headers;
	}

	/** Adds a given header key-value pair to the data structure
	 * 
	 * @param key Name of the header being added (e.g. "Content-Type")
	 * @param value Value of the header being added (e.g. "text/html") */
	protected void put(String key, String value) {
		headers.add(key + "#" + value);
	}

	/** Checks to see if a given key is contained in the set of headers already
	 * stored
	 * 
	 * @param key Field name of the header to look for
	 * @return True if data structure has been initialized and the a header of
	 *         that name is stored in it; else, returns false */
	protected boolean containsKey(String key) {
		if (headers == null)
			return false;
		for (String header : headers) {
			if (header.indexOf(key) >= 0)
				return true;
		}
		return false;
	}

	/** Retrieves the value of the header associated with the input field name
	 * 
	 * @param key Field name of the header to retrieve
	 * @return The requested field value if the data structure has been
	 *         initialized and a header of that name is stored in it; else,
	 *         returns null */
	protected String get(String key) {
		if (headers == null)
			return null;
		for (String header : headers) {
			if (header.indexOf(key) >= 0)
				return header.substring(header.indexOf("#") + 1).trim();
		}
		return null;
	}

}
