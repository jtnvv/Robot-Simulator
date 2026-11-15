package fr.tp.inf112.projects.robotsim.helloworld;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HelloWorldClient {

	private static final Logger LOGGER = Logger.getLogger(HelloWorldClient.class.getName());

	public static void main(final String[] args) {
		final String message = args.length > 0 ? args[0] : "my message";

		try (final Socket socket = new Socket("localhost", HelloWorldServer.PORT)) {
			final PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
			writer.println(message);

			final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			LOGGER.info("Server response: " + reader.readLine());
		}
		catch (final IOException ex) {
			LOGGER.log(Level.SEVERE, "Error while contacting the server.", ex);
		}
	}
}
