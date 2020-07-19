package com.gowtham.library.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.crystal.crystalrangeseekbar.interfaces.OnSeekbarFinalValueListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;
import com.crystal.crystalrangeseekbar.widgets.CrystalSeekbar;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.gowtham.library.R;
import com.gowtham.library.utils.Constants;
import com.gowtham.library.utils.LogMessage;
import com.gowtham.library.utils.TrimmerUtils;

import java.io.File;

public class ActVideoTrimmer extends AppCompatActivity {

    private PlayerView playerView;

    private static final int PER_REQ_CODE = 115;

    private SimpleExoPlayer videoPlayer;

    private ImageView imagePlayPause;

    private ImageView[] imageViews;

    private long totalDuration;

    private Uri uri;

    private TextView txtStartDuration, txtEndDuration;

    private CrystalRangeSeekbar seekbar;

    private long lastMinValue = 0;

    private long lastMaxValue = 0, totalVideoDuration;

    private MenuItem menuDone;

    private CrystalSeekbar seekbarController;

    private boolean isValidVideo = true, isVideoEnded;

    private Handler seekHandler;

    private FFmpeg ffmpeg;

    private long currentDuration;

    private AlertDialog alertDialog;

    private String outputPath, destinationPath;

    private int trimType;

    private long fixedGap, minGap, minFromGap, maxToGap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_video_trimmer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setUpToolBar(getSupportActionBar(), R.string.txt_empty);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        playerView = findViewById(R.id.player_view);
        imagePlayPause = findViewById(R.id.image_play_pause);
        seekbar = findViewById(R.id.range_seek_bar);
        txtStartDuration = findViewById(R.id.txt_start_duration);
        txtEndDuration = findViewById(R.id.txt_end_duration);
        seekbarController = findViewById(R.id.seekbar_controller);
        ImageView imageOne = findViewById(R.id.image_one);
        ImageView imageTwo = findViewById(R.id.image_two);
        ImageView imageThree = findViewById(R.id.image_three);
        ImageView imageFour = findViewById(R.id.image_four);
        ImageView imageFive = findViewById(R.id.image_five);
        ImageView imageSix = findViewById(R.id.image_six);
        ImageView imageSeven = findViewById(R.id.image_seven);
        ImageView imageEight = findViewById(R.id.image_eight);
        imageViews = new ImageView[]{imageOne, imageTwo, imageThree,
                imageFour, imageFive, imageSix, imageSeven, imageEight};
        seekHandler = new Handler();
        initPlayer();
        loadFFMpegBinary();

        String uriStr = getIntent().getStringExtra(Constants.TRIM_VIDEO_URI);
        if (uriStr == null)
            throw new NullPointerException("VideoUri can't be null");
        else if (checkStoragePermission())
            setDataInView();

    }

    private void setUpToolBar(ActionBar actionBar, int title) {
        try {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPlayer() {
        try {
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelector trackSelector =
                    new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(bandwidthMeter));
            videoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
            playerView.requestFocus();
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            playerView.setPlayer(videoPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setDataInView() {
        try {
            uri = Uri.parse(getIntent().getStringExtra(Constants.TRIM_VIDEO_URI));
            uri = Uri.parse(TrimmerUtils.getvideoPath(this, uri));
            totalDuration = TrimmerUtils.getVideoDuration(this, uri);
            trimType = getIntent().getIntExtra(Constants.TRIM_TYPE, 0);
            fixedGap = getIntent().getLongExtra(Constants.FIXED_GAP_DURATION, totalDuration);
            minGap = getIntent().getLongExtra(Constants.MIN_GAP_DURATION, totalDuration);
            minFromGap = getIntent().getLongExtra(Constants.MIN_FROM_DURATION, totalDuration);
            maxToGap = getIntent().getLongExtra(Constants.MAX_TO_DURATION, totalDuration);
            destinationPath = getIntent().getStringExtra(Constants.DESTINATION);
            validate();
            imagePlayPause.setOnClickListener(v ->
                    onVideoClicked());
            playerView.getVideoSurfaceView().setOnClickListener(v ->
                    onVideoClicked());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validate() {
        if (trimType > 3 || trimType < 0)
            throw new IllegalArgumentException("Invalid trim type" + " " + trimType);
        else if (fixedGap <= 0)
            throw new IllegalArgumentException("Invalid fixedgap duration" + " " + fixedGap);
        else if (minGap <= 0)
            throw new IllegalArgumentException("Invalid mingap duration" + " " + minGap);
        else if (minFromGap <= 0)
            throw new IllegalArgumentException("Invalid minFromGap duration" + " " + minFromGap);
        else if (maxToGap <= 0)
            throw new IllegalArgumentException("Invalid maxToGap duration" + " " + maxToGap);
        else if (trimType == 3) {
            if (minFromGap == maxToGap)
                throw new IllegalArgumentException("Min_from_duration and Max_to_duration are same..you could use Fixed gap");
            else if (minFromGap > maxToGap)
                throw new IllegalArgumentException("Min_from_duration must be smaller than Max_to_duration" + " " +
                        Constants.MIN_FROM_DURATION + ":" + minFromGap + " " +
                        Constants.MAX_TO_DURATION + ":" + maxToGap);
        }else if (destinationPath!=null){
            File outputDir= new File(destinationPath);
            outputDir.mkdirs();
            destinationPath= String.valueOf(outputDir);
            if (!outputDir.isDirectory())
                throw new IllegalArgumentException("Destination file path error" + " " + destinationPath);
        }
        buildMediaSource(uri);
    }

    private void onVideoClicked() {
        try {
            if (isVideoEnded) {
                seekTo(lastMinValue);
                videoPlayer.setPlayWhenReady(true);
                return;
            }
            if ((currentDuration - lastMaxValue) > 0)
                seekTo(lastMinValue);
            videoPlayer.setPlayWhenReady(!videoPlayer.getPlayWhenReady());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seekTo(long sec) {
        if (videoPlayer != null)
            videoPlayer.seekTo(sec * 1000);
    }

    private void buildMediaSource(Uri mUri) {
        try {
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                    Util.getUserAgent(this, getString(R.string.app_name)), bandwidthMeter);
            MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mUri);
            videoPlayer.prepare(videoSource);
            videoPlayer.setPlayWhenReady(true);
            videoPlayer.addListener(new Player.DefaultEventListener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    switch (playbackState) {
                        case Player.STATE_BUFFERING:
                            LogMessage.v("onPlayerStateChanged: Buffering video.");
                            break;
                        case Player.STATE_ENDED:
                            LogMessage.v("onPlayerStateChanged: Video ended.");
                            imagePlayPause.setVisibility(View.VISIBLE);
                            isVideoEnded = true;
                            break;
                        case Player.STATE_IDLE:
                            LogMessage.v("onPlayerStateChanged: Player.STATE_IDLE");
                            break;
                        case Player.STATE_READY:
                            isVideoEnded = false;
                            totalVideoDuration = videoPlayer.getDuration() / 1000;
                            startProgress();
                            imagePlayPause.setVisibility(videoPlayer.getPlayWhenReady() ? View.GONE :
                                    View.VISIBLE);
                            LogMessage.v("onPlayerStateChanged: Ready to play.");
                            break;
                        default:
                            break;
                    }
                }
            });
            setImageBitmaps();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setImageBitmaps() {
        try {
            long diff = totalDuration / 8;
            new Handler().postDelayed(() -> {
                int index = 1;
                for (ImageView img : imageViews) {
                    img.setImageBitmap(TrimmerUtils.getFrameBySec(ActVideoTrimmer.this, uri, diff * index));
                    index++;
                }
                seekbar.setVisibility(View.VISIBLE);
                txtStartDuration.setVisibility(View.VISIBLE);
                txtEndDuration.setVisibility(View.VISIBLE);
            }, 1000);

            seekbarController.setMaxValue(totalDuration).apply();
            seekbar.setMaxValue(totalDuration).apply();
            seekbar.setMaxStartValue((float) totalDuration).apply();
            if (trimType == 1) {
                seekbar.setFixGap(fixedGap).apply();
                lastMaxValue = totalDuration;
            } else if (trimType == 2) {
                seekbar.setMaxStartValue((float) minGap);
                seekbar.setGap(minGap).apply();
                lastMaxValue = totalDuration;
            } else if (trimType == 3) {
                seekbar.setMaxStartValue((float) maxToGap);
                seekbar.setGap(minFromGap).apply();
                lastMaxValue = maxToGap;
            } else {
                seekbar.setGap(2).apply();
                lastMaxValue = totalDuration;
            }

            seekbar.setOnRangeSeekbarChangeListener((minValue, maxValue) -> {
                long minVal = (long) minValue;
                long maxVal = (long) maxValue;
                if (lastMinValue != minVal)
                    seekTo((long) minValue);
                lastMinValue = minVal;
                lastMaxValue = maxVal;
                txtStartDuration.setText(TrimmerUtils.formatSeconds(minVal));
                txtEndDuration.setText(TrimmerUtils.formatSeconds(maxVal));
                if (trimType == 3)
                    setDoneColor(minVal, maxVal);
            });

            seekbarController.setOnSeekbarFinalValueListener(new OnSeekbarFinalValueListener() {
                @Override
                public void finalValue(Number value) {
                    long value1 = (long) value;
                    if (value1 < lastMaxValue && value1 > lastMinValue) {
                        seekTo(value1);
                        return;
                    }
                    if (value1 > lastMaxValue)
                        seekbarController.setMinStartValue((int) lastMaxValue).apply();
                    else if (value1 < lastMinValue) {
                        seekbarController.setMinStartValue((int) lastMinValue).apply();
                        if (videoPlayer.getPlayWhenReady())
                            seekTo(lastMinValue);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDoneColor(long minVal, long maxVal) {
        try {
            if (menuDone == null)
                return;
            if ((maxVal - minVal) <= maxToGap) {
                menuDone.getIcon().setColorFilter(
                        new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorWhite)
                                , PorterDuff.Mode.SRC_IN)
                );
                isValidVideo = true;
            } else {
                menuDone.getIcon().setColorFilter(
                        new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorWhiteLt)
                                , PorterDuff.Mode.SRC_IN)
                );
                isValidVideo = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFFMpegBinary() {
        try {
            if (ffmpeg == null) {
                LogMessage.v("ffmpeg : null");
                ffmpeg = FFmpeg.getInstance(this);
            }
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnSupportedDialog();
                    LogMessage.v("Ffmpeg Failed");
                }

                @Override
                public void onSuccess() {
                    LogMessage.v("Ffmpeg loaded successfully");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            LogMessage.v("FFmpegNotSupportedException:: " + e.getMessage());
            showUnSupportedDialog();
        } catch (Exception e) {
            LogMessage.v("Exception:: " + e.getMessage());
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PER_REQ_CODE) {
            if (isPermissionOk(grantResults))
                setDataInView();
            else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoPlayer != null)
            videoPlayer.release();
        stopRepeatingTask();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuDone = menu.findItem(R.id.action_done);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done) {
            validateVideo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void validateVideo() {
        if (isValidVideo) {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "";
            if (destinationPath != null)
                path= destinationPath;

            File newFile = new File(path + File.separator +
                    System.currentTimeMillis() + "." + TrimmerUtils.getFileExtension(this, uri));
            outputPath = String.valueOf(newFile);
            LogMessage.v("outputPath::" + outputPath);
            String[] complexCommand = {"-y", "-i", String.valueOf(uri), "-ss", "" + lastMinValue, "-t", "" + (lastMaxValue - lastMinValue), "-c", "copy", outputPath};
//            String[] complexCommand1 = {"-ss", "" + lastMinValue, "-y", "-i", String.valueOf(uri), "-t", "" + (lastMaxValue - lastMinValue), "-s", "320x240", "-r", "15", "-vcodec", "mpeg4", "-b:v", "2097152", "-b:a", "48000", "-ac", "2", "-ar", "22050", outputPath};
            execFFmpegBinary(complexCommand);
        } else
            Toast.makeText(this,getString(R.string.txt_smaller) + " " + maxToGap + "Secs",Toast.LENGTH_SHORT).show();

    }

    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onSuccess(String message) {
                    LogMessage.v("FFmpeg onSuccess:: " + message);
                }

                @Override
                public void onProgress(String message) {
                    LogMessage.v("FFmpeg onProgress:: " + message);
                }

                @Override
                public void onFailure(String message) {
                    alertDialog.dismiss();
                    Toast.makeText(ActVideoTrimmer.this, R.string.txt_failed_to_trim,Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStart() {
                    videoPlayer.setPlayWhenReady(false);
                    showProcessingDialog();
                }

                @Override
                public void onFinish() {
                    new Handler().postDelayed(() -> {
                        alertDialog.dismiss();
                        Intent intent = new Intent();
                        intent.putExtra(Constants.TRIMMED_VIDEO_PATH, outputPath);
                        setResult(RESULT_OK, intent);
                        finish();
                    }, 1000);
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }


    private void showProcessingDialog() {
        try {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.alert_convert, null);
            dialogBuilder.setView(R.layout.alert_convert);
            TextView txtCancel = dialogView.findViewById(R.id.txt_cancel);
            alertDialog = dialogBuilder.create();

            txtCancel.setOnClickListener(v -> {
                alertDialog.dismiss();
                if (ffmpeg.isFFmpegCommandRunning())
                    ffmpeg.killRunningProcesses();
            });
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void showUnSupportedDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.txt_title_not_supported)
                    .setMessage(R.string.txt_msg_not_supported)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> finish())
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkStoragePermission() {
        return checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private boolean checkPermission(String... permissions) {
        boolean allPermitted = false;
        for (String permission : permissions) {
            allPermitted = (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED);
            if (!allPermitted)
                break;
        }
        if (allPermitted)
            return true;
        ActivityCompat.requestPermissions(this, permissions,
                PER_REQ_CODE);
        return false;
    }

    private boolean isPermissionOk(int... results) {
        boolean isAllGranted = true;
        for (int result : results) {
            if (PackageManager.PERMISSION_GRANTED != result) {
                isAllGranted = false;
                break;
            }
        }
        return isAllGranted;
    }

    void startProgress() {
        updateSeekbar.run();
    }

    void stopRepeatingTask() {
        seekHandler.removeCallbacks(updateSeekbar);
    }

    Runnable updateSeekbar = new Runnable() {
        @Override
        public void run() {
            try {
                currentDuration = videoPlayer.getCurrentPosition() / 1000;
                if (!videoPlayer.getPlayWhenReady())
                    return;
                if (currentDuration <= lastMaxValue)
                    seekbarController.setMinStartValue((int) currentDuration).apply();
                else
                    videoPlayer.setPlayWhenReady(false);
            } finally {
                seekHandler.postDelayed(updateSeekbar, 1000);
            }
        }
    };
}