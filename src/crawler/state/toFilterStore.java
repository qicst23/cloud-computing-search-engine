/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler.state;

import java.net.InetAddress;
import java.net.UnknownHostException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/** Class encapsulating a record in the list of urls processing by the URL filter */
@Entity
public class toFilterStore {

	@PrimaryKey
	private String url = null;
	// private IPresource resource = null;
	private String resource = null;
	private Long lastVisited = null;
	private String inetString = null;

	/** Default constructor */
	public toFilterStore() {}

	public void setPKey(String target) {
		url = target;
	}

	public void setIPresource(IPresource res) {
		resource = res.getResource();
		lastVisited = res.getLastVisited();
		inetString = res.getIp().getHostName();
	}

	public String getPKey() {
		return url;
	}

	public IPresource getIPresource() {
		InetAddress host;
		try {
			host = InetAddress.getByName(inetString);
		} catch (UnknownHostException e) {
			return null;
		}
		if (resource == null)
			return null;
		return new IPresource(host, resource, lastVisited);
	}
}
