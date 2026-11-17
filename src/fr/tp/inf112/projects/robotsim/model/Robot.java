package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.canvas.model.impl.RGBColor;
import fr.tp.inf112.projects.robotsim.model.motion.Motion;
import fr.tp.inf112.projects.robotsim.model.path.FactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Robot extends Component {
	
	private static final long serialVersionUID = -1218857231970296747L;

	private static final Style STYLE = new ComponentStyle(RGBColor.GREEN, RGBColor.BLACK, 3.0f, null);

	private static final Style BLOCKED_STYLE = new ComponentStyle(RGBColor.RED, RGBColor.BLACK, 3.0f, new float[]{4.0f});

	private final Battery battery;
	
	private int speed;
	
	private List<Component> targetComponents;
	
	@JsonIgnore
	private transient Iterator<Component> targetComponentsIterator;
	
	private Component currTargetComponent;
	
	@JsonIgnore
	private transient Iterator<Position> currentPathPositionsIter;
	
	@JsonProperty("blocked")
	private transient boolean blocked;

	// volatile: it is also read from the threads of the other robots (see isLivelyLocked()).
	private volatile Position blockedTargetPosition;
	
	// Set while the robot moves to a neighbouring free cell to get out of a livelock.
	private transient boolean steppingAside;
	
	@JsonIgnore
	private transient SynchronizedPosition lockedPosition;
	
	private FactoryPathFinder pathFinder;

	public Robot() {
		super();

		this.pathFinder = null;
		this.battery = null;
		targetComponents = new ArrayList<>();
		currTargetComponent = null;
		currentPathPositionsIter = null;
		speed = 5;
		setBlocked(false);
		blockedTargetPosition = null;
	}

	public Robot(final Factory factory,
				 final FactoryPathFinder pathFinder,
				 final CircularShape shape,
				 final Battery battery,
				 final String name ) {
		super(factory, shape, name);
		
		this.pathFinder = pathFinder;
		
		this.battery = battery;
		
		targetComponents = new ArrayList<>();
		currTargetComponent = null;
		currentPathPositionsIter = null;
		speed = 5;
		setBlocked(false);
		blockedTargetPosition = null;
	}

	@Override
	public String toString() {
		return super.toString() + " battery=" + battery + "]";
	}

	protected int getSpeed() {
		return speed;
	}

	protected void setSpeed(final int speed) {
		this.speed = speed;
	}
	
	public Position getBlockedTargetPosition() {
		return blockedTargetPosition;
	}
	
	public Battery getBattery() {
		return battery;
	}

	public FactoryPathFinder getPathFinder() {
		return pathFinder;
	}

	public List<Component> getTargetComponents() {
		if (targetComponents == null) {
			targetComponents = new ArrayList<>();
		}
		
		return targetComponents;
	}
	
	public boolean addTargetComponent(final Component targetComponent) {
		return getTargetComponents().add(targetComponent);
	}
	
	public boolean removeTargetComponent(final Component targetComponent) {
		return getTargetComponents().remove(targetComponent);
	}
	
	@Override
	public boolean isMobile() {
		return true;
	}
	
	public boolean isBlocked() {
		return blocked;
	}

	public void setBlocked(boolean blocked) {
		if ( this.blocked != blocked) {
			this.blocked = blocked;
			notifyObservers();
		}
	}

	@Override
	public boolean behave() {
		if (getTargetComponents().isEmpty()) {
			return false;
		}
		
		if (currTargetComponent == null || hasReachedCurrentTarget()) {
			currTargetComponent = nextTargetComponentToVisit();
			
			computePathToCurrentTargetComponent();
		}

		return moveToNextPathPosition() != 0;
	}
		
	private Component nextTargetComponentToVisit() {
		if (targetComponentsIterator == null || !targetComponentsIterator.hasNext()) {
			targetComponentsIterator = getTargetComponents().iterator();
		}
		
		return targetComponentsIterator.hasNext() ? targetComponentsIterator.next() : null;
	}
	
	@JsonIgnore
	public boolean isLivelyLocked() {
		final Position blockedTargetPosition = getBlockedTargetPosition();
		
		if (blockedTargetPosition == null) {
			return false;
		}
		
		final Component otherRobot = getFactory().getMobileComponentAt(blockedTargetPosition, this);
		
		return otherRobot != null && 
			   getPosition().equals(((Robot) otherRobot).getBlockedTargetPosition());
	}
	
	private Position findFreeNeighbouringPosition() {
		final int resolution = getFactory().getPathResolution();
		
		for (int xOffset = -resolution; xOffset <= resolution; xOffset += resolution) {
			for (int yOffset = -resolution; yOffset <= resolution; yOffset += resolution) {
				final int x = getxCoordinate() + xOffset;
				final int y = getyCoordinate() + yOffset;
				
				if ((xOffset == 0 && yOffset == 0) || x < 0 || y < 0 || x + resolution > getFactory().getWidth() || y + resolution > getFactory().getHeight()) {
					continue;
				}
				
				if (!getFactory().hasObstacleAt(new RectangularShape(x, y, resolution, resolution)) &&
					!getFactory().hasMobileComponentAt(new RectangularShape(x, y, resolution, resolution), this)) {
					return new Position(x, y);
				}
			}
		}
		
		return null;
	}
	
	private int moveToNextPathPosition() {
		final Motion motion = computeMotion();
		
		int displacement = 0;
		
		try {
			displacement = motion == null ? 0 : motion.moveToTarget();
		}
		finally {
			unlockTargetPosition();
		}
			
		if (displacement != 0) {
			setBlocked(false);
			
			if (steppingAside) {
				steppingAside = false;
				computePathToCurrentTargetComponent();
			}
			
			notifyObservers();
		}
		else if (steppingAside) {
			steppingAside = false;
			blockedTargetPosition = null;
			computePathToCurrentTargetComponent();
		}
		else if (isLivelyLocked()) {
			final Position freeNeighbouringPosition = findFreeNeighbouringPosition();
			
			if (freeNeighbouringPosition != null) {
				blockedTargetPosition = freeNeighbouringPosition;
				steppingAside = true;
				displacement = moveToNextPathPosition();
			}
		}
		
		return displacement;
	}
	
	private void unlockTargetPosition() {
		if (lockedPosition != null) {
			lockedPosition.unlock();
			lockedPosition = null;
		}
	}
	
	private void computePathToCurrentTargetComponent() {
		final List<Position> currentPathPositions = pathFinder.findPath(this, currTargetComponent);
		currentPathPositionsIter = currentPathPositions.iterator();
		
		if (!currentPathPositions.isEmpty()) {
			setBlocked(false);
		}
	}
	
	private Motion computeMotion() {
		// Retry the memorized blocked position first; if there is none and the path is used up, compute a new one
		if (blockedTargetPosition == null && !currentPathPositionsIter.hasNext()) {
			
			computePathToCurrentTargetComponent();
			
			if (!currentPathPositionsIter.hasNext()) {

				// There is no free path to the target
				setBlocked(true);
				
				return null;
			}
		}
		
		
		final Position targetPosition = getTargetPosition();
		final PositionedShape shape = new RectangularShape(targetPosition.getxCoordinate(),
														   targetPosition.getyCoordinate(),
				   										   2,
				   										   2);
		
		// Only one robot at a time can check and move to a given position
		lockedPosition = getFactory().getSynchronizedPosition(targetPosition);
		lockedPosition.lock();
		
		// If there is another robot, memorize the blocked target position for the next run
		if (getFactory().hasMobileComponentAt(shape, this)) {
			this.blockedTargetPosition = targetPosition;
			unlockTargetPosition();
			
			return null;
		}

		// Reset the memorized position
		this.blockedTargetPosition = null;
			
		return new Motion(getPosition(), targetPosition);
	}
	
	private Position getTargetPosition() {
		// If a target position was memorized, it means that the robot was blocked during the last iteration 
		// so it waited for another robot to pass. So try to move to this memorized position otherwise move to  
		// the next position from the path
		return this.blockedTargetPosition == null ? currentPathPositionsIter.next() : this.blockedTargetPosition;
	}
	
	private boolean hasReachedCurrentTarget() {
		return getPositionedShape().overlays(currTargetComponent.getPositionedShape());
	}
	
	@Override
	public boolean canBeOverlayed(final PositionedShape shape) {
		return true;
	}
	
	@Override
	public Style getStyle() {
		return isBlocked() ? BLOCKED_STYLE : STYLE;
	}
}
