package net.chronicle.journal;

import net.chronicle.compat.Platform;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

// relativeAngle: 0 = geradeaus, positiv = rechts
// converted = oberwelt/nether koords wurden mit 8 umgerechnet
public record Nav(double distance, float relativeAngle, int dy, boolean converted) {
	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	public static Nav of(JournalEntry e, LocalPlayer player) {
		if (player.level() == null) return null;
		String here = Platform.dimensionId(player.level());
		double tx = e.x + 0.5, tz = e.z + 0.5;
		int ty = e.y;
		boolean converted = false;
		if (!e.dimension.equals(here)) {
			if (EventTracker.OVERWORLD.equals(e.dimension) && EventTracker.NETHER.equals(here)) {
				tx = e.x / 8.0 + 0.5;
				tz = e.z / 8.0 + 0.5;
				ty = player.getBlockY();
				converted = true;
			} else if (EventTracker.NETHER.equals(e.dimension) && EventTracker.OVERWORLD.equals(here)) {
				tx = e.x * 8.0 + 0.5;
				tz = e.z * 8.0 + 0.5;
				ty = player.getBlockY();
				converted = true;
			} else {
				return null;
			}
		}
		double dx = tx - player.getX();
		double dz = tz - player.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float relative = Mth.wrapDegrees(targetYaw - player.getYRot());
		return new Nav(distance, relative, ty - player.getBlockY(), converted);
	}

	public String arrow() {
		return ARROWS[Math.floorMod(Math.round(relativeAngle / 45f), 8)];
	}
}
