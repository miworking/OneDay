package edu.cmu.ebiz.oneday.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.cmu.ebiz.oneday.bean.TodoItemBean;

/**
 * Created by julie on 8/11/15.
 */
public class CSVReadWrite {
    private Context context;
    private String fileName = null;
    private File file = null;
    private FileOutputStream outputStream = null;
    private FileInputStream inputStream = null;

    public CSVReadWrite(Context context) {
        this.context = context;
        this.file = getFile();
    }

    public String getBaseFolder() {
        String baseFolder;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            baseFolder = context.getExternalFilesDir(null).getAbsolutePath();
        } else {
            baseFolder = context.getFilesDir().getAbsolutePath();
        }
        return baseFolder;

    }

    public String getFileName() {
        if (fileName == null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd");
            Date now = new Date();
            fileName = getBaseFolder() + "/" + df.format(now) + ".csv";
        }
        return fileName;
    }

    private File getFile() {
        try {
            if (file == null) {
                file = new File(getFileName());
            }
            if (!file.exists()) {
                Log.d("===", "Created new file" + fileName);
                file.createNewFile();
            }
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private FileOutputStream getOutputStream() {
        if (outputStream == null) {
            try {
                if (this.getFile() != null) {
                    outputStream = new FileOutputStream(this.getFile());
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return outputStream;
    }


    public boolean writeToCSV(List<TodoItemBean> todoList) {

        if (todoList != null && todoList.size() != 0) {
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(this.getFileName()));
                for (int i = 0; i < todoList.size(); i++) {
                    TodoItemBean todo = todoList.get(i);
                    StringBuilder line = new StringBuilder();
                    line.append(todo.getTitle());
                    line.append(",");
                    line.append(todo.getTimeLeft());
                    line.append(",");
                    line.append(todo.getTimeUsed());
                    if (i < todoList.size() - 1) {
                        line.append("\n");
                    }
                    bw.write(line.toString(), 0, line.length());
                }
                bw.flush();
                bw.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public List<TodoItemBean> readFromCSV() {
        List<TodoItemBean> todolist = new ArrayList<TodoItemBean>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.getFileName()));
            String strLine = null;
            while ((strLine = br.readLine()) != null) {
                String[] values = strLine.split(",");
                TodoItemBean todo = new TodoItemBean();
                todo.setTitle(values[0]);
                todo.setTimeLeft(Integer.parseInt(values[1]));
                todo.setTimeUsed(Integer.parseInt(values[2]));
                todolist.add(todo);
            }
            br.close();
            return todolist;
        } catch (Exception e) {
            e.printStackTrace();
            return todolist;
        }
    }


}
