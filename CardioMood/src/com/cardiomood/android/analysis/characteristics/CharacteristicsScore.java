package com.cardiomood.android.analysis.characteristics;

/**
 *
 * @author rogvold
 */
public class CharacteristicsScore {
    
    private String description;
    private Integer score;
    private String rusDescription;
    private String summary;
    
    public CharacteristicsScore(Integer score, String description, String rusDescription) {
        this.description = description;
        this.score = score;
        this.rusDescription = rusDescription;
    }

    public CharacteristicsScore(Integer score, String description) {
        this.description = description;
        this.score = score;
    }
    
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getRusDescription() {
        return rusDescription;
    }

    public void setRusDescription(String rusDescription) {
        this.rusDescription = rusDescription;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    
    
    
}
