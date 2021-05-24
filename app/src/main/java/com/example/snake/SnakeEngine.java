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

    // for playing sound effects - NOT WORKING
    private SoundPool soundPool;
    private int eat_bob = -1;
    private int snake_crash = -1;

    // directiile de deplasare
    public enum Heading {
        UP,
        RIGHT,
        DOWN,
        LEFT
    }
    // se deplaseaza din start spre dreapta
    private Heading heading = Heading.RIGHT;

    // dimensiunea ecranului
    private int screenX;
    private int screenY;

    // lungimea sarpelui
    private int snakeLength;

    // pozitia mancarii
    private int bobX;
    private int bobY;

    // dimensiunea unui bloc
    private int blockSize;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 18;
    private int numBlocksHigh;

    // Variabile pentru logica framerate-ului
    private long nextFrameTime;
    private final long FPS = 10;
    private final long MILLIS_PER_SECOND = 1000;

    // Scor
    private int score;

    // Pozitiile corpului
    private int[] snakeXs;
    private int[] snakeYs;

    // Variabile pentru modul de joc
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

        // Setarile jocurlui
        if (direction.equals("true"))
            isLeftWay = true;
        else isLeftWay = false;
        if (difficulty.equals("true"))
            isEasy = true;
        else isEasy = false;

        // Calcularea dimensiunii sarpelui
        blockSize = screenX / NUM_BLOCKS_WIDE;
        numBlocksHigh = screenY / blockSize;

        // COD CE NU MERGE
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

        // Scorul maxim este de 200
        snakeXs = new int[200];
        snakeYs = new int[200];

        // Start the game
        newGame();

    }

    @Override

    // Functia de rulare
    public void run() {
        while (isPlaying) {
            if(updateRequired()) {
                update();
                draw();
            }
        }
    }

    // Functia de pauza
    public void pause() {
        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }

    // Functia de restaurare joc
    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    // Functia de joc nou
    public void newGame() {

        // Pozitia si lung initiala
        snakeLength = 1;
        snakeXs[0] = 1;
        snakeYs[0] = 1;
        heading = Heading.RIGHT;

        // Se creeaza mancarea
        spawnBob();

        // Scor 0
        score = 0;

        // Setup nextFrameTime so an update is triggered
        nextFrameTime = System.currentTimeMillis();
    }

    // Functia pentru crearea mancarii (pozitie random pe ecran)
    public void spawnBob() {
        Random random = new Random();
        bobX = random.nextInt(NUM_BLOCKS_WIDE - 1) + 1;
        bobY = random.nextInt(numBlocksHigh - 1) + 1;
    }

    // Functia de mancare
    private void eatBob(){
        snakeLength++;
        score = score + 1;

        spawnBob();
        soundPool.play(eat_bob, 1, 1, 0, 0, 1);
    }

    // Functia de miscare a sarpelui
    private void moveSnake(){

        // Componentele din spate for urma pozitia varfului
        for (int i = snakeLength; i > 0; i--) {
            snakeXs[i] = snakeXs[i - 1];
            snakeYs[i] = snakeYs[i - 1];
        }
        // Logica pentru deplasarea capului
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

        // Logica pentru modul Easy (trece prin pereti)
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

    private boolean detectDeath() {

        boolean dead = false;

        // Functia pentru modul Hard (moare daca atinge peretii)
        if (!isEasy) {
            if (snakeXs[0] == -1) dead = true;
            if (snakeXs[0] >= NUM_BLOCKS_WIDE) dead = true;
            if (snakeYs[0] == -1) dead = true;
            if (snakeYs[0] == numBlocksHigh) dead = true;
        }

        // Daca se mananca singur (daca pozitia capului este egala cu pozitia unei parti din corp)
        for (int i = snakeLength - 1; i > 0; i--) {
            if ((i > 4) && (snakeXs[0] == snakeXs[i]) && (snakeYs[0] == snakeYs[i])) {
                dead = true;
            }
        }
        return dead;
    }

    // Functia de detectare a starilor jocului (misca sarpele, mananca, moare)
    public void update() {

        if (snakeXs[0] == bobX && snakeYs[0] == bobY) {
            eatBob();
        }

        moveSnake();

        if (detectDeath()) {
            soundPool.play(snake_crash, 1, 1, 0, 0, 1);
            newGame();
        }
    }

    // Functia de interfata
    public void draw() {

        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

            // Se deseneaza fundalul
            canvas.drawColor(Color.argb(255, 100, 125, 90));

            // Se deseneaza scorul
            paint.setColor(Color.argb(255, 50, 50, 50));
            paint.setTextSize(60);
            paint.setTypeface(typeface);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Score: " + score, screenX / 2,  150, paint);

            // Se deseneaza corpul sarpelui
            for (int i = 0; i < snakeLength; i++) {
                canvas.drawRect(snakeXs[i] * blockSize, (snakeYs[i] * blockSize), (snakeXs[i] * blockSize) + blockSize, (snakeYs[i] * blockSize) + blockSize, paint);
            }

            // Se deseneaza mancarea
            paint.setColor(Color.argb(255, 255, 255, 255));
            canvas.drawRect(bobX * blockSize, (bobY * blockSize), (bobX * blockSize) + blockSize, (bobY * blockSize) + blockSize, paint);

            // Unlock the canvas and reveal the graphics for this frame
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    // Functia pentru logica framerate-ului
    public boolean updateRequired() {
        if (nextFrameTime <= System.currentTimeMillis()){
            nextFrameTime =System.currentTimeMillis() + MILLIS_PER_SECOND / FPS;
            return true;
        }

        return false;
    }

    @Override
    // Functia pentru detectarea unei atingeri a ecranului (pentru logica de deplasare)
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                if (!isLeftWay) { // Se deplaseaza la dreapta
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
                    switch (heading) { // Se deplaseaza la stanga
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
