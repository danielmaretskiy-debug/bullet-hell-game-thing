import java.awt.*;
import java.util.ArrayList;

public class PurpleBoss {
    private int x, y;
    private int hp = 250;
    private int maxHp = 250;
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
    private double dashSpeed = 16.0; // variable dash speed that decays on bounce
    private int dashBounces = 0; // count bounces during a single dash to prevent infinite bouncing
    private int wallHitTimer = 0;
    private int wallHitX = 0;
    private int wallHitY = 0;
    // Per-dash scaling knobs (computed when a dash sequence is queued)
    private int currentDashAllowedBounces = 3;
    private int currentDashMaxFrames = DASH_MAX_FRAMES;
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
    private int rotatingBeamCueTimer = 0; // visual cue when rotating beams spawn
    
    // Attack durations
    private static final int BEAM_SPAM_DURATION = 180; // 3 seconds
    private static final int BEAM_SPIN_DURATION = 400; // 6.67 seconds - longer for rotating beams
    private static final int DASH_ATTACK_DURATION = 300; // 5 seconds per dash attempt
    private static final int DASH_MAX_FRAMES = 80; // cap per individual dash
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
        bossRotation += 0.01;
        if (bossRotation > Math.PI * 2) bossRotation -= Math.PI * 2;

        // Keep boss on-screen each tick to avoid getting stuck offscreen
        x = Math.max(60, Math.min(screenWidth - 60, x));
        y = Math.max(60, Math.min(screenHeight - 60, y));

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
        // Only increment the transition timer if it has been primed (>0).
        if (attackState != 2) {
            if (stateTransitionTimer > 0) {
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
            }
        } else {
            // While dashing, keep transition timer reset so no other attack starts
            stateTransitionTimer = 0;
        }
        // Debug: print when attack state changes
        if (prevState != attackState) {
            System.out.println("PurpleBoss: state changed " + prevState + " -> " + attackState + " (attackTimer=" + attackTimer + ")");
        }
        if (rotatingBeamCueTimer > 0) rotatingBeamCueTimer--;
        if (wallHitTimer > 0) wallHitTimer--;
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

        // Fire beams less frequently - fewer beams but wider spread
        if (attackTimer % 25 == 0) {
            // Fire 2-4 beams in random directions (reduced count)
            int beamCount = 2 + (int)(Math.random() * 3);
            for (int i = 0; i < beamCount; i++) {
                double beamAngle = Math.random() * Math.PI * 2;
                Beam beam = new Beam(x, y, screenWidth, screenHeight, beamAngle);
                beam.setRemoveAfterFade(true);
                beams.add(beam);
            }
        }

        beamSpamCounter++;
        if (beamSpamCounter % 90 == 0 && attackTimer < BEAM_SPAM_DURATION - 30) {
            // Occasionally fire a wider-spread volley toward player (fewer beams, bigger spread)
            for (int i = 0; i < 2; i++) {
                double offset = (i == 0) ? -0.6 : 0.6; // wider spread (~34 degrees)
                double beamAngle = Math.atan2(playerY - y, playerX - x) + offset;
                Beam beam = new Beam(x, y, screenWidth, screenHeight, beamAngle);
                beam.setRemoveAfterFade(true);
                beams.add(beam);
            }
        }

        if (attackTimer >= BEAM_SPAM_DURATION) {
            // Immediately move into beam-spin state so boss will dash to center
            attackState = 1;
            attackTimer = 0;
            stateTransitionTimer = 0;
            shieldActive = false;
            dashHighlightTimer = 0;
            persistentBeams.clear();
            System.out.println("PurpleBoss: transitioning to beam-spin (state=1)");
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
        } else if (attackTimer >= 100 && persistentBeams.isEmpty()) {
            // After dashing to center, create a smaller number of persistent rotating rectangular beams once
            int count = 6; // fewer beams for wider gaps
            for (int i = 0; i < count; i++) {
                double angle = (i * Math.PI * 2 / count);
                persistentBeams.add(new RotatingBeam(x, y, angle, screenWidth, screenHeight));
            }
            beamRotationAngle = 0;
            System.out.println("PurpleBoss: created persistent rotating beams at (" + x + "," + y + ")");
            rotatingBeamCueTimer = 60; // one second visual cue
        } else if (attackTimer < BEAM_SPIN_DURATION) {
            // Smooth slow rotation; slowed down overall and modestly faster at low HP
            double baseSpeed = 0.002; // much slower baseline
            if (hp < maxHp / 3) baseSpeed = 0.006; // low HP -> slightly faster
            else if (hp < (maxHp * 2) / 3) baseSpeed = 0.004; // mid HP
            beamRotationAngle += baseSpeed;

            // Update persistent beam positions and rotation
            for (RotatingBeam beam : persistentBeams) {
                beam.update(x, y, beamRotationAngle);
                beam.setScreenSize(screenWidth, screenHeight);
            }

            // Periodically fire additional temporary beams (less often)
            if (attackTimer % 50 == 0 && attackTimer > 40) {
                for (int i = 0; i < 2; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Beam beam = new Beam(x, y, screenWidth, screenHeight, angle);
                    beam.setRemoveAfterFade(true);
                    beams.add(beam);
                }
            }

            // Occasionally fire bullets outward while beams are active
            if (attackTimer % 30 == 0) {
                int shots = 6;
                for (int i = 0; i < shots; i++) {
                    double a = beamRotationAngle + (i * Math.PI * 2 / shots);
                    spiralBullets.add(new EnemyProjectile(x, y, a, 4, Color.MAGENTA, 1));
                }
            }
        }

        // End the beam-spin cleanly when duration is reached
        if (attackTimer >= BEAM_SPIN_DURATION) {
            System.out.println("PurpleBoss: beam spin complete, clearing beams and advancing state");
            persistentBeams.clear();
            shieldActive = false;
            // Advance to next state immediately
            attackState = (attackState + 1) % 4;
            attackTimer = 0;
            stateTransitionTimer = 0;
            dashHighlightTimer = 0;
            dashTrail.clear();
        }
    }

    private void updateDashAttack(int playerX, int playerY) {
        attackTimer++;
        
        // Calculate number of dashes based on HP percentage
        if (dashAttackQueue.isEmpty() && attackTimer < 50) {
            int hpPercent = (hp * 100) / maxHp;
            int dashCount = 2 + (100 - hpPercent) / 5; // base 2-5 dashes as HP depletes
            // Increase dash aggressiveness when low HP
            int extraDashes = (maxHp - hp) / (Math.max(1, maxHp / 6)); // 0..~5
            dashCount += extraDashes;
            for (int i = 0; i < dashCount; i++) {
                dashAttackQueue.add(i);
            }

            // Compute per-dash scaling: allowed bounces and max frames increase as HP drops
            int extra = (maxHp - hp) * 3 / Math.max(1, maxHp); // 0..3 roughly
            currentDashAllowedBounces = 1 + extra; // at least 1 bounce required
            currentDashMaxFrames = DASH_MAX_FRAMES + extra * 40; // longer dash cap at low HP
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
                    dashBounces = 0;
                    dashSpeed = 16.0;
                }

                dashTimer++;
                bossRotation += 0.15; // Spin during dash
                
                // Add to trail
                dashTrail.add(new int[]{x, y});
                if (dashTrail.size() > 30) {
                    dashTrail.remove(0);
                }
                
                // Move in dash direction using variable speed
                x += Math.cos(dashAngle) * dashSpeed;
                y += Math.sin(dashAngle) * dashSpeed;

                // Fire bullets occasionally while dashing
                if (dashTimer % 20 == 0 && Math.random() > 0.3) {
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

                // Bounce off screen bounds (use boss margins) instead of exiting the screen
                boolean bounced = false;
                int leftBound = 60;
                int rightBound = screenWidth - 60;
                int topBound = 60;
                int bottomBound = screenHeight - 60;
                if (x <= leftBound) {
                    x = leftBound;
                    dashAngle = Math.PI - dashAngle;
                    bounced = true;
                } else if (x >= rightBound) {
                    x = rightBound;
                    dashAngle = Math.PI - dashAngle;
                    bounced = true;
                }
                if (y <= topBound) {
                    y = topBound;
                    dashAngle = -dashAngle;
                    bounced = true;
                } else if (y >= bottomBound) {
                    y = bottomBound;
                    dashAngle = -dashAngle;
                    bounced = true;
                }

                if (bounced) {
                    dashBounces++;
                    // Slow down on bounce (less severe)
                    dashSpeed *= 0.85;
                    // nudge away from edge to avoid immediate re-collision
                    x += Math.cos(dashAngle) * 8;
                    y += Math.sin(dashAngle) * 8;
                    // Wall-hit effect: small radial burst and flash
                    wallHitTimer = 12;
                    wallHitX = x;
                    wallHitY = y;
                    for (int p = 0; p < 6; p++) {
                        double pa = Math.random() * Math.PI * 2;
                        int psz = 2 + (int)(Math.random() * 2);
                        spiralBullets.add(new EnemyProjectile(x, y, pa, psz, Color.CYAN, 0));
                    }
                    try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Exception ex) {}
                    // If we've hit the allowed number of bounces for this dash, treat it as completed
                    if (dashBounces >= currentDashAllowedBounces) {
                        if (!dashAttackQueue.isEmpty()) dashAttackQueue.remove(0);
                        dashTimer = 0;
                        if (!dashAttackQueue.isEmpty()) {
                            dashHighlightTimer = DASH_HIGHLIGHT_DURATION;
                        } else {
                            // end of dash sequence: perform a short bullet/beam spam, then return to beam-spam state
                            dashHighlightTimer = 0;
                            dashTrail.clear();
                            persistentBeams.clear();
                            dashFinishTimer = 20;
                            try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Exception ex) {}
                            // spawn a short spam burst then go to spiral attack
                            for (int s = 0; s < 6; s++) {
                                double a = Math.random() * Math.PI * 2;
                                Beam b = new Beam(x, y, screenWidth, screenHeight, a);
                                b.setRemoveAfterFade(true);
                                beams.add(b);
                            }
                            for (int s = 0; s < 8; s++) {
                                double a = Math.random() * Math.PI * 2;
                                spiralBullets.add(new EnemyProjectile(x, y, a, 4, Color.MAGENTA, 1));
                            }
                            // advance to spiral attack next
                            attackState = 3;
                            attackTimer = 0;
                            stateTransitionTimer = 0;
                        }
                    }
                }
                // Also end dash if it has run too long without sufficient bounces
                if (dashTimer >= DASH_MAX_FRAMES) {
                    if (!dashAttackQueue.isEmpty()) dashAttackQueue.remove(0);
                    dashTimer = 0;
                    dashHighlightTimer = 0;
                    dashTrail.clear();
                    persistentBeams.clear();
                    dashFinishTimer = 20;
                    try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Exception ex) {}
                    for (int s = 0; s < 4; s++) {
                        double a = Math.random() * Math.PI * 2;
                        Beam b = new Beam(x, y, screenWidth, screenHeight, a);
                        b.setRemoveAfterFade(true);
                        beams.add(b);
                    }
                    for (int s = 0; s < 6; s++) {
                        double a = Math.random() * Math.PI * 2;
                        spiralBullets.add(new EnemyProjectile(x, y, a, 4, Color.MAGENTA, 1));
                    }
                    attackState = 3;
                    attackTimer = 0;
                    stateTransitionTimer = 0;
                }
            }
        }

        if (dashAttackQueue.isEmpty() && attackTimer >= DASH_ATTACK_DURATION) {
            // Prime transition after dash attack completes
            // If dash loop runs too long, end the sequence and do a short spam, then return to beam-spam
            dashTrail.clear();
            x = Math.max(60, Math.min(screenWidth - 60, x));
            y = Math.max(60, Math.min(screenHeight - 60, y));
            for (int s = 0; s < 4; s++) {
                double a = Math.random() * Math.PI * 2;
                Beam b = new Beam(x, y, screenWidth, screenHeight, a);
                b.setRemoveAfterFade(true);
                beams.add(b);
            }
            for (int s = 0; s < 6; s++) {
                double a = Math.random() * Math.PI * 2;
                spiralBullets.add(new EnemyProjectile(x, y, a, 4, Color.MAGENTA, 1));
            }
            attackState = 0;
            attackTimer = 0;
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
            // End spiral attack and transition immediately
            System.out.println("PurpleBoss: spiral complete, advancing state");
            spiralAngle = 0;
            persistentBeams.clear();
            // Advance to next state
            attackState = (attackState + 1) % 4;
            attackTimer = 0;
            stateTransitionTimer = 0;
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
        // Then check regular timed beams
        for (Beam b : beams) {
            try {
                if (b.checkCollision(px, py)) return true;
            } catch (Exception ex) {
                // in case beam lacks method or errors, ignore
            }
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

        // Draw persistent rotating beams (spawned during beam spin)
        for (RotatingBeam beam : persistentBeams) {
            try {
                beam.draw(g);
            } catch (Exception ex) {
                // ignore drawing errors for safety
            }
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

        // Rotating-beam spawn visual cue (pulsing magenta ring)
        if (rotatingBeamCueTimer > 0) {
            float prog = rotatingBeamCueTimer / 60.0f;
            float alpha = 0.7f * prog;
            int cueSize = 120 + (int)((1.0f - prog) * 60);
            g.setColor(new Color(1f, 0f, 1f, alpha));
            g.fillOval(x - cueSize / 2, y - cueSize / 2, cueSize, cueSize);
            g.setColor(new Color(1f, 0f, 1f, Math.min(0.95f, alpha + 0.2f)));
            g.setStroke(new BasicStroke(3));
            g.drawOval(x - cueSize / 2, y - cueSize / 2, cueSize, cueSize);
        }

        // Wall-hit flash effect
        if (wallHitTimer > 0) {
            float prog = (float) wallHitTimer / 12.0f;
            float alpha = 0.9f * prog;
            int size = 40 + (int) ((1.0f - prog) * 80);
            g.setColor(new Color(0.4f, 0.9f, 1f, alpha));
            g.fillOval(wallHitX - size / 2, wallHitY - size / 2, size, size);
        }

        // HP bar drawing is handled by GamePanel to avoid duplicate UI elements
    }

    public void setRemoveAfterFade(boolean remove) {
        // This method exists for compatibility with Beam interface expectations
    }

    // Using external RotatingBeam class (RotatingBeam.java)
}
