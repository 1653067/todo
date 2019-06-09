package org.tranphucbol.todo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.design.button.MaterialButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.tranphucbol.todo.Model.MTask;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>{

    private List<MTask> mTasks;
    private MTaskDatabase mTaskDatabase;
    private static final String DATABASE_NAME = "mtasks_db";
    private Handler mHandler;
    private Context context;

    public RecyclerViewAdapter(List<MTask> mTasks, Context context, Handler mHandler) {
        this.context = context;
        this.mHandler = mHandler;
        this.mTasks = mTasks;

        mTaskDatabase = Room.databaseBuilder(context,
                MTaskDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.to_do_item, viewGroup, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int i) {
        final MTask task = mTasks.get(i);

        holder.taskNameTxtv.setText(task.getName());
        holder.checkBox.setChecked(task.isActive());

        //Set event click for item -> start activity update and remove task
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, InputTaskActivity.class);
                intent.putExtra("taskId", task.getTaskId());
                context.startActivity(intent);
            }
        });

        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //change activate of task
                        task.setActive(!task.isActive());
                        mTaskDatabase.mTaskDAO().updateMTask(task);
                        //remove notification
                        removeNotification(task.getTaskId());
                    }
                }).start();
            }
        });

        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mTaskDatabase.mTaskDAO().deleteMTask(task);
                        mTasks.remove(task);
                        removeNotification(task.getTaskId());
                        mHandler.obtainMessage(1, MainActivity.REFRESH).sendToTarget();
                    }
                }).start();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        MaterialButton deleteBtn;
        TextView taskNameTxtv;
        CheckBox checkBox;
        public RecyclerViewHolder(View itemView) {
            super(itemView);

            deleteBtn = itemView.findViewById(R.id.deleteBtn);
            taskNameTxtv = itemView.findViewById(R.id.taskNameTxtv);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    private void removeNotification(int id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent notificationIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(broadcast);
    }
}
