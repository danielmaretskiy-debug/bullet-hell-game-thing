import java.awt.*;

public class Beam {
    private int x, y;
    private int state = 0; // 0 = warning, 1 = flashing beam, 2 = shrinking, 3 = fading out
    private int stateTimer = 0;
    private int warningSize = 40;
    private int screenWidth, screenHeight;
    private double angle; // Direction the beam fires
    private static final int WARNING_DURATION = 40; // Increased warning time
    private static final int FLASH_DURATION = 15;
    private static final int SHRINK_DURATION = 20; // Shrink duration
    private static final int FADE_DURATION = 30; // Extra fade duration for complete visual fade

    public Beam(int x, int y, int screenWidth, int screenHeight, double angle) {
        this.x = x;
        this.y = y;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.angle = angle;
    }

    public void update() {
        stateTimer++;
        
        if (state == 0 && stateTimer >= WARNING_DURATION) {
            state = 1;
            stateTimer = 0;
        } else if (state == 1 && stateTimer >= FLASH_DURATION) {
            state = 2;
            stateTimer = 0;
        } else if (state == 2 && stateTimer >= SHRINK_DURATION) {
            state = 3;
            stateTimer = 0;
        }
    }

    public void draw(Graphics2D g) {
        if (state == 0) {
            // Warning state - yellow circle at beam origin with rectangle showing beam direction
            g.setColor(new Color(255, 255, 0, 200));
            g.fillOval(x - warningSize / 2, y - warningSize / 2, warningSize, warningSize);
            g.setColor(Color.YELLOW);
            g.setStroke(new BasicStroke(2));
            g.drawOval(x - warningSize / 2, y - warningSize / 2, warningSize, warningSize);
            
            // Draw transparent rectangle showing where beam will go
            java.awt.geom.AffineTransform originalTransform = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);
            
            int beamLength = (int) Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight) * 2;
            g.setColor(new Color(255, 255, 100, 100)); // Semi-transparent yellow rectangle
            g.fillRect(-beamLength / 2, -30, beamLength, 60);
            g.setColor(new Color(255, 255, 0, 150)); // Yellow border
            g.setStroke(new BasicStroke(1));
            g.drawRect(-beamLength / 2, -30, beamLength, 60);
            
            g.setTransform(originalTransform);
        } else if (state == 1) {
            // Flashing white beam spanning screen
            float flash = (stateTimer % 3 < 1) ? 1f : 0.7f;
            g.setColor(new Color(1f, 1f, 1f, flash * 0.8f));
            
            // Draw rectangular beam rotated around origin point
            java.awt.geom.AffineTransform originalTransform = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);
            
            // Beam is 60px tall and spans the screen width
            int beamLength = (int) Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight) * 2;
            g.fillRect(-beamLength / 2, -30, beamLength, 60);
            
            g.setTransform(originalTransform);
        } else if (state == 2) {
            // Shrinking 
            float progress = (float) stateTimer / SHRINK_DURATION;
            float alpha = 1f - (progress * 0.5f); // Fade to 50% during shrink
            g.setColor(new Color(1f, 1f, 1f, alpha * 0.6f));
            
            java.awt.geom.AffineTransform originalTransform = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);
            
            int beamLength = (int) Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight) * 2;
            int shrinkHeight = (int) (60 * (1 - progress));
            g.fillRect(-beamLength / 2, -shrinkHeight / 2, beamLength, shrinkHeight);
            
            g.setTransform(originalTransform);
        } else if (state == 3) {
            // Fading out completely
            float progress = (float) stateTimer / FADE_DURATION;
            float alpha = (1f - progress) * 0.9f; // Fade from 30% to 0%
            g.setColor(new Color(1f, 1f, 1f, alpha));
            
            java.awt.geom.AffineTransform originalTransform = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);
            
            int beamLength = (int) Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight) * 2;
            g.fillRect(-beamLength / 2, -15, beamLength, 30);
            
            g.setTransform(originalTransform);
        }
    }

    public boolean isFinished() {
        return state == 3 && stateTimer >= FADE_DURATION;
    }

    public boolean checkCollision(int px, int py) {
        // Use rotated-rectangle collision matching the visual beam rect.
        // Transform point into beam-local coordinates by translating to (x,y)
        double dx = px - x;
        double dy = py - y;
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        // rotate point by -angle: x' = c*dx + s*dy; y' = -s*dx + c*dy
        double localX = c * dx + s * dy;
        double localY = -s * dx + c * dy;

        int beamLength = (int) Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight) * 2;

        // Only active/flashing/shrinking beams should damage (state 1 or 2)
        if (state == 1) {
            int halfH = 30; // visual half-height during flash
            if (localX >= -beamLength / 2 && localX <= beamLength / 2 && localY >= -halfH && localY <= halfH) {
                // avoid near-origin hits
                if (Math.hypot(localX, localY) > 20) return true;
            }
        } else if (state == 2) {
            // shrinking height depends on progress
            float progress = (float) stateTimer / SHRINK_DURATION;
            int shrinkH = (int) (60 * (1 - progress));
            int halfH = Math.max(4, shrinkH / 2);
            if (localX >= -beamLength / 2 && localX <= beamLength / 2 && localY >= -halfH && localY <= halfH) {
                if (Math.hypot(localX, localY) > 20) return true;
            }
        }
        return false;
    }

    public void setRemoveAfterFade(boolean remove) {
        // Ensures beam will be removed after fade completes (already default behavior)
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getState() { return state; }
    public int getStateTimer() { return stateTimer; }
}

