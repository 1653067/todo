package org.tranphucbol.todo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.design.button.MaterialButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import org.tranphucbol.todo.Model.MTask;

import java.util.List;

public class ToDoListAdapter extends BaseAdapter {

    private List<MTask> mTasks;
    private LayoutInflater layoutInflater;
    private Context context;
    private MTaskDatabase mTaskDatabase;
    private static final String DATABASE_NAME = "mtasks_db";
    private Handler mHandler;

    public ToDoListAdapter(List<MTask> mTasks, Context context, Handler mHandler) {
        this.mTasks = mTasks;
        this.layoutInflater = LayoutInflater.from(context);
        this.context = context;
        this.mHandler = mHandler;
        mTaskDatabase = Room.databaseBuilder(context,
                MTaskDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Override
    public int getCount() {
        return mTasks.size();
    }

    @Override
    public Object getItem(int position) {
        return mTasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.to_do_item, null);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.checkBox);
            holder.taskNameTxtv = convertView.findViewById(R.id.taskNameTxtv);
            holder.deleteBtn = convertView.findViewById(R.id.deleteBtn);

            final MTask task = mTasks.get(position);

            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            task.setActive(!task.isActive());
                            mTaskDatabase.mTaskDAO().updateMTask(task);
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

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MTask task = (MTask)this.getItem(position);
        holder.taskNameTxtv.setText(task.getName());
        holder.checkBox.setChecked(task.isActive());

        return convertView;
    }

    static class ViewHolder {
        MaterialButton deleteBtn;
        TextView taskNameTxtv;
        CheckBox checkBox;
    }

    private void removeNotification(int id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent notificationIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(broadcast);
    }
}
