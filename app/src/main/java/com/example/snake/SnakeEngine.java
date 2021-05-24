package com.example.snake;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;

class SnakeEngine extends SurfaceView implements Runnable {

    // Our game thread for the main game loop
    private Thread thread = null;

    // To hold a reference to the Activity
    private Context context;

    // for playing sound effects
    private SoundPool soundPool;
    private int eat_bob = -1;
    private int snake_crash = -1;

    // For tracking movement Heading
    public enum Heading {UP, RIGHT, DOWN, LEFT}
    // Start by heading to the right
    private Heading heading = Heading.RIGHT;

    // To hold the screen size in pixels
    private int screenX;
    private int screenY;

    // How long is the snake
    private int snakeLength;

    // Where is Bob hiding?
    private int bobX;
    private int bobY;

    // The size in pixels of a snake segment
    private int blockSize;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 18;
    private int numBlocksHigh;

    // Control pausing between updates
    private long nextFrameTime;
    // Update the game 10 times per second
    private final long FPS = 10;
    // There are 1000 milliseconds in a second
    private final long MILLIS_PER_SECOND = 1000;
// We will draw the frame much more often

    // How many points does the player have
    private int score;

    // The location in the grid of all the segments
    private int[] snakeXs;
    private int[] snakeYs;

    // Everything we need for drawing
// Is the game currently playing?
    private volatile boolean isPlaying;
    boolean isEasy;
    boolean isLeftWay;

    // A canvas for our paint
    private Canvas canvas;

    // Required to use canvas
    private SurfaceHolder surfaceHolder;

    // Some paint for our canvas
    Typeface typeface = getResources().getFont(R.font.font);
    private Paint paint;

    public SnakeEngine(Context context, Point size, String direction, String difficulty) {
        super(context);

        context = context;

        screenX = size.x;
        screenY = size.y;

        if (direction.equals("true"))
            isLeftWay = true;
        else isLeftWay = false;
        if (difficulty.equals("true"))
            isEasy = true;
        else isEasy = false;

                    // Work out how many pixels each block is
        blockSize = screenX / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        numBlocksHigh = screenY / blockSize;

        // Set the sound up
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            // Create objects of the 2 required classes
            // Use m_Context because this is a reference to the Activity
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Prepare the two sounds in memory
            descriptor = assetManager.openFd("eat_bob.ogg");
            eat_bob = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("snake_crash.ogg");
            snake_crash = soundPool.load(descriptor, 0);

        } catch (IOException e) {
            // Error
        }

        // Initialize the drawing objects
        surfaceHolder = getHolder();
        paint = new Paint();

        // If you score 200 you are rewarded with a crash achievement!
        snakeXs = new int[200];
        snakeYs = new int[200];

        // Start the game
        newGame();

    }

    @Override

    public void run() {

        while (isPlaying) {

            // Update 10 times a second
            if(updateRequired()) {
                update();
                draw();
            }

        }
    }

    public void pause() {
        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public void newGame() {
        // Start with a single snake segment
        snakeLength = 1;
        snakeXs[0] = 1;
        snakeYs[0] = 1;
        heading = Heading.RIGHT;
        // Get Bob ready for dinner
        spawnBob();

        // Reset the score
        score = 0;

        // Setup nextFrameTime so an update is triggered
        nextFrameTime = System.currentTimeMillis();

        //Toast.makeText(getContext(), "Easy: " + isEasy + ", Left way: " + isLeftWay, Toast.LENGTH_SHORT).show();
    }

    public void spawnBob() {
        Random random = new Random();
        bobX = random.nextInt(NUM_BLOCKS_WIDE - 1) + 1;
        bobY = random.nextInt(numBlocksHigh - 1) + 1;
    }

    private void eatBob(){
        //  Got him!
        // Increase the size of the snake
        snakeLength++;
        //replace Bob
        // This reminds me of Edge of Tomorrow. Oneday Bob will be ready!
        spawnBob();
        //add to the score
        score = score + 1;
        soundPool.play(eat_bob, 1, 1, 0, 0, 1);

    }

    private void moveSnake(){
        // Move the body
        for (int i = snakeLength; i > 0; i--) {
            // Start at the back and move it
            // to the position of the segment in front of it
            snakeXs[i] = snakeXs[i - 1];
            snakeYs[i] = snakeYs[i - 1];

            // Exclude the head because
            // the head has nothing in front of it
        }

        // Move the head in the appropriate heading
        switch (heading) {
            case UP:
                snakeYs[0]--;
                break;

            case RIGHT:
                snakeXs[0]++;
                break;

            case DOWN:
                snakeYs[0]++;
                break;

            case LEFT:
                snakeXs[0]--;
                break;
        }

        if (isEasy) {
            if (snakeXs[0] > NUM_BLOCKS_WIDE - 1)
                snakeXs[0] = 0;

            if (snakeYs[0] > numBlocksHigh - 1)
                snakeYs[0] = 0;

            if (snakeXs[0] < 0)
                snakeXs[0] = NUM_BLOCKS_WIDE - 1;

            if (snakeYs[0] < 0)
                snakeYs[0] = numBlocksHigh - 1;
        }
    }

    private boolean detectDeath(){
        // Has the snake died?
        boolean dead = false;

        // Hit the screen edge
        // Hard
        if (!isEasy) {
            if (snakeXs[0] == -1) dead = true;
            if (snakeXs[0] >= NUM_BLOCKS_WIDE) dead = true;
            if (snakeYs[0] == -1) dead = true;
            if (snakeYs[0] == numBlocksHigh) dead = true;
        }
        // Eaten itself?
        for (int i = snakeLength - 1; i > 0; i--) {
            if ((i > 4) && (snakeXs[0] == snakeXs[i]) && (snakeYs[0] == snakeYs[i])) {
                dead = true;
            }
        }
        return dead;
    }

    public void update() {
        // Did the head of the snake eat Bob?
        if (snakeXs[0] == bobX && snakeYs[0] == bobY) {
            eatBob();
        }

        moveSnake();

        if (detectDeath()) {
            //start again
            soundPool.play(snake_crash, 1, 1, 0, 0, 1);

            newGame();
        }
    }

    public void draw() {
        // Get a lock on the canvas
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

            canvas.drawColor(Color.argb(255, 100, 125, 90));
            // Set the color of the paint to draw the snake white
            paint.setColor(Color.argb(255, 50, 50, 50));

            // Scale the HUD text
            paint.setTextSize(60);
            paint.setTypeface(typeface);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Score: " + score, screenX / 2,  150, paint);

            // Draw the snake one block at a time
            for (int i = 0; i < snakeLength; i++) {
                canvas.drawRect(snakeXs[i] * blockSize,
                        (snakeYs[i] * blockSize),
                        (snakeXs[i] * blockSize) + blockSize,
                        (snakeYs[i] * blockSize) + blockSize,
                        paint);
            }

            paint.setColor(Color.argb(255, 255, 255, 255));

            // Draw Bob
            canvas.drawRect(bobX * blockSize,
                    (bobY * blockSize),
                    (bobX * blockSize) + blockSize,
                    (bobY * blockSize) + blockSize,
                    paint);

            // Unlock the canvas and reveal the graphics for this frame
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    public boolean updateRequired() {

        // Are we due to update the frame
        if(nextFrameTime <= System.currentTimeMillis()){
            // Tenth of a second has passed

            // Setup when the next update will be triggered
            nextFrameTime =System.currentTimeMillis() + MILLIS_PER_SECOND / FPS;

            // Return true so that the update and draw
            // functions are executed
            return true;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                if (!isLeftWay) {
                    switch(heading) {
                        case UP:
                            heading = Heading.RIGHT;
                            break;
                        case RIGHT:
                            heading = Heading.DOWN;
                            break;
                        case DOWN:
                            heading = Heading.LEFT;
                            break;
                        case LEFT:
                            heading = Heading.UP;
                            break;
                    }
                }
                else {
                    // se duce doar la stanga
                    switch (heading) {
                        case UP:
                            heading = Heading.LEFT;
                            break;
                        case LEFT:
                            heading = Heading.DOWN;
                            break;
                        case DOWN:
                            heading = Heading.RIGHT;
                            break;
                        case RIGHT:
                            heading = Heading.UP;
                            break;
                    }
                }
        }
        return true;
    }

}
