package com.risk.rule;

/**
 * 风险等级枚举
 */
public enum RiskLevel {
    LOW(10, "低风险"),
    MEDIUM(50, "中风险"),
    HIGH(80, "高风险"),
    CRITICAL(100, "严重风险");

    private final int score;
    private final String description;

    RiskLevel(int score, String description) {
        this.score = score;
        this.description = description;
    }

    public int getScore() {
        return score;
    }

    public String getDescription() {
        return description;
    }

    public static RiskLevel fromScore(int score) {
        if (score >= 80) {
            return HIGH;
        } else if (score >= 50) {
            return MEDIUM;
        } else if (score >= 10) {
            return LOW;
        }
        return LOW;
    }

    @Override
    public String toString() {
        return "RiskLevel{" +
                "score=" + score +
                ", description='" + description + '\'' +
                '}';
    }
}