package org.tranphucbol.todo;

import android.app.DatePickerDialog;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import com.danimahardhika.cafebar.CafeBar;
import com.danimahardhika.cafebar.CafeBarCallback;
import com.danimahardhika.cafebar.CafeBarTheme;

import org.tranphucbol.todo.Model.MTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView todoList;
    private MTaskDatabase mTaskDatabase;
    private static final String DATABASE_NAME = "mtasks_db";
    private List<MTask> tasks;

    public static final int REFRESH = 1;
    public static final int TITLE = 2;
    public static final int DELETE_ITEM = 3;
    public static final int UNDO_DELETE_ITEM = 4;
    public static final int YESTERDAY = 5;
    public static final int MOVE_ITEM = 6;

    private String title;
    private Toolbar toolbar;
    private ToDoRecyclerViewAdapter toDoListAdapter;
    private SimpleDateFormat ft;

    Locale locale = new Locale("vi", "VN");
    private String dateStr;

    private boolean isImportant = false;
    private boolean isDone = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Integer code = (Integer) msg.obj;
            switch (code) {
                case REFRESH:
                    toDoListAdapter.sort();
                    toDoListAdapter.notifyDataSetChanged();
                    break;
                case TITLE:
                    toolbar.setSubtitle(title);
                    break;
                case DELETE_ITEM:
                    toDoListAdapter.notifyItemRemoved(msg.getData().getInt("POSITION"));
                    showUndoSnackBar();
                    break;
                case UNDO_DELETE_ITEM:
                    toDoListAdapter.notifyItemInserted(msg.getData().getInt("POSITION"));
                    break;
                case YESTERDAY:
                    showYesterdaySnackBar();
                    break;
                case MOVE_ITEM:
                    toDoListAdapter.notifyItemMoved(msg.getData().getInt("FROM"), msg.getData().getInt("TO"));
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ft.applyPattern("dd-MM-yyyy");
                try {
                    Date date = ft.parse(dateStr);
                    Date current = ft.parse(ft.format(new Date()));

//                    if(date.equals(current) || date.after(current)) {
                        Intent intent = new Intent(MainActivity.this, InputTaskActivity.class);
                        intent.putExtra("DATE", dateStr);
                        startActivity(intent);
//                    } else {
//                        Toast.makeText(MainActivity.this, "Bạn không thể tạo thêm công việc trong ngày này", Toast.LENGTH_SHORT).show();
//                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });

        Locale.setDefault(locale);

        mTaskDatabase = Room.databaseBuilder(getApplicationContext(),
                MTaskDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();

        todoList = findViewById(R.id.todoList);

        toDoListAdapter = new ToDoRecyclerViewAdapter(new ArrayList<MTask>(), MainActivity.this, mHandler);
        todoList.setAdapter(toDoListAdapter);

        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);

        todoList.addItemDecoration(itemDecoration);

        ItemTouchHelper itemTouchHelper = new
                ItemTouchHelper(new SwipeToDeleteCallback(toDoListAdapter));
        itemTouchHelper.attachToRecyclerView(todoList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        todoList.setLayoutManager(layoutManager);

        ft = new SimpleDateFormat("dd-MM-yyyy");
        dateStr = ft.format(new Date());

    }

    @Override
    protected void onStart() {
        super.onStart();
        loadTasks();
        checkTasksOfYesterday();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.calendar_setting) {
            final Calendar c = Calendar.getInstance();
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            dateStr = String.format("%02d-%02d-%04d", dayOfMonth, monthOfYear + 1, year);
                            ft.applyPattern("dd-MM-yyyy");
                            try {
                                final Date date = ft.parse(dateStr);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tasks = mTaskDatabase.mTaskDAO().getAllByDate(date);
                                        toDoListAdapter.setmTasks(tasks);
                                        ft.applyPattern("EEEEE, dd 'tháng' M");
                                        title = ft.format(date);
                                        //send message to change subtitle - date
                                        mHandler.obtainMessage(1, TITLE).sendToTarget();
                                        //send message to reload data of date
                                        mHandler.obtainMessage(1, REFRESH).sendToTarget();
                                    }
                                }).start();
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }, mYear, mMonth, mDay);
            datePickerDialog.show();
            return true;
        } else if(id == R.id.important_setting) {
            isImportant = !isImportant;
            if(isImportant) {
                List<MTask> notImportant = new ArrayList<>();
                for (MTask task : tasks) {
                    if (!task.isImportant()) {
                        notImportant.add(task);
                    }
                }
                tasks.removeAll(notImportant);
                toDoListAdapter.notifyDataSetChanged();
            } else {
                loadTasks();
            }
        } else if(id == R.id.done_setting) {
            isDone = !isDone;
            if(isDone) {
                List<MTask> done = new ArrayList<>();
                for(MTask task : tasks) {
                    if(task.isActive()) {
                        done.add(task);
                    }
                }
                if(done.isEmpty()) {
                    isDone = false;
                }
                tasks.removeAll(done);
                toDoListAdapter.notifyDataSetChanged();
            } else {
                loadTasks();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void showUndoSnackBar() {
        CafeBar.builder(MainActivity.this)
                .theme(CafeBarTheme.LIGHT)
                .icon(R.drawable.ic_delete_white_24dp)
                .content("Đã xóa công việc")
                .autoDismiss(false)
                .neutralText("Hoàn tác")
                .onNeutral(new CafeBarCallback() {
                    @Override
                    public void OnClick(CafeBar cafeBar) {
                        toDoListAdapter.undoDelete();
                        cafeBar.dismiss();
                    }
                })
                .show();
    }

    private void showYesterdaySnackBar() {
        CafeBar.builder(MainActivity.this)
                .floating(true)
                .theme(CafeBarTheme.LIGHT)
                .duration(5000)
                .content("Bạn còn công việc ngày hôm qua")
                .neutralText("Thêm")
                .onNeutral(new CafeBarCallback() {
                    @Override
                    public void OnClick(CafeBar cafeBar) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Calendar c = Calendar.getInstance();
                                ft.applyPattern("dd-MM-yyyy");
                                try {
                                    Date current = ft.parse(ft.format(new Date()));
                                    Date yesterday = null;
                                    c.setTime(current);
                                    c.add(Calendar.DATE, -1);
                                    yesterday = c.getTime();
                                    List<MTask> taskYesterdays = mTaskDatabase.mTaskDAO().getAllByDateAndActive(yesterday, false);
                                    toDoListAdapter.setmTasks(taskYesterdays);
                                    mHandler.obtainMessage(1, REFRESH).sendToTarget();
                                    ft.applyPattern("EEEEE, dd 'tháng' M");
                                    title = ft.format(yesterday);
                                    mHandler.obtainMessage(1, TITLE).sendToTarget();
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        cafeBar.dismiss();
                    }
                })
                .show();
    }

    void loadTasks() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ft.applyPattern("dd-MM-yyyy");
                try {
                    Date current = ft.parse(ft.format(new Date()));
                    List<MTask> taskAuto = mTaskDatabase.mTaskDAO().getAllByDateLessThan(current, false, true);

                    //delete the task have a deadline less than current date
                    //And update date for tasks have a deadline greater than current date or deadline null
                    List<MTask> taskUnfinished = new ArrayList<>();
                    for (MTask task : taskAuto) {
                        if(task.getDeadline() != null && task.getDeadline().before(current)) {
                            taskUnfinished.add(task);
                        } else {
                            task.setDate(current);
                        }
                    }
                    taskAuto.remove(taskUnfinished);

                    mTaskDatabase.mTaskDAO().updateMTask(taskAuto);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ft.applyPattern("dd-MM-yyyy");
                                Date date = ft.parse(dateStr);
                                tasks = mTaskDatabase.mTaskDAO().getAllByDate(date);
                                toDoListAdapter.setmTasks(tasks);
                                ft.applyPattern("EEEEE, dd 'tháng' M");
                                title = ft.format(date);
                                mHandler.obtainMessage(1, TITLE).sendToTarget();
                                mHandler.obtainMessage(1, REFRESH).sendToTarget();
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void checkTasksOfYesterday() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Calendar c = Calendar.getInstance();
                SimpleDateFormat ftYesterday = new SimpleDateFormat("dd-MM-yyyy");
                try {
                    Date current = ftYesterday.parse(ftYesterday.format(new Date()));
                    c.setTime(current);
                    c.add(Calendar.DATE, -1);
                    List<MTask> taskYesterdays = mTaskDatabase.mTaskDAO().getAllByDateAndActive(c.getTime(), false);
                    if(taskYesterdays.size() > 0) {
                        mHandler.obtainMessage(1, YESTERDAY).sendToTarget();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
