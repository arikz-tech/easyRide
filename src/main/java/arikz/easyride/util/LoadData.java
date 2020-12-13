package arikz.easyride.util;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Objects;

import arikz.easyride.models.Ride;
import arikz.easyride.models.UserInRide;
import arikz.easyride.ui.main.requests.RequestsAdapter;
import arikz.easyride.adapters.RidesAdapter;

public class LoadData {
    private static String TAG = ".LoadData";
    private long numOfUsers;
    private List<Ride> itemsList;
    private RidesAdapter ridesAdapter;
    private RequestsAdapter requestsAdapter;
    private ProgressBar pb;
    private ImageView ivNoData;
    private MaterialTextView tvNoData;

    public LoadData() {
    }

    /*Load rides data from firebase, first get current user id and iterate throw his ride's id(RID) and for
     * every RID check if the current user is in ride,
     * if user is in ride(inRide==true) add the ride into the rides list*/
    public void load() {
        /*Listener class that's adding ride into the rides array list*/
        class AddRideListener implements ValueEventListener {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Ride ride = snapshot.getValue(Ride.class);
                itemsList.add(ride);
                decrement(); // Decrement the amount of running threads

                /*Check if database finished load everything, numOfUser increment thread amount
                 by one when called and decrement amount by one when finished, only
                  when all thread finished notify data has changed to recycler view shut down progress bar*/
                if (isLoaded()) {
                    if (requestsAdapter == null) {
                        ridesAdapter.notifyDataSetChanged(); // update recyclerView changes
                    } else
                        requestsAdapter.notifyDataSetChanged();
                    pb.setVisibility(View.INVISIBLE);

                    if(itemsList.isEmpty()){
                        ivNoData.setVisibility(View.VISIBLE);
                        tvNoData.setVisibility(View.VISIBLE);
                    }else{
                        ivNoData.setVisibility(View.INVISIBLE);
                        tvNoData.setVisibility(View.INVISIBLE);
                    }
                }



            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }

        }

        /*Listener class that's iterate throw all users in ride,
         * check if the current user in the current ride(uid and rid transferred by constructor)
         * is in ride and if he approved it */
        class RideUsersListener implements ValueEventListener {
            DatabaseReference db = FirebaseDatabase.getInstance().getReference(); //firebase database reference
            String currentRid, currentUID;

            public RideUsersListener(String currentRid, String currentUID) {
                this.currentRid = currentRid;
                this.currentUID = currentUID;
            }

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    UserInRide userInRide = snap.getValue(UserInRide.class);

                    /*Parameter that's present if the user is approved ride*/
                    boolean inRide = Objects.requireNonNull(userInRide).isInRide();
                    String uid = userInRide.getUid();

                    /*check if this particular user object from loop is equals to current user id
                     *and if the user approved ride, if its true then call "AddRide" listener*/
                    if (uid != null)
                        if (uid.equals(currentUID) && inRide && requestsAdapter == null) {
                            increment(); //Increment the amount of threads by 1
                            /*Call "AddRide" listener, and user database reference to get all ride info from database*/
                            AddRideListener addRide = new AddRideListener();
                            db.child("rides").child(currentRid).addListenerForSingleValueEvent(addRide);
                        } else if (uid.equals(currentUID) && !inRide && ridesAdapter == null) {
                            increment(); //Increment the amount of threads by 1
                            /*Call "AddRide" listener, and user database reference to get all ride info from database*/
                            AddRideListener addRide = new AddRideListener();
                            db.child("rides").child(currentRid).addListenerForSingleValueEvent(addRide);
                        }

                }

                //TODO Show missing rides description
                if (isLoaded()) {
                    if (requestsAdapter == null) {
                        ridesAdapter.notifyDataSetChanged(); // update recyclerView changes
                    } else
                        requestsAdapter.notifyDataSetChanged();
                    pb.setVisibility(View.INVISIBLE);

                    if(itemsList.isEmpty()) {
                        ivNoData.setVisibility(View.VISIBLE);
                        tvNoData.setVisibility(View.VISIBLE);
                    }else{
                        ivNoData.setVisibility(View.INVISIBLE);
                        tvNoData.setVisibility(View.INVISIBLE);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        }

        /*Listener class that's iterate throw all particular user's rides from database, and for every ride
         *call RideUser listener to iterate all users for this specific ride*/
        class UserRidesListener implements ValueEventListener {
            DatabaseReference db = FirebaseDatabase.getInstance().getReference(); //firebase database reference
            String currentUID;

            public UserRidesListener(String currentUID) {
                this.currentUID = currentUID;
            }

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.getChildrenCount() == 0){
                    pb.setVisibility(View.INVISIBLE);
                    ivNoData.setVisibility(View.VISIBLE);
                    tvNoData.setVisibility(View.VISIBLE);
                }


                for (DataSnapshot snap : snapshot.getChildren()) {
                    String currentRID = snap.getValue(String.class);
                    if (currentRID != null) {
                        /*child==ride, for every ride in the database call RideUser listener to check if current user
                         *approved his ride*/
                        RideUsersListener rideUsers = new RideUsersListener(currentRID, currentUID);
                        db.child("rideUsers").child(currentRID).addListenerForSingleValueEvent(rideUsers);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            }
        }

        DatabaseReference db = FirebaseDatabase.getInstance().getReference(); //firebase database reference
        String uid = getCurrentUserId(); // Save current user id

        /*Creating UserRide listener to iterate all user's rides*/
        UserRidesListener userRides = new UserRidesListener(uid);
        if (uid != null)
            db.child("userRides").child(uid).addListenerForSingleValueEvent(userRides);

    }

    private synchronized boolean isLoaded() {
        return numOfUsers == 0;
    }

    private synchronized void decrement() {
        numOfUsers--;
    }

    private synchronized void increment() {
        numOfUsers++;
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            return user.getUid();
        else
            return null;
    }

    public void setItemsList(List<Ride> itemsList) {
        this.itemsList = itemsList;
    }

    public void setRidesAdapter(RidesAdapter ridesAdapter) {
        this.ridesAdapter = ridesAdapter;
    }

    public void setRequestsAdapter(RequestsAdapter requestsAdapter) {
        this.requestsAdapter = requestsAdapter;
    }

    public void setProgressBar(ProgressBar pb) {
        this.pb = pb;
    }

    public void setIvNoData(ImageView ivNoData) {
        this.ivNoData = ivNoData;
    }

    public void setTvNoData(MaterialTextView tvNoData) {
        this.tvNoData = tvNoData;
    }
}
