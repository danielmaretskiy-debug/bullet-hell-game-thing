import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class PurpleTriangleEnemy {
    private int x, y;
    private int hp = 2;
    private int spinTimer = 0;
    private int spinDuration;
    private int dashDuration = 0;
    private int dashTimer = 0;
    private int waitTimer = 0;
    private int waitDuration;
    private double dashAngle = 0;
    private double spinAngle = 0;
    private ArrayList<int[]> trailPositions;
    private static final int DASH_SPEED = 8; // Faster than green triangle
    private static final double SPIN_SPEED = 0.3;
    private int state = 0; // 0 = spinning, 1 = dashing, 2 = waiting

    public PurpleTriangleEnemy(int x, int y) {
        this.x = x;
        this.y = y;
        this.trailPositions = new ArrayList<>();
        this.spinDuration = 60;
        this.waitDuration = 180 + (int)(Math.random() * 120); // 3-5 seconds
    }

    public void update(int width, int height, int playerX, int playerY) {
        update(width, height, playerX, playerY, false);
    }

    public void update(int width, int height, int playerX, int playerY, boolean isSlowed) {
        if (state == 0) {
            // Spinning state
            spinAngle += SPIN_SPEED;
            trailPositions.add(new int[]{x, y});
            if (trailPositions.size() > 20) {
                trailPositions.remove(0);
            }
            
            spinTimer++;
            if (spinTimer >= spinDuration) {
                state = 1;
                dashTimer = 0;
                // Calculate angle to current player position right before dashing
                dashAngle = Math.atan2(playerY - y, playerX - x);
                dashDuration = 120;
                trailPositions.clear();
            }
        } else if (state == 1) {
            // Dashing state - slowly home toward player while dashing (unless slowed)
            if (!isSlowed) {
                // Start with initial dash angle but gradually turn toward player
                double angleToPlayer = Math.atan2(playerY - y, playerX - x);
                // Gradually adjust angle toward player (homing effect)
                double angleDiff = angleToPlayer - dashAngle;
                // Normalize angle difference to -pi to pi
                while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
                while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;
                // Slowly turn toward player (homing)
                dashAngle += angleDiff * 0.05; // Gradual homing effect
                
                int nextX = x + (int)(Math.cos(dashAngle) * DASH_SPEED);
                int nextY = y + (int)(Math.sin(dashAngle) * DASH_SPEED);
                
                // Check collision with walls and bounce
                if (nextX < 20) {
                    nextX = 20;
                    dashAngle = Math.PI - dashAngle; // Reflect horizontally
                } else if (nextX > width - 20) {
                    nextX = width - 20;
                    dashAngle = Math.PI - dashAngle; // Reflect horizontally
                }
                
                if (nextY < 20) {
                    nextY = 20;
                    dashAngle = -dashAngle; // Reflect vertically
                } else if (nextY > height - 20) {
                    nextY = height - 20;
                    dashAngle = -dashAngle; // Reflect vertically
                }

                x = nextX;
                y = nextY;
            }
            dashTimer++;
            trailPositions.add(new int[]{x, y});
            if (trailPositions.size() > 30) {
                trailPositions.remove(0);
            }
            
            if (dashTimer >= dashDuration) {
                state = 2;
                waitTimer = 0;
                trailPositions.clear();
            }
        } else if (state == 2) {
            // Waiting state
            waitTimer++;
            if (waitTimer >= waitDuration) {
                state = 0;
                spinTimer = 0;
                spinAngle = 0;
                trailPositions.clear();
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
        return Math.abs(x - px) < 20 && Math.abs(y - py) < 20;
    }

    public void draw(Graphics2D g) {
        // Draw trail
        for (int i = 0; i < trailPositions.size(); i++) {
            int[] pos = trailPositions.get(i);
            float alpha = (float) i / trailPositions.size();
            g.setColor(new Color(180, 100, 200, (int)(alpha * 100)));
            g.fillRect(pos[0] - 5, pos[1] - 5, 10, 10);
        }
        
        // Draw sprite with rotation (same size as green triangles)
        var sprite = SpriteLoader.getSprite("purple_triangle");
        if (sprite != null) {
            // Calculate rotation angle
            double rotationAngle;
            if (state == 0) {
                rotationAngle = spinAngle;
            } else {
                // Add π/2 because sprite point faces up, but dashAngle is measured from right
                rotationAngle = dashAngle + Math.PI / 2;
            }
            
            // Save graphics transform
            AffineTransform originalTransform = g.getTransform();
            
            // Translate to position, rotate around center, then draw (40x40 like green triangle)
            g.translate(x, y);
            g.rotate(rotationAngle);
            g.drawImage(sprite, -20, -20, 40, 40, null);
            
            // Restore transform
            g.setTransform(originalTransform);
        }
    }

    public ArrayList<int[]> getTrailPositions() {
        return trailPositions;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
