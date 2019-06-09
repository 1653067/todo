package org.tranphucbol.todo;

import android.app.DatePickerDialog;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
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

import org.tranphucbol.todo.Model.MTask;

import java.text.DateFormatSymbols;
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

    private String title;
    private Toolbar toolbar;
    private ToDoRecyclerViewAdapter toDoListAdapter;

    Locale locale = new Locale("vi", "VN");
    DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(locale);


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Integer code = (Integer) msg.obj;
            switch (code) {
                case REFRESH:
                    toDoListAdapter.notifyDataSetChanged();
                    break;
                case TITLE:
                    toolbar.setSubtitle(title);
                    break;
                case DELETE_ITEM:
                    toDoListAdapter.notifyItemRemoved(msg.getData().getInt("POSITION"));
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
                Intent intent = new Intent(MainActivity.this, InputTaskActivity.class);
                startActivity(intent);
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

        dateFormatSymbols.setWeekdays(new String[]{
                "Unused",
                "Chủ nhật",
                "Thứ hai",
                "Thứ ba",
                "Thứ tư",
                "Thứ năm",
                "Thứ sáu",
                "Thứ bảy",
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Calendar c = Calendar.getInstance();
                SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy", dateFormatSymbols);
                Date start, end;
                try {
                    start = ft.parse(ft.format(new Date()));
                    c.setTime(start);
                    c.add(Calendar.MINUTE, 23 * 60 + 59);
                    end = c.getTime();
                    tasks = mTaskDatabase.mTaskDAO().getAllByDate(start, end);
                    toDoListAdapter.setmTasks(tasks);
                    ft = new SimpleDateFormat("EEEEE, dd 'tháng' M", dateFormatSymbols);
                    title = ft.format(new Date());
                    mHandler.obtainMessage(1, TITLE).sendToTarget();
                    mHandler.obtainMessage(1, REFRESH).sendToTarget();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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

        if (id == R.id.action_settings) {
            final Calendar c = Calendar.getInstance();
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);


            DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            String date = String.format("%02d-%02d-%04d", dayOfMonth, monthOfYear + 1, year);
                            SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy",  dateFormatSymbols);
                            try {
                                final Date start = ft.parse(date);
                                c.setTime(start);
                                c.add(Calendar.MINUTE, 23 * 60 + 59);
                                final Date end = c.getTime();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tasks = mTaskDatabase.mTaskDAO().getAllByDate(start, end);
                                        toDoListAdapter.setmTasks(tasks);
                                        SimpleDateFormat ft = new SimpleDateFormat("EEEEE, dd 'tháng' M", dateFormatSymbols);
                                        title = ft.format(start);
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
        }

        return super.onOptionsItemSelected(item);
    }
}
