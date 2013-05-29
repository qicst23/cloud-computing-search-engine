/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

/** Class encapsulating a record of a URL mapping to a document content hash */
@Entity
public class UrlToDocHash {

	@PrimaryKey
	private String url = null;
	
	@SecondaryKey(relate = Relationship.MANY_TO_ONE)
	private String docHash = null;

	/** Default constructor */
	public UrlToDocHash() {}

	public void setPKey(String target) {
		url = target;
	}

	public String getPKey() {
		return url;
	}

	public void setDocHash(String hash) {
		docHash = hash;
	}

	public String getDocHash() {
		return docHash;
	}

}
