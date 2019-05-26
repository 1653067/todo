package org.tranphucbol.todo;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import org.tranphucbol.todo.DAO.MTaskDAO;
import org.tranphucbol.todo.Model.MTask;

@Database(entities = {MTask.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class MTaskDatabase extends RoomDatabase {
    public abstract MTaskDAO mTaskDAO();
}
