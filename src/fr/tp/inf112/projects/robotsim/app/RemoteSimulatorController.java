package fr.tp.inf112.projects.robotsim.app;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.canvas.model.impl.BasicPoint;
import fr.tp.inf112.projects.graph.impl.BasicVertex;
import fr.tp.inf112.projects.robotsim.model.Component;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.LocalFactoryModelChangedNotifier;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;

public class RemoteSimulatorController extends SimulatorController {

	private static final Logger LOGGER = Logger.getLogger(RemoteSimulatorController.class.getName());

	public static final String SERVER_HOST = "localhost";

	public static final int SERVER_PORT = 8080;

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

	private final HttpClient httpClient;

	private final ObjectMapper objectMapper;

	private final LocalFactoryModelChangedNotifier notifier;

	private volatile boolean animationRunning;

	private Thread viewerUpdater;

	public RemoteSimulatorController(final CanvasPersistenceManager persistenceManager) {
		this(null, persistenceManager);
	}

	public RemoteSimulatorController(final Factory factoryModel,
									 final CanvasPersistenceManager persistenceManager) {
		super(factoryModel, persistenceManager);

		notifier = new LocalFactoryModelChangedNotifier();
		animationRunning = false;

		if (factoryModel != null) {
			factoryModel.setNotifier(notifier);
		}

		httpClient = HttpClient.newHttpClient();

		objectMapper = new ObjectMapper();
		final PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
				.allowIfSubType(PositionedShape.class.getPackageName())
				.allowIfSubType(Component.class.getPackageName())
				.allowIfSubType(BasicVertex.class.getPackageName())
				.allowIfSubType(BasicPoint.class.getPackageName())
				.allowIfSubType(ArrayList.class.getName())
				.allowIfSubType(LinkedHashSet.class.getName())
				.build();
		objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
	}

	@Override
	public boolean addObserver(final Observer observer) {
		return notifier.addObserver(observer);
	}

	@Override
	public boolean removeObserver(final Observer observer) {
		return notifier.removeObserver(observer);
	}

	@Override
	public void setCanvas(final Canvas canvasModel) {
		super.setCanvas(canvasModel);

		final Factory factory = (Factory) getCanvas();

		if (factory != null) {
			factory.setNotifier(notifier);
		}

		notifier.notifyObservers();
	}

	public void setCanvas(final String canvasAsJson) {
		try {
			setCanvas(objectMapper.readValue(canvasAsJson, Factory.class));
		}
		catch (final IOException ex) {
			LOGGER.log(Level.SEVERE, "Cannot parse the received factory model.", ex);
		}
	}

	@Override
	public boolean isAnimationRunning() {
		return animationRunning;
	}

	@Override
	public void startAnimation() {
		try {
			if (!callService("start")) {
				LOGGER.warning("The simulation service could not start the simulation of the factory model.");

				return;
			}

			setCanvas(getFactory());
			animationRunning = true;

			viewerUpdater = new Thread(new Runnable() {

				@Override
				public void run() {
					final FactorySimulationEventConsumer consumer = new FactorySimulationEventConsumer(RemoteSimulatorController.this);
					consumer.consumeMessages();
				}
			});
			viewerUpdater.start();
		}
		catch (final InterruptedException | URISyntaxException | IOException ex) {
			LOGGER.log(Level.SEVERE, "Cannot start the remote simulation.", ex);
		}
	}

	@Override
	public void stopAnimation() {
		try {
			if (!callService("stop")) {
				LOGGER.warning("The simulation service could not stop the simulation of the factory model.");
			}

			animationRunning = false;

			if (viewerUpdater != null) {
				viewerUpdater.join();
				viewerUpdater = null;
			}
		}
		catch (final InterruptedException | URISyntaxException | IOException ex) {
			LOGGER.log(Level.SEVERE, "Cannot stop the remote simulation.", ex);
		}

		// The remote model is not simulated anymore, so stop the local copy that is displayed
		final Factory factory = (Factory) getCanvas();

		if (factory != null) {
			factory.stopSimulation();
		}
	}

	private boolean callService(final String operation)
	throws URISyntaxException, IOException, InterruptedException {
		final String factoryId = getCanvas().getId();
		final URI uri = new URI("http", null, SERVER_HOST, SERVER_PORT, "/simulation/" + operation + "/" + factoryId, null, null);
		final HttpRequest request = HttpRequest.newBuilder().uri(uri).timeout(REQUEST_TIMEOUT).GET().build();
		final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		return response.statusCode() == 200 && Boolean.parseBoolean(response.body());
	}

	private Factory getFactory()
	throws URISyntaxException, IOException, InterruptedException {
		final String factoryId = getCanvas().getId();
		final URI uri = new URI("http", null, SERVER_HOST, SERVER_PORT, "/simulation/" + factoryId, null, null);
		final HttpRequest request = HttpRequest.newBuilder().uri(uri).timeout(REQUEST_TIMEOUT).GET().build();
		final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new IOException("The simulation service answered with status " + response.statusCode() + ".");
		}

		return objectMapper.readValue(response.body(), Factory.class);
	}
}
