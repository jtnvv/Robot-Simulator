package fr.tp.slr201.projects.robotsim.service.simulation;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.RemoteFactoryPersistenceManager;

@RestController
public class SimulationController {

	private static final Logger LOGGER = Logger.getLogger(SimulationController.class.getName());

	private final Map<String, Factory> simulatedFactories = new ConcurrentHashMap<>();

	private final CanvasPersistenceManager persistenceManager = new RemoteFactoryPersistenceManager(null);

	@GetMapping("/simulation/start/{factoryId}")
	public boolean startSimulation(@PathVariable final String factoryId) {
		LOGGER.info("Starting the simulation of factory model '" + factoryId + "'.");

		try {
			final Canvas canvas = persistenceManager.read(factoryId);

			if (!(canvas instanceof Factory)) {
				LOGGER.warning("The model '" + factoryId + "' read from the persistence server is not a factory model.");

				return false;
			}

			final Factory factory = (Factory) canvas;
			final Factory previousFactory = simulatedFactories.put(factoryId, factory);

			if (previousFactory != null) {
				LOGGER.info("Stopping the previous simulation of factory model '" + factoryId + "'.");
				previousFactory.stopSimulation();
			}

			factory.startSimulation();
			LOGGER.info("Simulation of factory model '" + factoryId + "' started.");

			return true;
		}
		catch (final IOException | RuntimeException ex) {
			LOGGER.log(Level.SEVERE, "Cannot start the simulation of factory model '" + factoryId + "'.", ex);

			return false;
		}
	}

	@GetMapping("/simulation/{factoryId}")
	public Factory getSimulatedFactory(@PathVariable final String factoryId) {
		LOGGER.fine("Retrieving the simulated factory model '" + factoryId + "'.");

		final Factory factory = simulatedFactories.get(factoryId);

		if (factory == null) {
			LOGGER.warning("Factory model '" + factoryId + "' is not being simulated.");

			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factory model '" + factoryId + "' is not being simulated.");
		}

		return factory;
	}

	@GetMapping("/simulation/stop/{factoryId}")
	public boolean stopSimulation(@PathVariable final String factoryId) {
		LOGGER.info("Stopping the simulation of factory model '" + factoryId + "'.");

		final Factory factory = simulatedFactories.remove(factoryId);

		if (factory == null) {
			LOGGER.warning("Factory model '" + factoryId + "' is not being simulated.");

			return false;
		}

		factory.stopSimulation();
		LOGGER.info("Simulation of factory model '" + factoryId + "' stopped.");

		return true;
	}
}
