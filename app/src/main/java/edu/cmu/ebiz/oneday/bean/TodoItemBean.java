package edu.cmu.ebiz.oneday.bean;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by julie on 8/9/15.
 */
public class TodoItemBean {
    private String title;
    private long lastStarted; // timestamp
    private long expectedTime; // in seconds
    private int status;
    private int timeLeft; // in seconds
    private int timeUsed; // in seconds

    public static final int NOT_STARTED = 0;
    public static final int STARTED = 1;
    public static final int STOPPED = 2;
    public static final int FINISHED = 3;
    public static final int DELETED = 4;
    public static final int DRY = 5;


    public TodoItemBean() {
        this.status = NOT_STARTED;
        this.timeUsed = 0;
    }

    /**
     * @param title         : description of this todo item
     * @param expectedTime: expectedTime in minutes
     */

    public TodoItemBean(String title, int expectedTime) {
        this.title = title;
        this.expectedTime = (long) (expectedTime) * 60 * 1000;
        this.timeLeft = expectedTime * 60; // in seconds
        this.status = NOT_STARTED;
        this.timeUsed = 0;
    }

    public void addTime(int addTime) {
        this.timeLeft += addTime;
        this.expectedTime += addTime * 60;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isFinished() {
        return this.status == FINISHED;
    }

    public void finish(boolean isFinished) {
        this.status = FINISHED;
    }

    public boolean isDeleted() {
        return this.status == DELETED;
    }

    public void delete() {
        this.status = DELETED;
    }


    public String getExpectedTime() {
        int minutes = (int) (expectedTime / 1000 / 60);
        int hour = minutes / 60;
        int min = minutes % 60;
        StringBuilder res = new StringBuilder();
        if (hour > 0) {
            res.append(hour);
        } else {
            res.append(0);
        }
        res.append(":");
        if (min > 10) {
            res.append(min);
        } else if (min > 0) {
            res.append("0");
            res.append(min);
        } else {
            res.append("00");
        }
        return res.toString();
    }

    public void setExpectedTime(int minutes) {
        this.expectedTime = (long) (minutes * 60 * 1000);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * get time left in seconds
     *
     * @return time left in seconds
     */
    public int getTimeLeft() {
        return this.timeLeft;
    }

    public String getTimeleftString() {
        return secondsToString(this.getTimeLeft());
    }

    public String getTimeUsedString() {
        return secondsToStringHHMM(this.getTimeUsed());
    }

    public void setTimeLeft(int timeleft) {
        this.timeLeft = timeleft;
    }

    public boolean countDown() {
        this.timeUsed++;
        this.timeLeft--;
        if (this.timeLeft > 0) {
            return true;
        } else {
            return false;
        }
    }

    private String secondsToString(int seconds) {
        int sign = 1;
        if (seconds < 0) {
            sign = -1;
            seconds = 0 - seconds;
        }
        int sec = seconds % 60;
        int min = seconds / 60;
        int hour = min / 60;
        min = min % 60;
        StringBuilder result = new StringBuilder();
        if (sign == -1) {
            result.append("-");
        }
        if (hour >= 10) {
            result.append(hour);
        } else {
            result.append("0");
            result.append(hour);
        }
        result.append(":");

        if (min >= 10) {
            result.append(min);
        } else {
            result.append("0");
            result.append(min);
        }
        result.append(":");

        if (sec >= 10) {
            result.append(sec);
        } else {
            result.append("0");
            result.append(sec);
        }
        return result.toString();
    }

    private String secondsToStringHHMM(int seconds) {
        int sign = 1;
        if (seconds < 0) {
            sign = -1;
            seconds = 0 - seconds;
        }
        int sec = seconds % 60;
        int min = seconds / 60;
        int hour = min / 60;
        min = min % 60;
        StringBuilder result = new StringBuilder();
        if (sign == -1) {
            result.append("-");
        }
        if (hour >= 10) {
            result.append(hour);
        } else {
            result.append("0");
            result.append(hour);
        }
        result.append(":");

        if (min >= 10) {
            result.append(min);
        } else {
            result.append("0");
            result.append(min);
        }

        return result.toString();
    }

    public String getEndTimeString() {
        if (this.status == STARTED) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.SECOND, timeLeft);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
            Date date = c.getTime();
            return simpleDateFormat.format(date);
        } else {
            return "";
        }

    }

    public int getTimeUsed() {
        return timeUsed;
    }

    public void setTimeUsed(int timeUsed) {
        this.timeUsed = timeUsed;
    }

    public void addTimeUsed(int timeused) {
        this.timeUsed += timeused;
    }


    public void onStarted() {
        lastStarted = System.currentTimeMillis();
        this.status = STARTED;
    }

    public void onStopped() {
        addTimeUsed((int) (System.currentTimeMillis() - lastStarted) / 1000);
        this.status = STOPPED;
    }
}
