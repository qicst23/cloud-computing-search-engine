/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package storage;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.TreeSet;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/** Encapsulation of URL, content and crawl date of a registered user for storage
 * in the BDB instance
 * 
 * @author Michael Collis (mcollis@seas.upenn.edu)
 * @version 20130410 */
@Entity
public class UrlContent implements Serializable {

	public static final String NULL_FIELD = "%&%NULL_FIELD%&%";

	/** Default serial version number */
	private static final long serialVersionUID = 1L;

	/** HTTP status line retrieved on last crawl, including HTTP status code */
	private String status = null;

	/** Serializable storage abstraction for header key-value pairs retrieved on
	 * last crawl @see headerStore */
	private HeaderStore headerstore = null;

	/** Byte array representation of resource content */
	private byte[] contentBytes = null;

	/** String representation of HTTP content type */
	private String contentType = null;

	/** String respresentation of resource's full URL */
	private String url = null;

	/** String representation of the resource's host */
	private String host = null;

	/** Title for the resource */
	private String title = null;

	/** Time the resource was last crawled */
	private Long crawlTime = null;

	/** Time the resource was last indexed */
	private Long lastIndexed = null;

	/** Time the resource was last ranked */
	private Long lastRanked = null;

	/** Time the resource was last returned in search results */
	private Long lastSearched = null;

	/** SHA-256 hash of the UTF-8 encoding of the resource content */
	@PrimaryKey
	private String hash = null;

	private static final String msgDelim = "#&&#";
	private static final String headerDelim = "&##&";

	public String convertToMessage() {
		Hashtable<String, String> headers = getHeaders();
		StringBuilder s = new StringBuilder("status" + msgDelim); // #0
		s.append(status); // #1
		s.append(msgDelim + "headers" + msgDelim); // #2
		// within headers, does not use name+delim+value+delim, just
		// value+delim+value+delim
		StringBuilder headersString = new StringBuilder();
		for (String header : headers.keySet()) {
			if (headersString.length() != 0) {
				headersString.append(headerDelim);
			}
			headersString.append(header);
			headersString.append(headerDelim);
			headersString.append(headers.get(header));
		}
		s.append(headersString); // #3
		/*
		 * s.append(msgDelim + "contentType" + msgDelim); s.append(contentType);
		 */// contentType is derived from headers
		s.append(msgDelim + "url" + msgDelim); // #4
		s.append(url); // #5
		s.append(msgDelim + "host" + msgDelim); // #6
		s.append(host); // #7
		s.append(msgDelim + "title" + msgDelim); // #8
		if (title != null) {
			s.append(title); // #9
		} else {
			s.append(NULL_FIELD);
		}
		s.append(msgDelim + "crawlTime" + msgDelim); // #10
		if (crawlTime != null) {
			s.append(crawlTime); // #11
		} else {
			s.append(NULL_FIELD);
		}
		s.append(msgDelim + "lastIndexed" + msgDelim); // #12
		if (lastIndexed != null) {
			s.append(lastIndexed); // #13
		} else {
			s.append(NULL_FIELD);
		}
		s.append(msgDelim + "lastRanked" + msgDelim); // #14
		if (lastRanked != null) {
			s.append(lastRanked); // #15
		} else {
			s.append(NULL_FIELD);
		}
		s.append(msgDelim + "lastSearched" + msgDelim); // #16
		if (lastSearched != null) {
			s.append(lastSearched); // #17
		} else {
			s.append(NULL_FIELD);
		}
		s.append(msgDelim + "hash" + msgDelim); // #18
		s.append(hash); // #19
		s.append(msgDelim + "content" + msgDelim); // #20
		s.append(getContentString()); // #21
		return s.toString();
	}

	public static UrlContent convertFromMessage(String msg)
			throws NumberFormatException {
		String[] mainSplit = msg.split(msgDelim, 22);
		if (mainSplit.length < 22)
			return null;
		UrlContent res = new UrlContent();
		res.setStatus(mainSplit[1]);// every value we access should be odd, names
																// are even
		String[] headers = mainSplit[3].split(headerDelim);
		// header size should be even
		for (int i = 0; i < headers.length; i += 2) {
			res.setHeader(headers[i], headers[i + 1]);
		}
		res.setUrl(mainSplit[5]);
		res.setHost(mainSplit[7]);
		if (!mainSplit[9].equals(NULL_FIELD)) {
			res.setTitle(mainSplit[9]);
		}
		if (!mainSplit[11].equals(NULL_FIELD)) {
			Long _crawlTime = Long.valueOf(mainSplit[11]);
			res.setCrawlTime(_crawlTime);
		}
		if (!mainSplit[13].equals(NULL_FIELD)) {
			Long _lastIndexed = Long.valueOf(mainSplit[13]);
			res.setLastIndexed(_lastIndexed);
		}
		if (!mainSplit[15].equals(NULL_FIELD)) {
			Long _lastRanked = Long.valueOf(mainSplit[15]);
			res.setLastRanked(_lastRanked);
		}
		if (!mainSplit[17].equals(NULL_FIELD)) {
			Long _lastSearched = Long.valueOf(mainSplit[17]);
			res.setLastSearched(_lastSearched);
		}

		res.setPKey(mainSplit[19]);
		res.setContentString(mainSplit[21]);
		return res;
	}

	/** Default constructor */
	public UrlContent() {}

	/** Set the primary key (SHA-256 hash of the resource's content)
	 * 
	 * @param input 256-bit hash in String format of the content */
	public void setPKey(String input) {
		hash = input;
	}

	/** Retrieve the primary key
	 * 
	 * @return Hash of the resource's content if it has been initialized; else,
	 *         returns null */
	public String getPKey() {
		return hash;
	}

	/** Set the HTTP Status line returned last time the page was crawled
	 * 
	 * @param inputStatus First line of the response to the HTTP request that
	 *          retrieved the page */
	public void setStatus(String inputStatus) {
		status = inputStatus;
	}

	/** Retrieve the HTTP status code returned last time the page was crawled
	 * 
	 * @return HTTP status code if the status line has been initialized and the
	 *         code is well-formed; else, returns -1 */
	public int getStatus() {
		if (status == null)
			return -1;
		int statusCode = -1;
		try {
			statusCode = Integer.parseInt(status.split(" ")[1]);
		} catch (NumberFormatException e) {}
		return statusCode;
	}

	/** Set a header key-value pair, initializing the header storage abstraction if
	 * this has not yet taken place
	 * 
	 * @param key Header name
	 * @param value Header value */
	public void setHeader(String key, String value) {
		if (headerstore == null) {
			headerstore = new HeaderStore();
			headerstore.setup();
		}
		headerstore.put(key.toLowerCase(), value);
	}

	/** Delete records of all headers */
	public void resetHeaders() {
		headerstore = null;
	}

	/** Retrieve all the headers last time the page was crawled
	 * 
	 * @return Hashtable of the header key-value pairs */
	public Hashtable<String, String> getHeaders() {
		Hashtable<String, String> headerTable = new Hashtable<String, String>();
		TreeSet<String> headerSet = headerstore.getHeaders();
		for (String header : headerSet) {
			String[] keyval = header.split("#");
			if (keyval.length == 2) {
				headerTable.put(keyval[0].trim(), keyval[1].trim());
			}
		}
		return headerTable;
	}

	/** Set the page content using a String input
	 * 
	 * @param retrieved Input string format of page content */
	public void setContentString(String retrieved) {
		try {
			contentBytes = retrieved.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {}
	}

	/** Set the page content
	 * 
	 * @param retrieved Input raw byte array format of page content */
	public void setContent(byte[] retrieved) {
		contentBytes = retrieved;
	}

	/** Retrieve the stored page content as a String
	 * 
	 * @return Page content if it has been initialized; else, returns null */
	public String getContentString() {
		try {
			return new String(contentBytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public byte[] getContentBytes() {
		return contentBytes;
	}

	/** Returns the content type of the page
	 * 
	 * @return String representation of the HTTP content type if the content type
	 *         is among the headers retrieved; else returns null */
	public String getContentType() {
		if (contentType == null) {
			if (headerstore == null)
				return null;
			else {
				if (headerstore.containsKey("content-type")) {
					contentType = headerstore.get("content-type").split(";")[0].trim();
				}
			}
		}
		return contentType;
	}

	/** Set the full URL of the resource
	 * 
	 * @param url Location of resource being created */
	public void setUrl(String url) {
		this.url = url;
	}

	/** Retrieve the full URL of the resource
	 * 
	 * @return URL of the resource, if it has been initialized; else, return null */
	public String getUrl() {
		return url;
	}

	/** Set the hostname of the resource (terminated with '/')
	 * 
	 * @param inputHost Hostname */
	public void setHost(String inputHost) {
		if (inputHost.endsWith("/")) {
			host = inputHost;
		} else {
			host = inputHost + "/";
		}
	}

	/** Retrieve the hostname of the resource
	 * 
	 * @return Hostname if it has been initialized; else, return null */
	public String getHost() {
		return host;
	}

	/** Set the title of the resource
	 * 
	 * @param inputTitle Title parsed from the title tag of the resource */
	public void setTitle(String inputTitle) {
		title = inputTitle;
	}

	/** Retrieve the title of the resource
	 * 
	 * @return Title of the page if it has been initialized; else, returns null */
	public String getTitle() {
		return title;
	}

	/** Set the time the page was last crawled
	 * 
	 * @param time Standard UNIX long integer representation of the crawl time */
	public void setCrawlTime(Long time) {
		crawlTime = time;
	}

	/** Retrieve the time the page was last crawled
	 * 
	 * @return Standard UNIX long integer representation of the crawl time if it
	 *         has been initialized; else, returns null */
	public Long lastCrawled() {
		return crawlTime;
	}

	/** Set the time the page was last indexed
	 * 
	 * @param time Standard UNIX long integer representation of the indexing time */
	public void setLastIndexed(Long time) {
		lastIndexed = time;
	}

	/** Retrieve the time the page was last indexed
	 * 
	 * @return Standard UNIX long integer representation of the indexing time if
	 *         it has been initialized; else, returns null */
	public Long lastIndexed() {
		return lastIndexed;
	}

	/** Set the time the page was last ranked
	 * 
	 * @param time Standard UNIX long integer representation of the ranking time */
	public void setLastRanked(Long time) {
		lastRanked = time;
	}

	/** Retrieve the time the page was last ranked
	 * 
	 * @return Standard UNIX long integer representation of the ranking time if it
	 *         has been initialized; else, returns null */
	public Long lastRanked() {
		return lastRanked;
	}

	/** Set the time the page was last returned in search results
	 * 
	 * @param time Standard UNIX long integer representation of the time the
	 *          resource was last returned in search results */
	public void setLastSearched(Long time) {
		lastSearched = time;
	}

	/** Retrieve the time the page was last searched
	 * 
	 * @return Standard UNIX long integer representation of the time the resource
	 *         was last returned in search results if it has been initialized;
	 *         else, returns null */
	public Long lastSearched() {
		return lastSearched;
	}

	/** Retrieve the time the page was last modified from the resource's headers
	 * 
	 * @return Standard UNIX long integer representation of the time the resource
	 *         was last modified, if a "Last-Modified" header was retrieved; else,
	 *         returns -1L */
	public Long getLastModified() {
		if (headerstore != null) {
			if (headerstore.containsKey("last-modified")) {
				String modDateStr = headerstore.get("last-modified");
				SimpleDateFormat dateFormat1 = new SimpleDateFormat(
						"EEE, d MMM yyyy HH:mm:ss z");
				SimpleDateFormat dateFormat2 = new SimpleDateFormat(
						"EEEEEE, d-MMM-yy HH:mm:ss z");
				SimpleDateFormat dateFormat3 = new SimpleDateFormat(
						"EEE MMM d HH:mm:ss yyyy");
				Date modDate = null;
				try {
					modDate = dateFormat1.parse(modDateStr);
				} catch (ParseException e1) {
					try {
						modDate = dateFormat2.parse(modDateStr);
					} catch (ParseException e2) {
						try {
							modDate = dateFormat3.parse(modDateStr);
						} catch (ParseException e3) {
							return -1L;
						}
					}
				}
				if (modDate != null)
					return modDate.getTime();
			}
			return -1L;
		}
		return -1L;
	}

	/** Retrieve the character length of the resource content
	 * 
	 * @return Length in UTF-8 characters of the resource content if a
	 *         "Content-Length" header was retrieved, and is well-formed; else,
	 *         returns -1 */
	public long getContentLength() {
		if (headerstore != null) {
			if (headerstore.containsKey("content-length")) {
				try {
					return Long.parseLong(headerstore.get("content-length"));
				} catch (NumberFormatException e) {
					return -1;
				}
			}
			return -1;
		}
		return -1;
	}

	/** Retrieve the character set encoding of the document, if available
	 * @return Charset header value given in the HTTP response */
	public String getEncoding() {
		if (headerstore != null) {
			if (headerstore.containsKey("content-type")
					&& headerstore.get("content-type").split(";").length > 1
					&& headerstore.get("content-type").split(";")[1].split("=").length > 1)
				return headerstore.get("content-type").split(";")[1].split("=")[1]
						.trim();
			return null;
		}
		return null;
	}

	/** Returns the URL indicated by the "Location" header if the page sent a
	 * redirect request
	 * 
	 * @return Redirected destination URL, if a "Location" header was retrieved;
	 *         else, returns null */
	public String getRedirect() {
		if (headerstore != null) {
			if (headerstore.containsKey("location"))
				return headerstore.get("location");
			return null;
		}
		return null;
	}

	/** Check if the resource is of a MIME type want to crawl (HTML, XML, XHTML,
	 * and PDF)
	 * 
	 * @return True if the "Content-Type" header has been retrieved and the MIME
	 *         type matches the crawler requirements; else, returns false */
	public boolean conformingMimeType() {
		if (headerstore != null) {
			if (contentType != null || getContentType() != null)
				return contentType.equalsIgnoreCase("text/html")
						|| contentType.equalsIgnoreCase("text/xml")
						|| contentType.equalsIgnoreCase("application/xml")
						|| contentType.toLowerCase().indexOf("+xml") >= 0
						|| contentType.equalsIgnoreCase("application/pdf");
		}
		return false;
	}

	/** Checks if the resource's host sent the file with chunked encoding
	 * 
	 * @return True if the "Transfer-Encoding" header has been retrieved and value
	 *         is "Chunked"; else, returns false */
	public boolean hasChunkedEncoding() {
		if (headerstore != null) {
			if (headerstore.containsKey("transfer-encoding"))
				return headerstore.get("transfer-encoding").equalsIgnoreCase("chunked");
			return false;
		}
		return false;
	}

	/** Checks if the resource's host requires the use of HTTP Strict Transport
	 * Security to retrieve the resource
	 * 
	 * @return True if the "Strict-Transport-Security" header has been retrieved;
	 *         else, returns false */
	public boolean requiresHSTS() {
		if (headerstore != null) {
			if (headerstore.containsKey("strict-transport-security"))
				return true;
			return false;
		}
		return false;
	}

	/** Checks if the resource's host sent the file with "Connection: keep-alive"
	 * header
	 * 
	 * @return True if the "Connection" header has been retrieved and value is
	 *         "keep-alive"; else, returns false */
	public boolean isKeepAlive() {
		if (headerstore != null) {
			if (headerstore.containsKey("connection"))
				return headerstore.get("connection").equalsIgnoreCase("keep-alive");
			return false;
		}
		return false;
	}

}
