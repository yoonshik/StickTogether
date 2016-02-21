package com.yoonshikhong.sticktogether;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.TreeMap;



public class ContactListActivity extends Activity {

    private final String TAG = "ContactListActivity";

    MyCustomAdapter dataAdapter = null;
    final Activity myActivity = this;
    private ArrayList<Contact> contacts;
    private TreeMap<String, String> contactNamesNumbers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        contacts = new ArrayList<Contact>();
        contactNamesNumbers = new TreeMap<String,String>();

        Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null);
        while (cursor.moveToNext()) {
            final String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            final String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
            final String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            if("1".equals(hasPhone) || Boolean.parseBoolean(hasPhone)) {
                // You know it has a number so now query it like this
                final Cursor phones = myActivity.getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId, null, null);
                while (phones!=null&&phones.moveToNext()) {
                    String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    Log.i(TAG, name + ' ' + phoneNumber);
                    if (!contactNamesNumbers.containsKey(name)) {
                        contactNamesNumbers.put(name, phoneNumber);
                    }
                }
                if (phones!=null){
                    phones.close();
                }
            }
        }

        for (String name : contactNamesNumbers.keySet()) {
            contacts.add(new Contact(contactNamesNumbers.get(name), name, false));
        }

        //Generate list View from ArrayList
        displayListView();

        checkButtonClick();

    }

    private void displayListView() {

        //create an ArrayAdaptar from the String Array
        dataAdapter = new MyCustomAdapter(this,
                R.layout.contact_info, contacts);
        ListView listView = (ListView) findViewById(R.id.listView1);
        // Assign adapter to ListView
        listView.setAdapter(dataAdapter);

        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the TextView text
                Contact contact = (Contact) parent.getItemAtPosition(position);
                Toast.makeText(getApplicationContext(),
                        "Clicked on Row: " + contact.getName(),
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private class MyCustomAdapter extends ArrayAdapter<Contact> {

        private ArrayList<Contact> contactList;

        public MyCustomAdapter(Context context, int textViewResourceId,
                               ArrayList<Contact> contactList) {
            super(context, textViewResourceId, contactList);
            this.contactList = new ArrayList<Contact>();
            this.contactList.addAll(contactList);
        }

        private class ViewHolder {
            TextView code;
            CheckBox name;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;
            Log.v("ConvertView", String.valueOf(position));

            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.contact_info, null);

                holder = new ViewHolder();
                holder.name = (CheckBox) convertView.findViewById(R.id.checkBox1);
                convertView.setTag(holder);

                holder.name.setOnClickListener( new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v;
                        Contact contact = (Contact) cb.getTag();
                        Toast.makeText(getApplicationContext(),
                                "Clicked on Checkbox: " + cb.getText() +
                                        " is " + cb.isChecked(),
                                Toast.LENGTH_LONG).show();
                        contact.setSelected(cb.isChecked());
                    }
                });
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }

            Contact contact = contactList.get(position);

            holder.name.setText(contact.getName());
            holder.name.setChecked(contact.isSelected());
            holder.name.setTag(contact);

            return convertView;

        }

    }

    private void checkButtonClick() {


        Button myButton = (Button) findViewById(R.id.findSelected);
        myButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                StringBuffer responseText = new StringBuffer();
                responseText.append("The following were selected...\n");

                ArrayList<Contact> contactList = dataAdapter.contactList;

                ArrayList<String> names = new ArrayList<String>();
                ArrayList<String> numbers = new ArrayList<String>();

                for(int i=0;i< contactList.size();i++){
                    Contact contact = contactList.get(i);
                    if(contact.isSelected()){
                        responseText.append("\n" + contact.getName());
                        names.add(contact.getName());
                        numbers.add(contact.getCode());
                    }
                }

                Toast.makeText(getApplicationContext(),
                        responseText, Toast.LENGTH_LONG).show();

                Bundle b = new Bundle();
                b.putStringArrayList("names", names);
                b.putStringArrayList("numbers", numbers);
                Intent i = getIntent(); //gets the intent that called this intent
                i.putExtras(b);
                setResult(Activity.RESULT_OK, i);
                finish();

            }
        });

    }

    class Contact {

        String code = null;
        String name = null;
        boolean selected = false;

        public Contact(String code, String name, boolean selected) {
            super();
            this.code = code;
            this.name = name;
            this.selected = selected;
        }

        public String getCode() {
            return code;
        }
        public void setCode(String code) {
            this.code = code;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public boolean isSelected() {
            return selected;
        }
        public void setSelected(boolean selected) {
            this.selected = selected;
        }

    }

}