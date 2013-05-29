/**
 * @author Michael Collis (mcollis@seas.upenn.edu)
 */
package storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ContentDaemon {

	private static ServerSocket sock = null;
	private static PrintWriter hashList = null;
	private static File hashFile = null;
	protected static ContentQueryHandler handler = null;
	private static final String ARGS_ERROR = "Error parsing arguments. Please check your input and try again.";
	private static final String USAGE = "Usage: java ContentDaemon pastry-port-no pastry-bootstrap-ip pastry-bootstrap-port total-num-pastry-nodes node-index BDB-path hash-output-file-path";
	@SuppressWarnings("unused")
	private static final int SOCK_TIMEOUT = 3000;

	public static void main(String args[]) {

		if (args.length < 7 || args.length > 7) {
			System.out.println(USAGE);
			return;
		}

		String dbLocation = args[5];
		int localPastryPort, bootstrapPort, numNodes, nodeIndex;
		try {
			localPastryPort = Integer.parseInt(args[0]);
			numNodes = Integer.parseInt(args[3]);
			nodeIndex = Integer.parseInt(args[4]);
			if (nodeIndex < 0 || nodeIndex >= numNodes) {
				System.out.println(ARGS_ERROR);
				System.out.println(USAGE);
				return;
			}
		} catch (NumberFormatException e) {
			System.out.println(ARGS_ERROR);
			System.out.println(USAGE);
			return;
		}

		hashFile = new File(args[6]);
		if (!hashFile.exists()) {
			try {
				hashFile.createNewFile();
			} catch (IOException e) {
				System.err
						.println("Unable to create file to store content hashes. Setup failed.");
				return;
			}
		}
		try {
			hashList = new PrintWriter(new BufferedWriter(new FileWriter(hashFile,
					true)));
		} catch (IOException e) {
			System.err
					.println("Unable to open file to store content hashes. Setup failed.");
			return;
		}

		InetAddress bootstrapIP;
		InetSocketAddress bootstrapAddress;
		try {
			bootstrapIP = InetAddress.getByName(args[1]);
			bootstrapPort = Integer.parseInt(args[2]);
			bootstrapAddress = new InetSocketAddress(bootstrapIP, bootstrapPort);
			if (bootstrapAddress.isUnresolved()) {
				handler = new ContentQueryHandler(dbLocation, localPastryPort,
						numNodes, nodeIndex);
			} else {
				handler = new ContentQueryHandler(dbLocation, localPastryPort,
						bootstrapAddress, numNodes, nodeIndex);
			}
		} catch (IllegalArgumentException e) {
			System.out.println(ARGS_ERROR);
			System.out.println(USAGE);
			return;
		} catch (UnknownHostException e) {
			System.out.println("Unknown bootstrap host, creating new pastry ring");
			handler = new ContentQueryHandler(dbLocation, localPastryPort, numNodes,
					nodeIndex);
		}

		try {
			sock = new ServerSocket(StorageHelper.SEND_MESSAGE_PORT);
		} catch (IOException e) {
			System.err.println("Database server unable to listen on port "
					+ StorageHelper.SEND_MESSAGE_PORT
					+ " for incoming requests. Shutting down.");
			return;
		}
		if (sock == null)
			return;

		System.err
				.println("Content node set up successfully. Ready to accept client requests.\n");

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("\nExecuting Content Storage Node shutdown.");
				hashList.close();
				handler.close();
				try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}));

		while (true) {

			try {
				Socket incoming = sock.accept();
				BufferedReader responseBuffer = new BufferedReader(
						new InputStreamReader(incoming.getInputStream()));

				String message = "";
				while (message.length() == 0) {
					message += responseBuffer.readLine();
				}
				int messageType = StorageHelper.parseMessageType(message);
				String resourceKey = StorageHelper.parseKey(message, messageType);
				if (messageType == StorageHelper.UNKNOWN
						|| (resourceKey == null && StorageHelper.shouldHaveKey(messageType))) {
					System.out.println("Bad message received: " + message);
					// Incoming message is malformed
					incoming.close();
					continue;
				}

				InetAddress remoteAddress = incoming.getInetAddress();
				if (handler.dbIsInitialized()) {
					if (StorageHelper.shouldHaveKey(messageType)) {
						if (messageType == StorageHelper.PUTCONTENT) {
							hashList.println(resourceKey);
						}
						handler
								.sendMessage(handler.nodeFactory.getIdFromString(resourceKey),
										message, StorageHelper.parseContentLength(message), false,
										remoteAddress);
					} else {
						if (messageType == StorageHelper.GETALLDOCHASH) {
							Socket responseSock = null;
							OutputStream sendBuffer = null;
							try {
								int portNo = StorageHelper.parsePortNo(message);
								responseSock = new Socket(remoteAddress, portNo);
								sendBuffer = responseSock.getOutputStream();
								getAllDocHashes(hashFile, sendBuffer);
								sendBuffer.flush();
								responseSock.close();
							} catch (IOException e) {
								e.printStackTrace();
								System.err
										.println("Error connecting to remote client socket to send response");
								continue;
							}
						} else if (messageType == StorageHelper.DELALLRIENTRIES) {
							System.out.println("Recieved DELALLRIENTRIES");
							Socket responseSock = null;
							OutputStream sendBuffer = null;
							try {
								String returnStr;
								if (handler.deleteAllRIEntries()) {
									returnStr = "true";
								} else {
									returnStr = "false";
								}
								responseSock = new Socket(remoteAddress,
										StorageHelper.parsePortNo(message));
								sendBuffer = responseSock.getOutputStream();
								sendBuffer.write(returnStr.getBytes());
								sendBuffer.flush();
								responseSock.close();
							} catch (IOException e) {
								e.printStackTrace();
								System.err
										.println("Error connecting to remote client socket to send response");
								continue;
							}
						}
					}
				} else {
					System.err
							.println("Unable to properly initialize Berkeley DB. Exiting...");
					handler.close();
					break;
				}
				incoming.close();
			} catch (SocketException f) {
				handler.close();
				break;
			} catch (IOException e) {
				System.err
						.println("Search server encountered an IO error while listening for servlet requests.");
				e.printStackTrace();
				handler.close();
				break;
			}
		}

		try {
			Thread.sleep(3000);
			sock.close();
		} catch (IOException e) {
			System.err.println("Search server unable to close socket. Exiting...");
		} catch (InterruptedException e) {}

	}

	private static void getAllDocHashes(File hashFile, OutputStream sendBuffer) {
		if (hashFile != null) {
			try {
				BufferedReader inFile = new BufferedReader(new FileReader(hashFile));
				// String result = "";
				String line = inFile.readLine();
				// HashSet<String> hashes = new HashSet<String>();
				while (line != null) {
					// hashes.add(line);
					System.out.println(line);
					sendBuffer.write((line + " ").getBytes());
					sendBuffer.flush();
					line = inFile.readLine();
				}
				inFile.close();
				// for (String item : hashes) {
				// result += item + " ";
				// }
			} catch (FileNotFoundException e) {} catch (IOException e) {}
		}
	}
}
