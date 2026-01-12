import java.awt.*;
import java.util.ArrayList;

public class PurpleBoss {
    private int x, y;
    private int hp = 150;
    private int maxHp = 150;
    private int attackState = 0; // 0 = beam spam, 1 = beam spin, 2 = dash attack, 3 = spiral bullets
    private int attackTimer = 0;
    private int stateTransitionTimer = 0;
    private int screenWidth, screenHeight;
    private ArrayList<Beam> beams;
    private ArrayList<EnemyProjectile> spiralBullets;
    private ArrayList<Integer> dashAttackQueue; // Number of dashes to perform
    private int dashDashCount = 0; // Current dash count in sequence
    private int dashTimer = 0;
    private double dashAngle = 0;
    private double spiralAngle = 0;
    private boolean shieldActive = false;
    private int shieldTimer = 0;
    private int beamSpamCounter = 0;
    private double beamSpamAngle = 0;
    
    // Shield visual parameters
    private static final int SHIELD_SIZE = 120;
    private int dashHighlightTimer = 0;
    private static final int DASH_HIGHLIGHT_DURATION = 30;
    private double beamRotationAngle = 0; // For rotating beams in beam spin attack
    private ArrayList<Double> activeBeamAngles; // Beams that are rotating
    private ArrayList<RotatingBeam> persistentBeams; // Beams that stay active and rotate
    private ArrayList<int[]> dashTrail; // Trail of positions during dash
    private double bossRotation = 0; // For spinning during dash attack
    
    private int dashFinishTimer = 0; // visual cue timer after dash ends
    
    // Attack durations
    private static final int BEAM_SPAM_DURATION = 180; // 3 seconds
    private static final int BEAM_SPIN_DURATION = 400; // 6.67 seconds - longer for rotating beams
    private static final int DASH_ATTACK_DURATION = 300; // 5 seconds per dash attempt
    private static final int SPIRAL_DURATION = 180; // 3 seconds
    private static final int STATE_TRANSITION_DURATION = 60; // 1 second between attacks
    private static final int SHIELD_DURATION = 250; // Shield lasts 4.17 seconds

    public PurpleBoss(int x, int y, int screenWidth, int screenHeight) {
        this.x = x;
        this.y = y;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.beams = new ArrayList<>();
        this.spiralBullets = new ArrayList<>();
        this.dashAttackQueue = new ArrayList<>();
        this.activeBeamAngles = new ArrayList<>();
        this.persistentBeams = new ArrayList<>();
        this.dashTrail = new ArrayList<>();
    }

    public void setScreenSize(int w, int h) {
        this.screenWidth = w;
        this.screenHeight = h;
        // propagate to persistent beams
        for (RotatingBeam rb : persistentBeams) {
            rb.setScreenSize(w, h);
        }
    }

    public void update(int playerX, int playerY) {
        // Shield management
        if (shieldActive) {
            shieldTimer++;
        }

        // Boss slowly spins at all times
        bossRotation += 0.005;
        if (bossRotation > Math.PI * 2) bossRotation -= Math.PI * 2;

        // Dash finish cue countdown
        if (dashFinishTimer > 0) dashFinishTimer--;

        // Track state changes for debugging
        int prevState = attackState;

        if (attackState == 0) {
            // Beam spam attack
            updateBeamSpam(playerX, playerY);
        } else if (attackState == 1) {
            // Beam spin attack
            updateBeamSpin(playerX, playerY);
        } else if (attackState == 2) {
            // Dash attack
            updateDashAttack(playerX, playerY);
        } else if (attackState == 3) {
            // Spiral bullets attack
            updateSpiralBullets(playerX, playerY);
        }

        // Update beams
        for (Beam beam : beams) {
            beam.update();
        }
        beams.removeIf(Beam::isFinished);

        // Update state transition (do not transition while dash attack is active)
        if (attackState != 2) {
            stateTransitionTimer++;
            if (stateTransitionTimer >= STATE_TRANSITION_DURATION) {
                stateTransitionTimer = 0;
                // Move to next attack state
                attackState = (attackState + 1) % 4;
                attackTimer = 0;
                shieldActive = false;
                dashHighlightTimer = 0;
                dashTrail.clear(); // Clear trail when transitioning states
                persistentBeams.clear(); // Clear rotating beams when transitioning
            }
        } else {
            // While dashing, keep transition timer reset so no other attack starts
            stateTransitionTimer = 0;
        }
        // Debug: print when attack state changes
        if (prevState != attackState) {
            System.out.println("PurpleBoss: state changed " + prevState + " -> " + attackState + " (attackTimer=" + attackTimer + ")");
        }
    }

    private void updateBeamSpam(int playerX, int playerY) {
        attackTimer++;
        
        // Move around screen while firing beams
        x += Math.cos(beamSpamAngle) * 2;
        y += Math.sin(beamSpamAngle) * 2;
        
        // Keep boss on screen
        x = Math.max(60, Math.min(screenWidth - 60, x));
        y = Math.max(60, Math.min(screenHeight - 60, y));
        
        // Change direction occasionally
        if (attackTimer % 60 == 0) {
            beamSpamAngle = Math.random() * Math.PI * 2;
        }

        // Fire beams less frequently - every 30 frames instead of 15
        if (attackTimer % 30 == 0) {
            // Fire 1-2 beams in random directions
            int beamCount = 1 + (int)(Math.random() * 2);
            for (int i = 0; i < beamCount; i++) {
                double beamAngle = Math.random() * Math.PI * 2;
                Beam beam = new Beam(x, y, screenWidth, screenHeight, beamAngle);
                beam.setRemoveAfterFade(true);
                beams.add(beam);
            }
        }

        beamSpamCounter++;
        if (beamSpamCounter % 90 == 0 && attackTimer < BEAM_SPAM_DURATION - 30) {
            // Occasionally fire a volley toward player
            for (int i = 0; i < 3; i++) {
                double beamAngle = Math.atan2(playerY - y, playerX - x) + (i - 1.0) * 0.2;
                Beam beam = new Beam(x, y, screenWidth, screenHeight, beamAngle);
                beam.setRemoveAfterFade(true);
                beams.add(beam);
            }
        }

        if (attackTimer >= BEAM_SPAM_DURATION) {
            stateTransitionTimer = STATE_TRANSITION_DURATION - 1; // Force state transition
        }
    }

    private void updateBeamSpin(int playerX, int playerY) {
        attackTimer++;
        
        if (attackTimer < 100) {
            // Dash to center
            int targetX = screenWidth / 2;
            int targetY = screenHeight / 2;
            double dist = Math.sqrt(Math.pow(targetX - x, 2) + Math.pow(targetY - y, 2));
            if (dist > 5) {
                x += (targetX - x) / dist * 6;
                y += (targetY - y) / dist * 6;
            } else {
                x = targetX;
                y = targetY;
                shieldActive = true;
                shieldTimer = 0;
            }
        } else if (persistentBeams.isEmpty()) {
            // Create 8 beams in a circle that will persist and rotate (only once, when beams list is empty)
            for (int i = 0; i < 8; i++) {
                double angle = (i * Math.PI * 2 / 8);
                persistentBeams.add(new RotatingBeam(x, y, angle, screenWidth, screenHeight));
            }
            beamRotationAngle = 0;
            System.out.println("PurpleBoss: created persistent rotating beams at (" + x + "," + y + ")");
        } else if (attackTimer < BEAM_SPIN_DURATION) {
            // Rotate the beams
            beamRotationAngle += 0.02; // Slow rotation
            
            // Update persistent beam positions and rotation
            for (RotatingBeam beam : persistentBeams) {
                beam.update(x, y, beamRotationAngle);
                beam.setScreenSize(screenWidth, screenHeight);
            }
            
            // Periodically fire additional temporary beams
            if (attackTimer % 60 == 0 && attackTimer > 100) {
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Beam beam = new Beam(x, y, screenWidth, screenHeight, angle);
                    beam.setRemoveAfterFade(true);
                    beams.add(beam);
                }
            }
        }

        if (attackTimer >= BEAM_SPIN_DURATION) {
            stateTransitionTimer = STATE_TRANSITION_DURATION - 1;
            shieldActive = false;
            // Don't clear persistentBeams here - let state transition handle it
        }
    }

    private void updateDashAttack(int playerX, int playerY) {
        attackTimer++;
        
        // Calculate number of dashes based on HP percentage
        if (dashAttackQueue.isEmpty() && attackTimer < 50) {
            int hpPercent = (hp * 100) / maxHp;
            int dashCount = 1 + (100 - hpPercent) / 25; // 1-4 dashes as HP depletes
            for (int i = 0; i < dashCount; i++) {
                dashAttackQueue.add(i);
            }
        }

        if (!dashAttackQueue.isEmpty()) {
            if (dashHighlightTimer < DASH_HIGHLIGHT_DURATION) {
                // Highlight animation before dashing
                dashHighlightTimer++;
            } else {
                // Perform dash attack
                if (dashTimer == 0) {
                    // Start new dash - target PLAYER position, not same spot
                    dashAngle = Math.atan2(playerY - y, playerX - x);
                    dashTrail.clear();
                }

                dashTimer++;
                bossRotation += 0.15; // Spin during dash
                
                // Add to trail
                dashTrail.add(new int[]{x, y});
                if (dashTrail.size() > 30) {
                    dashTrail.remove(0);
                }
                
                // Move in dash direction
                x += Math.cos(dashAngle) * 14;
                y += Math.sin(dashAngle) * 14;

                // Fire bullets occasionally while dashing
                if (dashTimer % 20 == 0 && Math.random() > 0.5) {
                    // Random bullets or beams
                    if (Math.random() > 0.6) {
                        // Fire beam
                        double beamAngle = Math.random() * Math.PI * 2;
                        Beam beam = new Beam(x, y, screenWidth, screenHeight, beamAngle);
                        beam.setRemoveAfterFade(true);
                        beams.add(beam);
                    } else {
                        // Fire bullets in spread
                        for (int i = -1; i <= 1; i++) {
                            spiralBullets.add(new EnemyProjectile(x, y, dashAngle + i * 0.3, 5, Color.MAGENTA, 1));
                        }
                    }
                }

                // Check if dash hit border (with 50 pixel margin)
                if (x < -50 || x > screenWidth + 50 || y < -50 || y > screenHeight + 50) {
                    // remove this dash and prepare next
                    dashAttackQueue.remove(0);
                    dashTimer = 0;
                    // If there are more dashes queued, skip highlight to chain immediately
                    if (!dashAttackQueue.isEmpty()) {
                        dashHighlightTimer = DASH_HIGHLIGHT_DURATION; // treat highlight as already finished
                    } else {
                        dashHighlightTimer = 0;
                    }
                    // If all dashes done, trigger end-of-dash cue and transition
                    if (dashAttackQueue.isEmpty()) {
                        dashTrail.clear();
                        persistentBeams.clear();
                        // clamp back inside screen
                        x = Math.max(60, Math.min(screenWidth - 60, x));
                        y = Math.max(60, Math.min(screenHeight - 60, y));
                        // visual/audio cue
                        dashFinishTimer = 20;
                        try {
                            java.awt.Toolkit.getDefaultToolkit().beep();
                        } catch (Exception ex) {
                            // ignore if beep fails
                        }
                        // move to next attack state now that dash fully completed
                        attackState = (attackState + 1) % 4;
                        attackTimer = 0;
                    }
                }
            }
        }

        if (dashAttackQueue.isEmpty() && attackTimer >= DASH_ATTACK_DURATION) {
            stateTransitionTimer = STATE_TRANSITION_DURATION - 1;
            dashTrail.clear();
            // Bring boss back to screen
            x = Math.max(60, Math.min(screenWidth - 60, x));
            y = Math.max(60, Math.min(screenHeight - 60, y));
        }
    }

    private void updateSpiralBullets(int playerX, int playerY) {
        attackTimer++;
        
        // Fire bullets in expanding spiral
        if (attackTimer % 8 == 0) {
            int bulletCount = 8 + (attackTimer / 30);
            for (int i = 0; i < bulletCount; i++) {
                double angle = spiralAngle + (i * Math.PI * 2 / bulletCount);
                spiralBullets.add(new EnemyProjectile(x, y, angle, 4, Color.MAGENTA, 1));
            }
            spiralAngle += 0.2; // Rotate spiral
        }

        if (attackTimer >= SPIRAL_DURATION) {
            stateTransitionTimer = STATE_TRANSITION_DURATION - 1;
            spiralAngle = 0;
        }
    }

    public void spawnProjectiles(ArrayList<EnemyProjectile> projectiles) {
        projectiles.addAll(spiralBullets);
        spiralBullets.clear();
    }

    public void takeDamage(int damage) {
        if (!shieldActive) {
            hp -= damage;
        }
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public boolean collidesWith(int px, int py) {
        // First check body collision
        if (Math.abs(x - px) < 40 && Math.abs(y - py) < 40) return true;
        // Then check persistent rotating beams for contact damage
        for (RotatingBeam beam : persistentBeams) {
            if (beam.checkCollision(px, py)) return true;
        }
        return false;
    }

    public int getHP() { return hp; }
    public int getMaxHP() { return maxHp; }

    public void draw(Graphics2D g) {
        
        

        // Draw regular beams (behind)
        for (Beam beam : beams) {
            beam.draw(g);
        }

        // Draw dash trail (older positions = more faded and smaller)
        if (!dashTrail.isEmpty()) {
            for (int i = 0; i < dashTrail.size(); i++) {
                int[] pos = dashTrail.get(i);
                // Alpha increases toward end of trail (newer = brighter)
                float alpha = (float) (i + 1) / (dashTrail.size() + 1) * 0.8f;
                g.setColor(new Color(1f, 0f, 1f, alpha));
                // Size matches boss body (80 pixel diameter), decreases toward start of trail
                int size = 20 + (dashTrail.size() - i) * 2;
                g.fillOval(pos[0] - size / 2, pos[1] - size / 2, size, size);
            }
        }

        // Draw shield if active (full semi-transparent circle)
        if (shieldActive) {
            float alpha = 0.4f;
            g.setColor(new Color(0f, 0.6f, 1f, alpha));
            g.fillOval(x - SHIELD_SIZE / 2, y - SHIELD_SIZE / 2, SHIELD_SIZE, SHIELD_SIZE);
            g.setColor(new Color(0f, 0.8f, 1f, 0.7f));
            g.setStroke(new BasicStroke(3));
            g.drawOval(x - SHIELD_SIZE / 2, y - SHIELD_SIZE / 2, SHIELD_SIZE, SHIELD_SIZE);
        }

        // Draw highlight effect during dash charge
        if (attackState == 2 && dashHighlightTimer > 0) {
            float progress = (float) dashHighlightTimer / DASH_HIGHLIGHT_DURATION;
            g.setColor(new Color(1f, 1f, 0f, 0.3f * progress));
            int highlightSize = 80 + (int)(progress * 20);
            g.fillOval(x - highlightSize / 2, y - highlightSize / 2, highlightSize, highlightSize);
        }

        // Draw main boss body with rotation if dashing
        java.awt.geom.AffineTransform oldTransform = g.getTransform();
        
        if (Math.abs(bossRotation) > 0.01) {
            // Apply rotation for dash attack
            g.translate(x, y);
            g.rotate(bossRotation);
            g.translate(-x, -y);
        }

        // Giant spiked purple circle
        g.setColor(new Color(150, 0, 150));
        g.fillOval(x - 40, y - 40, 80, 80);
        g.setColor(Color.MAGENTA);
        g.setStroke(new BasicStroke(3));
        g.drawOval(x - 40, y - 40, 80, 80);

        // Draw spikes around the circle (12 triangle spikes)
        g.setColor(new Color(200, 50, 200));
        for (int i = 0; i < 12; i++) {
            double angle = (i * Math.PI * 2 / 12);
            int centerX = x + (int) (Math.cos(angle) * 45);
            int centerY = y + (int) (Math.sin(angle) * 45);
            int tipX = x + (int) (Math.cos(angle) * 70);
            int tipY = y + (int) (Math.sin(angle) * 70);
            
            // Calculate spike points (triangle)
            double perpAngle = angle + Math.PI / 2;
            int leftX = centerX + (int) (Math.cos(perpAngle) * 12);
            int leftY = centerY + (int) (Math.sin(perpAngle) * 12);
            int rightX = centerX - (int) (Math.cos(perpAngle) * 12);
            int rightY = centerY - (int) (Math.sin(perpAngle) * 12);
            
            // Fill spike triangle
            int[] xPoints = {leftX, rightX, tipX};
            int[] yPoints = {leftY, rightY, tipY};
            g.fillPolygon(xPoints, yPoints, 3);
            g.setColor(Color.MAGENTA);
            g.drawPolygon(xPoints, yPoints, 3);
            g.setColor(new Color(200, 50, 200));
        }

        g.setTransform(oldTransform);

        // Dash finish visual cue
        if (dashFinishTimer > 0) {
            float progress = (20 - dashFinishTimer) / 20.0f;
            int cueSize = 80 + (int)(progress * 80);
            float alpha = 0.8f * (1.0f - progress);
            g.setColor(new Color(1f, 0.9f, 0.4f, alpha));
            g.fillOval(x - cueSize / 2, y - cueSize / 2, cueSize, cueSize);
            g.setColor(new Color(1f, 0.9f, 0.4f, Math.min(0.9f, alpha + 0.2f)));
            g.setStroke(new BasicStroke(3));
            g.drawOval(x - cueSize / 2, y - cueSize / 2, cueSize, cueSize);
        }

        // HP bar drawing is handled by GamePanel to avoid duplicate UI elements
    }

    public void setRemoveAfterFade(boolean remove) {
        // This method exists for compatibility with Beam interface expectations
    }

    // Using external RotatingBeam class (RotatingBeam.java)
}
