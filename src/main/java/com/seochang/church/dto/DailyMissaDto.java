package com.seochang.church.dto;

import java.util.ArrayList;
import java.util.List;

public class DailyMissaDto {
    private String title;
    private String date; // yyyy-MM-dd
    private String dateText; // e.g., 2026년 7월 27일 월요일
    private String liturgicalDay; // e.g., [(녹) 연중 제17주간 월요일]
    private String prevDate; // yyyy-MM-dd
    private String nextDate; // yyyy-MM-dd
    private List<Reading> readings = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDateText() { return dateText; }
    public void setDateText(String dateText) { this.dateText = dateText; }

    public String getLiturgicalDay() { return liturgicalDay; }
    public void setLiturgicalDay(String liturgicalDay) { this.liturgicalDay = liturgicalDay; }

    public String getPrevDate() { return prevDate; }
    public void setPrevDate(String prevDate) { this.prevDate = prevDate; }

    public String getNextDate() { return nextDate; }
    public void setNextDate(String nextDate) { this.nextDate = nextDate; }

    public List<Reading> getReadings() {
        return readings;
    }

    public void setReadings(List<Reading> readings) {
        this.readings = readings;
    }

    public void addReading(String type, String content) {
        this.readings.add(new Reading(type, content));
    }

    public static class Reading {
        private String type;
        private String content;

        public Reading(String type, String content) {
            this.type = type;
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
