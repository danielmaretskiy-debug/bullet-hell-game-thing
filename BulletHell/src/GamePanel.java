import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class GamePanel extends JPanel {
    private int WIDTH = 1000;
    private int HEIGHT = 700;
    private static final int FPS = 60;

    private Player player;
    private ArrayList<Enemy> enemies;
    private ArrayList<TriangleEnemy> triangleEnemies;
    private ArrayList<GreenTriangleEnemy> greenTriangleEnemies;
    private ArrayList<PurpleTriangleEnemy> purpleTriangleEnemies;
    private ArrayList<PurpleCircleEnemy> purpleCircleEnemies;
    private ArrayList<PlayerProjectile> playerProjectiles;
    private ArrayList<EnemyProjectile> enemyProjectiles;
    private ArrayList<HealingItem> healingItems;
    private Boss boss;
    private PurpleBoss purpleBoss;
    private int waveNumber = 1;
    private boolean waveInProgress = false;
    private int enemiesSpawned = 0;
    private boolean bossFight = false;
    private Set<Integer> keysPressed;
    private Point mousePos;
    private Achievements achievements;
    private int totalKills = 0;
    private int damageFreakoutWave = -1; // Track current no-damage wave
    private boolean isPaused = false;
    private PauseMenu pauseMenu;
    private ArrayList<AchievementPopup> achievementPopups;
    private GamePanelListener gamePanelListener;
    private boolean showingUpgradeMenu = false;
    private UpgradeMenu upgradeMenu;
    private int lastUpgradeWave = 0;

    public GamePanel(Achievements achievements) {
        this(achievements, 1); // Default to wave 1
    }

    public GamePanel(Achievements achievements, int startingWave) {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        this.achievements = achievements;
        this.waveNumber = startingWave;
        this.achievementPopups = new ArrayList<>();

        // Load sprites
        SpriteLoader.loadSprites();

        player = new Player(WIDTH / 2, HEIGHT / 2);
        enemies = new ArrayList<>();
        triangleEnemies = new ArrayList<>();
        greenTriangleEnemies = new ArrayList<>();
        purpleTriangleEnemies = new ArrayList<>();
        purpleCircleEnemies = new ArrayList<>();
        playerProjectiles = new ArrayList<>();
        enemyProjectiles = new ArrayList<>();
        
        pauseMenu = new PauseMenu(WIDTH, HEIGHT);
        pauseMenu.setListener(new PauseMenu.PauseMenuListener() {
            @Override
            public void onResumeClicked() {
                isPaused = false;
            }
            
            @Override
            public void onMainMenuClicked() {
                if (gamePanelListener != null) {
                    gamePanelListener.onReturnToMenu();
                }
            }
        });
        healingItems = new ArrayList<>();
        keysPressed = new HashSet<>();
        mousePos = new Point(WIDTH / 2, HEIGHT / 2);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    isPaused = !isPaused;
                    return;
                }
                keysPressed.add(e.getKeyCode());
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    int dirX = 0, dirY = 0;
                    if (keysPressed.contains(KeyEvent.VK_W)) dirY--;
                    if (keysPressed.contains(KeyEvent.VK_S)) dirY++;
                    if (keysPressed.contains(KeyEvent.VK_A)) dirX--;
                    if (keysPressed.contains(KeyEvent.VK_D)) dirX++;
                    
                    if (dirX == 0 && dirY == 0) {
                        dirX = 1;
                    }
                    player.dash(dirX, dirY);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keysPressed.remove(e.getKeyCode());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (showingUpgradeMenu && upgradeMenu != null) {
                    upgradeMenu.dispatchEvent(new MouseEvent(upgradeMenu, MouseEvent.MOUSE_CLICKED, 
                        System.currentTimeMillis(), 0, e.getX(), e.getY(), 1, false));
                } else if (isPaused) {
                    pauseMenu.dispatchEvent(new MouseEvent(pauseMenu, MouseEvent.MOUSE_CLICKED, 
                        System.currentTimeMillis(), 0, e.getX(), e.getY(), 1, false));
                } else {
                    player.shoot(playerProjectiles, e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePos = new Point(e.getX(), e.getY());
                if (showingUpgradeMenu && upgradeMenu != null) {
                    upgradeMenu.dispatchEvent(new MouseEvent(upgradeMenu, MouseEvent.MOUSE_MOVED, 
                        System.currentTimeMillis(), 0, e.getX(), e.getY(), 0, false));
                } else if (isPaused) {
                    pauseMenu.dispatchEvent(new MouseEvent(pauseMenu, MouseEvent.MOUSE_MOVED, 
                        System.currentTimeMillis(), 0, e.getX(), e.getY(), 0, false));
                }
            }
        });

        startWave();

        Timer timer = new Timer(1000 / FPS, e -> {
            updateDimensions();
            update();
            repaint();
        });
        timer.start();
    }

    private void updateDimensions() {
        int newWidth = getWidth();
        int newHeight = getHeight();
        if (newWidth > 0 && newHeight > 0) {
            WIDTH = newWidth;
            HEIGHT = newHeight;
            // Propagate size to boss if present
            if (purpleBoss != null) {
                purpleBoss.setScreenSize(WIDTH, HEIGHT);
            }
            // Update menu dimensions when screen size changes
            if (showingUpgradeMenu && upgradeMenu != null) {
                upgradeMenu.updateScreenSize(WIDTH, HEIGHT);
            }
            if (isPaused && pauseMenu != null) {
                pauseMenu.updateScreenSize(WIDTH, HEIGHT);
            }
        }
    }

    private void startWave() {
        // Show upgrade menu every 10 waves (at waves 11, 21, 31, etc.)
        if (waveNumber > 10 && waveNumber % 10 == 1 && waveNumber > lastUpgradeWave) {
            showingUpgradeMenu = true;
            isPaused = true;
            Upgrade[] upgrades = Upgrade.getRandomUpgrades(3);
            upgradeMenu = new UpgradeMenu(upgrades, WIDTH, HEIGHT);
            upgradeMenu.setListener(upgrade -> {
                upgrade.apply(player);
                showingUpgradeMenu = false;
                isPaused = false;
                lastUpgradeWave = waveNumber;
            });
        }
        
        // Check wave achievements
        if (waveNumber >= 5) {
            unlockAchievement("Wave Survivor");
        }
        if (waveNumber >= 10) {
            if (damageFreakoutWave != waveNumber - 1 || player.getHP() == 5) {
                unlockAchievement("Untouchable");
            }
            damageFreakoutWave = waveNumber;
        }
        if (waveNumber >= 20) {
            unlockAchievement("Wave Master");
        }
        if (waveNumber >= 30) {
            unlockAchievement("Legendary");
        }
        
        if (waveNumber % 10 == 0) {
            bossFight = true;
            if (waveNumber == 20) {
                // Second boss on wave 20
                purpleBoss = new PurpleBoss(WIDTH / 2, 150, WIDTH, HEIGHT);
            } else {
                // Regular boss on other waves (10, 30, etc.)
                boss = new Boss(WIDTH / 2, 100, waveNumber);
            }
        } else {
            bossFight = false;
            enemies.clear();
            triangleEnemies.clear();
            greenTriangleEnemies.clear();
            purpleTriangleEnemies.clear();
            purpleCircleEnemies.clear();
            enemiesSpawned = 0;
            
            // Scale enemy spawning based on wave
            // Regular enemies cap at lower numbers, triangles and green triangles increase
            int regularEnemies = Math.min(2 + waveNumber / 5, 3);
            if (!purpleCircleEnemies.isEmpty()) {
                regularEnemies = Math.max(0, regularEnemies - 2); // Further reduce when purple circles present
            }
            
            for (int i = 0; i < regularEnemies; i++) {
                enemies.add(Enemy.spawnRandom(WIDTH, HEIGHT, waveNumber));
            }
            
            // Spawn more triangle enemies as waves progress (reduced if purple circles are present)
            if (waveNumber >= 2) {
                int triangleCount = 1 + (waveNumber - 2) / 2;
                triangleCount = Math.min(triangleCount, 4);
                if (!purpleCircleEnemies.isEmpty()) {
                    triangleCount = Math.max(0, triangleCount - 2); // Further reduce when purple circles present
                }
                for (int i = 0; i < triangleCount; i++) {
                    int spawnX = WIDTH / 4 + (i % 3) * (WIDTH / 3) + (int)(Math.random() * 100 - 50);
                    triangleEnemies.add(new TriangleEnemy(spawnX, 30 + i * 20));
                }
            }
            
            // Spawn green triangle OR purple triangle enemies starting from wave 5
            if (waveNumber >= 5) {
                int triangleCount = 1 + (waveNumber - 5) / 3;
                triangleCount = Math.min(triangleCount, 5);
                if (!purpleCircleEnemies.isEmpty()) {
                    triangleCount = Math.max(1, triangleCount - 2); // Further reduce when purple circles present
                }
                
                // Use purple triangles starting from wave 15
                if (waveNumber >= 15) {
                    for (int i = 0; i < triangleCount; i++) {
                        int spawnX = WIDTH / 3 + (i % 2) * (WIDTH / 3) + (int)(Math.random() * 80 - 40);
                        int spawnY = HEIGHT / 3 + (int)(Math.random() * 60 - 30);
                        purpleTriangleEnemies.add(new PurpleTriangleEnemy(spawnX, spawnY));
                    }
                } else {
                    // Use green triangles before wave 15
                    for (int i = 0; i < triangleCount; i++) {
                        int spawnX = WIDTH / 3 + (i % 2) * (WIDTH / 3) + (int)(Math.random() * 80 - 40);
                        int spawnY = HEIGHT / 3 + (int)(Math.random() * 60 - 30);
                        greenTriangleEnemies.add(new GreenTriangleEnemy(spawnX, spawnY));
                    }
                }
            }
            
            // Spawn purple circle enemies starting from wave 11
            if (waveNumber >= 11) {
                int purpleCircleCount = 1 + (waveNumber - 11) / 5;
                purpleCircleCount = Math.min(purpleCircleCount, 3);
                for (int i = 0; i < purpleCircleCount; i++) {
                    int spawnX = WIDTH / 2 + (int)(Math.random() * 300 - 150);
                    int spawnY = HEIGHT / 2 + (int)(Math.random() * 150 - 75);
                    purpleCircleEnemies.add(new PurpleCircleEnemy(spawnX, spawnY, WIDTH, HEIGHT));
                }
            }
        }
        waveInProgress = true;
    }

    private void update() {
        // Update achievement popups
        for (int i = achievementPopups.size() - 1; i >= 0; i--) {
            AchievementPopup popup = achievementPopups.get(i);
            popup.update();
            if (popup.isFinished()) {
                achievementPopups.remove(i);
            }
        }
        
        // Skip game updates if paused
        if (isPaused) {
            return;
        }
        
        // Player movement
        player.update(keysPressed, WIDTH, HEIGHT);

        // Player projectile updates
        for (int i = playerProjectiles.size() - 1; i >= 0; i--) {
            PlayerProjectile proj = playerProjectiles.get(i);
            proj.update();
            if (proj.isOutOfBounds(WIDTH, HEIGHT)) {
                playerProjectiles.remove(i);
            }
        }

        // Enemy updates and projectile spawning
        if (!bossFight) {
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);
                enemy.update(WIDTH, HEIGHT, waveNumber, player.hasSlowField());
                enemy.spawnProjectiles(enemyProjectiles, player.getX(), player.getY(), waveNumber);

                // Check player projectile collisions
                for (int j = playerProjectiles.size() - 1; j >= 0; j--) {
                    PlayerProjectile proj = playerProjectiles.get(j);
                    if (enemy.collidesWith(proj.getX(), proj.getY())) {
                        enemy.takeDamage(1);
                        if (!proj.isPiercing()) {
                            playerProjectiles.remove(j);
                        }
                        break;
                    }
                }

                if (enemy.isDead()) {
                    totalKills++;
                    if (totalKills == 1) {
                        unlockAchievement("First Blood");
                    }
                    if (totalKills >= 50) {
                        unlockAchievement("Sharpshooter");
                    }
                    if (Math.random() < 0.25) {
                        healingItems.add(new HealingItem(enemy.getX(), enemy.getY()));
                    }
                    enemies.remove(i);
                }
            }

            if (enemies.isEmpty() && triangleEnemies.isEmpty() && greenTriangleEnemies.isEmpty() && purpleTriangleEnemies.isEmpty() && waveInProgress) {
                waveNumber++;
                startWave();
            }
        } else {
            // Boss updates
            if (waveNumber == 20 && purpleBoss != null) {
                // Purple boss fight
                purpleBoss.update(player.getX(), player.getY());
                purpleBoss.spawnProjectiles(enemyProjectiles);

                // Check player projectile collisions with purple boss
                for (int j = playerProjectiles.size() - 1; j >= 0; j--) {
                    PlayerProjectile proj = playerProjectiles.get(j);
                    if (purpleBoss.collidesWith(proj.getX(), proj.getY())) {
                        purpleBoss.takeDamage(1);
                        if (!proj.isPiercing()) {
                            playerProjectiles.remove(j);
                        }
                        break;
                    }
                }

                if (purpleBoss.isDead()) {
                    unlockAchievement("Second Boss");
                    waveNumber++;
                    startWave();
                }

                // Check player collision with purple boss
                if (purpleBoss.collidesWith(player.getX(), player.getY())) {
                    player.takeDamage(1);
                }
            } else {
                // Regular boss fight
                boss.update(WIDTH, HEIGHT, waveNumber);
                boss.spawnProjectiles(enemyProjectiles, player.getX(), player.getY(), WIDTH, HEIGHT);

                // Check player projectile collisions with boss
                for (int j = playerProjectiles.size() - 1; j >= 0; j--) {
                    PlayerProjectile proj = playerProjectiles.get(j);
                    if (boss.collidesWith(proj.getX(), proj.getY())) {
                        boss.takeDamage(1);
                        if (!proj.isPiercing()) {
                            playerProjectiles.remove(j);
                        }
                        break;
                    }
                }

                if (boss.isDead()) {
                    unlockAchievement("First Boss");
                    waveNumber++;
                    startWave();
                }
            }
        }

        // Triangle enemy updates
        for (int i = triangleEnemies.size() - 1; i >= 0; i--) {
            TriangleEnemy enemy = triangleEnemies.get(i);
            enemy.update(WIDTH, HEIGHT, player.hasSlowField());
            enemy.spawnProjectiles(enemyProjectiles, player.getX(), player.getY());

            // Check player projectile collisions
            for (int j = playerProjectiles.size() - 1; j >= 0; j--) {
                PlayerProjectile proj = playerProjectiles.get(j);
                if (enemy.collidesWith(proj.getX(), proj.getY())) {
                    enemy.takeDamage(1);
                    if (!proj.isPiercing()) {
                        playerProjectiles.remove(j);
                    }
                    break;
                }
            }

            if (enemy.isDead()) {
                if (Math.random() < 0.3) {
                    healingItems.add(new HealingItem(enemy.getX(), enemy.getY()));
                }
                triangleEnemies.remove(i);
            }
        }

        // Green triangle enemy updates
        for (int i = greenTriangleEnemies.size() - 1; i >= 0; i--) {
            GreenTriangleEnemy enemy = greenTriangleEnemies.get(i);
            enemy.update(WIDTH, HEIGHT, player.getX(), player.getY(), player.hasSlowField());

            // Check player projectile collisions
            for (int j = playerProjectiles.size() - 1; j >= 0; j--) {
                PlayerProjectile proj = playerProjectiles.get(j);
                if (enemy.collidesWith(proj.getX(), proj.getY())) {
                    enemy.takeDamage(1);
                    if (!proj.isPiercing()) {
                        playerProjectiles.remove(j);
                    }
                    break;
                }
            }

            if (enemy.isDead()) {
                if (Math.random() < 0.4) {
                    healingItems.add(new HealingItem(enemy.getX(), enemy.getY()));
                }
                greenTriangleEnemies.remove(i);
            }
        }

        // Purple triangle enemy updates
        for (int i = purpleTriangleEnemies.size() - 1; i >= 0; i--) {
            PurpleTriangleEnemy enemy = purpleTriangleEnemies.get(i);
            enemy.update(WIDTH, HEIGHT, player.getX(), player.getY(), player.hasSlowField());

            // Check player projectile collisions
            for (int j = playerProjectiles.size() - 1; j >= 0; j--) {
                PlayerProjectile proj = playerProjectiles.get(j);
                if (enemy.collidesWith(proj.getX(), proj.getY())) {
                    enemy.takeDamage(1);
                    if (!proj.isPiercing()) {
                        playerProjectiles.remove(j);
                    }
                    break;
                }
            }

            if (enemy.isDead()) {
                if (Math.random() < 0.4) {
                    healingItems.add(new HealingItem(enemy.getX(), enemy.getY()));
                }
                purpleTriangleEnemies.remove(i);
            }
        }

        // Purple circle enemy updates
        for (int i = purpleCircleEnemies.size() - 1; i >= 0; i--) {
            PurpleCircleEnemy enemy = purpleCircleEnemies.get(i);
            enemy.update(WIDTH, HEIGHT, player.hasSlowField());
            enemy.spawnProjectiles(enemyProjectiles);

            // Check player projectile collisions
            for (int j = playerProjectiles.size() - 1; j >= 0; j--) {
                PlayerProjectile proj = playerProjectiles.get(j);
                if (enemy.collidesWith(proj.getX(), proj.getY())) {
                    enemy.takeDamage(1);
                    playerProjectiles.remove(j);
                    break;
                }
            }

            if (enemy.isDead()) {
                totalKills++;
                if (totalKills >= 50) {
                    unlockAchievement("Sharpshooter");
                }
                if (Math.random() < 0.5) {
                    healingItems.add(new HealingItem(enemy.getX(), enemy.getY()));
                }
                purpleCircleEnemies.remove(i);
                unlockAchievement("Purple Hunter");
            }
        }

        // Healing item collision detection
        for (int i = healingItems.size() - 1; i >= 0; i--) {
            HealingItem item = healingItems.get(i);
            if (item.collidesWith(player.getX(), player.getY())) {
                player.heal(1);
                if (player.getHP() <= 5) {
                    int healingCount = 6 - player.getHP();
                    if (healingCount % 5 == 0) {
                        unlockAchievement("Medic");
                    }
                }
                healingItems.remove(i);
            }
        }

        // Enemy projectile updates
        for (int i = enemyProjectiles.size() - 1; i >= 0; i--) {
            EnemyProjectile proj = enemyProjectiles.get(i);
            proj.update();
            if (proj.isOutOfBounds(WIDTH, HEIGHT)) {
                enemyProjectiles.remove(i);
            } else if (player.collidesWith(proj.getX(), proj.getY())) {
                player.takeDamage(proj.getDamage());
                enemyProjectiles.remove(i);
            }
        }

        // Check if player is dead
        if (player.isDead()) {
            waveNumber = 1;
            player.reset(WIDTH / 2, HEIGHT / 2);
            enemies.clear();
            triangleEnemies.clear();
            greenTriangleEnemies.clear();
            enemyProjectiles.clear();
            playerProjectiles.clear();
            healingItems.clear();
            startWave();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw dash trail
        if (player.getTrailPositions().size() > 0) {
            for (int i = 0; i < player.getTrailPositions().size(); i++) {
                int[] pos = player.getTrailPositions().get(i);
                float alpha = (float) i / player.getTrailPositions().size();
                g2d.setColor(new Color(0, 0.5f, 1f, alpha * 0.5f));
                g2d.fillRect(pos[0] - 20, pos[1] - 20, 40, 40);
            }
        }

        // Draw player
        var playerSprite = SpriteLoader.getSprite("player");
        if (playerSprite != null) {
            g2d.drawImage(playerSprite, player.getX() - 20, player.getY() - 20, 40, 40, null);
        }

        // Draw dash cooldown meter below player (only show when cooling down)
        if (player.getDashCooldown() > 0) {
            int meterWidth = 30;
            int meterHeight = 4;
            int meterX = player.getX() - meterWidth / 2;
            int meterY = player.getY() + 15;
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(meterX, meterY, meterWidth, meterHeight);
            g2d.setColor(Color.GREEN);
            int fillWidth = (int) (meterWidth * (1.0 - (double) player.getDashCooldown() / player.getMaxDashCooldown()));
            g2d.fillRect(meterX, meterY, fillWidth, meterHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(meterX, meterY, meterWidth, meterHeight);
        }

        // Draw player projectiles
        var playerProjSprite = SpriteLoader.getSprite("player_projectile");
        for (PlayerProjectile proj : playerProjectiles) {
            if (playerProjSprite != null) {
                g2d.drawImage(playerProjSprite, proj.getX() - 6, proj.getY() - 6, 12, 12, null);
            }
        }

        // Draw enemies and boss
        if (!bossFight) {
            var enemySprite = SpriteLoader.getSprite("enemy");
            for (Enemy enemy : enemies) {
                if (enemySprite != null) {
                    g2d.drawImage(enemySprite, enemy.getX() - 20, enemy.getY() - 20, 40, 40, null);
                }
            }
            // Draw triangle enemies
            for (TriangleEnemy enemy : triangleEnemies) {
                enemy.draw(g2d);
            }
            // Draw green triangle enemies
            for (GreenTriangleEnemy enemy : greenTriangleEnemies) {
                enemy.draw(g2d);
            }
            // Draw purple triangle enemies
            for (PurpleTriangleEnemy enemy : purpleTriangleEnemies) {
                enemy.draw(g2d);
            }
            // Draw purple circle enemies
            for (PurpleCircleEnemy enemy : purpleCircleEnemies) {
                enemy.draw(g2d);
            }
        } else {
            if (waveNumber == 20 && purpleBoss != null) {
                purpleBoss.draw(g2d);
            } else {
                boss.draw(g2d);
            }
        }

        // Draw healing items
        for (HealingItem item : healingItems) {
            item.draw(g2d);
        }

        // Draw enemy projectiles
        var enemyProjSprite = SpriteLoader.getSprite("enemy_projectile");
        for (EnemyProjectile proj : enemyProjectiles) {
            if (enemyProjSprite != null) {
                int size = proj.getSize();
                g2d.drawImage(enemyProjSprite, proj.getX() - size, proj.getY() - size, size * 2, size * 2, null);
            }
        }

        // Draw UI
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("HP: " + player.getHP(), 20, 30);
        g2d.drawString("Wave: " + waveNumber, WIDTH - 150, 30);

        if (bossFight) {
            g2d.drawString("BOSS", WIDTH / 2 - 30, 30);
            
            // Draw boss health bar
            int barWidth = 300;
            int barHeight = 20;
            int barX = (WIDTH - barWidth) / 2;
            int barY = 50;
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(barX, barY, barWidth, barHeight);
            g2d.setColor(Color.RED);
            
            if (waveNumber == 20 && purpleBoss != null) {
                int healthWidth = (int) (barWidth * ((float) purpleBoss.getHP() / purpleBoss.getMaxHP()));
                g2d.fillRect(barX, barY, healthWidth, barHeight);
            } else if (boss != null) {
                int healthWidth = (int) (barWidth * boss.getHealthPercent());
                g2d.fillRect(barX, barY, healthWidth, barHeight);
            }
            
            g2d.setColor(Color.WHITE);
            g2d.drawRect(barX, barY, barWidth, barHeight);
        }
        
        // Draw achievement popups
        for (AchievementPopup popup : achievementPopups) {
            popup.draw(g2d);
        }
        
        // Draw upgrade menu if showing
        if (showingUpgradeMenu && upgradeMenu != null) {
            upgradeMenu.setSize(WIDTH, HEIGHT);
            upgradeMenu.paintComponent(g2d);
        }
        
        // Draw pause menu if paused
        if (isPaused && !showingUpgradeMenu) {
            pauseMenu.setSize(WIDTH, HEIGHT);
            pauseMenu.paintComponent(g2d);
        }
    }

    public void keyPressed(KeyEvent e) {
        keysPressed.add(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            int dirX = 0, dirY = 0;
            if (keysPressed.contains(KeyEvent.VK_W)) dirY--;
            if (keysPressed.contains(KeyEvent.VK_S)) dirY++;
            if (keysPressed.contains(KeyEvent.VK_A)) dirX--;
            if (keysPressed.contains(KeyEvent.VK_D)) dirX++;
            
            if (dirX == 0 && dirY == 0) {
                dirX = 1;
            }
            player.dash(dirX, dirY);
        }
        if (e.getKeyCode() == KeyEvent.VK_F11) {
            // Fullscreen toggle handled by Window
        }
    }

    public void keyReleased(KeyEvent e) {
        keysPressed.remove(e.getKeyCode());
    }

    private void unlockAchievement(String achievement) {
        if (achievements.unlock(achievement)) {
            achievementPopups.add(new AchievementPopup(achievement, WIDTH));
        }
    }

    public void setGamePanelListener(GamePanelListener listener) {
        this.gamePanelListener = listener;
    }

    public interface GamePanelListener {
        void onReturnToMenu();
    }
}
