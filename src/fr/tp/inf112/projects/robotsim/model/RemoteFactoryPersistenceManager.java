package fr.tp.inf112.projects.robotsim.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.canvas.model.impl.AbstractCanvasPersistenceManager;
import fr.tp.inf112.projects.robotsim.app.FactoryPersistenceServer;

public class RemoteFactoryPersistenceManager extends AbstractCanvasPersistenceManager {

	public static final String SERVER_HOST = "localhost";

	public RemoteFactoryPersistenceManager(final CanvasChooser canvasChooser) {
		super(canvasChooser);
	}

	static Object sendRequest(final Object request)
	throws IOException {
		try (final Socket socket = new Socket(SERVER_HOST, FactoryPersistenceServer.PORT)) {
			final ObjectOutputStream objOutputStream = new ObjectOutputStream(socket.getOutputStream());
			objOutputStream.writeObject(request);
			objOutputStream.flush();

			final ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
			final Object response = objInputStream.readObject();

			if (response instanceof IOException) {
				throw new IOException((IOException) response);
			}

			return response;
		}
		catch (final ClassNotFoundException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public Canvas read(final String canvasId)
	throws IOException {
		return (Canvas) sendRequest(canvasId);
	}

	@Override
	public void persist(final Canvas canvasModel)
	throws IOException {
		sendRequest(canvasModel);
	}

	@Override
	public boolean delete(final Canvas canvasModel)
	throws IOException {
		throw new UnsupportedOperationException("The delete operation is never called by the simulator.");
	}
}
