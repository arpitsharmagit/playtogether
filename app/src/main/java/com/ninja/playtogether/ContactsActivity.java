package com.ninja.playtogether;

import android.app.SearchManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import adapters.ContactsAdapter;
import datastore.Store;
import extras.PlayApplication;
import extras.UtilityFunctions;
import model.Contact;
import model.User;

public class ContactsActivity extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView contactsView;
    private ContactsAdapter adapter;
    private static ArrayList<Contact> contacts;
    private List<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        contacts = filterContactsByUser();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        contactsView = findViewById(R.id.contact_list);
        adapter = new ContactsAdapter(this, contacts, new ContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Contact item) {

            }
        });
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        contactsView.setLayoutManager(mLayoutManager);
        contactsView.setItemAnimator(new DefaultItemAnimator());
        contactsView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        contactsView.setAdapter(adapter);
    }

    public ArrayList<Contact> filterContactsByUser(){
        ArrayList<Contact> filterContacts = new ArrayList<>();
        users = PlayApplication.users;
        contacts = PlayApplication.contacts;
        for (Contact contact:contacts) {
            if(users.contains(contact)){
                filterContacts.add(contact);
            }
        }
        return filterContacts;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);

        SearchManager searchManager = (SearchManager) getApplicationContext().getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                adapter.getFilter().filter(query);
                return false;
            }
        });


        return super.onCreateOptionsMenu(menu);
    }
}
