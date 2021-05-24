package com.example.snake;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button button;
    private Button directionButton;
    private Button difficultyButton;

    // true = left, false = right
    Boolean setDirection = false;
    // true = easy, false = hard
    Boolean setDifficulty = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.play);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSnakeActivity();
            }
        });

        directionButton = (Button) findViewById(R.id.direction);
        directionButton.setText("Right");
        directionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDirection();
            }
        });

        difficultyButton = (Button) findViewById(R.id.difficulty);
        difficultyButton.setText("Easy");
        difficultyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDifficulty();
            }
        });
    }

    private void setDifficulty() {
        if (!setDifficulty) {
            setDifficulty = true;
            difficultyButton.setText("Easy");
        }
        else {
            setDifficulty = false;
            difficultyButton.setText("Hard");
        }
    }

    private void setDirection() {
        if (!setDirection) {
            setDirection = true;
            directionButton.setText("Left");
        }
        else {
            setDirection = false;
            directionButton.setText("Right");
        }
    }

    public void openSnakeActivity() {
        Intent intent = new Intent(this, SnakeActivity.class);

        intent.putExtra("gameDifficulty", String.valueOf(setDifficulty));
        intent.putExtra("gameDirection", String.valueOf(setDirection));
        startActivity(intent);
    }
}