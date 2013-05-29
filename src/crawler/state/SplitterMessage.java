/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler.state;

import rice.p2p.commonapi.Message;

/** @author Michael Collis (mcollis@seas.upenn.edu) */
public class SplitterMessage implements Message {

	/** Default generated serial */
	private static final long serialVersionUID = 1L;
	private String link = null;

	/** Default message constructor */
	public SplitterMessage(String link) {
		this.link = link;
	}

	@Override
	public int getPriority() {
		return 0;
	}

	public String getLink() {
		return link;
	}
}
