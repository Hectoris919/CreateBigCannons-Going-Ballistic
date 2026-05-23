package org.hectoris919.CBCGoingBallistic.data;

public record CannonComponentProperties(double lengthMeters, double velocityMultiplier) {
	public CannonComponentProperties {
		if (!Double.isFinite(lengthMeters) || lengthMeters <= 0.0D) lengthMeters = 1.0D;
		if (!Double.isFinite(velocityMultiplier) || velocityMultiplier <= 0.0D) velocityMultiplier = 1.0D;
	}

	public static CannonComponentProperties normalBarrel() { return new CannonComponentProperties(1.0D, 1.0D); }
}
