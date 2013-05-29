/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package storage;

import java.net.InetAddress;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/** Encapsulation of a message used in the FreePastry service to locate the node
 * responsible for a given piece of content */
public class DbMessage implements Message {

	private static final long serialVersionUID = 1L;
	private NodeHandle from = null;
	private String content = null;
	private boolean isResponse = false;
	private InetAddress clientAddress = null;
	private int contentLength = -1;
	private boolean isReplica = false;
	private UrlContent putReplica = null;

	/** Default constructor */
	public DbMessage(NodeHandle from, String docHash, int contentLength,
			boolean isResponse, InetAddress clientAddress) {
		this.from = from;
		content = docHash;
		this.isResponse = isResponse;
		this.clientAddress = clientAddress;
		this.contentLength = contentLength;
	}

	public void setReplica(boolean rep) {
		isReplica = rep;
	}

	public boolean isReplica() {
		return isReplica;
	}

	public void setPutReplica(UrlContent replicaData) {
		putReplica = replicaData;
	}

	public UrlContent getPutReplica() {
		return putReplica;
	}

	@Override
	public int getPriority() {
		return 0;
	}

	public NodeHandle getFrom() {
		return from;
	}

	public String getContent() {
		return content;
	}

	public int getContentLength() {
		return contentLength;
	}

	public boolean isResponse() {
		return isResponse;
	}

	public InetAddress getClientAddress() {
		return clientAddress;
	}

}
