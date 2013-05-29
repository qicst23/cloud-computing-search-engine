/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler;

import java.net.InetSocketAddress;
import crawler.state.SplitterMessage;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import storage.NodeFactory;

/** @author Michael Collis (mcollis@seas.upenn.edu) */
public class HostSplitterHandler implements Application {

	protected NodeFactory nodeFactory = null;
	protected Node node = null;
	private Endpoint endpoint = null;

	/** Default constructor given a bootstrap node address */
	public HostSplitterHandler(int pastryport, InetSocketAddress bootstrapNode) {
		if (bootstrapNode == null) {
			nodeFactory = new NodeFactory(pastryport);
		} else {
			nodeFactory = new NodeFactory(pastryport, bootstrapNode);
		}
		node = nodeFactory.getNode();
		endpoint = node.buildEndpoint(this, "CrawlerHostSplitter");
		endpoint.register();
	}

	@Override
	public void deliver(Id id, Message msg) {
		String linkToAdd = ((SplitterMessage) msg).getLink();
		if (linkToAdd != null && DnsResolver.urlsToResolve != null) {
			DnsResolver.urlsToResolve.add(linkToAdd);
		}
	}

	public void sendMessage(Id destination, String link) {
		CrawlerDrone.addLog("Host Splitter: Sending " + link + " to "
				+ destination.toString());
		SplitterMessage outgoing;
		synchronized (node) {
			outgoing = new SplitterMessage(link);
		}
		synchronized (endpoint) {
			endpoint.route(destination, outgoing, null);
		}
	}

	@Override
	public boolean forward(RouteMessage arg0) {
		return true;
	}

	@Override
	public void update(NodeHandle arg0, boolean arg1) {}

	public void closeNode() {
		nodeFactory.shutdownNode(node);
	}
}
