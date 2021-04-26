package cs310.trojancheckinout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cs310.trojancheckinout.models.User;

public class NavActivity extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentSnapshot userDoc;

    private LinearLayout popupMsg;
    public TextView close_message;
    private Button closePopupbutton;

    public String message = "";
    public static final int GET_FROM_GALLERY = 1;
    String role = "";
    Map<String,String> map = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav);
        Button profileButton = (Button) findViewById(R.id.profile_button);
        Button showBuildingsButton = (Button) findViewById(R.id.button);
        Button csvButton = (Button) findViewById(R.id.csv_button);
      //  errortxt = findViewById(R.id.errortxt);
        popupMsg = findViewById(R.id.pop_up_csv);
        close_message = findViewById(R.id.close_message_csv);
        closePopupbutton = findViewById(R.id.closePopupbutton_csv);

        DocumentReference docIdRef2 = db.collection("users").document(sharedData.getCurr_email());

        docIdRef2.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                userDoc = task.getResult();
                if (task.isSuccessful()) {
                    role = userDoc.getString("occupation");
                    if(role.compareTo("Manager")==0) {
                        showBuildingsButton.setVisibility(View.VISIBLE);
                        csvButton.setVisibility(View.VISIBLE);
                    } else {
                        showBuildingsButton.setVisibility(View.INVISIBLE);
                        csvButton.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileActivityIntent = new Intent(NavActivity.this, ProfileActivity.class);
                startActivityForResult(profileActivityIntent, 0);
            }
        });

        showBuildingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent buildingActivityIntent = new Intent(NavActivity.this, ShowAllBuildingsActivity.class);
                startActivityForResult(buildingActivityIntent, 0);
            }
        });

        Button checkInButton = (Button) findViewById(R.id.check_button);
        checkInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent checkActivityIntent = new Intent(NavActivity.this, CheckIn.class);
                startActivityForResult(checkActivityIntent, 0);
            }
        });



        csvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent mediaIntent = new Intent(Intent.ACTION_GET_CONTENT);
                mediaIntent.setType("*/*"); // Set MIME type as per requirement
                startActivityForResult(mediaIntent,GET_FROM_GALLERY);


            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Doc", "browse file");

        //Detects request codes
        if(requestCode==GET_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            Uri csvUri = data.getData();
            Log.d("Doc", "CSV URI= " + csvUri);

            try {
                //File csvFile = new File(csvUri.getPath());
                Log.d("Doc", "CSV path= " + csvUri.getPath());
                InputStream input = getContentResolver().openInputStream(csvUri);
                CSVReader reader = new CSVReader(new InputStreamReader(input)); //csvreader

                //ashna's code
                String[] line = reader.readNext();
                while ((line = reader.readNext()) != null) {
                    // Split the line into different tokens (using the comma as a separator).
                    // String[] tokens = line.split(",");
                    Log.d("FILE READER CSV","CSV " + line[0] + line[1]);

                    String building_name = line[0];
                    String building_cap = line[1];
                    map.put(building_name, building_cap);

                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "File not formatted properly", Toast.LENGTH_SHORT).show();
            } finally {
                Iterator it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    Log.d("iterator", pair.getKey() + " = " + pair.getValue());
                    checkBuildingMap((String)pair.getKey(), (String)pair.getValue());
                }

                popupMsg.setVisibility(View.VISIBLE);

                closePopupbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupMsg.setVisibility(View.INVISIBLE);
                        close_message.setText("");

                        map.clear();
                    }
                });
            }

        }else{
            Log.d("Doc", "BAD ERROR");
        }
    }


    public void checkBuildingMap(String building_name, String building_cap){
        // check if building exists in db
        DocumentReference docIdRef = db.collection("buildings").document(building_name);
        docIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (!document.exists()) {
                        // SHOW ERROR
                        String currErr = (String) close_message.getText();
                        close_message.setText(currErr + "\n" + building_name + " Building does not exist - Check file");
                        Log.d("document", "Document does not exist!");
                    }

                    else {
                        String cur_cap = document.getString("currCapacity");
                        double curcap = Double.parseDouble(cur_cap);
                        int newbuildingcap = Integer.parseInt(building_cap);
                        if(newbuildingcap < curcap){
                            String currErr = (String) close_message.getText();
                            close_message.setText(currErr + "\n" + building_name + "Current building capacity is greater than new capacity - Check file");
                            Log.d("document", "check file cannot update");

                        }
                        else{

                            changeDatabaseCapacity(building_cap, building_name);
                        }
                        Log.d("Document", "Document exists!");

                    }
                } else {
                    Log.d("document", "Failed with: ", task.getException());
                }
            }
        });


    }
    public void changeDatabaseCapacity(String newcap, String bName ){
        Log.d("in change cap", "in change cap " + bName + " " + newcap);
        DocumentReference currDoc = db.collection("buildings").document(bName);
        currDoc.update("maxCapacity", newcap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        String currErr = (String) close_message.getText();
                       // message =  currErr + "\n" + bName + " Capacity changed";
                        close_message.setText(currErr + "\n" + bName + " Capacity changed");

                        Log.d("Doc", "DocumentSnapshot successfully written! - change capacity asdf" +  message);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Doc", "Error writing document - change capacity", e);
                    }
                });
    }

}