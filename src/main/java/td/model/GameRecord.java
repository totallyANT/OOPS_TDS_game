package td.model;

/**
 * Immutable record of one completed wave, displayed in the Match History table.
 */
public class GameRecord {

    private final int waveNumber;
    private final String playerName;
    private final String difficulty;
    private final int score;
    private final int remainingLives;
    private final String result;
    private final String timestamp;

    public GameRecord(int waveNumber, String playerName, String difficulty, int score,
                      int remainingLives, String result, String timestamp) {
        this.waveNumber = waveNumber;
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.score = score;
        this.remainingLives = remainingLives;
        this.result = result;
        this.timestamp = timestamp;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getScore() {
        return score;
    }

    public int getRemainingLives() {
        return remainingLives;
    }

    public String getResult() {
        return result;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
