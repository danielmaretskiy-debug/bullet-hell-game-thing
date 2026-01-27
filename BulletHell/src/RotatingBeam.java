import java.awt.*;

public class RotatingBeam {
    private int centerX, centerY;
    private double baseAngle; // starting angle
    private double rotationOffset = 0;
    private int screenWidth, screenHeight;
    private static final int BEAM_WIDTH = 80;

    public RotatingBeam(int bossX, int bossY, double angle, int screenWidth, int screenHeight) {
        this.centerX = bossX;
        this.centerY = bossY;
        this.baseAngle = angle;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void setCenter(int x, int y) {
        this.centerX = x;
        this.centerY = y;
    }

    public void setRotation(double offset) {
        this.rotationOffset = offset;
    }

    public void update(int bossX, int bossY, double rotationOffset) {
        setCenter(bossX, bossY);
        setRotation(rotationOffset);
    }

    public void setScreenSize(int w, int h) {
        this.screenWidth = w;
        this.screenHeight = h;
    }

    public void draw(Graphics2D g) {
        double finalAngle = baseAngle + rotationOffset;
        java.awt.geom.AffineTransform old = g.getTransform();
        g.translate(centerX, centerY);
        g.rotate(finalAngle);

        int beamLength = (int) Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight);

        // Glow
        g.setColor(new Color(1f, 1f, 0.6f, 0.6f));
        g.fillRect(0, -BEAM_WIDTH / 2, beamLength, BEAM_WIDTH);

        // Core
        g.setColor(new Color(1f, 1f, 0.2f, 1f));
        g.fillRect(0, -BEAM_WIDTH / 4, beamLength, BEAM_WIDTH / 2);

        g.setTransform(old);
    }

    public boolean checkCollision(int px, int py) {
        // Transform point into beam-local coordinates (beam drawn at x=0..beamLength, y=-BEAM_WIDTH/2..BEAM_WIDTH/2)
        double finalAngle = baseAngle + rotationOffset;
        double dx = px - centerX;
        double dy = py - centerY;
        double c = Math.cos(finalAngle);
        double s = Math.sin(finalAngle);
        // rotate point by -finalAngle
        double localX = c * dx + s * dy;
        double localY = -s * dx + c * dy;

        int beamLength = (int) Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight);
        int halfH = BEAM_WIDTH / 2;

        // Beam rectangle spans from localX in [0, beamLength]
        if (localX >= 0 && localX <= beamLength && localY >= -halfH && localY <= halfH) {
            if (Math.hypot(localX, localY) > 20) return true;
        }
        return false;
    }
}
