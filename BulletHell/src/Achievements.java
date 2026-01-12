import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Achievements {
    private Map<String, Boolean> achievements;
    private static final String SAVE_FILE = "achievements.dat";

    public Achievements() {
        achievements = new HashMap<>();
        initializeAchievements();
        loadAchievements();
    }

    private void initializeAchievements() {
        achievements.put("First Blood", false);           // Kill first enemy
        achievements.put("Wave Survivor", false);         // Survive wave 5
        achievements.put("First Boss", false);           // Defeat a boss
        achievements.put("Medic", false);                 // Collect 5 healing items
        achievements.put("Wave Slayer", false);                // Survive 10 waves without taking damage
        achievements.put("Sharpshooter", false);          // Kill 50 enemies
        achievements.put("Wave Master", false);           // Reach wave 20
        achievements.put("Wave Obliterator", false);             // Reach wave 30
        achievements.put("Purple Hunter", false);         // Kill 3 purple circle enemies
        achievements.put("Untouchable", false);           // Reach wave 10 without damage
        achievements.put("Second Boss", false);           // Reach wave 10 without damage
    }

    public boolean unlock(String achievement) {
        if (achievements.containsKey(achievement) && !achievements.get(achievement)) {
            achievements.put(achievement, true);
            saveAchievements();
            return true;
        }
        return false;
    }

    public boolean isUnlocked(String achievement) {
        return achievements.getOrDefault(achievement, false);
    }

    public Map<String, Boolean> getAllAchievements() {
        return new HashMap<>(achievements);
    }

    public int getUnlockedCount() {
        return (int) achievements.values().stream().filter(v -> v).count();
    }

    public int getTotalCount() {
        return achievements.size();
    }

    private void saveAchievements() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(achievements);
        } catch (IOException e) {
            // Silently fail if save doesn't work
        }
    }

    @SuppressWarnings("unchecked")
    private void loadAchievements() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            Map<String, Boolean> loaded = (Map<String, Boolean>) ois.readObject();
            achievements.putAll(loaded);
        } catch (IOException | ClassNotFoundException e) {
            // Use defaults if no save file exists
        }
    }

    public void resetAchievements() {
        for (String key : achievements.keySet()) {
            achievements.put(key, false);
        }
        saveAchievements();
    }
}
