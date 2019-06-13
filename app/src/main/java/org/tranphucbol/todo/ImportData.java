package org.tranphucbol.todo;

import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.tranphucbol.todo.Model.MTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ImportData {
    private Gson gson;
    private Context context;
    private MTaskDatabase mTaskDatabase;
    private static final String DATABASE_NAME = "mtasks_db";

    public ImportData(Context context) {
        this.context = context;
        gson = new Gson();
    }

    public void readData(Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        int value = 0;
        StringBuilder stringBuilder = new StringBuilder();

        while((value = reader.read()) != -1) {
            char c = (char) value;
            stringBuilder.append(c);
        }

        reader.close();
        inputStream.close();

        String json = stringBuilder.toString();
        Type collectionType = new TypeToken<Collection<MTask>>(){}.getType();
        List<MTask> mTasksInput = gson.fromJson(json, collectionType);

        mTaskDatabase = Room.databaseBuilder(context,
                MTaskDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();

        List<MTask> mTasks = mTaskDatabase.mTaskDAO().getAll();
        List<MTask> mergeMTask = new ArrayList<>();

        for(MTask input : mTasksInput) {
            if(!mTasks.contains(input)) {
                input.setTaskId(0);
                mergeMTask.add(input);
            }
        }

        List<Long> ids = mTaskDatabase.mTaskDAO().insertMTasks(mergeMTask);

        int i = 0;
        for(MTask task : mergeMTask) {
            task.setTaskId(ids.get(i++).intValue());
            InputTaskActivity.createNotification(context, task);
        }
    }
}
