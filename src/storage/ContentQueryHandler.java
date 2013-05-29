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
import java.net.InetSocketAddress;
import java.net.Socket;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityIndex;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.NodeHandleSet;
import rice.p2p.commonapi.RouteMessage;

public class ContentQueryHandler implements Application {

	protected FixedIdNodeFactory nodeFactory = null;
	private Node node = null;
	private Endpoint endpoint = null;
	private BDBWrapper bdb = null;
	private PrimaryIndex<String, UrlContent> contentIndex = null;
	private PrimaryIndex<String, RIEntry> riEntryIndex = null;
	private PrimaryIndex<String, UrlLinks> urlLinksIndex = null;
	private PrimaryIndex<String, UrlToDocHash> urlToHashIndex = null;
	private SecondaryIndex<String, String, UrlToDocHash> hashToUrlIndex = null;
	private long numPages = 0;
	private boolean dbInitialized = false;

	public ContentQueryHandler(String dbLocation, int pastryport, int numNodes,
			int nodeIndex) {
		nodeFactory = new FixedIdNodeFactory(pastryport, numNodes);
		node = nodeFactory.getNode(nodeIndex);
		if (node != null) {
			System.out.println("Globus Distributed Storage node initializing....");
			System.out.println("This node ID is: " + node.getId().toString());
		}
		endpoint = node.buildEndpoint(this, "ContentDaemon");
		endpoint.register();
		try {
			bdb = new BDBWrapper();
			boolean everythingAllRight = false;
			if (!bdb.setupDB(dbLocation)) {
				dbInitialized = false;
			} else {
				everythingAllRight = true;
				if (bdb.setupStore(storage.BDBWrapper.DB_CONTENT_STORE)) {
					contentIndex = bdb.getPrimaryIndex(
							storage.BDBWrapper.DB_CONTENT_STORE, String.class,
							UrlContent.class);
				} else {
					everythingAllRight = false;
				}
				if (bdb.setupStore(storage.BDBWrapper.DB_RIENTRY_STORE)) {
					riEntryIndex = bdb.getPrimaryIndex(
							storage.BDBWrapper.DB_RIENTRY_STORE, String.class, RIEntry.class);
				} else {
					everythingAllRight = false;
				}
				if (bdb.setupStore(storage.BDBWrapper.DB_URLLINKS_STORE)) {
					urlLinksIndex = bdb.getPrimaryIndex(
							storage.BDBWrapper.DB_URLLINKS_STORE, String.class,
							UrlLinks.class);
				} else {
					everythingAllRight = false;
				}
				if (bdb.setupStore(storage.BDBWrapper.DB_URL_TO_HASH_STORE)) {
					urlToHashIndex = bdb.getPrimaryIndex(
							storage.BDBWrapper.DB_URL_TO_HASH_STORE, String.class,
							UrlToDocHash.class);
					hashToUrlIndex = bdb.getSecondaryIndex(
							storage.BDBWrapper.DB_URL_TO_HASH_STORE, urlToHashIndex,
							String.class, "docHash");
				} else {
					everythingAllRight = false;
				}
				dbInitialized = everythingAllRight;
				numPages = contentIndex.count();
				System.out.println("DHT Node capacity at initialization: " + numPages
						+ " pages.");
			}
		} catch (DatabaseException dbe) {
			System.err
					.println("Content Query Handler encountered a problem setting up the database instance");
		}
	}

	public ContentQueryHandler(String dbLocation, int pastryport,
			InetSocketAddress bootstrapNode, int numNodes, int nodeIndex) {
		nodeFactory = new FixedIdNodeFactory(pastryport, bootstrapNode, numNodes);
		node = nodeFactory.getNode(nodeIndex);
		if (node != null) {
			System.out.println("Globus Distributed Storage node initializing....");
			System.out.println("This node ID is: " + node.getId().toString());
		}
		endpoint = node.buildEndpoint(this, "ContentDaemon");
		endpoint.register();
		try {
			bdb = new BDBWrapper();
			if (dbInitialized = bdb.setupDB(dbLocation)) {
				boolean everythingAllRight = true;
				if (bdb.setupStore(storage.BDBWrapper.DB_CONTENT_STORE)) {
					contentIndex = bdb.getPrimaryIndex(
							storage.BDBWrapper.DB_CONTENT_STORE, String.class,
							UrlContent.class);
				} else {
					everythingAllRight = false;
				}
				if (bdb.setupStore(storage.BDBWrapper.DB_RIENTRY_STORE)) {
					riEntryIndex = bdb.getPrimaryIndex(
							storage.BDBWrapper.DB_RIENTRY_STORE, String.class, RIEntry.class);
				} else {
					everythingAllRight = false;
				}
				if (bdb.setupStore(storage.BDBWrapper.DB_URLLINKS_STORE)) {
					urlLinksIndex = bdb.getPrimaryIndex(
							storage.BDBWrapper.DB_URLLINKS_STORE, String.class,
							UrlLinks.class);
				} else {
					everythingAllRight = false;
				}
				if (bdb.setupStore(storage.BDBWrapper.DB_URL_TO_HASH_STORE)) {
					urlToHashIndex = bdb.getPrimaryIndex(
							storage.BDBWrapper.DB_URL_TO_HASH_STORE, String.class,
							UrlToDocHash.class);
					hashToUrlIndex = bdb.getSecondaryIndex(
							storage.BDBWrapper.DB_URL_TO_HASH_STORE, urlToHashIndex,
							String.class, "docHash");
				} else {
					everythingAllRight = false;
				}
				numPages = contentIndex.count();
				System.out.println("DHT Node capacity at initialization: " + numPages
						+ " pages.");
				dbInitialized = everythingAllRight;
			}
		} catch (DatabaseException dbe) {
			System.err
					.println("Content Query Handler encountered a problem setting up the database instance");
		}
		if (dbInitialized) {
			System.out.println("Inverted Index contains: " + riEntryIndex.count()
					+ " records.");
		}
	}

	public boolean dbIsInitialized() {
		return dbInitialized;
	}

	@Override
	public void deliver(Id id, Message msg) {
		DbMessage incoming = (DbMessage) msg;

		String message = incoming.getContent();
		int msgType = StorageHelper.parseMessageType(message);

		if (incoming.isReplica()) {
			if (msgType == StorageHelper.PUTCONTENT && contentIndex != null) {
				contentIndex.put(incoming.getPutReplica());
				return;
			}
		}

		String key = StorageHelper.parseKey(message,
				StorageHelper.parseMessageType(message));

		if (msgType != StorageHelper.PUTPAGERANKSCORE
				&& msgType != StorageHelper.PUTURLTOHASH
				&& msgType != StorageHelper.CONTAINSCONTENT
				&& msgType != StorageHelper.PUTCONTENT
				&& msgType != StorageHelper.GETCONTENT
				&& msgType != StorageHelper.GETURLSFROMHASH
				&& msgType != StorageHelper.PUTINDEXERFEEDBACKSCORE
				&& msgType != StorageHelper.PUTRIENTRY
				&& msgType != StorageHelper.GETHASHFROMURL) {
			System.out.println("Received " + incoming.getContent()
					+ " from gateway ID " + incoming.getFrom().getId().toString());
		}
		if (message != null && msgType != StorageHelper.UNKNOWN
				&& (key != null || !StorageHelper.shouldHaveKey(msgType))) {
			if (dbInitialized && incoming.getClientAddress() != null) {
				Socket responseSock = null;
				OutputStream sendBuffer = null;
				InputStream receiveBuffer = null;
				if (StorageHelper.needsOutgoingSocket(msgType)) {
					try {
						responseSock = new Socket(incoming.getClientAddress(),
								StorageHelper.parsePortNo(message));
						sendBuffer = responseSock.getOutputStream();
						receiveBuffer = responseSock.getInputStream();
					} catch (IOException e) {
						e.printStackTrace();
						System.err
								.println(msgType
										+ ": Error connecting to remote client socket to send response");
						try {
							if (responseSock != null) {
								responseSock.close();
							}
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						return;
					}
				}

				if (msgType == StorageHelper.GETCONTENT) {
					key = key.trim();
					if (contentIndex.contains(key)) {
						try {
							String outgoingMsg = contentIndex.get(key).convertToMessage();
							OutputStream buffOutStream = new BufferedOutputStream(sendBuffer);
							BufferedWriter outputBuffer = new BufferedWriter(
									new OutputStreamWriter(buffOutStream));
							outputBuffer.write(outgoingMsg);
							outputBuffer.flush();
							outputBuffer.close();
							buffOutStream.close();
							sendBuffer.close();
							responseSock.close();
						} catch (DatabaseException e) {
							System.err.println("Error retrieving content from datastore");
						} catch (IOException e) {
							System.err
									.println("Error sending retrieving content from datastore to client");
						}
					} else {
						try {
							sendBuffer.close();
							responseSock.close();
						} catch (IOException e) {}
					}
				} else if (msgType == StorageHelper.PUTCONTENT) {
					int contentLength = StorageHelper.parseContentLength(message);
					if (contentLength < 0) {
						try {
							if (responseSock != null) {
								responseSock.close();
							}
						} catch (IOException e) {
							System.err.println("Error closing socket to remote client");
							return;
						}
						return;
					}
					StringBuilder receivedContent = new StringBuilder();
					try {
						InputStream buffInStream = new BufferedInputStream(receiveBuffer);
						BufferedReader inputBuffer = new BufferedReader(
								new InputStreamReader(buffInStream));
						char[] incomingContent = new char[1024 * 4];
						int readChars = 0;
						while ((readChars = inputBuffer.read(incomingContent)) != -1) {
							receivedContent.append(incomingContent, 0, readChars);
						}
						inputBuffer.close();
						buffInStream.close();
						receiveBuffer.close();
						responseSock.close();
					} catch (IOException e) {
						System.err.println("Error receiving content from client");
					}
					try {
						UrlContent incomingObject = UrlContent
								.convertFromMessage(receivedContent.toString().trim());
						if (incomingObject != null) {
							contentIndex.put(incomingObject);
							numPages++;
							if (numPages % 100 == 0) {
								System.err.println("\n---STATUS UPDATE---");
								System.err.println("DHT Node currently holds: " + numPages
										+ " pages.");
								System.err.println("-------END-------\n");
							}

							NodeHandleSet replicaDestinations = endpoint.replicaSet(
									nodeFactory.getIdFromString(incoming.getContent()), 2);
							DbMessage replicaMsg = incoming;
							replicaMsg.setReplica(true);
							replicaMsg.setPutReplica(incomingObject);
							for (int i = 0; i < replicaDestinations.size(); i++) {
								NodeHandle nodeDest = replicaDestinations.getHandle(i);
								synchronized (endpoint) {
									endpoint.route(null, incoming, nodeDest);
								}
							}

						}
					} catch (DatabaseException e) {
						System.err.println("Error storing content to datastore");
					}
				} else if (msgType == StorageHelper.CONTAINSCONTENT) {
					try {
						if (contentIndex.contains(key)) {
							sendBuffer.write("True".getBytes());
						} else {
							sendBuffer.write("False".getBytes());
						}
						sendBuffer.close();
						if (responseSock != null) {
							responseSock.close();
						}
					} catch (IOException e) {
						System.err.println("Error closing socket to remote client");
						return;
					}
				} else if (msgType == StorageHelper.UPDATECRAWLTIME) {
					Long crawlTime = StorageHelper.parseNewCrawlTime(message);
					System.err.println("Updating crawltime for " + key + " to "
							+ crawlTime);
					if (crawlTime == null || contentIndex == null)
						return;
					UrlContent toUpdate = contentIndex.get(key);
					toUpdate.setCrawlTime(crawlTime);
					contentIndex.put(toUpdate);
				}
				// Indexer Methods
				else if (msgType == StorageHelper.GETRIENTRY) {
					try {
						if (riEntryIndex.contains(key)) {
							RIEntry toSend = riEntryIndex.get(key);
							byte[] toSendByteArray = toSend.convertToMessage().getBytes();
							sendBuffer.write(StorageHelper
									.convertLongToBytes(toSendByteArray.length));
							sendBuffer.flush();
							sendBuffer.write(toSendByteArray);
							sendBuffer.flush();
						}
					} catch (DatabaseException e) {
						System.err.println("Error retrieving content from datastore");

					} catch (IOException e) {
						System.err
								.println("Error sending retrieving content from datastore to client");
					} finally {
						try {
							if (responseSock != null) {
								responseSock.close();
							}
						} catch (IOException e) {
							System.err.println("Error closing socket to remote client");
							return;
						}
					}
				} else if (msgType == StorageHelper.PUTRIENTRY) {
					Double ndf = StorageHelper.parseNdf(message);
					if (ndf == -1)
						return;
					RIEntry incomingObject = new RIEntry(key);
					incomingObject.setNdf(ndf);
					if (riEntryIndex != null) {
						riEntryIndex.put(incomingObject);
					}
				}
				// Ranker Methods
				else if (msgType == StorageHelper.PUTPAGERANKSCORE) {
					Double pagerankscore = StorageHelper.parseNewPageRank(message);
					if (pagerankscore == null || urlLinksIndex == null)
						return;
					UrlLinks toUpdate;
					if (urlLinksIndex.contains(key)) {
						toUpdate = urlLinksIndex.get(key);
					} else {
						toUpdate = new UrlLinks(key);
					}
					toUpdate.setPagerankscore(pagerankscore);
					urlLinksIndex.put(toUpdate);
				} else if (msgType == StorageHelper.PUTPRFEEDBACKSCORE) {
					Double prfeedbackscore = StorageHelper
							.parseNewPRFeedbackScore(message);
					if (prfeedbackscore == null || urlLinksIndex == null)
						return;
					UrlLinks toUpdate;
					if (urlLinksIndex.contains(key)) {
						toUpdate = urlLinksIndex.get(key);
						toUpdate.setFeedbackscore(toUpdate.getFeedbackscore()
								+ prfeedbackscore);
						urlLinksIndex.put(toUpdate);
					}
				} else if (msgType == StorageHelper.GETPAGERANKSCORE) {
					if (urlLinksIndex.contains(key)) {
						try {
							String delim = "#&&#";
							UrlLinks urllinks = urlLinksIndex.get(key);
							String outgoingMsg = String.valueOf(urllinks.getPagerankscore())
									+ delim + String.valueOf(urllinks.getFeedbackscore());
							OutputStream buffOutStream = new BufferedOutputStream(sendBuffer);
							BufferedWriter outputBuffer = new BufferedWriter(
									new OutputStreamWriter(buffOutStream));
							outputBuffer.write(outgoingMsg);
							outputBuffer.flush();
							outputBuffer.close();
							buffOutStream.close();
							sendBuffer.close();
							responseSock.close();
						} catch (DatabaseException e) {
							System.err.println("Error retrieving content from datastore");
						} catch (IOException e) {
							System.err
									.println("Error sending retrieving content from datastore to client");
						}
					} else {
						try {
							sendBuffer.write(StorageHelper.convertDoubleToBytes(0));
							responseSock.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else if (msgType == StorageHelper.UPDATECRAWLTIME) {
					Long crawlTime = StorageHelper.parseNewCrawlTime(message);
					System.err.println("Updating crawltime for " + key + " to "
							+ crawlTime);
					if (crawlTime == null || contentIndex == null)
						return;
					UrlContent toUpdate = contentIndex.get(key);
					toUpdate.setCrawlTime(crawlTime);
					contentIndex.put(toUpdate);
				} else if (msgType == StorageHelper.GETHASHFROMURL) {
					try {
						if (urlToHashIndex.contains(key)) {
							String outgoingHash = urlToHashIndex.get(key).getDocHash();
							sendBuffer.write(StorageHelper.convertLongToBytes(outgoingHash
									.getBytes().length));

							sendBuffer.flush();
							sendBuffer.write(outgoingHash.getBytes());
						} else {
							String nullString = "NULL";
							sendBuffer.write(StorageHelper.convertLongToBytes(nullString
									.getBytes().length));
							sendBuffer.flush();
							sendBuffer.write(nullString.getBytes());
						}
						sendBuffer.flush();
					} catch (DatabaseException e) {
						System.err.println("Error retrieving content from datastore");
					} catch (IOException e) {
						System.err
								.println("Error sending retrieving content from datastore to client");
					} finally {
						try {
							if (responseSock != null) {
								responseSock.close();
							}
						} catch (IOException e) {
							System.err.println("Error closing socket to remote client");
							return;
						}
					}
				} else if (msgType == StorageHelper.PUTURLTOHASH) {
					String hash = StorageHelper.parseUrlToHashValue(message);
					if (hash == null || urlToHashIndex == null)
						return;
					UrlToDocHash url2doc = new UrlToDocHash();
					url2doc.setPKey(key);
					url2doc.setDocHash(hash);
					urlToHashIndex.put(url2doc);
					try {
						if (responseSock != null) {
							responseSock.close();
						}
					} catch (IOException e) {
						System.err.println("Error closing socket to remote client");
						return;
					}
				} else if (msgType == StorageHelper.ADDRIINFO) {
					int contentLength = StorageHelper.parseContentLength(message);
					String url;
					if (contentLength < 0 || message.split(" ").length < 4) {
						try {
							if (responseSock != null) {
								responseSock.close();
							}
						} catch (IOException e) {
							System.err.println("Error closing socket to remote client");
							return;
						}
						return;
					} else {
						url = message.split(" ", 5)[4];
					}
					StringBuilder receivedContent = new StringBuilder();
					try {
						InputStream buffInStream = new BufferedInputStream(receiveBuffer);
						BufferedReader inputBuffer = new BufferedReader(
								new InputStreamReader(buffInStream));
						char[] incomingContent = new char[1024 * 4];
						int readChars = 0;
						while ((readChars = inputBuffer.read(incomingContent)) != -1) {
							receivedContent.append(incomingContent, 0, readChars);
						}
						inputBuffer.close();
						buffInStream.close();
						receiveBuffer.close();
						responseSock.close();
					} catch (IOException e) {
						System.err.println("Error receiving content from client");
					}
					try {
						RIInfo incomingObject = RIInfo.convertFromMessage(receivedContent
								.toString().trim());
						if (incomingObject != null) {
							if (riEntryIndex.contains(key)) {

								RIEntry toUpdate = riEntryIndex.get(key);
								toUpdate.putHit(url, incomingObject);
								riEntryIndex.put(toUpdate);
							}
						}
					} catch (DatabaseException e) {
						System.err.println("Error storing content to datastore");
					}
				} else if (msgType == StorageHelper.PUTINDEXERFEEDBACKSCORE) {
					String[] split = message.split(" ", 4);
					String word = split[1];
					double scoreChange = Double.valueOf(split[2]);
					String url = split[3];
					if (riEntryIndex.contains(word)) {
						RIEntry toUpdate = riEntryIndex.get(word);
						RIInfo riInfo = toUpdate.getDocRIInfoMap().get(url);
						if (riInfo != null) {
							riInfo
									.setFeedbackWeight(riInfo.getFeedbackWeight() + scoreChange);
						}
					}
				}

				else if (msgType == StorageHelper.GETURLSFROMHASH) {
					key = key.trim();
					if (hashToUrlIndex.contains(key)) {
						String outgoingMsg = "";
						try {
							EntityIndex<String, UrlToDocHash> subIndex = hashToUrlIndex
									.subIndex(key);
							EntityCursor<UrlToDocHash> cursor = subIndex.entities();
							try {
								for (UrlToDocHash entity : cursor) {
									if (outgoingMsg.length() != 0) {
										outgoingMsg += "\t";
									}
									outgoingMsg += entity.getPKey();
								}
							} finally {
								cursor.close();
							}
							OutputStream buffOutStream = new BufferedOutputStream(sendBuffer);
							BufferedWriter outputBuffer = new BufferedWriter(
									new OutputStreamWriter(buffOutStream));
							outputBuffer.write(outgoingMsg);
							outputBuffer.flush();
							outputBuffer.close();
							buffOutStream.close();
							sendBuffer.close();
							responseSock.close();
						} catch (DatabaseException e) {
							System.err.println("Error retrieving content from datastore");
						} catch (IOException e) {
							System.err
									.println("Error sending retrieving content from datastore to client");
						}
					} else {
						try {
							sendBuffer.close();
							responseSock.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public void sendMessage(Id destination, String msg, int contentLength,
			boolean isResponse, InetAddress clientAddress) {
		DbMessage outgoing;
		synchronized (node) {
			outgoing = new DbMessage(node.getLocalNodeHandle(), msg, contentLength,
					isResponse, clientAddress);
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

	public boolean deleteAllRIEntries() {
		boolean flag = true;
		for (String key : riEntryIndex.keys()) {
			if (!riEntryIndex.delete(key)) {
				flag = false; // if only one of many items with the same key are
											// deleted, this returns wrong
			}
		}
		return flag;
	}

	public void close() {
		if (dbInitialized) {
			bdb.closeDB();
			dbInitialized = false;
		}
		nodeFactory.shutdownNode(node);
	}
}
