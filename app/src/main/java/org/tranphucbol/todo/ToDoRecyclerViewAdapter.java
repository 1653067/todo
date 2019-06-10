package org.tranphucbol.todo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.button.MaterialButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.tranphucbol.todo.Model.MTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ToDoRecyclerViewAdapter extends RecyclerView.Adapter<ToDoRecyclerViewAdapter.RecyclerViewHolder> {

    private List<MTask> mTasks;
    private MTaskDatabase mTaskDatabase;
    private static final String DATABASE_NAME = "mtasks_db";
    private Handler mHandler;
    private Context context;
    private SimpleDateFormat ft;
    private MTask recentlyDeletedItem;
    private int recentlyDeletedItemPosition;

    public ToDoRecyclerViewAdapter(List<MTask> mTasks, Context context, Handler mHandler) {
        this.context = context;
        this.mHandler = mHandler;
        this.mTasks = mTasks;

        mTaskDatabase = Room.databaseBuilder(context,
                MTaskDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();

        ft = new SimpleDateFormat("dd-MM-yyyy");
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

        ft.applyPattern("HH:mm, EEE, dd MMM");
        if(task.getDeadline() != null) {
            holder.taskTimeTxtv.setVisibility(View.VISIBLE);
            holder.taskTimeTxtv.setText(ft.format(task.getDeadline()));
        } else {
            holder.taskTimeTxtv.setVisibility(View.GONE);
        }

        if(holder.checkBox.isActivated()) {
            holder.taskNameTxtv.setTypeface(holder.taskNameTxtv.getTypeface(), Typeface.ITALIC);
        } else {
            holder.taskNameTxtv.setTypeface(Typeface.DEFAULT);
        }
        holder.checkBox.setChecked(task.isActive());

        //Set event click for item -> start activity update and remove task
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, InputTaskActivity.class);
                intent.putExtra("TASKID", task.getTaskId());
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

        try {
            ft.applyPattern("dd-MM-yyyy");
            final Date current = ft.parse(ft.format(new Date()));
            if(task.getDate().before(current))
                holder.addBtn.setVisibility(View.VISIBLE);
            else {
                holder.addBtn.setVisibility(View.GONE);
            }
            holder.addBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            task.setDate(current);
                            mTaskDatabase.mTaskDAO().updateMTask(task);
                            mTasks.remove(task);
//                            removeNotification(task.getTaskId());
                            mHandler.obtainMessage(1, MainActivity.REFRESH).sendToTarget();
                        }
                    }).start();
                }
            });
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void deleteItem(final int position) {
        final MTask task = mTasks.get(position);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mTaskDatabase.mTaskDAO().deleteMTask(task);
                recentlyDeletedItem = task;
                recentlyDeletedItemPosition = position;

                mTasks.remove(task);
                removeNotification(task.getTaskId());

                Bundle data = new Bundle();
                data.putInt("POSITION", position);

                Message message = mHandler.obtainMessage(1, MainActivity.DELETE_ITEM);
                message.setData(data);
                message.sendToTarget();
            }
        }).start();

    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        MaterialButton addBtn;
        TextView taskNameTxtv, taskTimeTxtv;
        CheckBox checkBox;

        public RecyclerViewHolder(View itemView) {
            super(itemView);

            addBtn = itemView.findViewById(R.id.addBtn);
            taskNameTxtv = itemView.findViewById(R.id.taskNameTxtv);
            taskTimeTxtv = itemView.findViewById(R.id.taskTimeTxtv);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    private void removeNotification(int id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent notificationIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(broadcast);
    }

    public Context getContext() {
        return context;
    }

    public void setmTasks(List<MTask> mTasks) {
        this.mTasks = mTasks;
    }

    public void undoDelete() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mTasks.add(recentlyDeletedItemPosition, recentlyDeletedItem);
                mTaskDatabase.mTaskDAO().insertOnlySingleMTask(recentlyDeletedItem);
                Bundle data = new Bundle();
                data.putInt("POSITION", recentlyDeletedItemPosition);

                Message message = mHandler.obtainMessage(1, MainActivity.UNDO_DELETE_ITEM);
                message.setData(data);
                message.sendToTarget();
            }
        }).start();
    }
}
