package edu.cmu.ebiz.oneday.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ebiz.oneday.bean.TodoItemBean;
import edu.cmu.ebiz.oneday.utils.CSVReadWrite;

public class CountdownService extends Service {

    private List<TodoItemBean> todoList = new ArrayList<TodoItemBean>();
    private List<TodoItemBean> finishedTasks = new ArrayList<TodoItemBean>();
    private List<TodoItemBean> deletedTasks = new ArrayList<TodoItemBean>();

    private TodoItemBean bufferItem;
    private CSVReadWrite csvrw;

    private Vibrator vibrator;

    private Handler timerHanlder;
    private CountDownRunnable countDownRunnable;
    private int[] currentCountdownIndex ;
    boolean binded = false;

    SharedPreferences sharedPreferences;

    public CountdownService() {
    }


    @Override
    public void onCreate() {

        super.onCreate();
        Log.d("$$$", "onCreate");
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        currentCountdownIndex[0] = sharedPreferences.getInt("INDEX",-1);
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
//        throw new UnsupportedOperationException("Not yet implemented");
        countDownRunnable = new CountDownRunnable();
        timerHanlder = new Handler();
        timerHanlder.postDelayed(countDownRunnable, 999);
        return new CountdownBinder();
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


    class CountDownRunnable implements Runnable {
        @Override
        public synchronized void run() {
            Log.d("$$$","current index = " + currentCountdownIndex[0]);
            if (currentCountdownIndex[0] == -1) {
                bufferItem.countDown();
            } else {
                if (!todoList.get(currentCountdownIndex[0]).countDown()) {
                    todoList.get(currentCountdownIndex[0]).setStatus(TodoItemBean.DRY);
                    currentCountdownIndex[0] = -1;
                    vibrator.vibrate(500);
                }

            }
//            adapter.notifyDataSetChanged();
            timerHanlder.postDelayed(countDownRunnable, 999);
        }
    }
}
