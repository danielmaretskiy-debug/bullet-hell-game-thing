import java.awt.*;
import java.util.ArrayList;

public class Boss {
    private int x, y;
    private int maxHP;
    private int hp;
    private int shootCooldown = 0;
    private int attackPattern = 0;
    private int patternTimer = 0;
    private static final int PATTERN_DURATION = 120;
    private ArrayList<Beam> beams;
    private int beamSpawnTimer = 0;

    public Boss(int x, int y, int wave) {
        this.x = x;
        this.y = y;
        this.maxHP = 50 + (wave / 10) * 30;
        this.hp = maxHP;
        this.beams = new ArrayList<>();
    }

    public void update(int width, int height, int wave) {
        // Boss moves slowly
        x += (Math.random() - 0.5) * 2;
        x = Math.max(50, Math.min(width - 50, x));

        patternTimer++;
        if (patternTimer >= PATTERN_DURATION) {
            patternTimer = 0;
            // Beam attacks are much more common (3 out of 5)
            int choice = (int)(Math.random() * 5);
            if (choice < 3) {
                attackPattern = 3; // Beam attack
            } else {
                attackPattern = choice - 3; // Other patterns (0, 1, 2)
            }
        }

        if (shootCooldown > 0) shootCooldown--;

        // Update beams
        for (int i = beams.size() - 1; i >= 0; i--) {
            Beam beam = beams.get(i);
            beam.update();
            if (beam.isFinished()) {
                beams.remove(i);
            }
        }
    }

    public void spawnProjectiles(ArrayList<EnemyProjectile> projectiles, int playerX, int playerY, int width, int height) {
        // Handle beam spawning separately from cooldown - can happen during any pattern
        if (attackPattern == 3) {
            beamSpawnTimer++;
            if (beamSpawnTimer == 1) {
                int numBeams = 2 + (int)(Math.random() * 5); // 2-6 beams
                if (hp < maxHP / 2) numBeams += 3;
                if (hp < maxHP / 3) numBeams += 4;
                for (int i = 0; i < numBeams; i++) {
                    int beamX = 100 + (int)(Math.random() * (width - 200));
                    int beamY = 100 + (int)(Math.random() * (height - 200));
                    double randomAngle = Math.random() * Math.PI * 2;
                    beams.add(new Beam(beamX, beamY, width, height, randomAngle));
                }
            }

            // Add a sweeping vertical beam wave at low HP
            if (hp < maxHP / 2 && beamSpawnTimer % 120 == 0) {
                int columns = 5;
                for (int c = 0; c < columns; c++) {
                    int bx = 50 + c * (width - 100) / (columns - 1);
                    beams.add(new Beam(bx, -10, width, height, Math.PI / 2)); // downward beam from top
                }
            }
            
            // Fire projectiles when beams finish flashing
            for (Beam beam : beams) {
                if (beam.getState() == 2 && beam.getStateTimer() == 1) {
                    // Fire projectile at this location
                    projectiles.add(new EnemyProjectile(beam.getX(), beam.getY(), 0, 8, new Color(255, 100, 100), 3));
                }
            }
            
            if (beamSpawnTimer > 60) {
                beamSpawnTimer = 0;
            }
        }
        
        // Regular projectile shooting from other patterns
        if (shootCooldown <= 0) {
            if (attackPattern == 0) {
                // Spiral attack
                for (int i = 0; i < 8; i++) {
                    double angle = (i / 8.0) * Math.PI * 2 + (patternTimer / 30.0);
                    projectiles.add(new EnemyProjectile(x, y, angle, 5, Color.RED));
                }
            } else if (attackPattern == 1) {
                // Aimed burst
                double angle = Math.atan2(playerY - y, playerX - x);
                for (int i = -2; i <= 2; i++) {
                    projectiles.add(new EnemyProjectile(x, y, angle + (i * 0.2), 5, Color.RED));
                }
            } else if (attackPattern == 2) {
                // Random spread
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    projectiles.add(new EnemyProjectile(x, y, angle, 5, Color.RED));
                }
            } else if (attackPattern == 3) {
                // Beam attack - also fire regular projectiles for variety
                for (int i = 0; i < 4; i++) {
                    double angle = (i / 4.0) * Math.PI * 2;
                    projectiles.add(new EnemyProjectile(x, y, angle, 3, Color.RED));
                }
            }
            shootCooldown = 20;
        }
    }

    public void takeDamage(int damage) {
        hp -= damage;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public boolean collidesWith(int px, int py) {
        return Math.abs(x - px) < 30 && Math.abs(y - py) < 40;
    }

    public double getHealthPercent() {
        return (double) hp / maxHP;
    }

    public void draw(Graphics2D g) {
        var sprite = SpriteLoader.getSprite("boss");
        if (sprite != null) {
            g.drawImage(sprite, x - 60, y - 60, 120, 120, null);
        }

        // Draw beams
        for (Beam beam : beams) {
            beam.draw(g);
        }
    }

    public ArrayList<Beam> getBeams() {
        return beams;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
