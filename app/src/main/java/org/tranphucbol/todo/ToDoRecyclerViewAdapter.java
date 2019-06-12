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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    public void onBindViewHolder(RecyclerViewHolder holder, final int i) {
        final MTask task = mTasks.get(i);

        holder.taskNameTxtv.setText(task.getName());

        ft.applyPattern("HH:mm, EEE, dd MMM");
        if(task.getDeadline() != null) {
            holder.taskTimeTxtv.setVisibility(View.VISIBLE);
            holder.taskTimeTxtv.setText(ft.format(task.getDeadline()));
        } else {
            holder.taskTimeTxtv.setVisibility(View.GONE);
        }

        holder.checkBox.setChecked(task.isActive());
        holder.starCheckBox.setChecked(task.isImportant());

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

        holder.starCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //change activate of task
                        task.setImportant(!task.isImportant());
                        mTaskDatabase.mTaskDAO().updateMTask(task);
                        int oldPosition = mTasks.indexOf(task);
                        sort();
                        int newPosition = mTasks.indexOf(task);
                        Message message = mHandler.obtainMessage(1, MainActivity.MOVE_ITEM);
                        Bundle data = new Bundle();
                        data.putInt("FROM", oldPosition);
                        data.putInt("TO", newPosition);
                        message.setData(data);
                        message.sendToTarget();
                    }
                }).start();
            }
        });

        try {
            ft.applyPattern("dd-MM-yyyy");
            final Date current = ft.parse(ft.format(new Date()));
            if(task.getDate().before(current)) {
                holder.addBtn.setVisibility(View.VISIBLE);
                holder.starCheckBox.setVisibility(View.GONE);
            }
            else {
                holder.addBtn.setVisibility(View.GONE);
                holder.starCheckBox.setVisibility(View.VISIBLE);
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
        CheckBox checkBox, starCheckBox;

        public RecyclerViewHolder(View itemView) {
            super(itemView);

            addBtn = itemView.findViewById(R.id.addBtn);
            taskNameTxtv = itemView.findViewById(R.id.taskNameTxtv);
            taskTimeTxtv = itemView.findViewById(R.id.taskTimeTxtv);
            checkBox = itemView.findViewById(R.id.checkBox);
            starCheckBox = itemView.findViewById(R.id.starCheckBox);
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

    public void sort() {
        Collections.sort(mTasks, comparator);
    }

    public static Comparator comparator = new Comparator<MTask>() {
        @Override
        public int compare(MTask o1, MTask o2) {
            Date o1Date = o1.getCreatedOn();
            Date o2Date = o2.getCreatedOn();
            boolean o1IsImportant = o1.isImportant();
            boolean o2IsImportant = o2.isImportant();

            if(o1IsImportant == o2IsImportant) {
                if (o1Date.before(o2Date)) {
                    return 1;
                } else if (o1Date.after(o2Date)) {
                    return -1;
                }
            } else if (o1IsImportant) {
                return -1;
            } else {
                return 1;
            }
            return 0;
        }
    };
}
