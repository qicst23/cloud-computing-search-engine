/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler.state;

import java.net.InetAddress;
import java.util.Date;
import java.util.Random;

/** Class encapsulating a resolved IP address and the requested resource */
public class IPresource implements Comparable<IPresource> {

	private InetAddress ip = null;
	private String resource = null;
	private Long lastVisited = null;
	private String host = null;
	private static Random chaos = new Random(new Date().getTime());

	/** Default constructor */
	public IPresource(InetAddress inputAddr, String resourceRequested,
			Long lastVisited) {
		ip = inputAddr;
		if (resourceRequested != null && !resourceRequested.equals("")) {
			resource = resourceRequested;
		} else {
			resource = "/";
		}
		this.lastVisited = lastVisited;
	}

	/** @return the ip */
	public InetAddress getIp() {
		return ip;
	}

	/** @return the hostname */
	public String getHost() {
		if (ip == null)
			return null;
		if (host == null) {
			host = ip.getHostName();
		}
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	/** @return the resource */
	public String getResource() {
		return resource;
	}

	public String getFullURL() {
		if (ip != null && resource != null) {
			if (ip.getHostName().endsWith("/") && resource.indexOf("/") == 0)
				return ip.getHostName().substring(0, ip.getHostName().length() - 2)
						+ resource;
			else if (!ip.getHostName().endsWith("/") && resource.indexOf("/") != 0)
				return ip.getHostName() + "/" + resource;
			else
				return ip.getHostName() + resource;
		}
		return null;
	}

	public Long getLastVisited() {
		return lastVisited;
	}

	public void setLastVisited(long lastVisited) {
		this.lastVisited = lastVisited;
	}

	@Override
	public String toString() {
		return "[ip=" + ip.getHostAddress() + ", resource=" + resource + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IPresource other = (IPresource) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.getHostName().equals(other.ip.getHostName()))
			return false;
		if (resource == null) {
			if (other.resource != null)
				return false;
		} else if (!resource.equals(other.resource))
			return false;
		return true;
	}

	@Override
	public int compareTo(IPresource other) {
		if (this.equals(other))
			return 0;
		if (this.getLastVisited() != null) {
			if (other.getLastVisited() != null)
				return this.getLastVisited() < other.getLastVisited() ? 1 : -1;
			else
				return -1;
		} else {
			if (other.getLastVisited() != null)
				return 1;
			else
				return chaos.nextInt(3) - 1;
		}
	}
}
