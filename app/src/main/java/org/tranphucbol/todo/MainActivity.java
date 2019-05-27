package org.tranphucbol.todo;

import android.app.DatePickerDialog;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.DatePicker;
import android.widget.ListView;

import org.tranphucbol.todo.Model.MTask;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ListView todoList;
    private MTaskDatabase mTaskDatabase;
    private static final String DATABASE_NAME = "mtasks_db";
    private List<MTask> tasks;

    public static final int REFRESH = 1;
    public static final int RELOAD = 2;
    public static final int TITLE = 3;

    private String title;
    private Toolbar toolbar;
    private ToDoListAdapter toDoListAdapter;

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
                default:
                    toDoListAdapter = new ToDoListAdapter(tasks, MainActivity.this, mHandler);
                    todoList.setAdapter(toDoListAdapter);
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
        todoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.i("CLICK", "beng");
                Intent intent = new Intent(MainActivity.this, InputTaskActivity.class);
                intent.putExtra("taskId", tasks.get(position).getTaskId());
                startActivity(intent);
            }
        });

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
                    ft = new SimpleDateFormat("EEEEE, dd 'tháng' M", dateFormatSymbols);
                    title = ft.format(new Date());
                    mHandler.obtainMessage(1, TITLE).sendToTarget();
                    mHandler.obtainMessage(1, RELOAD).sendToTarget();
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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
                                        SimpleDateFormat ft = new SimpleDateFormat("EEEEE, dd 'tháng' M", dateFormatSymbols);
                                        title = ft.format(start);
                                        mHandler.obtainMessage(1, TITLE).sendToTarget();
                                        mHandler.obtainMessage(1, RELOAD).sendToTarget();
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
