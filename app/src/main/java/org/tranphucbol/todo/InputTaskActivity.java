package org.tranphucbol.todo;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.button.MaterialButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import org.tranphucbol.todo.Model.MTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class InputTaskActivity extends AppCompatActivity {

    private MTaskDatabase mTaskDatabase;
    private static final String DATABASE_NAME = "mtasks_db";
    private MaterialButton createBtn, timeBtn, dateBtn, updateBtn, deleteBtn;
    private EditText nameTxt, noteTxt;
    private String date;
    private String time;
    private SimpleDateFormat ft;
    private MTask task;
    private Date dateCreate;

    private static final int BACK = 1;
    private static final int UPDATE = 2;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Integer code = (Integer)msg.obj;
            switch (code) {
                case BACK:
                    onBackPressed();
                    break;
                case UPDATE:
                    createBtn.setVisibility(View.INVISIBLE);
                    updateBtn.setVisibility(View.VISIBLE);
                    deleteBtn.setVisibility(View.VISIBLE);

                    nameTxt.setText(task.getName());
                    noteTxt.setText(task.getContent());

                    if(task.getDeadline() != null) {
                        String[] d = ft.format(task.getDeadline()).split(" ");
                        dateBtn.setText(d[0]);
                        timeBtn.setText(d[1]);
                        date = d[0];
                        time = d[1];
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_task);
        Toolbar toolbar = findViewById(R.id.toolbarInput);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mTaskDatabase = Room.databaseBuilder(getApplicationContext(),
                MTaskDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();
        final Intent intent = getIntent();

        final int taskId = intent.getIntExtra("TASKID", -1);

        String dateStr = intent.getStringExtra("DATE");
        if(dateStr != null) {
            ft = new SimpleDateFormat("dd-MM-yyyy");
            try {
                dateCreate = ft.parse(dateStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        createBtn = findViewById(R.id.createBtn);
        updateBtn = findViewById(R.id.updateTaskBtn);
        deleteBtn = findViewById(R.id.deleteTaskBtn);
        timeBtn = findViewById(R.id.timeBtn);
        dateBtn = findViewById(R.id.dateBtn);
        nameTxt = findViewById(R.id.nameTxt);
        noteTxt = findViewById(R.id.noteTxt);

        ft = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        if(taskId == -1) {
            task = new MTask("");
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    task = mTaskDatabase.mTaskDAO().getById(taskId);
                    dateCreate = task.getDate();
                    mHandler.obtainMessage(1, UPDATE).sendToTarget();
                }
            }).start();
        }


        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nameTxt.getText().toString().length() > 0) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //check if date or time = null -> don't create notification for this task
                            if (date == null || time == null) {
                                task = new MTask(nameTxt.getText().toString(), noteTxt.getText().toString());
                            } else {
                                Date deadline;
                                try {
                                    deadline = ft.parse(date + " " + time);
                                    if(deadline.before(new Date())) {
                                        task = new MTask(nameTxt.getText().toString(), noteTxt.getText().toString());
                                    } else
                                        task = new MTask(nameTxt.getText().toString(), noteTxt.getText().toString(), deadline);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                            task.setDate(dateCreate);
                            long id = mTaskDatabase.mTaskDAO().insertOnlySingleMTask(task);

                            createNotification((int) id);
                            mHandler.obtainMessage(1, BACK).sendToTarget();
                        }
                    }).start();
                }
            }
        });

        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                c.setTime(dateCreate);
                int mYear = c.get(Calendar.YEAR);
                int mMonth = c.get(Calendar.MONTH);
                int mDay = c.get(Calendar.DAY_OF_MONTH);


                DatePickerDialog datePickerDialog = new DatePickerDialog(InputTaskActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                date = String.format("%02d-%02d-%04d", dayOfMonth, monthOfYear + 1, year);
                                dateBtn.setText(date);

                            }
                        }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });

        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(InputTaskActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                time = String.format("%02d:%02d", hourOfDay, minute);
                                timeBtn.setText(time);
                            }
                        }, mHour, mMinute, true);
                timePickerDialog.show();
            }
        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        task.setName(nameTxt.getText().toString());
                        task.setContent(noteTxt.getText().toString());

                        if(date != null && time != null) {
                            try {
                                task.setDeadline(ft.parse(date + " " + time));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        mTaskDatabase.mTaskDAO().updateMTask(task);
                        removeNotification(task.getTaskId());
                        createNotification(task.getTaskId());
                        mHandler.obtainMessage(1, BACK).sendToTarget();
                    }
                }).start();
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(InputTaskActivity.this);

                builder.setMessage("Bạn có chắc chắn muốn xóa công việc này không?");

                builder.setPositiveButton("Có", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mTaskDatabase.mTaskDAO().deleteMTask(task);
                                removeNotification(task.getTaskId());
                                mHandler.obtainMessage(1, BACK).sendToTarget();
                            }
                        }).start();
                    }
                });
                builder.setNegativeButton("Không", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void createNotification(int id) {
        if(task.getDeadline() != null) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            Intent notificationIntent = new Intent(InputTaskActivity.this, AlarmReceiver.class);
            notificationIntent.putExtra("TASKID", id);
            notificationIntent.putExtra("NAME", task.getName());
            notificationIntent.putExtra("CONTENT", task.getContent());
            PendingIntent broadcast = PendingIntent.getBroadcast(InputTaskActivity.this, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Calendar cal = Calendar.getInstance();
            int sec = (int) (task.getDeadline().getTime() - task.getCreatedOn().getTime()) / 1000;
            if(sec > 0) {
                cal.add(Calendar.SECOND, sec);
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), broadcast);
            }
        }
    }

    private void removeNotification(int id) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent notificationIntent = new Intent(InputTaskActivity.this, AlarmReceiver.class);
        PendingIntent broadcast = PendingIntent.getBroadcast(InputTaskActivity.this, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(broadcast);
    }
}
