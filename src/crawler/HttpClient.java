/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package crawler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.net.ssl.SSLSocketFactory;
import storage.UrlContent;

/** @author Michael Collis (mcollis@seas.upenn.edu) */
public class HttpClient {

	private Socket sock = null;
	private String hostUrl = null;
	private String resource = null;
	private BufferedReader input = null;
	private DataOutputStream output = null;
	private InetAddress ip = null;
	private static final String blankLineRegExp = "^[\\r]+[\\n]+";
	private static final boolean DEBUG = false;
	private Charset UTF8 = Charset.forName("UTF-8");

	/** Constructor with unresolved hostname */

	public HttpClient(String host, String file) throws UnknownHostException,
			IOException {
		this(host, file, false);
	}

	/** Constructor with resolved host IP address */
	public HttpClient(InetAddress host, String file) throws UnknownHostException,
			IOException {
		this(host.getHostName(), file, false);
		ip = host;
	}

	/** Constructor for SSL socket with unresolved hostname */
	public HttpClient(String host, String file, Boolean ssl)
			throws UnknownHostException, IOException {
		hostUrl = host;
		resource = file;
		if (ssl) {
			// Security.addProvider(new Provider());
			SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory
					.getDefault();
			sock = factory.createSocket(hostUrl, 443);
		} else {
			sock = new Socket(hostUrl, 80);
		}
		sock.setSoTimeout(3000);
	}

	/** Constructor for SSL socket with resolved host IP address */
	public HttpClient(InetAddress host, String file, Boolean ssl)
			throws UnknownHostException, IOException {
		this(host.getHostName(), file, true);
		ip = host;
	}

	public UrlContent sendHead() throws IOException {
		return sendHead(null);
	}

	public UrlContent sendHead(Long ifModSince) throws IOException {
		String request = makeRequestLine("HEAD");
		if (!openStreams()) {
			closeStreams();
			return null;
		}
		if (ifModSince != null && ifModSince != -1) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"EEE, d MMM yyyy HH:mm:ss z");
			request += "If-Modified-Since: "
					+ dateFormat.format(new Date(ifModSince)) + "\n";
		}
		output.writeBytes(request + "\r\n");
		output.flush();
		UrlContent response = retrieveHeaders();
		closeStreams();
		return response;
	}

	public UrlContent sendGet(boolean keepAlive) throws IOException {
		String request = makeRequestLine("GET");
		if (!openStreams())
			return null;

		output.writeBytes(request);
		output.flush();
		UrlContent response = retrieveHeaders();
		if (response == null) {
			closeStreams();
			return null;
		}
		response.setCrawlTime(new Date().getTime());

		int targetLength = 0;
		if (response.getContentLength() > 0) {
			targetLength = (int) response.getContentLength();
		}

		String returnedFile = "", line = "", lastLine = "";
		while ((targetLength == 0 || returnedFile.length() < targetLength)
				&& (line = input.readLine()) != null) {
			if (targetLength == 0 && lastLine.matches(blankLineRegExp)
					&& line.matches(blankLineRegExp)) {
				break;
			}
			lastLine = line;
			returnedFile += line + "\r\n";
		}

		if (targetLength == 0) {
			response.setHeader("Content-Length",
					Integer.toString(returnedFile.length()));
		}

		if (response.getContentType() != null
				&& response.getContentType().toLowerCase().indexOf("pdf") == -1) {
			String encodedContent = convertToUTF8(response, returnedFile);
			closeStreams();
			if (encodedContent != null) {
				response.setPKey(generateHash(returnedFile));
				response.setContentString(encodedContent);
				printDebug("HTTP Client: GET request complete.");
				return response;
			} else
				return null;
		} else {
			response.setContent(returnedFile.getBytes());
			return response;
		}
	}

	public InetAddress getInetAddress() {
		return ip;
	}

	private boolean openStreams() {
		if (sock != null) {
			try {
				if (input == null) {
					input = new BufferedReader(new InputStreamReader(
							sock.getInputStream()));
				}
				if (output == null) {
					output = new DataOutputStream(sock.getOutputStream());
				}

				if (input == null || output == null)
					return false;
				else
					return true;
			} catch (IOException e) {
				return false;
			}
		} else
			return false;
	}

	private boolean closeStreams() {
		if (sock != null) {
			try {
				if (input == null) {
					input.close();
					input = null;
				}
				if (output == null) {
					output.close();
					output = null;
				}
			} catch (IOException e) {
				return false;
			}
			return true;
		} else
			return false;
	}

	private String makeRequestLine(String httpMethod) {
		int protocol = -1;
		if (hostUrl != null) {
			protocol = hostUrl.indexOf("://");
		}
		String request = null;
		if (protocol >= 0) {
			request = httpMethod + " " + resource + " HTTP/1.1\r\nHost: "
					+ hostUrl.substring(protocol + 3)
					+ "\r\nUser-Agent: cis455crawler\r\nConnection: close\r\n\r\n";
		} else {
			request = httpMethod + " " + resource + " HTTP/1.1\r\nHost: " + hostUrl
					+ "\r\nUser-Agent: cis455crawler\r\nConnection: close\r\n\r\n";
		}
		return request;
	}

	private UrlContent retrieveHeaders() {
		UrlContent response = new UrlContent();
		String statusReply = null;
		try {
			String line = input.readLine();
			while (line != null && line.equals("")) {
				line = input.readLine();
			}

			while (line != null && !line.equals("")) {
				if (statusReply == null) {
					statusReply = line;
					response.setStatus(statusReply);
					printDebug("HTTP Client: " + statusReply);
				} else {
					int divider = line.indexOf(":");
					if (divider > 0 && divider < line.length() - 1) {
						response.setHeader(line.substring(0, divider).trim(), line
								.substring(divider + 1).trim());
					}
				}
				line = input.readLine();
			}
		} catch (IOException e) {
			return null;
		}
		response.setHost(hostUrl);
		response.setUrl(hostUrl + resource);
		return response;
	}

	public void close() throws IOException {
		sock.close();
	}

	private static void printDebug(String debugInfo) {
		if (DEBUG) {
			System.out.println(debugInfo);
		}
	}

	private String generateHash(String content) {
		if (content.length() == 0)
			return null;
		StringBuilder hashed = new StringBuilder();
		try {
			MessageDigest cryptHash = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = cryptHash.digest(content.getBytes());
			for (int k = 0; k < hashBytes.length; k++) {
				hashed.append(String.format("%02x", hashBytes[k]));
			}
		} catch (NoSuchAlgorithmException e) {
			CrawlerDrone.addLog("HTTP Client: Content hashing failed.");
		}
		return hashed.toString();
	}

	private String convertToUTF8(UrlContent content, String toConvert) {
		String encoding = content.getEncoding();
		String charsetToUse = null;
		if (encoding == null)
			return toConvert;
		if (encoding.equalsIgnoreCase("UTF-8")
				|| encoding.equalsIgnoreCase("\"UTF-8\"")
				|| encoding.equalsIgnoreCase("UTF8")
				|| encoding.equalsIgnoreCase("UTF-8;"))
			return toConvert;
		else if (encoding.equalsIgnoreCase("ISO-8859-1")
				|| encoding.equalsIgnoreCase("ISO8859_1")
				|| encoding.equalsIgnoreCase("ISO8859-1")
				|| encoding.equalsIgnoreCase("\"ISO-8859-1\"")
				|| encoding.equalsIgnoreCase("latin-1")
				|| encoding.equalsIgnoreCase("latin1")
				|| encoding.equalsIgnoreCase("iso latin-1")
				|| encoding.equalsIgnoreCase("iso-latin-1")) {
			charsetToUse = "ISO-8859-1";
		} else if (encoding.equalsIgnoreCase("ISO-8859-2")
				|| encoding.equalsIgnoreCase("ISO8859_2")
				|| encoding.equalsIgnoreCase("ISO8859-2")
				|| encoding.equalsIgnoreCase("\"ISO-8859-2\"")
				|| encoding.equalsIgnoreCase("latin-2")
				|| encoding.equalsIgnoreCase("latin2")
				|| encoding.equalsIgnoreCase("iso latin-2")
				|| encoding.equalsIgnoreCase("iso-latin-2")) {
			charsetToUse = "ISO-8859-2";
		} else if (encoding.equalsIgnoreCase("ISO-8859-3")
				|| encoding.equalsIgnoreCase("ISO8859_3")
				|| encoding.equalsIgnoreCase("ISO8859-3")
				|| encoding.equalsIgnoreCase("\"ISO-8859-3\"")
				|| encoding.equalsIgnoreCase("latin-3")
				|| encoding.equalsIgnoreCase("latin3")) {
			charsetToUse = "ISO-8859-3";
		} else if (encoding.equalsIgnoreCase("ISO-8859-4")
				|| encoding.equalsIgnoreCase("ISO8859_4")
				|| encoding.equalsIgnoreCase("ISO8859-4")
				|| encoding.equalsIgnoreCase("\"ISO-8859-4\"")
				|| encoding.equalsIgnoreCase("latin-4")
				|| encoding.equalsIgnoreCase("latin4")) {
			charsetToUse = "ISO-8859-4";
		} else if (encoding.equalsIgnoreCase("ISO-8859-5")
				|| encoding.equalsIgnoreCase("ISO8859_5")
				|| encoding.equalsIgnoreCase("ISO8859-5")
				|| encoding.equalsIgnoreCase("\"ISO-8859-5\"")
				|| encoding.equalsIgnoreCase("latin-5")
				|| encoding.equalsIgnoreCase("latin5")) {
			charsetToUse = "ISO-8859-5";
		} else if (encoding.equalsIgnoreCase("ISO-8859-6")
				|| encoding.equalsIgnoreCase("ISO8859_6")
				|| encoding.equalsIgnoreCase("ISO8859-6")
				|| encoding.equalsIgnoreCase("\"ISO-8859-6\"")
				|| encoding.equalsIgnoreCase("latin-6")
				|| encoding.equalsIgnoreCase("latin6")) {
			charsetToUse = "ISO-8859-6";
		} else if (encoding.equalsIgnoreCase("ISO-8859-7")
				|| encoding.equalsIgnoreCase("ISO8859_7")
				|| encoding.equalsIgnoreCase("ISO8859-7")
				|| encoding.equalsIgnoreCase("\"ISO-8859-7\"")
				|| encoding.equalsIgnoreCase("latin-7")
				|| encoding.equalsIgnoreCase("latin7")) {
			charsetToUse = "ISO-8859-7";
		} else if (encoding.equalsIgnoreCase("ISO-8859-8")
				|| encoding.equalsIgnoreCase("ISO8859_8")
				|| encoding.equalsIgnoreCase("ISO8859-8")
				|| encoding.equalsIgnoreCase("\"ISO-8859-8\"")
				|| encoding.equalsIgnoreCase("latin-8")
				|| encoding.equalsIgnoreCase("latin8")) {
			charsetToUse = "ISO-8859-8";
		} else if (encoding.equalsIgnoreCase("ISO-8859-9")
				|| encoding.equalsIgnoreCase("ISO8859_9")
				|| encoding.equalsIgnoreCase("ISO8859-9")
				|| encoding.equalsIgnoreCase("\"ISO-8859-9\"")
				|| encoding.equalsIgnoreCase("latin-9")
				|| encoding.equalsIgnoreCase("latin9")) {
			charsetToUse = "ISO-8859-9";
		} else if (encoding.equalsIgnoreCase("ISO-8859-13")
				|| encoding.equalsIgnoreCase("ISO8859_13")
				|| encoding.equalsIgnoreCase("ISO8859-13")
				|| encoding.equalsIgnoreCase("\"ISO-8859-13\"")) {
			charsetToUse = "ISO-8859-13";
		} else if (encoding.equalsIgnoreCase("ISO-8859-15")
				|| encoding.equalsIgnoreCase("ISO8859_15")
				|| encoding.equalsIgnoreCase("ISO8859-15")
				|| encoding.equalsIgnoreCase("\"ISO-8859-15\"")) {
			charsetToUse = "ISO-8859-15";
		} else if (encoding.equalsIgnoreCase("ISO-2022-JP")) {
			charsetToUse = "ISO-2022-JP";
		} else if (encoding.equalsIgnoreCase("ISO-2022-KR")) {
			charsetToUse = "ISO-2022-KR";
		} else if (encoding.equalsIgnoreCase("ISO-2022-CN")) {
			charsetToUse = "ISO-2022-CN";
		} else if (encoding.equalsIgnoreCase("US-ASCII")
				|| encoding.equalsIgnoreCase("ASCII")) {
			charsetToUse = "US-ASCII";
		} else if (encoding.equalsIgnoreCase("KOI8-R")
				|| encoding.equalsIgnoreCase("KOI8_R")) {
			charsetToUse = "KOI8-R";
		} else if (encoding.equalsIgnoreCase("windows-1250")
				|| encoding.equalsIgnoreCase("win-1250")
				|| encoding.equalsIgnoreCase("Cp1250")
				|| encoding.equalsIgnoreCase("Cp-1250")) {
			charsetToUse = "windows-1250";
		} else if (encoding.equalsIgnoreCase("windows-1251")
				|| encoding.equalsIgnoreCase("win-1251")
				|| encoding.equalsIgnoreCase("windows-1251;")
				|| encoding.equalsIgnoreCase("Cp1251")
				|| encoding.equalsIgnoreCase("Cp-1251")) {
			charsetToUse = "windows-1251";
		} else if (encoding.equalsIgnoreCase("windows-1252")
				|| encoding.equalsIgnoreCase("win-1252")
				|| encoding.equalsIgnoreCase("Cp1252")
				|| encoding.equalsIgnoreCase("Cp-1252")) {
			charsetToUse = "windows-1252";
		} else if (encoding.equalsIgnoreCase("windows-1253")
				|| encoding.equalsIgnoreCase("win-1253")
				|| encoding.equalsIgnoreCase("Cp1253")
				|| encoding.equalsIgnoreCase("Cp-1253")) {
			charsetToUse = "windows-1253";
		} else if (encoding.equalsIgnoreCase("windows-1254")
				|| encoding.equalsIgnoreCase("win-1254")
				|| encoding.equalsIgnoreCase("Cp1254")
				|| encoding.equalsIgnoreCase("Cp-1254")) {
			charsetToUse = "windows-1254";
		} else if (encoding.equalsIgnoreCase("windows-1255")
				|| encoding.equalsIgnoreCase("win-1255")
				|| encoding.equalsIgnoreCase("Cp1255")
				|| encoding.equalsIgnoreCase("Cp-1255")) {
			charsetToUse = "windows-1255";
		} else if (encoding.equalsIgnoreCase("windows-1256")
				|| encoding.equalsIgnoreCase("win-1256")
				|| encoding.equalsIgnoreCase("Cp1256")
				|| encoding.equalsIgnoreCase("Cp-1256")) {
			charsetToUse = "windows-1256";
		} else if (encoding.equalsIgnoreCase("windows-1257")
				|| encoding.equalsIgnoreCase("win-1257")
				|| encoding.equalsIgnoreCase("Cp1257")
				|| encoding.equalsIgnoreCase("Cp-1257")) {
			charsetToUse = "windows-1257";
		} else if (encoding.equalsIgnoreCase("windows-1258")
				|| encoding.equalsIgnoreCase("win-1258")
				|| encoding.equalsIgnoreCase("Cp1258")
				|| encoding.equalsIgnoreCase("Cp-1258")) {
			charsetToUse = "windows-1258";
		} else if (encoding.equalsIgnoreCase("windows-31j")
				|| encoding.equalsIgnoreCase("win-31j")
				|| encoding.equalsIgnoreCase("MS932")
				|| encoding.equalsIgnoreCase("MS-932")) {
			charsetToUse = "windows-31j";
		} else if (encoding.equalsIgnoreCase("x-windows-874")
				|| encoding.equalsIgnoreCase("windows-874")
				|| encoding.equalsIgnoreCase("MS874")
				|| encoding.equalsIgnoreCase("MS-874")) {
			charsetToUse = "x-windows-874";
		} else if (encoding.equalsIgnoreCase("Shift_JIS")
				|| encoding.equalsIgnoreCase("SJIS")
				|| encoding.equalsIgnoreCase("Shift-JIS")) {
			charsetToUse = "Shift_JIS";
		} else if (encoding.equalsIgnoreCase("Big5")) {
			charsetToUse = "Big5";
		} else if (encoding.equalsIgnoreCase("Big5-HKSCS")
				|| encoding.equalsIgnoreCase("Big5_HKSCS")) {
			charsetToUse = "Big5-HKSCS";
		} else if (encoding.equalsIgnoreCase("GB18030")) {
			charsetToUse = "GB18030";
		} else if (encoding.equalsIgnoreCase("GB2312")
				|| encoding.equalsIgnoreCase("EUC_CN")) {
			charsetToUse = "GB2312";
		} else if (encoding.equalsIgnoreCase("GBK")) {
			charsetToUse = "GBK";
		} else if (encoding.equalsIgnoreCase("EUC-JP")
				|| encoding.equalsIgnoreCase("EUC_JP")) {
			charsetToUse = "EUC-JP";
		} else if (encoding.equalsIgnoreCase("EUC-KR")
				|| encoding.equalsIgnoreCase("EUC_KR")) {
			charsetToUse = "EUC-KR";
		} else if (encoding.equalsIgnoreCase("TIS-620")
				|| encoding.equalsIgnoreCase("TIS620")) {
			charsetToUse = "TIS-620";
		}

		if (charsetToUse == null)
			return null;
		boolean supportedCharset = false;
		try {
			supportedCharset = Charset.isSupported(charsetToUse);
		} catch (IllegalCharsetNameException e) {}

		if (!supportedCharset)
			return null;

		try {
			Charset inputEncoding = Charset.forName(charsetToUse);
			CharBuffer internalEncoding = inputEncoding.newDecoder().decode(
					ByteBuffer.wrap(toConvert.getBytes()));
			ByteBuffer outputByteBuffer = UTF8.newEncoder().encode(
					CharBuffer.wrap(internalEncoding));
			return new String(outputByteBuffer.array(), 0, outputByteBuffer.limit(),
					UTF8);
		} catch (IllegalCharsetNameException e) {
			return null;
		} catch (CharacterCodingException e) {
			return null;
		}
	}
}
