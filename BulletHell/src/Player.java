import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Set;

public class Player {
    private int x, y;
    private int hp = 10;
    private int maxHp = 10;
    private int dashCooldown = 0;
    private int shootCooldown = 0;
    private static final int SHOOT_COOLDOWN_BASE = 8;
    private int dashDuration = 0;
    private int dashDirectionX = 0;
    private int dashDirectionY = 0;
    private ArrayList<int[]> trailPositions;
    private static final int DASH_SPEED = 10;
    private static final int MOVE_SPEED = 5;
    private static final int DASH_DURATION_MAX = 15;
    private int dashCooldownMax = 300;
    private int fireRateBonus = 0;
    private boolean piercingShots = false;
    private boolean tripleShot = false;
    private boolean slowFieldActive = false;
    private int damageCooldown = 0; // frames of invulnerability after taking damage
    private static final int DAMAGE_COOLDOWN_FRAMES = 30;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.trailPositions = new ArrayList<>();
    }

    public void update(Set<Integer> keysPressed, int width, int height) {
        int moveX = 0;
        int moveY = 0;
        
        if (keysPressed.contains(KeyEvent.VK_W)) moveY -= MOVE_SPEED;
        if (keysPressed.contains(KeyEvent.VK_S)) moveY += MOVE_SPEED;
        if (keysPressed.contains(KeyEvent.VK_A)) moveX -= MOVE_SPEED;
        if (keysPressed.contains(KeyEvent.VK_D)) moveX += MOVE_SPEED;

        if (dashDuration > 0) {
            x += dashDirectionX * DASH_SPEED;
            y += dashDirectionY * DASH_SPEED;
            trailPositions.add(new int[]{x, y});
            dashDuration--;
        } else {
            x += moveX;
            y += moveY;
            if (trailPositions.size() > 0) {
                trailPositions.remove(0);
            }
        }

        x = Math.max(10, Math.min(width - 10, x));
        y = Math.max(10, Math.min(height - 10, y));

        if (dashCooldown > 0) dashCooldown--;
        if (shootCooldown > 0) shootCooldown--;
        if (damageCooldown > 0) damageCooldown--;
    }

    public void dash(int directionX, int directionY) {
        if (dashCooldown == 0 && dashDuration == 0) {
            dashDuration = DASH_DURATION_MAX;
            dashCooldown = dashCooldownMax;
            dashDirectionX = directionX;
            dashDirectionY = directionY;
            trailPositions.clear();
        }
    }

    public boolean canShoot() {
        return shootCooldown <= 0;
    }

    public void shoot(ArrayList<PlayerProjectile> projectiles, int targetX, int targetY) {
        if (!canShoot()) return;
        
        double angle = Math.atan2(targetY - y, targetX - x);
        
        if (tripleShot) {
            // Fire 3 projectiles at slightly different angles
            projectiles.add(new PlayerProjectile(x, y, angle - 0.3, piercingShots));
            projectiles.add(new PlayerProjectile(x, y, angle, piercingShots));
            projectiles.add(new PlayerProjectile(x, y, angle + 0.3, piercingShots));
        } else {
            projectiles.add(new PlayerProjectile(x, y, angle, piercingShots));
        }
        
        // Apply fire rate bonus (higher bonus = shorter cooldown)
        shootCooldown = Math.max(2, SHOOT_COOLDOWN_BASE - fireRateBonus);
    }

    public void takeDamage(int damage) {
        if (dashDuration == 0 && damageCooldown == 0) {
            hp -= damage;
            damageCooldown = DAMAGE_COOLDOWN_FRAMES;
        }
    }

    public void heal(int amount) {
        hp = Math.min(5, hp + amount);
    }

    public boolean isDashing() {
        return dashDuration > 0;
    }

    public int getDashCooldown() {
        return dashCooldown;
    }

    public int getMaxDashCooldown() {
        return dashCooldownMax;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public void reset(int startX, int startY) {
        x = startX;
        y = startY;
        hp = 5;
        dashCooldown = 0;
    }

    public boolean collidesWith(int px, int py) {
        return Math.abs(x - px) < 20 && Math.abs(y - py) < 20;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getHP() { return hp; }
    public int getMaxHP() { return maxHp; }
    public ArrayList<int[]> getTrailPositions() { return trailPositions; }
    public boolean hasPiercingShots() { return piercingShots; }
    public boolean hasTripleShot() { return tripleShot; }
    public boolean hasSlowField() { return slowFieldActive; }
    public int getFireRateBonus() { return fireRateBonus; }
    
    public void increaseFireRate() {
        fireRateBonus += 5;
    }
    
    public void decreaseDashCooldown() {
        dashCooldownMax = Math.max(150, dashCooldownMax - 50);
    }
    
    public void increaseMaxHealth() {
        maxHp++;
        hp = maxHp;
    }
    
    public void setPiercingShots(boolean value) {
        piercingShots = value;
    }
    
    public void setTripleShot(boolean value) {
        tripleShot = value;
    }
    
    public void setSlowFieldActive(boolean value) {
        slowFieldActive = value;
    }
}
