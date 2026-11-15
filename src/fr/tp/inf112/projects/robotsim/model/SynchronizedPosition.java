package fr.tp.inf112.projects.robotsim.model;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronizedPosition extends Position {

	private static final long serialVersionUID = 4283918472810451273L;

	private final Lock lock;

	public SynchronizedPosition() {
		this(0, 0);
	}

	public SynchronizedPosition(final int xCoordinate,
								final int yCoordinate) {
		super(xCoordinate, yCoordinate);
		
		lock = new ReentrantLock();
	}
	
	public void lock() {
		lock.lock();
	}
	
	public void unlock() {
		lock.unlock();
	}
}
