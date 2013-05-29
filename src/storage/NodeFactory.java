package storage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import rice.p2p.commonapi.Node;
import rice.environment.Environment;
import rice.pastry.NodeHandle;
import rice.pastry.Id;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

/** A simple class for creating multiple Pastry nodes in the same ring
 * 
 * 
 * @author Original: Nick Taylor, Modified by: Michael Collis
 *         (mcollis@seas.upenn.edu) */
public class NodeFactory {
	Environment env;
	NodeIdFactory nidFactory;
	SocketPastryNodeFactory factory;
	NodeHandle bootHandle;
	int createdCount = 0;
	int port;

	public NodeFactory(int port, InetSocketAddress bootPort) {
		this(port);
		bootHandle = factory.getNodeHandle(bootPort);
	}

	public NodeFactory(int port) {
		Environment env = new Environment();
		env.getParameters().setInt("pastry_socket_writer_max_queue_length", 500);
		this.env = env;
		this.port = port;
		nidFactory = new RandomNodeIdFactory(env);
		try {
			factory = new SocketPastryNodeFactory(nidFactory, port, env);
		} catch (java.io.IOException ioe) {
			throw new RuntimeException(ioe.getMessage(), ioe);
		}

	}

	public Node getNode() {
		try {
			synchronized (this) {
				if (bootHandle == null && createdCount > 0) {
					InetAddress localhost = InetAddress.getLocalHost();
					InetSocketAddress bootaddress = new InetSocketAddress(localhost, port);
					bootHandle = factory.getNodeHandle(bootaddress);
				}
			}

			PastryNode node = factory.newNode(bootHandle);
			/*
			 * while (!node.isReady()) { Thread.sleep(100); }
			 */
			synchronized (node) {
				while (!node.isReady() && !node.joinFailed()) {
					node.wait(500);
					if (node.joinFailed())
						throw new IOException("Could not join the FreePastry ring. Reason:"
								+ node.joinFailedReason());
				}
			}

			synchronized (this) {
				++createdCount;
			}
			return node;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void shutdownNode(Node n) {
		((PastryNode) n).destroy();
		env.destroy();
	}

	public Id getIdFromBytes(byte[] material) {
		return Id.build(material);
	}

	public Id getIdFromString(String keyString) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		byte[] content = keyString.getBytes();
		md.update(content);
		byte shaDigest[] = md.digest();
		// rice.pastry.Id keyId = new rice.pastry.Id(shaDigest);
		return Id.build(shaDigest);
	}

	// Added by Michael Collis 20130412
	public Id getIdFromHash(String hash) {
		byte shaDigest[] = hash.getBytes();
		return Id.build(shaDigest);
	}
}
