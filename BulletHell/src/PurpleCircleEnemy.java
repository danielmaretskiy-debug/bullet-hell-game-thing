import java.awt.*;
import java.util.ArrayList;

public class PurpleCircleEnemy {
    private int x, y;
    private int hp = 5;
    private int rotationState = 0; // 0 = spinning fast, 1 = spinning slow, 2 = firing
    private int rotationTimer = 0;
    private double beamRotation = 0;
    private double rotationSpeed = 0.15; // Slower initial rotation
    private int fireTimer = 0;
    private ArrayList<int[]> beamPositions;
    private ArrayList<Beam> beams;
    private static final int SPIN_FAST_DURATION = 200; // 3.3 seconds - slower spin
    private static final int SPIN_SLOW_DURATION = 120; // 2 seconds - more warning before firing
    private static final int RADIUS = 60; // Distance of beams from center
    private int screenWidth, screenHeight;

    public PurpleCircleEnemy(int x, int y, int screenWidth, int screenHeight) {
        this.x = x;
        this.y = y;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.beamPositions = new ArrayList<>();
        this.beams = new ArrayList<>();
        initializeBeamPositions();
    }

    private void initializeBeamPositions() {
        beamPositions.clear();
        for (int i = 0; i < 5; i++) {
            beamPositions.add(new int[]{0, 0});
        }
    }

    public void update(int width, int height) {
        update(width, height, false);
    }

    public void update(int width, int height, boolean isSlowed) {
        // Update beam positions around the circle
        for (int i = 0; i < 5; i++) {
            double angle = beamRotation + (i * Math.PI * 2 / 5);
            int beamX = x + (int) (Math.cos(angle) * RADIUS);
            int beamY = y + (int) (Math.sin(angle) * RADIUS);
            beamPositions.get(i)[0] = beamX;
            beamPositions.get(i)[1] = beamY;
        }

        rotationTimer++;

        if (rotationState == 0) {
            // Fast spinning (always spin regardless of slowField)
            beamRotation += rotationSpeed;
            if (rotationTimer >= SPIN_FAST_DURATION) {
                rotationState = 1;
                rotationTimer = 0;
                rotationSpeed = 0.02; // Slow down gradually
                // Randomize rotation for next cycle so beams fire in different directions
                beamRotation += Math.random() * Math.PI * 2;
            }
        } else if (rotationState == 1) {
            // Slow spinning (always spin regardless of slowField)
            beamRotation += rotationSpeed;
            rotationSpeed *= 0.98; // Gradual slowdown
            
            if (rotationTimer >= SPIN_SLOW_DURATION) {
                rotationState = 2;
                rotationTimer = 0;
                // Spawn 5 beams at the current circle positions after spinning
                beams.clear();
                for (int i = 0; i < 5; i++) {
                    // Use current beam positions from the circle rotation
                    int beamX = beamPositions.get(i)[0];
                    int beamY = beamPositions.get(i)[1];
                    double angle = beamRotation + (i * Math.PI * 2 / 5);
                    Beam beam = new Beam(beamX, beamY, screenWidth, screenHeight, angle);
                    //beam.setRemoveAfterFade(true); // Ensure beams fully fade
                    beams.add(beam);
                }
            }
        } else if (rotationState == 2) {
            // Firing state - update beams
            for (Beam beam : beams) {
                
                beam.update();
            }
            // Remove finished beams
            for (int i = beams.size() - 1; i >= 0; i--) {
            Beam beam = beams.get(i);
            beam.update();
            if (beam.isFinished()) {
                beams.remove(i);
            }
        }
            
            fireTimer++;
            if (fireTimer > 60) {
                rotationState = 0;
                rotationTimer = 0;
                rotationSpeed = 0.3;
                fireTimer = 0;
            }
        }
    }

    public void spawnProjectiles(ArrayList<EnemyProjectile> projectiles) {
        // Fire projectiles when beams finish flashing
        for (int i = 0; i < beams.size(); i++) {
            Beam beam = beams.get(i);
            if (beam.getState() == 2 && beam.getStateTimer() == 1) {
                // Calculate beam direction
                double angle = beamRotation + (i * Math.PI * 2 / 5);
                int beamX = beamPositions.get(i)[0];
                int beamY = beamPositions.get(i)[1];
                // Fire projectile in beam direction
                projectiles.add(new EnemyProjectile(beamX, beamY, angle, 4, Color.MAGENTA, 1));
            }
        }
    }

    public void takeDamage(int damage) {
        hp -= damage;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public boolean collidesWith(int px, int py) {
        return Math.abs(x - px) < 25 && Math.abs(y - py) < 25;
    }

    public void draw(Graphics2D g) {
        // Draw beams first (behind the enemy)
        for (Beam beam : beams) {
            beam.draw(g);
        }
        
        // Draw purple circle
        g.setColor(new Color(128, 0, 128));
        g.fillOval(x - 25, y - 25, 50, 50);
        g.setColor(Color.MAGENTA);
        g.setStroke(new BasicStroke(2));
        g.drawOval(x - 25, y - 25, 50, 50);

        // Draw beam warnings during spinning/slow phases
        if (rotationState < 2) {
            for (int[] pos : beamPositions) {
                g.setColor(new Color(1f, 0.5f, 1f, 0.6f));
                g.fillOval(pos[0] - 8, pos[1] - 8, 16, 16);
                g.setColor(new Color(1f, 0f, 1f, 0.6f));
                g.setStroke(new BasicStroke(1));
                g.drawOval(pos[0] - 8, pos[1] - 8, 16, 16);
            }
        }
    }

    public ArrayList<int[]> getBeamPositions() {
        return beamPositions;
    }

    public ArrayList<Beam> getBeams() {
        return beams;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
