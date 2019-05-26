package org.tranphucbol.todo.Model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.Date;

@Entity
public class MTask {

    @NonNull
    @PrimaryKey(autoGenerate=true)
    int taskId;
    String name;
    String content;
    boolean active;
    Date createdOn;
    Date deadline;

    public MTask() {
        this.active = false;
        this.createdOn = new Date();
    }

    public MTask(String name) {
        this.name = name;
        this.content = "";
        this.active = false;
        this.createdOn = new Date();
    }

    public MTask(String name, String content) {
        this.name = name;
        this.content = content;
        this.active = false;
        this.createdOn = new Date();
    }

    public MTask(String name, String content, Date deadline) {
        this.name = name;
        this.content = content;
        this.deadline = deadline;
        this.active = false;
        this.createdOn = new Date();
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
