package com.ucsf.ui.admin;

/**
 * Created by yanrongli on 3/12/16.
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.ucsf.R;

/**
 * Created by yanrongli on 3/12/16.
 */
public class RegisterSensorTagActivity extends Activity {
    SensorTagDBHelper myDB;
    EditText sensortag_id;
    EditText entry_id;
    RadioGroup sensor_type_radio;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_sensortag);
        myDB = new SensorTagDBHelper(this);

        entry_id = (EditText) findViewById(R.id.input_id);
        sensortag_id = (EditText) findViewById(R.id.input_sensortag_id);
        sensor_type_radio = (RadioGroup) findViewById(R.id.input_sensor_type);
    }

    public void onClickInputEntry(View view) {
        if(sensor_type_radio.getCheckedRadioButtonId() == -1)
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Please select a sensor type!", Toast.LENGTH_SHORT).show();
            return;
        }
        String sensor_type_string = ((RadioButton) findViewById(sensor_type_radio.getCheckedRadioButtonId())).getText().toString();
        String sensortag_id_string = sensortag_id.getText().toString();
        if(sensortag_id_string.equals(""))
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Please input sensortag id!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isInserted = myDB.insertData(sensortag_id_string, sensor_type_string);
        if (isInserted == true)
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Entry insertion succeeded!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Entry insertion failed!", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickViewAll(View view) {
        Cursor res = myDB.getAllData();
        if(res.getCount() == 0) {
            showMessage("Error", "Nothing found");
            return;
        }

        StringBuffer buffer = new StringBuffer();
        while(res.moveToNext()) {
            buffer.append("Id: " + res.getString(0) + "\n");
            buffer.append("SensorTagId: " + res.getString(1) + "\n");
            buffer.append("SensorType: " + res.getString(2) + "\n\n");
        }
        showMessage("Data", buffer.toString());

    }

    public void onClickUpdateEntry(View view){
        if(sensor_type_radio.getCheckedRadioButtonId() == -1)
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Please select a sensor type!", Toast.LENGTH_SHORT).show();
            return;
        }
        String sensor_type_string = ((RadioButton) findViewById(sensor_type_radio.getCheckedRadioButtonId())).getText().toString();
        String sensortag_id_string = sensortag_id.getText().toString();
        String entry_id_string = entry_id.getText().toString();
        if(sensortag_id_string.equals(""))
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Please input sensortag id!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(entry_id_string.equals(""))
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Please input entry id!", Toast.LENGTH_SHORT).show();
        }

        boolean isUpdated = myDB.updateData(entry_id_string, sensortag_id_string, sensor_type_string);
        if (isUpdated == true)
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Entry update succeeded!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Entry update failed!", Toast.LENGTH_SHORT).show();
        }

    }

    public void onClickDeleteEntry(View view) {
        String entry_id_string = entry_id.getText().toString();
        if(entry_id_string.equals(""))
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Please input entry id!", Toast.LENGTH_SHORT).show();
        }
        Integer deleteRow = myDB.deleteData(entry_id_string);
        if (deleteRow >= 0)
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Entry deletion succeeded!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(RegisterSensorTagActivity.this, "Entry deletion failed!", Toast.LENGTH_SHORT).show();
        }
    }

    public void showMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }
}