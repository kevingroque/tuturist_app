package com.huich.roque.app.tuturist_app;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.huich.roque.app.tuturist_app.adpters.PostsRecyclerViewAdapter;
import com.huich.roque.app.tuturist_app.models.Post;

import java.util.ArrayList;
import java.util.List;

public class PostActivity extends AppCompatActivity {

    private RecyclerView mPostListRecycler;
    private List<Post> postList;
    private FloatingActionButton mCrearPostFab;

    private PostsRecyclerViewAdapter mPostAdapter;

    private FirebaseFirestore mFirestore;
    private FirebaseAuth mFirebaseAuth;

    private DocumentSnapshot mLastVisible;
    private Boolean isFirstPageFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        showToolbar("", true);

        mFirestore = FirebaseFirestore.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();

        postList = new ArrayList<>();

        mCrearPostFab = (FloatingActionButton) findViewById(R.id.post_fab_agregar);
        mPostListRecycler = (RecyclerView) findViewById(R.id.post_recycler_postlist);

        mPostAdapter = new PostsRecyclerViewAdapter(this,postList);
        mPostListRecycler.setHasFixedSize(true);
        mPostListRecycler.setLayoutManager(new LinearLayoutManager(this));
        mPostListRecycler.setAdapter(mPostAdapter);

        if(mFirebaseAuth.getCurrentUser() != null) {
            mPostListRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    Boolean reachedBottom = !recyclerView.canScrollVertically(1);

                    if (reachedBottom) {
                        loadMorePost();
                    }
                }
            });
        }

        Query firstQuery = mFirestore.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(10);
        firstQuery.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                if (!documentSnapshots.isEmpty()) {

                    if (isFirstPageFirstLoad) {
                        mLastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                        postList.clear();
                    }

                    for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {

                        if (doc.getType() == DocumentChange.Type.ADDED) {

                            String postId = doc.getDocument().getId();
                            Post post = doc.getDocument().toObject(Post.class).withId(postId);

                            if (isFirstPageFirstLoad) {
                                postList.add(post);
                            } else {
                                postList.add(0, post);
                            }
                            mPostAdapter.notifyDataSetChanged();
                        }
                    }
                    isFirstPageFirstLoad = false;
                }
            }
        });

        mCrearPostFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PostActivity.this, CrearPostActivity.class);
                startActivity(intent);
            }
        });

    }

    public void loadMorePost(){

        if(mFirebaseAuth.getCurrentUser() != null) {

            Query nextQuery = mFirestore.collection("posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(mLastVisible)
                    .limit(3);

            nextQuery.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                    if (!documentSnapshots.isEmpty()) {

                        mLastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                        for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {

                            if (doc.getType() == DocumentChange.Type.ADDED) {

                                String blogPostId = doc.getDocument().getId();
                                Post post = doc.getDocument().toObject(Post.class).withId(blogPostId);
                                postList.add(post);

                                mPostAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            });
        }
    }

    public void showToolbar(String titulo, Boolean upButton){
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        TextView title = (TextView)findViewById(R.id.toolbar_tittle);
        title.setText("Posts");
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(titulo);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_chevron_left);
        getSupportActionBar().setDisplayHomeAsUpEnabled(upButton);
    }

}
