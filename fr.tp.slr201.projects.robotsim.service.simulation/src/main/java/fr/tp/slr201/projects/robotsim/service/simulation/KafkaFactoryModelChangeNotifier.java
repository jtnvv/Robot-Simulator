package fr.tp.slr201.projects.robotsim.service.simulation;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.robotsim.app.SimulationServiceUtils;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryModelChangedNotifier;

public class KafkaFactoryModelChangeNotifier implements FactoryModelChangedNotifier {

	private final Factory factoryModel;

	private final KafkaTemplate<String, Factory> simulationEventTemplate;

	public KafkaFactoryModelChangeNotifier(final Factory factoryModel,
										   final KafkaTemplate<String, Factory> simulationEventTemplate) {
		this.factoryModel = factoryModel;
		this.simulationEventTemplate = simulationEventTemplate;

		TopicBuilder.name(SimulationServiceUtils.getTopicName(factoryModel)).build();
	}

	@Override
	public void notifyObservers() {
		final Message<Factory> factoryMessage = MessageBuilder.withPayload(factoryModel)
				.setHeader(KafkaHeaders.TOPIC, SimulationServiceUtils.getTopicName(factoryModel))
				.build();

		final CompletableFuture<SendResult<String, Factory>> sendResult = simulationEventTemplate.send(factoryMessage);
		sendResult.whenComplete((result, ex) -> {
			if (ex != null) {
				throw new RuntimeException(ex);
			}
		});
	}

	@Override
	public boolean addObserver(final Observer observer) {
		return false;
	}

	@Override
	public boolean removeObserver(final Observer observer) {
		return false;
	}
}
