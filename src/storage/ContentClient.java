/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/** Encapsulates sending messages and receiving responses from the distributed
 * content storage DHT
 * 
 * @author Michael Collis (mcollis@seas.upenn.edu)
 * @version 20130411 */
public class ContentClient {

	/** InetAddresses for DHT bootstrap gateways */
	private ArrayList<InetAddress> dbGateways = null;

	/** Number of bytes read in to indicate content length */
	private static final int BYTES_IN_LONG = 8;

	@SuppressWarnings("unused")
	private static final int BYTES_IN_DOUBLE = 8;

	/** Number of bytes read in to indicate true or false response for
	 * sendContains() query */
	private static final int BYTES_IN_BOOL_STR = 12;

	private static final int SOCK_TIMEOUT = 3000;

	/** Default constructor
	 * 
	 * @param gateway InetAddress of bootstrap node of content storage DHT */
	public ContentClient(ArrayList<InetAddress> gateways) {
		dbGateways = gateways;
	}

	/** Send a GET CONTENT request to the storage system
	 * 
	 * @param docHash SHA-256 hash of the document content to be retrieved
	 * @return UrlContent object with the primary key requested if it exists in
	 *         the database; else, returns null if object with requested key does
	 *         not exist or if there was an error transmitting the message */
	public UrlContent sendGetContent(String docHash) {
		for (InetAddress gateway : dbGateways) {
			ServerSocket serverSocketTest = null;
			int portNo = StorageHelper.RECEIVE_DATA_PORT;
			try {

				serverSocketTest = new ServerSocket(0);
				serverSocketTest.setSoTimeout(SOCK_TIMEOUT);
				portNo = serverSocketTest.getLocalPort();
			} catch (IOException e) {
				continue;
			}
			Socket incomingSocket = null;
			Socket outgoingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer
						.write(StorageHelper.makeGetContentMessage(docHash, portNo));
				outputBuffer.flush();
				outgoingSocket.close();
				incomingSocket = serverSocketTest.accept();
				StringBuilder receivedContent = new StringBuilder();
				InputStream inStream = incomingSocket.getInputStream();
				InputStream buffInStream = new BufferedInputStream(inStream);
				BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(
						buffInStream));
				char[] incomingContent = new char[1024 * 4];
				int readChars = 0;
				while ((readChars = inputBuffer.read(incomingContent)) != -1) {
					receivedContent.append(incomingContent, 0, readChars);
				}
				inputBuffer.close();
				buffInStream.close();
				inStream.close();
				incomingSocket.close();
				return UrlContent.convertFromMessage(receivedContent.toString());
			} catch (IOException e) {
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}

				try {
					if (incomingSocket != null) {
						incomingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return null;
	}

	/** Send a PUT CONTENT request to the storage system
	 * 
	 * @param toPut UrlContent object to be stored in the database
	 * @return True if the object was sent correctly; else, returns False if there
	 *         was a port or connection error (True return statement makes no
	 *         guarantee about the consistency of the storage system after the
	 *         message was successfully transmitted) */

	public boolean sendPutContent(UrlContent toPut) {
		for (InetAddress gateway : dbGateways) {
			ServerSocket serverSocketTest = null;
			int portNo = StorageHelper.RECEIVE_DATA_PORT;
			try {

				serverSocketTest = new ServerSocket(0);
				serverSocketTest.setSoTimeout(SOCK_TIMEOUT);
				portNo = serverSocketTest.getLocalPort();
			} catch (IOException e) {
				continue;
			}
			Socket outgoingSocket = null;
			Socket incomingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				String toPutStr = toPut.convertToMessage();
				OutputStream outStream = outgoingSocket.getOutputStream();
				OutputStream buffOutStream = new BufferedOutputStream(outStream);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(buffOutStream));
				outputBuffer.write(StorageHelper.makePutContentMessage(toPut.getPKey(),
						toPutStr.length(), portNo));
				outputBuffer.flush();
				outputBuffer.close();
				buffOutStream.close();
				outStream.close();
				outgoingSocket.close();
				incomingSocket = serverSocketTest.accept();
				OutputStream sendStream = incomingSocket.getOutputStream();
				OutputStream buffSendStream = new BufferedOutputStream(sendStream);
				BufferedWriter sendingBuffer = new BufferedWriter(
						new OutputStreamWriter(buffSendStream));
				sendingBuffer.write(toPutStr);
				sendingBuffer.flush();
				sendingBuffer.close();
				buffSendStream.close();
				sendStream.close();
				incomingSocket.close();
				return true;
			} catch (IOException e) {
				System.err
						.println("PUTCONTENT: Unable to open incoming and outgoing ports. Retrying on another gateway.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}

				try {
					if (incomingSocket != null) {
						incomingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return true;
	}

	/** Send a CONTAINS CONTENT request to the storage system
	 * 
	 * @param docHash SHA-256 hash of the document to check if it currently exists
	 *          in the database
	 * @return True if the message was sent correctly and the storage system was
	 *         able to locate the requested document; else, returns False */
	public boolean sendContainsContent(String docHash) {
		for (InetAddress gateway : dbGateways) {
			ServerSocket serverSocketTest = null;
			int portNo = StorageHelper.RECEIVE_DATA_PORT;
			try {

				serverSocketTest = new ServerSocket(0);
				serverSocketTest.setSoTimeout(SOCK_TIMEOUT);
				portNo = serverSocketTest.getLocalPort();
			} catch (IOException e) {
				continue;
			}
			Socket outgoingSocket = null;
			Socket incomingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makeContainsContentMessage(docHash,
						portNo));
				outputBuffer.flush();
				outgoingSocket.close();
				incomingSocket = serverSocketTest.accept();
				BufferedReader inBuffer = new BufferedReader(new InputStreamReader(
						incomingSocket.getInputStream()));
				char[] boolStr = new char[BYTES_IN_BOOL_STR];
				inBuffer.read(boolStr);
				String contains = new String(boolStr);
				incomingSocket.close();
				serverSocketTest.close();
				if (contains.equalsIgnoreCase("True"))
					return true;
				else
					return false;
			} catch (IOException e) {
				System.err
						.println("CONTAINSCONTENT: Unable to open incoming and outgoing ports. Retrying on another gateway.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}

				try {
					if (incomingSocket != null) {
						incomingSocket.close();
					}
				} catch (IOException e1) {}
				try {
					serverSocketTest.close();
				} catch (IOException e1) {}
				continue;
			}
		}
		return false;
	}

	public boolean sendUpdateContentCrawlTime(String docHash, long timestamp) {
		for (InetAddress gateway : dbGateways) {
			Socket outgoingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makeUpdateContentCrawlTimeMessage(
						docHash, timestamp));
				outputBuffer.flush();
				outgoingSocket.close();
				return true;
			} catch (IOException e) {
				System.err
						.println("UPDATECRAWLTIME: Unable to open incoming and outgoing ports. Retrying on another gateway.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return false;
	}

	/** Send a GET HASH request to the storage system
	 * 
	 * @param URL of the document content hash to be retrieved
	 * @return Document content hash of the URL requested if it exists in the
	 *         database; else, returns null if object with requested key does not
	 *         exist or if there was an error transmitting the message */
	public String sendGetHashForUrl(String url) {
		for (InetAddress gateway : dbGateways) {
			ServerSocket serverSocketTest = null;
			int portNo = StorageHelper.RECEIVE_DATA_PORT;
			try {
				serverSocketTest = new ServerSocket(0);
				serverSocketTest.setSoTimeout(SOCK_TIMEOUT);
				portNo = serverSocketTest.getLocalPort();
			} catch (IOException e) {
				continue;
			}
			Socket outgoingSocket = null;
			Socket incomingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer
						.write(StorageHelper.makeGetHashFromUrlMessage(url, portNo));
				outputBuffer.flush();
				outgoingSocket.close();
				incomingSocket = serverSocketTest.accept();
				byte[] hashLengthBytes = new byte[BYTES_IN_LONG];
				incomingSocket.getInputStream().read(hashLengthBytes, 0, BYTES_IN_LONG);
				int hashLength = (int) StorageHelper
						.convertBytesToLong(hashLengthBytes);
				byte[] incomingHash = new byte[hashLength];
				incomingSocket.getInputStream().read(incomingHash);
				incomingSocket.close();
				String msgReceived = new String(incomingHash);
				if (msgReceived.equalsIgnoreCase("null"))
					return null;
				return msgReceived;
			} catch (IOException e) {
				System.err
						.println("GETHASHFORURL: Unable to open incoming and outgoing ports. Retrying on another gateway.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}
				try {
					if (incomingSocket != null) {
						incomingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return null;
	}

	/** Send a PUT URL TO HASH request to the storage system
	 * 
	 * @param toPut UrlContent object to be stored in the database
	 * @return True if the object was sent correctly; else, returns False if there
	 *         was a port or connection error (True return statement makes no
	 *         guarantee about the consistency of the storage system after the
	 *         message was successfully transmitted) */

	public boolean sendPutUrlToHash(String url, String docHash) {
		for (InetAddress gateway : dbGateways) {
			Socket outgoingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makePutUrlToHashMessage(url, docHash));
				outputBuffer.flush();
				outgoingSocket.close();
				return true;
			} catch (IOException e) {
				System.err
						.println("SENDPUTURLTOHASH: Unable to open incoming and outgoing ports. Retrying on another gateway.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return false;
	}

	/** Send a GET RIEntry request to the storage system
	 * 
	 * @word of the document content to be retrieved
	 * @return RIEntry object with the primary key requested if it exists in the
	 *         database; else, returns null if object with requested key does not
	 *         exist or if there was an error transmitting the message */
	public RIEntry sendGetRIEntry(String word) {
		System.out.println("in sendGetRIEntry");
		for (InetAddress gateway : dbGateways) {
			ServerSocket serverSocketTest = null;
			int portNo = StorageHelper.RECEIVE_DATA_PORT;
			try {
				serverSocketTest = new ServerSocket(0);
				serverSocketTest.setSoTimeout(SOCK_TIMEOUT);
				portNo = serverSocketTest.getLocalPort();
			} catch (IOException e) {
				continue;
			}
			Socket outgoingSocket = null;
			Socket incomingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makeGetRIEntryMessage(word, portNo));
				outputBuffer.flush();
				outgoingSocket.close();
				incomingSocket = serverSocketTest.accept();
				byte[] contentLengthBytes = new byte[BYTES_IN_LONG];
				incomingSocket.getInputStream().read(contentLengthBytes, 0,
						BYTES_IN_LONG);
				int contentLength = (int) StorageHelper
						.convertBytesToLong(contentLengthBytes);
				if (contentLength != 0) {
					byte[] incomingContent = new byte[contentLength];
					incomingSocket.getInputStream().read(incomingContent);
					incomingSocket.close();
					String msgReceived = new String(incomingContent);
					return RIEntry.convertFromMessage(msgReceived);
				} else {
					incomingSocket.close();
					return null;
				}
			} catch (IOException e) {
				System.err
						.println("Unable to open incoming and outgoing ports. Retrying on another gateway.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}

				try {
					if (incomingSocket != null) {
						incomingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return null;
	}

	/** Send a PUT RIEntry request to the storage system
	 * 
	 * @param toPut RIEntry object to be stored in the database
	 * @return True if the object was sent correctly; else, returns False if there
	 *         was a port or connection error (True return statement makes no
	 *         guarantee about the consistency of the storage system after the
	 *         message was successfully transmitted) */
	public boolean sendPutRIEntry(RIEntry toPut) {
		for (InetAddress gateway : dbGateways) {
			Socket outgoingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				String toPutStr = toPut.convertToMessage();
				OutputStream outStream = outgoingSocket.getOutputStream();
				OutputStream buffOutStream = new BufferedOutputStream(outStream);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(buffOutStream));
				outputBuffer.write(StorageHelper.makePutRIEntryMessage(toPut.getPKey(),
						toPutStr.length(), toPut.getNdf()));
				outputBuffer.flush();
				outputBuffer.close();
				buffOutStream.close();
				outStream.close();
				outgoingSocket.close();
				return true;
			} catch (IOException e) {
				System.err
						.println("PUTRIEntry: Unable to open incoming and outgoing ports. Retrying on another gateway.");
				e.printStackTrace();
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return false;
	}

	public HashSet<String> sendGetAllDocHash() {
		HashSet<String> result = new HashSet<String>();
		for (InetAddress gateway : dbGateways) {
			ServerSocket serverSocketTest = null;
			int portNo = StorageHelper.RECEIVE_DATA_PORT;
			try {
				serverSocketTest = new ServerSocket(0);
				portNo = serverSocketTest.getLocalPort();
			} catch (IOException e) {
				continue;
			}
			Socket outgoingSocket = null;
			Socket incomingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makeGetAllDocHashMessage(portNo));
				outputBuffer.flush();
				outgoingSocket.close();
				incomingSocket = serverSocketTest.accept();
				StringBuilder receivedContent = new StringBuilder();
				InputStream inStream = incomingSocket.getInputStream();
				InputStream buffInStream = new BufferedInputStream(inStream);
				BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(
						buffInStream));
				char[] incomingContent = new char[1024 * 4];
				int readChars = 0;
				while ((readChars = inputBuffer.read(incomingContent)) != -1) {
					receivedContent.append(incomingContent, 0, readChars);
				}
				inputBuffer.close();
				buffInStream.close();
				inStream.close();
				incomingSocket.close();
				for (String hash : new ArrayList<String>(Arrays.asList(new String(
						incomingContent).split(" ")))) {
					result.add(hash);
				}
			} catch (IOException e) {
				System.err
						.println("Unable to open incoming and outgoing ports. Moving on to next gateway.");
				e.printStackTrace();
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}

				try {
					if (incomingSocket != null) {
						incomingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return result;
	}

	/** Send a PUT PAGERANKSCORE request to the storage system
	 * 
	 * @param URL of the page to PUT the pagerank score for
	 * @param Page Rank of the URL in question to store
	 * @return True if the object was sent correctly; else, returns False if there
	 *         was a port or connection error (True return statement makes no
	 *         guarantee about the consistency of the storage system after the
	 *         message was successfully transmitted) */
	public boolean sendPutPageRankScore(String url, double newPageRank) {
		for (InetAddress gateway : dbGateways) {
			Socket outgoingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makePutPageRankScoreMessage(url,
						newPageRank));
				outputBuffer.flush();
				outgoingSocket.close();
				return true;
			} catch (IOException e) {
				System.err
						.println("Unable to open incoming and outgoing ports. Request not sent.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return false;
	}

	/** Send a PUT PRFEEDBACKSCORE request to the storage system
	 * 
	 * @param URL of the page to PUT the feedback score for
	 * @param Feedback of the URL in question to store
	 * @return True if the object was sent correctly; else, returns False if there
	 *         was a port or connection error (True return statement makes no
	 *         guarantee about the consistency of the storage system after the
	 *         message was successfully transmitted) */
	public boolean sendPRFeedbackScore(String url, double newPRFeedBackScore) {
		for (InetAddress gateway : dbGateways) {
			Socket outgoingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makePutPRFeedBackScoreMessage(url,
						newPRFeedBackScore));
				outputBuffer.flush();
				outgoingSocket.close();
				return true;
			} catch (IOException e) {
				System.err
						.println("Unable to open incoming and outgoing ports. Request not sent.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return false;
	}

	/** Send an GET PAGERANKSCORE request to the storage system
	 * 
	 * @param URL of the page to GET the pagerank score
	 * @return True if the object was sent correctly; else, returns False if there
	 *         was a port or connection error (True return statement makes no
	 *         guarantee about the consistency of the storage system after the
	 *         message was successfully transmitted) */
	public double[] sendGetPageRankScore(String url) {
		for (InetAddress gateway : dbGateways) {
			ServerSocket serverSocketTest = null;
			int portNo = StorageHelper.RECEIVE_DATA_PORT;
			try {

				serverSocketTest = new ServerSocket(0);
				serverSocketTest.setSoTimeout(SOCK_TIMEOUT);
				portNo = serverSocketTest.getLocalPort();
			} catch (IOException e) {
				continue;
			}
			Socket outgoingSocket = null;
			Socket incomingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makeGetPageRankScoreMessage(url,
						portNo));
				outputBuffer.flush();
				outgoingSocket.close();
				incomingSocket = serverSocketTest.accept();

				StringBuilder receivedContent = new StringBuilder();
				InputStream inStream = incomingSocket.getInputStream();
				InputStream buffInStream = new BufferedInputStream(inStream);
				BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(
						buffInStream));
				char[] incomingContent = new char[1024 * 4];
				int readChars = 0;
				while ((readChars = inputBuffer.read(incomingContent)) != -1) {
					receivedContent.append(incomingContent, 0, readChars);
				}
				inputBuffer.close();
				buffInStream.close();
				inStream.close();
				incomingSocket.close();
				double[] scores = new double[2];

				if (receivedContent.toString().contains("#&&#")) {
					String[] receivedScores = receivedContent.toString().split("#&&#");
					scores[0] = Double.parseDouble(receivedScores[0]);
					scores[1] = Double.parseDouble(receivedScores[1]);
				} else {
					scores[0] = 0.0000;
					scores[1] = 0.0000;
				}
				System.out.println("GET PAGE RANK response: " + scores);
				return scores;
			} catch (IOException e) {
				System.err
						.println("GET PAGE RANKCONTAINSCONTENT: Unable to open incoming and outgoing ports. Request not sent.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}

				try {
					if (incomingSocket != null) {
						incomingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return null;
	}

	public boolean sendAddRIInfo(String word, String url, RIInfo toAdd) {
		for (InetAddress gateway : dbGateways) {
			ServerSocket serverSocketTest = null;
			int portNo = StorageHelper.RECEIVE_DATA_PORT;
			try {

				serverSocketTest = new ServerSocket(0);
				serverSocketTest.setSoTimeout(SOCK_TIMEOUT);
				portNo = serverSocketTest.getLocalPort();
			} catch (IOException e) {
				continue;
			}
			Socket outgoingSocket = null;
			Socket incomingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				String toAddStr = toAdd.convertToMessage();
				OutputStream outStream = outgoingSocket.getOutputStream();
				OutputStream buffOutStream = new BufferedOutputStream(outStream);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(buffOutStream));
				outputBuffer.write(StorageHelper.makeAddRIInfoMessage(word, url,
						toAddStr.length(), portNo));
				outputBuffer.flush();
				outputBuffer.close();
				buffOutStream.close();
				outStream.close();
				outgoingSocket.close();
				incomingSocket = serverSocketTest.accept();
				OutputStream sendStream = incomingSocket.getOutputStream();
				OutputStream buffSendStream = new BufferedOutputStream(sendStream);
				BufferedWriter sendingBuffer = new BufferedWriter(
						new OutputStreamWriter(buffSendStream));
				sendingBuffer.write(toAddStr);
				sendingBuffer.flush();
				sendingBuffer.close();
				buffSendStream.close();
				sendStream.close();
				incomingSocket.close();
				return true;
			} catch (IOException e) {
				System.err
						.println("SENDRIINFO: Unable to open incoming and outgoing ports. Retrying on another gateway.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}

				try {
					if (incomingSocket != null) {
						incomingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return true;
	}

	public boolean sendDelAllRIEntries() {
		boolean result = true;
		for (InetAddress gateway : dbGateways) {
			ServerSocket serverSocketTest = null;
			int portNo = StorageHelper.RECEIVE_DATA_PORT;
			try {

				serverSocketTest = new ServerSocket(0);
				serverSocketTest.setSoTimeout(SOCK_TIMEOUT);
				portNo = serverSocketTest.getLocalPort();
			} catch (IOException e) {
				continue;
			}
			Socket outgoingSocket = null;
			Socket incomingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makeDeleteAllRIEntriesMessage(portNo));
				outputBuffer.flush();
				outgoingSocket.close();

				incomingSocket = serverSocketTest.accept();

				// can put this into a different method
				StringBuilder receivedContent = new StringBuilder();
				InputStream inStream = incomingSocket.getInputStream();
				InputStream buffInStream = new BufferedInputStream(inStream);
				BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(
						buffInStream));
				char[] incomingContent = new char[1024 * 4];
				int readChars = 0;
				while ((readChars = inputBuffer.read(incomingContent)) != -1) {
					receivedContent.append(incomingContent, 0, readChars);
				}
				inputBuffer.close();
				buffInStream.close();
				inStream.close();
				incomingSocket.close();
				// end other method

				if (!(new String(incomingContent).equals("true"))) {
					result = false;
				}
			} catch (IOException e) {
				System.err
						.println("Unable to open incoming and outgoing ports. Moving on to next gateway.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}

				try {
					if (incomingSocket != null) {
						incomingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return result;
	}

	public HashSet<String> sendGetUrlsFromHash(String docHash) {
		HashSet<String> result = new HashSet<String>();
		for (InetAddress gateway : dbGateways) {
			ServerSocket serverSocketTest = null;
			int portNo = StorageHelper.RECEIVE_DATA_PORT;
			try {
				serverSocketTest = new ServerSocket(0);
				serverSocketTest.setSoTimeout(SOCK_TIMEOUT);
				portNo = serverSocketTest.getLocalPort();
			} catch (IOException e) {
				continue;
			}
			Socket incomingSocket = null;
			Socket outgoingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makeGetUrlsFromHashMessage(docHash,
						portNo));
				outputBuffer.flush();
				outgoingSocket.close();
				incomingSocket = serverSocketTest.accept();
				StringBuilder receivedContent = new StringBuilder();
				InputStream inStream = incomingSocket.getInputStream();
				InputStream buffInStream = new BufferedInputStream(inStream);
				BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(
						buffInStream));
				char[] incomingContent = new char[1024 * 4];
				int readChars = 0;
				while ((readChars = inputBuffer.read(incomingContent)) != -1) {
					receivedContent.append(incomingContent, 0, readChars);
				}
				inputBuffer.close();
				buffInStream.close();
				inStream.close();
				incomingSocket.close();
				for (String url : new ArrayList<String>(Arrays.asList(new String(
						incomingContent).split("\t")))) {
					result.add(url);
				}
			} catch (IOException e) {
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}

				try {
					if (incomingSocket != null) {
						incomingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return result;
	}

	public boolean sendPutIndexerScore(String word, int toPut, String url) {
		for (InetAddress gateway : dbGateways) {
			Socket outgoingSocket = null;
			try {
				outgoingSocket = new Socket(gateway, StorageHelper.SEND_MESSAGE_PORT);
				outgoingSocket.setSoTimeout(SOCK_TIMEOUT);
				BufferedWriter outputBuffer = new BufferedWriter(
						new OutputStreamWriter(outgoingSocket.getOutputStream()));
				outputBuffer.write(StorageHelper.makePutIndexerFeedBackScoreMessage(
						word, url, toPut));
				outputBuffer.flush();
				outgoingSocket.close();
				return true;
			} catch (IOException e) {
				System.err
						.println("Unable to open incoming and outgoing ports. Request not sent.");
				try {
					if (outgoingSocket != null) {
						outgoingSocket.close();
					}
				} catch (IOException e1) {}
				continue;
			}
		}
		return false;
	}

}
