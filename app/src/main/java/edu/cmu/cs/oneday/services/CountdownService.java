package edu.cmu.cs.oneday.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.oneday.bean.TodoItemBean;
import edu.cmu.cs.oneday.utils.CSVReadWrite;

public class CountdownService extends Service {

    private List<TodoItemBean> todoList = new ArrayList<TodoItemBean>();
    private List<TodoItemBean> finishedTasks = new ArrayList<TodoItemBean>();
    private List<TodoItemBean> deletedTasks = new ArrayList<TodoItemBean>();

    private TodoItemBean bufferItem;
    private CSVReadWrite csvrw;

    private Vibrator vibrator;


    private int[] currentCountdownIndex;
    boolean binded = false;

    SharedPreferences sharedPreferences;

    public CountdownService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("$$$", "onCreate");
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        binded = true;
        Log.d("===", "Binded successfully!");

        new CountdownTask().execute();
        return new CountdownBinder();
    }


    class CountdownTask extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPostExecute(Boolean result) {
        }

        @Override
        protected Boolean doInBackground(String... params) {
            while (binded) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                countdown();
            }
            return true;
        }
    }

    private void countdown() {
        if (currentCountdownIndex[0] == -1) {
            bufferItem.countDown();
        } else {
            if (!todoList.get(currentCountdownIndex[0]).countDown()) {
                todoList.get(currentCountdownIndex[0]).setStatus(TodoItemBean.DRY);
                currentCountdownIndex[0] = -1;
                vibrator.vibrate(2000);
            }

        }
    }


    public class CountdownBinder extends Binder {

        public void setTodos(List<TodoItemBean> todos) {
            todoList = todos;
        }

        public void setBufferItem(TodoItemBean item) {
            bufferItem = item;
        }

        public boolean isBinded() {
            return binded;
        }

        public void setCurrentCountdownIndex(int[] index) {
            currentCountdownIndex = index;
        }
    }


}
