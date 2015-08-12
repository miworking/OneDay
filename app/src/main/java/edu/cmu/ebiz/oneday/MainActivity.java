package edu.cmu.ebiz.oneday;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.cmu.ebiz.oneday.bean.TodoItemBean;
import edu.cmu.ebiz.oneday.services.CountdownService;
import edu.cmu.ebiz.oneday.utils.CSVReadWrite;
import edu.cmu.ebiz.oneday.view.NewItem;
import edu.cmu.ebiz.oneday.view.SlideListView;
import edu.cmu.ebiz.oneday.view.SlideListView.RemoveDirection;
import edu.cmu.ebiz.oneday.view.SlideListView.RemoveListener;

public class MainActivity extends Activity {
    public static final int NEW_ITEM_REQUEST = 200;
    private CSVReadWrite csvrw;


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
    private boolean isBinded = false;

    private CountdownService.CountdownBinder mBinderService;
    SharedPreferences sharedPreferences;


    private ServiceConnection connection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("===", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        csvrw = new CSVReadWrite(this);
        todoList = csvrw.readFromCSV();
        bufferItem = new TodoItemBean("Buffer", 60);

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
                currentCountdownIndex[0] = -1;
                updateBufferTimeleft();

            }
        });

        buffertimetv = (TextView) findViewById(R.id.buffertime);
        buffertimetv.setText(bufferItem.getExpectedTime());
        buffertimetv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewItem();
            }
        });

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        currentCountdownIndex[0] = sharedPreferences.getInt("INDEX", -1);
        if (currentCountdownIndex[0] != -1) {
            Log.d("$$$", "Current status:" + todoList.get(currentCountdownIndex[0]).getStatus());
            todoList.get(currentCountdownIndex[0]).onStarted();
        } else {
            focusOnBufferTimer();
        }
        adapter.notifyDataSetChanged();
        updateBufferTimeleft();


        connection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d("===", "onServiceConnected");
                mBinderService = (CountdownService.CountdownBinder) service;
                mBinderService.setTodos(todoList);
                Log.d("===", "set todolist:" + todoList.size());
                mBinderService.setBufferItem(bufferItem);
                Log.d("===", "set bufferItem:" + bufferItem.getTimeleftString());

                if (mBinderService.isBinded()) {
                    isBinded = true;
                }
                mBinderService.setCurrentCountdownIndex(currentCountdownIndex);
                Log.d("===", "set currentCountdownIndex:" + currentCountdownIndex[0]);

            }
        };
        Intent bindIntent = new Intent(MainActivity.this, CountdownService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public synchronized void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentCountdownIndex[0] != position) { //有其他的timer在运行
                    //将正在运行的pause
                    if (currentCountdownIndex[0] == -1) {
                        bufferItem.onStopped();
                        Log.d("===", "buffertimetv.setBackgroundColor1");
                        buffertimetv.setBackgroundColor(Color.parseColor("#EEEEEE"));
                        buffertimetv.setTextColor(Color.parseColor("#000000"));
                    } else {
                        todoList.get(currentCountdownIndex[0]).onStopped();
                    }

                    TodoItemBean currentTodo = todoList.get(position);
                    if (currentTodo.getStatus() == TodoItemBean.DRY) {
                        currentTodo.addTime(30 * 60);
                        updateBufferTimeleft();
                    }

                    currentTodo.onStarted();
                    todoList.remove(position);
                    todoList.add(0, currentTodo);
                    currentCountdownIndex[0] = 0;
                } else {
                    todoList.get(position).setStatus(TodoItemBean.STOPPED);
                    bufferItem.onStarted();
                    focusOnBufferTimer();
                }
                adapter.notifyDataSetChanged();
            }
        });
        updateUIRunnable = new UpdateUIRunnable();
        timerHanlder = new Handler();
        timerHanlder.postDelayed(updateUIRunnable, 1000);
    }

    @Override
    protected void onResume() {
        Log.d("===", "onResume");
        super.onResume();
        updateBufferTimeleft();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        Log.d("$$$", "onPause");
        csvrw.writeToCSV(todoList);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("INDEX", currentCountdownIndex[0]);
        editor.commit();
        Log.d("$$$", "Going to write index into sharedprefrence:" + currentCountdownIndex[0]);
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        Log.d("===", "onDestroy");
        super.onDestroy();
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
            viewHolder.endtime.setText("to: " + bean.getEndTimeString());
            viewHolder.timeused.setText("spent: " + bean.getTimeUsedString());
            if (bean.getStatus() == TodoItemBean.STARTED) { //highlighted
                viewHolder.layout.setBackgroundColor(Color.parseColor("#6F89AB"));
                viewHolder.timeleft.setBackgroundColor(Color.parseColor("#6F89AB"));
                viewHolder.timeleft.setTextColor(Color.parseColor("#FFFF6F"));
                viewHolder.title.setBackgroundColor(Color.parseColor("#6F89AB"));
                viewHolder.title.setTextColor(Color.parseColor("#FFFFFF"));
                viewHolder.endtime.setBackgroundColor(Color.parseColor("#6F89AB"));
                viewHolder.endtime.setTextColor(Color.parseColor("#FFFFFF"));
                viewHolder.timeused.setBackgroundColor(Color.parseColor("#6F89AB"));
                viewHolder.timeused.setTextColor(Color.parseColor("#FFFFFF"));

            } else {// not highlighted
                viewHolder.layout.setBackgroundColor(Color.parseColor("#EEEEEE"));
                viewHolder.timeleft.setBackgroundColor(Color.parseColor("#EEEEEE"));
                viewHolder.timeleft.setTextColor(Color.parseColor("#ADADAD"));//grey
                viewHolder.title.setBackgroundColor(Color.parseColor("#EEEEEE"));
                viewHolder.title.setTextColor(Color.parseColor("#111111"));
                viewHolder.endtime.setBackgroundColor(Color.parseColor("#EEEEEE"));
                viewHolder.endtime.setTextColor(Color.parseColor("#ADADAD"));
                viewHolder.endtime.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
                viewHolder.timeused.setBackgroundColor(Color.parseColor("#EEEEEE"));
                viewHolder.timeused.setTextColor(Color.parseColor("#ADADAD"));
                viewHolder.timeused.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
            }
            return convertView;
        }

    }

    private static class ViewHolder {
        public LinearLayout layout;
        public TextView title;
        public TextView timeleft;
        public TextView endtime;
        public TextView timeused;

    }


    private void addNewItem() {
        Intent it = new Intent(MainActivity.this, NewItem.class);
        startActivityForResult(it, NEW_ITEM_REQUEST);
    }


    private void updateBufferTimeleft() {
        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();

        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        long timeleft = c.getTimeInMillis() - now;
        for (int i = 0; i < todoList.size(); i++) {
            timeleft -= todoList.get(i).getTimeLeft() * 1000;
        }
        bufferItem.setTimeLeft((int) (timeleft / 1000));
        buffertimetv.setText(bufferItem.getTimeleftString());

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NEW_ITEM_REQUEST && resultCode == RESULT_OK) {
            String item_title = data.getStringExtra("NEW_ITEM_TITLE");
            int item_duration_min = data.getIntExtra("NEW_ITEM_DUR_MIN", 0);
            TodoItemBean todoItemBean = new TodoItemBean(item_title, item_duration_min);
            this.todoList.add(todoItemBean);
            updateBufferTimeleft();
            adapter.notifyDataSetChanged();
        }
    }

    class UpdateUIRunnable implements Runnable {
        @Override
        public synchronized void run() {
            if (currentCountdownIndex[0] == -1) {
                buffertimetv.setText(bufferItem.getTimeleftString());
                focusOnBufferTimer();
            } else {
                if (todoList.get(currentCountdownIndex[0]).getStatus() == TodoItemBean.DRY) {
                    focusOnBufferTimer();
                }
            }
            adapter.notifyDataSetChanged();
            timerHanlder.postDelayed(updateUIRunnable, 1000);
        }
    }


    private void focusOnBufferTimer() {
        if (buffertimetv != null) {
            buffertimetv.setBackgroundColor(Color.parseColor("#6F89AB"));
            buffertimetv.setTextColor(Color.parseColor("#FFFFFF"));
            currentCountdownIndex[0] = -1;
        }
    }
}
