package com.example.protectpet;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.protectpet.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends Fragment {
    private RecyclerView blog_list_post;
    private FirebaseAuth firebaseAuth;
    private List<User> user_list;
    private DatabaseReference mDatabase;


    private List<BlogPost> blogPosts;
    private FirebaseFirestore firebaseFirestore;
    private BlogRecyclerAdapter blogRecyclerAdapter;
    private DocumentSnapshot lastVisible;
    private Boolean isFirstPageFirstLoad = true;
    private String location;

    public SearchFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_adopt, container, false);
        blogPosts = new ArrayList<>();
        user_list = new ArrayList<>();

        firebaseAuth = FirebaseAuth.getInstance();
        blog_list_post = view.findViewById(R.id.blog_list_view);
        blog_list_post.setLayoutManager(new LinearLayoutManager(container.getContext()));
        blogRecyclerAdapter = new BlogRecyclerAdapter(blogPosts,user_list);
        blog_list_post.setAdapter(blogRecyclerAdapter);
        blog_list_post.setHasFixedSize(true);

        SearchPage activity = (SearchPage) getActivity();
        location = activity.getlocation();
        //Log.i("loc",location);

        if(firebaseAuth.getCurrentUser() != null) {


            firebaseFirestore = FirebaseFirestore.getInstance();
            blog_list_post.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    Boolean reachedBottom = !recyclerView.canScrollVertically(1);
                    if(reachedBottom){
                        //String desc = lastVisible.getString("desc");
                        //Toast.makeText(container.getContext(),"Reached:"+desc, Toast.LENGTH_LONG).show();
                        loadMorePost();

                    }
                }
            });

            Query firstQuery = firebaseFirestore.collection("Posts").orderBy("timestamp",
                    Query.Direction.DESCENDING).whereEqualTo("location",location).limit(3);

            firstQuery.addSnapshotListener(getActivity(),new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                    if( e == null){
                        if (!documentSnapshots.isEmpty()) {
                            if (isFirstPageFirstLoad) {
                                lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                                blogPosts.clear();
                                user_list.clear();
                            }
                            for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {
                                if (doc.getType() == DocumentChange.Type.ADDED) {


                                    String blogPostId = doc.getDocument().getId();
                                    final BlogPost blogPost = doc.getDocument().toObject(BlogPost.class).withId(blogPostId);
                                    String blogUserId = doc.getDocument().getString("user_id");
                                    firebaseFirestore.collection("Users").document(blogUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                User user = task.getResult().toObject(User.class);

                                                if (isFirstPageFirstLoad) {
                                                    user_list.add(user);
                                                    blogPosts.add(blogPost);

                                                } else {
                                                    user_list.add(0, user);
                                                    blogPosts.add(0, blogPost);
                                                }
                                                blogRecyclerAdapter.notifyDataSetChanged();
                                            }
                                        }
                                    });


                                }

                            }

                            isFirstPageFirstLoad = false;
                        }
                    }}
            });
        }
        return  view;
    }


    public void loadMorePost(){

        if(firebaseAuth.getCurrentUser() != null) {

            Query nextQuery = firebaseFirestore.collection("Posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING).whereEqualTo("location",location).startAfter(lastVisible).limit(3);

            nextQuery.addSnapshotListener(getActivity( ), new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                    if( e == null){
                        if (!documentSnapshots.isEmpty()) {

                            lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                            for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {

                                if (doc.getType() == DocumentChange.Type.ADDED) {

                                    String blogPostId = doc.getDocument().getId();
                                    final BlogPost blogPost = doc.getDocument().toObject(BlogPost.class).withId(blogPostId);

                                    String blogUserId = doc.getDocument().getString("user_id");
                                    firebaseFirestore.collection("Users").document(blogUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if(task.isSuccessful()){
                                                User user = task.getResult().toObject(User.class);


                                                user_list.add(user);
                                                blogPosts.add(blogPost);

                                                blogRecyclerAdapter.notifyDataSetChanged();
                                            }
                                        }
                                    });
                                }

                            }
                        }

                    }}
            });

        }

    }


}
