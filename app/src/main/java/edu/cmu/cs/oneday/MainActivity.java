package edu.cmu.cs.oneday;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.cmu.cs.oneday.bean.TodoItemBean;
import edu.cmu.cs.oneday.utils.CSVReadWrite;
import edu.cmu.cs.oneday.view.NewItem;
import edu.cmu.cs.oneday.view.SlideListView;
import edu.cmu.cs.oneday.view.SlideListView.RemoveDirection;
import edu.cmu.cs.oneday.view.SlideListView.RemoveListener;

public class MainActivity extends Activity {
    private final String TAG = "$$$MainActivity";
    public static final int NEW_ITEM_REQUEST = 200;
    private CSVReadWrite csvrw;
    private boolean dataResumed = false;


    private int[] currentCountdownIndex = new int[1];


    private TextView buffertimetv;
    private TodoItemBean bufferItem;

    private UpdateUIRunnable updateUIRunnable;
    private Handler timerHanlder;

    private SlideListView listView;
    private SlideAdapter adapter;
    private List<TodoItemBean> todoList = new ArrayList<TodoItemBean>();
    private List<TodoItemBean> finishedTasks = new ArrayList<TodoItemBean>();
    private List<TodoItemBean> deletedTasks = new ArrayList<TodoItemBean>();

    TodoItemBean newItem = null;
    private Vibrator vibrator;


    SharedPreferences sharedPreferences;
    private ServiceConnection connection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("$$$", "onCreate");
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        setContentView(R.layout.activity_main);

        // prepare buffer timer
        bufferItem = new TodoItemBean("Buffer", 60);
        buffertimetv = (TextView) findViewById(R.id.buffertime);
        buffertimetv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewItem();
            }
        });
        updateBufferTimeleft();

        // load data from csv and sharedpreference
        csvrw = new CSVReadWrite(this);
        resumeData();


        // start default item
        Log.d(TAG, "get sharedPreferences of index:" + currentCountdownIndex[0]);
        if (currentCountdownIndex[0] != -1 && todoList != null && todoList.size() != 0) {
            Log.d(TAG, "Current status:" + todoList.get(currentCountdownIndex[0]).getStatus());
            todoList.get(currentCountdownIndex[0]).start();
        } else {
            focusOnBufferTimer();
        }


        listView = (SlideListView) findViewById(R.id.onedaylistview);
        adapter = new SlideAdapter();
        listView.setAdapter(adapter);
        listView.setRemoveListener(new RemoveListener() {
            @Override
            public synchronized void removeItem(RemoveDirection direction, int position) {
                String this_title = todoList.get(position).getTitle();
                switch (direction) {
                    case LEFT:
                        deletedTasks.add(todoList.remove(position));
                        Toast.makeText(MainActivity.this, "Remove" + this_title, Toast.LENGTH_SHORT).show();
                        break;
                    case RIGHT:
                        finishedTasks.add(todoList.remove(position));
                        Toast.makeText(MainActivity.this, "Finish " + this_title, Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                if (position == 0) {
                    currentCountdownIndex[0] = -1;
                    focusOnBufferTimer();
                }
                updateBufferTimeleft();
                adapter.notifyDataSetChanged();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                vibrator.vibrate(20);
                if (currentCountdownIndex[0] != position) { //有其他的timer在运行
                    Log.d(TAG, "Going to stop  task:" + currentCountdownIndex[0]);
                    stopCurrentItem(currentCountdownIndex[0]);
                    Log.d(TAG, "Going to start  task:" + position);
                    startItem(position);
                } else {
                    stopItem(position);
                    startBuffer();
                }
                adapter.notifyDataSetChanged();
                updateBufferTimeleft();
                listView.smoothScrollToPosition(0);

            }
        });


        adapter.notifyDataSetChanged();
        updateUIRunnable = new UpdateUIRunnable();
        timerHanlder = new Handler();
        timerHanlder.postDelayed(updateUIRunnable, 1000);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
//        backupData();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        backupData();
        super.onStop();
    }

    private void topItem(int position) {
        TodoItemBean currentTodo = todoList.get(position);
        todoList.remove(position);
        todoList.add(0, currentTodo);
        updateEndTime();
    }

    private synchronized void startItem(int position) {
        if (position >= 0) {
            TodoItemBean currentTodo = todoList.get(position);
            if (currentTodo.getStatus() == TodoItemBean.DRY) {
                currentTodo.addTime(30);
                updateBufferTimeleft();
            }
            currentTodo.start();
            currentCountdownIndex[0] = 0;
            topItem(position);
        }
    }


    private void stopItem(int position) {
        todoList.get(position).stop();
    }

    private void startBuffer() {
        bufferItem.start();
        focusOnBufferTimer();
    }

    private void stopBuffer() {
        bufferItem.stop();
        removeFocusOnBufferTimer();
    }

    private void stopCurrentItem(int index) {
        if (index == -1) {
            stopBuffer();
        } else {
            stopItem(index);
        }
    }

    private void backupData() {
        csvrw.writeToCSV(todoList);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("INDEX", currentCountdownIndex[0]);
        editor.commit();
    }

    @Override
    protected void onDestroy() {
        Log.d("===", "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Log.d(TAG, "onLowMemory");
        super.onLowMemory();
    }


    private class SlideAdapter extends BaseAdapter {

        private LayoutInflater mInflater = getLayoutInflater();

        @Override
        public int getCount() {
            return todoList.size();
        }

        @Override
        public Object getItem(int position) {
            return todoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.item, null);
                viewHolder.layout = (LinearLayout) convertView.findViewById(R.id.item);
                viewHolder.title = (TextView) convertView.findViewById(R.id.item_title);
                viewHolder.timeleft = (TextView) convertView.findViewById(R.id.timer);
                viewHolder.endtime = (TextView) convertView.findViewById(R.id.end_time);
                viewHolder.timeused = (TextView) convertView.findViewById(R.id.used_time);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            TodoItemBean bean = todoList.get(position);
            viewHolder.title.setText(bean.getTitle());
            viewHolder.timeleft.setText(bean.getTimeleftString());
            viewHolder.endtime.setText("to: " + bean.getExpectedEndTimeString());
            viewHolder.timeused.setText("spent: " + bean.getTimeUsedString());
            int backgroundcolor = R.color.seamlessgray;
            int textcolor = R.color.black;
            switch (bean.getStatus()) {
                case TodoItemBean.STARTED: {
                    backgroundcolor = R.color.graceblue;
                    textcolor = R.color.littleyellow;
                    break;
                }
                case TodoItemBean.STOPPED: {
                    backgroundcolor = R.color.seamlessgray;
                    textcolor = R.color.black;
                    break;
                }
                case TodoItemBean.DRY: {
                    backgroundcolor = R.color.darkred;
                    textcolor = R.color.darkgrey;
                    break;
                }
                case TodoItemBean.FINISHED: {
                    backgroundcolor = R.color.greenyellow;
                    textcolor = R.color.black;
                    break;
                }
            }
            viewHolder.setSpecialColor(MainActivity.this, backgroundcolor, textcolor);
            return convertView;
        }

    }

    private static class ViewHolder {
        public LinearLayout layout;
        public TextView title;
        public TextView timeleft;
        public TextView endtime;
        public TextView timeused;

        public void setSpecialColor(Context context, int backgroundcolor, int textcolor) {
            this.layout.setBackgroundResource(backgroundcolor);
            this.title.setBackgroundResource(backgroundcolor);
            this.timeleft.setBackgroundResource(backgroundcolor);
            this.endtime.setBackgroundResource(backgroundcolor);
            this.timeused.setBackgroundResource(backgroundcolor);

            this.title.setTextColor(context.getResources().getColor(textcolor));
            this.timeleft.setTextColor(context.getResources().getColor(textcolor));
            this.endtime.setTextColor(context.getResources().getColor(textcolor));
            this.timeused.setTextColor(context.getResources().getColor(textcolor));

            this.timeused.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        }

    }


    private void addNewItem() {
        Intent it = new Intent(MainActivity.this, NewItem.class);
        startActivityForResult(it, NEW_ITEM_REQUEST);
    }


    // update buffer time left, -- will recalculate  time left for today
    private void updateBufferTimeleft() {
        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();

        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        int timeleft = (int) ((c.getTimeInMillis() - now) / 1000);
        for (int i = 0; i < todoList.size(); i++) {
            timeleft -= todoList.get(i).getTimeLeft();
        }
        if (buffertimetv != null) {
            buffertimetv.setText(secondsToString(timeleft));
        }
        updateEndTime();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        if (requestCode == NEW_ITEM_REQUEST && resultCode == RESULT_OK) {
            String item_title = data.getStringExtra("NEW_ITEM_TITLE");
            int item_duration_min = data.getIntExtra("NEW_ITEM_DUR_MIN", 30);
            newItem = new TodoItemBean(item_title, item_duration_min);
            todoList.add(newItem);
            updateEndTime();
            Log.d(TAG, "new item added");
            updateBufferTimeleft();
            adapter.notifyDataSetChanged();
        }
    }

    class UpdateUIRunnable implements Runnable {
        @Override
        public void run() {
            if (currentCountdownIndex[0] == -1) {
                focusOnBufferTimer();
            }
            else if (currentCountdownIndex[0] == 0 && todoList.get(0).isDry()){
//                todoList.get(0).stop();
//                todoList.get(0).setStatus(TodoItemBean.DRY);
                int addedTime = todoList.get(0).getExpectedDuration() / 60;
                if (addedTime > 30) {
                    todoList.get(0).addTime(30);
                }
                else {
                    todoList.get(0).addTime(addedTime);
                }
                todoList.get(0).startDry();
                vibrator.vibrate(2000);
//                currentCountdownIndex[0] = -1;
//                focusOnBufferTimer();
            }
            updateBufferTimeleft();
            updateEndTime();
            adapter.notifyDataSetChanged();
            timerHanlder.postDelayed(updateUIRunnable, 1000);
        }
    }


    private synchronized void focusOnBufferTimer() {
        if (buffertimetv != null) {
            updateBufferTimeleft();
            buffertimetv.setBackgroundColor(Color.parseColor("#6F89AB"));
            buffertimetv.setTextColor(Color.parseColor("#FFFFFF"));
            currentCountdownIndex[0] = -1;
        }

    }

    private void removeFocusOnBufferTimer() {
        if (buffertimetv != null) {
            updateBufferTimeleft();
            buffertimetv.setBackgroundColor(Color.parseColor("#EEEEEE"));
            buffertimetv.setTextColor(Color.parseColor("#000000"));
        }
    }


    /**
     * Load both index and todolist
     */
    private synchronized void resumeData() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        currentCountdownIndex[0] = sharedPreferences.getInt("INDEX", 0);
        if (currentCountdownIndex != null && csvrw != null) {
            todoList = csvrw.readFromCSV();
            Log.d(TAG, "resumeData:" + todoList.size());
            updateEndTime();
        }

    }

    private void countdown() {
        if (currentCountdownIndex[0] == -1) {
            focusOnBufferTimer();
        } else {
            Log.d(TAG, "Count Down:" + todoList.get(currentCountdownIndex[0]).getTitle() + ":[" + todoList.get(
                    currentCountdownIndex[0]).getTimeLeft());
            if (!todoList.get(currentCountdownIndex[0]).isDry()) {
                todoList.get(currentCountdownIndex[0]).setStatus(TodoItemBean.DRY);
                vibrator.vibrate(2000);
                focusOnBufferTimer();
            }
        }
        adapter.notifyDataSetChanged();
    }

    private String secondsToString(int seconds) {
        int sign = 1;
        if (seconds < 0) {
            sign = -1;
            seconds = 0 - seconds;
        }
        int sec = seconds % 60;
        int min = seconds / 60;
        int hour = min / 60;
        min = min % 60;
        StringBuilder result = new StringBuilder();
        if (sign == -1) {
            result.append("-");
        }
        if (hour >= 10) {
            result.append(hour);
        } else {
            result.append("0");
            result.append(hour);
        }
        result.append(":");

        if (min >= 10) {
            result.append(min);
        } else {
            result.append("0");
            result.append(min);
        }
        result.append(":");

        if (sec >= 10) {
            result.append(sec);
        } else {
            result.append("0");
            result.append(sec);
        }
        return result.toString();
    }


    private void updateEndTime() {
        if (todoList.size() > 0) {
            Calendar c = Calendar.getInstance();
            TodoItemBean cur;
            if (currentCountdownIndex[0] == -1) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                for (int i = 0; i < todoList.size(); i++) {
                    cur = todoList.get(i);
                    c.add(Calendar.SECOND, cur.getExpectedDuration() - cur.getTimeUsed());
                    cur.setExpectEndTime(c.getTime());
                }
            } else { // = -1
                c.setTime(todoList.get(0).getExpectEndTime());
                for (int i = 1; i < todoList.size(); i++) {
                    cur = todoList.get(i);
                    c.add(Calendar.SECOND, (cur.getExpectedDuration() - cur.getTimeUsed()));
                    cur.setExpectEndTime(c.getTime());
                }
            }
        }
    }
}
