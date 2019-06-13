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
    Date date;
    Date createdOn;
    Date deadline;
    boolean autoAdd;
    int before;
    boolean important;
    Date updatedOn;

    public MTask() {
        this.active = false;
        this.createdOn = new Date();
        this.autoAdd = false;
        this.before = 0;
        this.important = false;
        this.updatedOn = new Date();
    }

    public MTask(String name) {
        this.name = name;
        this.content = "";
        this.active = false;
        this.createdOn = new Date();
        this.autoAdd = false;
        this.before = 0;
        this.important = false;
        this.updatedOn = new Date();
    }

    public MTask(String name, String content, boolean autoAdd) {
        this.name = name;
        this.content = content;
        this.active = false;
        this.createdOn = new Date();
        this.autoAdd = autoAdd;
        this.before = 0;
        this.important = false;
        this.updatedOn = new Date();
    }

    public MTask(String name, String content, Date deadline, boolean autoAdd) {
        this.name = name;
        this.content = content;
        this.deadline = deadline;
        this.active = false;
        this.createdOn = new Date();
        this.autoAdd = autoAdd;
        this.before = 0;
        this.important = false;
        this.updatedOn = new Date();
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
        this.updatedOn = new Date();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedOn = new Date();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedOn = new Date();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedOn = new Date();
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
        this.updatedOn = new Date();
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
        this.updatedOn = new Date();
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        this.updatedOn = new Date();
    }

    public boolean isAutoAdd() {
        return autoAdd;
    }

    public int getBefore() {
        return before;
    }

    public void setBefore(int before) {
        this.before = before;
        this.updatedOn = new Date();
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
        this.updatedOn = new Date();
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public void setAutoAdd(boolean autoAdd) {
        this.autoAdd = autoAdd;
        this.updatedOn = new Date();
    }

    @Override
    public boolean equals(Object obj) {
        MTask mTask = (MTask)obj;
        return (name.equals(mTask.getName()) && createdOn.equals(mTask.getCreatedOn()));
    }

    @Override
    public String toString() {
        return this.name;
    }
}
