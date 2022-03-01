package com.sahandilshan.voice1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class player extends AppCompatActivity implements SongChangeListener {

    private final List<MusicList> musicLists = new ArrayList<>();
    private RecyclerView musicRecyclerView;
    private MediaPlayer mediaPlayer;
    private TextView endTime,startTime;
    private boolean isPlaying = false;
    private SeekBar playerSeekBar;
    private ImageView playPauseImg;
    private Timer timer;
    private int currentSongListPosition = 0;
    private MusicAdapter musicAdapter;


    //*****
    EditText voiceHint;
    ImageButton voice_btn;
    final int VOICE_CODE = 100;
    String output;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        View decodeView = getWindow().getDecorView();

        int options =View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decodeView.setSystemUiVisibility(options);

        setContentView(R.layout.activity_player);


        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        final CardView playPauseCard = findViewById(R.id.playPauseCard);
        playPauseImg = findViewById(R.id.playPauseImg);
        final ImageView nextBtn = findViewById(R.id.nextBtn);
        final ImageView previousBtn = findViewById(R.id.previousBtn);
        playerSeekBar = findViewById(R.id.playerSeekBar);

        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);

        musicRecyclerView.setHasFixedSize(true);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mediaPlayer = new MediaPlayer();


        //******
        voiceHint = (EditText) findViewById(R.id.voicehint);
        voice_btn = (ImageButton) findViewById(R.id.voice_btn);

        voice_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                voice_to_text();
            }
        });


        //@@@@@@

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            getMusicFiles();
        }
        else {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},11);
            }
            else{
                getMusicFiles();
            }
        }

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                playNext();

            }
        });



       previousBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {

               playPrevious();

           }
       });



        playPauseCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               playPause();

            }
        });


        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if(fromUser){
                    if(isPlaying){
                        mediaPlayer.seekTo(progress);
                    }
                    else {
                        mediaPlayer.seekTo(0);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    //****
    private void voice_to_text() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Voice2Text \n Say Something!!");
        try {
            startActivityForResult(intent, VOICE_CODE);
        } catch (ActivityNotFoundException e) {

        }
    }


    // receive voice input and set it to textbox(voiceHint)

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case VOICE_CODE: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    voiceHint.setText(result.get(0));
                    output = result.get(0);

                    if(output.equals("next") || output.equals("next song") || output.equals("play next") ){
                        Toast.makeText(this, output, Toast.LENGTH_SHORT).show();
                        playNext();
                        output = "";
                    }

                    else if(output.equals("previous") || output.equals("previous song") || output.equals("play previous") ){
                        Toast.makeText(this, output, Toast.LENGTH_SHORT).show();
                        playPrevious();
                        output = "";
                    }

                    else if(output.equals("play") || output.equals("pause") || output.equals("play song")|| output.equals("pause song") ){
                        Toast.makeText(this, output, Toast.LENGTH_SHORT).show();
                        playPause();
                        output = "";
                    }

                    else if(output.equals("go back") || output.equals("back") || output.equals("go to home") || output.equals("home")){
                        Toast.makeText(this, output, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent (player.this,MainActivity.class);
                        startActivity(intent);
                        output = "";
                    }

                    else {
                        Toast.makeText(this, "Invalid Command", Toast.LENGTH_SHORT).show();
                        output = "";
                    }


                }
                break;
            }

        }
    }





    //@@@@@@

    private void getMusicFiles(){

        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri,null,MediaStore.Audio.Media.DATA+" Like?",new String[]{"%.mp3%"},null);

        if(cursor == null){
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
        }else if(!cursor.moveToNext()){
            Toast.makeText(this, "No Music Found", Toast.LENGTH_SHORT).show();
        }else{
            while (cursor.moveToNext()){
                final String getMusicFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                final String getArtistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,cursorId);
                String getDuration ="00:00";

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    getDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                }

                final MusicList musicList = new MusicList(getMusicFileName,getArtistName,getDuration,false,musicFileUri);
                musicLists.add(musicList);

            }

            musicAdapter = new MusicAdapter(musicLists,player.this);
            musicRecyclerView.setAdapter(musicAdapter);
        }

        cursor.close();

    }




    private void playNext(){
        int nextSongListPosition = currentSongListPosition + 1;

        if(nextSongListPosition >= musicLists.size()){
            nextSongListPosition = 0;
        }

        musicLists.get(currentSongListPosition).setPlaying(false);
        musicLists.get(nextSongListPosition).setPlaying(true);

        musicAdapter.updateList(musicLists);

        musicRecyclerView.scrollToPosition(nextSongListPosition);
        onChanged(nextSongListPosition);
    }

    private void playPrevious(){
        int prevSongListPosition = currentSongListPosition - 1;

        if(prevSongListPosition<0){
            prevSongListPosition = musicLists.size()-1;
        }

        musicLists.get(currentSongListPosition).setPlaying(false);
        musicLists.get(prevSongListPosition).setPlaying(true);

        musicAdapter.updateList(musicLists);

        musicRecyclerView.scrollToPosition(prevSongListPosition);
        onChanged(prevSongListPosition);
    }

    private void playPause(){
        if(isPlaying){
            isPlaying = false;
            mediaPlayer.pause();
            playPauseImg.setImageResource(R.drawable.play_icon);
        }
        else{
            isPlaying = true;
            mediaPlayer.start();
            playPauseImg.setImageResource(R.drawable.pause_btn);
        }
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            getMusicFiles();
        }
        else{
            Toast.makeText(this,"Permission Declined By User",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
       if(hasFocus){
           View decodeView = getWindow().getDecorView();

           int options =View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
           decodeView.setSystemUiVisibility(options);
       }
    }

    @Override
    public void onChanged(int position) {

        currentSongListPosition = position;

        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            mediaPlayer.reset();
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mediaPlayer.setDataSource(player.this,musicLists.get(position).getMusicFile());
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(player.this, "Unable to play track", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                final int getTotalDuration = mediaPlayer.getDuration();

                String generateDuration = String.format(Locale.getDefault(),"%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(getTotalDuration),
                        TimeUnit.MILLISECONDS.toSeconds(getTotalDuration) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getTotalDuration)));

                endTime.setText(generateDuration);
                isPlaying = true;

                mediaPlayer.start();
                playerSeekBar.setMax(getTotalDuration);
                playPauseImg.setImageResource(R.drawable.pause_btn);
            }
        });

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final int getCurrentDuration = mediaPlayer.getCurrentPosition();

                        String generateDuration = String.format(Locale.getDefault(),"%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration),
                                TimeUnit.MILLISECONDS.toSeconds(getCurrentDuration) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration)));

                        playerSeekBar.setProgress(getCurrentDuration);
                        startTime.setText(generateDuration);
                    }
                });
            }
        },1000,1000);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.reset();

                timer.purge();
                timer.cancel();

                isPlaying = false;

                playPauseImg.setImageResource(R.drawable.play_icon);
                playerSeekBar.setProgress(0);
            }
        });

    }
}