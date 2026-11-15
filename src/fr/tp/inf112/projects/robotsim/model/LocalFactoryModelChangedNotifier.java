package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.List;

import fr.tp.inf112.projects.canvas.controller.Observer;

public class LocalFactoryModelChangedNotifier implements FactoryModelChangedNotifier {

	private List<Observer> observers;

	public LocalFactoryModelChangedNotifier() {
		observers = null;
	}

	private List<Observer> getObservers() {
		if (observers == null) {
			observers = new ArrayList<>();
		}

		return observers;
	}

	@Override
	public void notifyObservers() {
		for (final Observer observer : getObservers()) {
			observer.modelChanged();
		}
	}

	@Override
	public boolean addObserver(final Observer observer) {
		return getObservers().add(observer);
	}

	@Override
	public boolean removeObserver(final Observer observer) {
		return getObservers().remove(observer);
	}
}
