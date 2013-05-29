/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package storage;

import java.nio.ByteBuffer;

/** Helper class for generating and parsing messages sent to and received from
 * the distributed content storage database
 * 
 * @author Michael Collis (mcollis@seas.upenn.edu)
 * @version 20130411 */
public class StorageHelper {

	private static final int BYTES_IN_LONG = 8;
	private static final int BYTES_IN_DOUBLE = 8;
	private static final String CRLF = "\r\n";

	/** Indicates message is a GET request */
	public static final int GETCONTENT = 0;
	/** Indicates message is a PUT request */
	public static final int PUTCONTENT = 1;
	/** Indicates message is a CONTAINS request */
	public static final int CONTAINSCONTENT = 2;
	public static final int GETRIENTRY = 3;
	public static final int PUTRIENTRY = 4;
	public static final int GETHASHFROMURL = 5;
	public static final int PUTURLTOHASH = 6;
	public static final int GETALLDOCHASH = 7;
	public static final int PUTPAGERANKSCORE = 8;
	public static final int GETPAGERANKSCORE = 9;
	public static final int UPDATECRAWLTIME = 10;
	public static final int ADDRIINFO = 11;
	public static final int DELALLRIENTRIES = 12;
	public static final int GETURLSFROMHASH = 13;
	public static final int PUTPRFEEDBACKSCORE = 14;
	public static final int PUTINDEXERFEEDBACKSCORE = 15;

	/** Indicates message is of an unknown type */
	public static final int UNKNOWN = -1;

	/** Outgoing port for clients to use for sending messages to the content
	 * gateway */
	public static final int SEND_MESSAGE_PORT = 11001;

	/** ServerSocket port for clients to listen on for receiving responses from the
	 * content datastore nodes */
	public static final int RECEIVE_DATA_PORT = 10001;

	/** Constructs a GET request message for the content database
	 * 
	 * @param docHash SHA-256 hash of document content to be retrieved
	 * @return Message formatted to be understood by the content database */
	public static String makeGetContentMessage(String docHash, int portNo) {
		return "GETCONTENT " + docHash + " " + portNo + CRLF;
	}

	/** Constructs a PUT request message for the content database
	 * 
	 * @param docHash SHA-256 hash of document content to be added to the database
	 * @param byteLength length in bytes of UrlContent to be transmitted
	 * @return Message formatted to be understood by the content database */
	public static String makePutContentMessage(String docHash, int byteLength,
			int portNo) {
		return "PUTCONTENT " + docHash + " " + portNo + " "
				+ ((Integer) byteLength).toString() + CRLF;
	}

	/** Constructs a CONTAINS query message for the content database
	 * 
	 * @param docHash SHA-256 hash of document content to be checked
	 * @return Message formatted to be understood by the content database */
	public static String makeContainsContentMessage(String docHash, int portNo) {
		return "CONTAINSCONTENT " + docHash + " " + portNo + CRLF;
	}

	public static String makeUpdateContentCrawlTimeMessage(String docHash,
			long timestamp) {
		return "UPDATECRAWLTIME " + docHash + " " + timestamp + CRLF;
	}

	/** Constructs a GET HASH request message for the content database
	 * 
	 * @param URL URL of document content hash to be retrieved
	 * @return Message formatted to be understood by the content database */
	public static String makeGetHashFromUrlMessage(String url, int portNo) {
		return "GETHASHFROMURL " + url + " " + portNo + CRLF;
	}

	/** Constructs a PUT request message for the content database
	 * 
	 * @param docHash SHA-256 hash of document content to be added to the database
	 * @param byteLength length in bytes of UrlContent to be transmitted
	 * @return Message formatted to be understood by the content database */
	public static String makePutUrlToHashMessage(String url, String hash) {
		return "PUTURLTOHASH " + url + " " + hash + CRLF;
	}

	public static String makeGetRIEntryMessage(String word, int portNo) {
		return "GETRIENTRY " + word + " " + portNo + CRLF;
	}

	/** Constructs a PUT request message for the ReverseIndexEntry database
	 * 
	 * @param word word to map RIEntry with (will be overwritten if it exists)
	 * @param byteLength length in bytes of UrlContent to be transmitted
	 * @return Message formatted to be understood by the content database */
	public static String makePutRIEntryMessage(String word, int byteLength,
			double ndf) {
		return "PUTRIENTRY " + word + " " + ndf + " "
				+ ((Integer) byteLength).toString() + CRLF;
	}

	public static String makeGetAllDocHashMessage(int portNo) {
		return "GETALLDOCHASH #" + " " + portNo + CRLF;
	}

	/** Constructs a PUT request message for the UrlLinks database
	 * 
	 * @param url url to map UrlLinks with (will be overwritten if it exists)
	 * @param byteLength length in bytes of UrlLinks to be transmitted
	 * @return Message formatted to be understood by the content database */
	public static String makePutPageRankScoreMessage(String url, double pagerank) {
		return "PUTPAGERANKSCORE " + pagerank + " " + url + CRLF;
	}

	/** Constructs a PUT request message for the UrlLinks database
	 * 
	 * @param url url to map UrlLinks with (will be overwritten if it exists)
	 * @param byteLength length in bytes of UrlLinks to be transmitted
	 * @return Message formatted to be understood by the content database */
	public static String makePutPRFeedBackScoreMessage(String url,
			double feedbackscore) {
		return "PUTPRFEEDBACKSCORE " + feedbackscore + " " + url + CRLF;
	}

	// feedback score should be relative to the original score, eg +1 or -1 to
	// increment or decrement
	public static String makePutIndexerFeedBackScoreMessage(String word,
			String url, double feedbackscore) {
		// putting url in the last 'coz url might have spaces
		return "PUTINDEXERFEEDBACKSCORE " + word + " "
				+ String.valueOf(feedbackscore) + " " + url + CRLF;
	}

	/** Constructs a GETURLLINKS request message for the content database
	 * 
	 * @param docHash SHA-256 hash of document content to be retrieved
	 * @return Message formatted to be understood by the content database */
	public static String makeGetPageRankScoreMessage(String url, int portNo) {
		return "GETPAGERANKSCORE " + url + " " + portNo + CRLF;
	}

	public static String makeAddRIInfoMessage(String word, String url,
			int byteLength, int portNo) {
		return "ADDRIINFO " + word + " " + portNo + " " + byteLength + " " + url
				+ CRLF;
	}

	public static String makeDeleteAllRIEntriesMessage(int portNo) {
		return "DELALLRIENTRIES # " + portNo + CRLF;
	}

	public static String makeGetUrlsFromHashMessage(String hash, int portNo) {
		return "GETURLSFROMHASH " + hash + " " + portNo + CRLF;
	}

	/** Parses the key from an incoming message
	 * 
	 * @param message Message sent to the storage DHT
	 * @return SHA-256 hash of the document contents being referenced by the
	 *         message */
	public static String parseKey(String message, int msgType) {
		if (message == null || message.split(" ").length < 2)
			return null;
		if (msgType == PUTPAGERANKSCORE || msgType == PUTPRFEEDBACKSCORE) {
			if (message.split(" ").length < 3)
				return null;
			return message.split(" ")[2];
		}
		return message.split(" ")[1];
	}

	public static Long parseNewCrawlTime(String updateCrawlTimeMsg) {
		if (updateCrawlTimeMsg == null || updateCrawlTimeMsg.split(" ").length < 3)
			return null;
		Long newCrawlTime;
		try {
			newCrawlTime = Long.parseLong(updateCrawlTimeMsg.split(" ")[2]);
		} catch (NumberFormatException e) {
			return null;
		}
		return newCrawlTime;
	}

	public static Double parseNewPageRank(String updatePageRankMsg) {
		if (updatePageRankMsg == null || updatePageRankMsg.split(" ").length < 3)
			return null;
		Double newPR;
		try {
			newPR = Double.parseDouble(updatePageRankMsg.split(" ")[1]);
		} catch (NumberFormatException e) {
			return null;
		}
		return newPR;
	}

	public static Double parseNewPRFeedbackScore(String updatePRFeedbackMsg) {
		if (updatePRFeedbackMsg == null
				|| updatePRFeedbackMsg.split(" ").length < 3)
			return null;
		Double newPRFB;
		try {
			newPRFB = Double.parseDouble(updatePRFeedbackMsg.split(" ")[1]);
		} catch (NumberFormatException e) {
			return null;
		}
		return newPRFB;
	}

	public static int parsePortNo(String message) {
		if (message == null)
			return 10001;
		return Integer.parseInt(message.split(" ")[2]);
	}

	public static String parseUrlToHashValue(String message) {
		if (message == null || message.split(" ").length < 3)
			return null;
		return message.split(" ")[2];
	}

	public static double parseNdf(String message) {
		if (message == null || message.split(" ").length < 3)
			return -1;
		return Double.parseDouble(message.split(" ")[2]);
	}

	public static boolean shouldHaveKey(int messageType) {
		switch (messageType) {
			case GETCONTENT:
			case PUTCONTENT:
			case CONTAINSCONTENT:
			case GETRIENTRY:
			case PUTRIENTRY:
			case UPDATECRAWLTIME:
			case PUTPAGERANKSCORE:
			case PUTPRFEEDBACKSCORE:
			case PUTINDEXERFEEDBACKSCORE:
			case GETPAGERANKSCORE:
			case GETHASHFROMURL:
			case PUTURLTOHASH:
			case ADDRIINFO:
			case GETURLSFROMHASH:
				return true;
			case GETALLDOCHASH:
			case DELALLRIENTRIES:
				return false;
			default:
				return true;
		}
	}

	public static boolean needsReplication(int messageType) {
		switch (messageType) {
			case PUTCONTENT:
			case PUTRIENTRY:
			case UPDATECRAWLTIME:
			case PUTPAGERANKSCORE:
			case PUTPRFEEDBACKSCORE:
			case PUTINDEXERFEEDBACKSCORE:
			case PUTURLTOHASH:
			case ADDRIINFO:
				return true;
			default:
				return false;
		}
	}

	public static boolean needsOutgoingSocket(int messageType) {
		switch (messageType) {
			case PUTURLTOHASH:
			case PUTPAGERANKSCORE:
			case UPDATECRAWLTIME:
			case PUTPRFEEDBACKSCORE:
			case PUTINDEXERFEEDBACKSCORE:
			case PUTRIENTRY:
				return false;
			default:
				return true;
		}
	}

	/** Parses the message type from an incoming message
	 * 
	 * @param message Message sent to the storage DHT
	 * @return Message type indicated by one of the constants enumerated in this
	 *         class */

	public static int parseMessageType(String message) {
		if (message.trim().equalsIgnoreCase("getalldochash"))
			return GETALLDOCHASH;
		if (message == null || message.split(" ").length < 2)
			return UNKNOWN;
		if (message.split(" ")[0].equalsIgnoreCase("getcontent"))
			return GETCONTENT;
		else if (message.split(" ")[0].equalsIgnoreCase("putcontent"))
			return PUTCONTENT;
		else if (message.split(" ")[0].equalsIgnoreCase("containscontent"))
			return CONTAINSCONTENT;
		else if (message.split(" ")[0].equalsIgnoreCase("getrientry"))
			return GETRIENTRY;
		else if (message.split(" ")[0].equalsIgnoreCase("putrientry"))
			return PUTRIENTRY;
		else if (message.split(" ")[0].equalsIgnoreCase("gethashfromurl"))
			return GETHASHFROMURL;
		else if (message.split(" ")[0].equalsIgnoreCase("puturltohash"))
			return PUTURLTOHASH;
		else if (message.split(" ")[0].equalsIgnoreCase("getalldochash"))
			return GETALLDOCHASH;
		else if (message.split(" ")[0].equalsIgnoreCase("putpagerankscore"))
			return PUTPAGERANKSCORE;
		else if (message.split(" ")[0].equalsIgnoreCase("getpagerankscore"))
			return GETPAGERANKSCORE;
		else if (message.split(" ")[0].equalsIgnoreCase("updatecrawltime"))
			return UPDATECRAWLTIME;
		else if (message.split(" ")[0].equalsIgnoreCase("addriinfo"))
			return ADDRIINFO;
		else if (message.split(" ")[0].equalsIgnoreCase("delallrientries"))
			return DELALLRIENTRIES;
		else if (message.split(" ")[0].equalsIgnoreCase("geturlsfromhash"))
			return GETURLSFROMHASH;
		else if (message.split(" ")[0].equalsIgnoreCase("putprfeedbackscore"))
			return PUTPRFEEDBACKSCORE;
		else if (message.split(" ")[0].equalsIgnoreCase("putindexerfeedbackscore"))
			return PUTINDEXERFEEDBACKSCORE;
		return UNKNOWN;
	}

	/** Parses the content length from an incoming PUT message
	 * 
	 * @param message Message sent to the storage DHT
	 * @return Length of the content being sent in characters; else, returns -1 if
	 *         the message is malformed or the length is invalid */
	public static int parseContentLength(String message) {
		int result = -1;
		if (message != null && message.split(" ").length > 3) {
			try {
				result = Integer.parseInt(message.split(" ")[3]);
			} catch (NumberFormatException e) {
				return result;
			}
			return result;
		}
		return result;
	}

	public static byte[] convertLongToBytes(long number) {
		return ByteBuffer.allocate(BYTES_IN_LONG).putLong(number).array();
	}

	public static long convertBytesToLong(byte[] byteArray) {
		return ByteBuffer.wrap(byteArray).getLong();
	}

	public static byte[] convertDoubleToBytes(double number) {
		return ByteBuffer.allocate(BYTES_IN_DOUBLE).putDouble(number).array();
	}

	public static double convertBytesToDouble(byte[] byteArray) {
		return ByteBuffer.wrap(byteArray).getDouble();
	}

}
