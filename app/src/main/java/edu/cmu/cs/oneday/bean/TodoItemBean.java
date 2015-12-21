package edu.cmu.cs.oneday.bean;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by julie on 8/9/15.
 */
public class TodoItemBean {
    private String title;
    private long lastStarted; // timestamp
    private Date expectEndTime; // timestamp
    private int expectDuration; // in seconds
    private int timeUsed; // in seconds
    private int status;


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
     * @param title           : description of this todo item
     * @param expectDurationMin: expectDuration in minutes
     */

    public TodoItemBean(String title, int expectDurationMin) {
        this.title = title;
        this.expectDuration = expectDurationMin * 60;
        this.timeUsed = 0;
    }

    // addTime in minutes
    public void addTime(int addMinutes) {
//        this.expectDuration += addMinutes * 60;
        Calendar c = Calendar.getInstance();
        c.setTime(this.expectEndTime);
        c.add(Calendar.MINUTE, addMinutes);
        this.expectEndTime = c.getTime();
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

    public int getExpectedDuration() {
        return this.expectDuration;
    }

    public String getExpectDurationString() {
        int minutes = expectDuration / 60;
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

    public void setExpectDuration(int seconds) {
        this.expectDuration = seconds;
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
        if (this.status == STARTED) {
            Calendar c = Calendar.getInstance();
            return (int) (expectEndTime.getTime() - Calendar.getInstance().getTimeInMillis()) / 1000;
        }
        else {
            return expectDuration - timeUsed;
        }
    }

    public String getTimeleftString() {
        return secondsToString(this.getTimeLeft());
    }

    public String getTimeUsedString() {
        return secondsToStringHHMM(this.getTimeUsed());
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

    public void setExpectEndTime(Date end) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(end);
        this.expectEndTime = cal.getTime();
    }

    public Date getExpectEndTime() {
        if (this.expectEndTime == null) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.SECOND,expectDuration - timeUsed);
            this.expectEndTime = c.getTime();
        }
        return this.expectEndTime;
    }

    public String getExpectedEndTimeString() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        return simpleDateFormat.format(expectEndTime);
    }


    public int getTimeUsed() {
        if (this.status == STARTED) {
            Calendar c = Calendar.getInstance();
            return (timeUsed + (int)(c.getTimeInMillis() - lastStarted)/1000);
        }
        else {
            return timeUsed;
        }

    }

    public void addTimeUsed(int timeused) {
        this.timeUsed += timeused;
    }

    public void start() {
        lastStarted = Calendar.getInstance().getTimeInMillis();
        updateExpectedEndTime();
        this.status = STARTED;
    }

    public void startDry() {
//        lastStarted = Calendar.getInstance().getTimeInMillis();
        updateExpectedEndTime();
        this.status = STARTED;
    }
    private void updateExpectedEndTime() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, expectDuration - timeUsed);
        this.expectEndTime = c.getTime();
    }
    public void setTimeUsed(int timeUsed) {
        this.timeUsed = timeUsed;
    }

    public void stop() {
        addTimeUsed((int) (System.currentTimeMillis() - lastStarted) / 1000);
        this.status = STOPPED;
    }


    public boolean isDry() {
        Calendar c = Calendar.getInstance();
        return c.getTime().after(expectEndTime);
    }
}
