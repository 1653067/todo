package org.tranphucbol.todo.DAO;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import org.tranphucbol.todo.Model.MTask;

import java.util.List;

@Dao
public interface MTaskDAO {
    @Insert
    long insertOnlySingleMTask(MTask task);
    @Query("SELECT * FROM MTask WHERE taskId = :taskId")
    MTask getById(int taskId);
    @Query("SELECT * FROM MTask")
    List<MTask> getAll();
    @Update
    void updateMTask(MTask task);
    @Delete
    void deleteMTask(MTask task);
}
