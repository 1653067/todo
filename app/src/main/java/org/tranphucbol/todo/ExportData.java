package org.tranphucbol.todo;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;

import org.tranphucbol.todo.Model.MTask;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class ExportData {
    private Gson gson;
    private Context context;
    private MTaskDatabase mTaskDatabase;
    private static final String DATABASE_NAME = "mtasks_db";

    public ExportData(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public void writeFile(Uri uri) throws IOException {
        mTaskDatabase = Room.databaseBuilder(context,
                MTaskDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();

        List<MTask> mTasks = mTaskDatabase.mTaskDAO().getAll();

        OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        String mTaskJson = gson.toJson(mTasks);
        writer.write(mTaskJson);
        writer.close();
        outputStream.close();
    }
}
