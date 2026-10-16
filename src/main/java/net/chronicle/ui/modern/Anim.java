package net.chronicle.ui.modern;

/** Exponentiell geglätteter Wert für flüssige, framerate-unabhängige Animationen. */
public final class Anim {
	private float value;
	private float target;
	private final float speed;

	public Anim(float initial, float speed) {
		this.value = initial;
		this.target = initial;
		this.speed = speed;
	}

	public float update(float dt) {
		value += (target - value) * (1f - (float) Math.exp(-speed * dt));
		if (Math.abs(target - value) < 0.001f) value = target;
		return value;
	}

	public void set(float target) {
		this.target = target;
	}

	public void snap(float value) {
		this.value = value;
		this.target = value;
	}

	public float get() {
		return value;
	}

	public float target() {
		return target;
	}

	/** Misst die Zeit zwischen zwei Frames in Sekunden (begrenzt, damit Ruckler nicht springen). */
	public static final class Clock {
		private long last = System.nanoTime();

		public float tick() {
			long now = System.nanoTime();
			float dt = (now - last) / 1_000_000_000f;
			last = now;
			return Math.min(dt, 0.1f);
		}
	}
}
