package fr.tp.inf112.projects.robotsim.helloworld;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HelloWorldServer {

	private static final Logger LOGGER = Logger.getLogger(HelloWorldServer.class.getName());

	public static final int PORT = 8888;

	public static void main(final String[] args) {
		try (final ServerSocket serverSocket = new ServerSocket(PORT)) {
			LOGGER.info("Hello World server listening on port " + PORT + ".");

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
				final InputStream inpStr = socket.getInputStream();
				final BufferedReader reader = new BufferedReader(new InputStreamReader(inpStr));
				final String message = reader.readLine();
				LOGGER.config("Received '" + message + "'.");

				final OutputStream outStr = socket.getOutputStream();
				final PrintWriter writer = new PrintWriter(outStr, true);
				writer.println("I received " + message + "! ");
			}
			catch (final IOException ex) {
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
