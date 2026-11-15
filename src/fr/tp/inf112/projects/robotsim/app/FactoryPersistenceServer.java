package fr.tp.inf112.projects.robotsim.app;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryPersistenceManager;

public class FactoryPersistenceServer {

	private static final Logger LOGGER = Logger.getLogger(FactoryPersistenceServer.class.getName());

	public static final int PORT = 8888;

	public static void main(final String[] args) {
		try (final ServerSocket serverSocket = new ServerSocket(PORT)) {
			LOGGER.info("Factory persistence server listening on port " + PORT + ".");

			do {
				try {
					final Socket socket = serverSocket.accept();
					new Thread(new RequestProcessor(socket)).start();
				}
				catch (final IOException ex) {
					LOGGER.log(Level.SEVERE, "Error while accepting a connection.", ex);
				}
			} while (true);
		}
		catch (final IOException ex) {
			LOGGER.log(Level.SEVERE, "Cannot start the server.", ex);
		}
	}

	private static class RequestProcessor implements Runnable {

		private final Socket socket;

		private RequestProcessor(final Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				final ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
				final Object request = objInputStream.readObject();
				final ObjectOutputStream objOutputStream = new ObjectOutputStream(socket.getOutputStream());
				final FactoryPersistenceManager persistenceManager = new FactoryPersistenceManager(null);
				Object response;

				try {
					if (request instanceof String) {
						LOGGER.config("Reading model '" + request + "'.");
						response = persistenceManager.read((String) request);
					}
					else if (request instanceof Factory) {
						final Canvas factory = (Canvas) request;
						LOGGER.config("Persisting model '" + factory.getId() + "'.");
						persistenceManager.persist(factory);
						response = Boolean.TRUE;
					}
					else if (request instanceof File) {
						LOGGER.config("Listing the model files.");
						final List<String> fileNames = new ArrayList<>();
						final File[] files = new File(".").listFiles(File::isFile);

						if (files != null) {
							for (final File file : files) {
								fileNames.add(file.getName());
							}
						}

						response = fileNames.toArray(new String[0]);
					}
					else {
						response = new IOException("Unsupported request " + request + ".");
					}
				}
				catch (final IOException | RuntimeException ex) {
					LOGGER.log(Level.WARNING, "Error while processing a request.", ex);
					response = ex instanceof IOException ? ex : new IOException(ex.toString());
				}

				objOutputStream.writeObject(response);
				objOutputStream.flush();
			}
			catch (final IOException | ClassNotFoundException ex) {
				LOGGER.log(Level.SEVERE, "Error while processing a request.", ex);
			}
			finally {
				try {
					socket.close();
				}
				catch (final IOException ex) {
					LOGGER.log(Level.WARNING, "Error while closing the socket.", ex);
				}
			}
		}
	}
}
