package fi.tamk.hello;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * MainClass is just for demo purposes in this project.
 */
public class Hello extends ApplicationAdapter implements HighScoreListener {
	private Stage stage;
	private Skin skin;

	private Table content;

	@Override
	public void create () {
		HighScoreServer.readConfig("highscore.config");
		HighScoreServer.setVerbose(true);
		HighScoreServer.fetchHighScores(this);

		otherSetup();
	}

	@Override
	public void receiveHighScore(List<HighScoreEntry> highScores) {
		Gdx.app.log("MainClass", "Received new high scores successfully");
		updateScores(highScores);
	}

	@Override
	public void receiveSendReply(Net.HttpResponse httpResponse) {
		Gdx.app.log("MainClass", "Received response from server: "
				+ httpResponse.getStatus().getStatusCode());
		HighScoreServer.fetchHighScores(this);
	}

	@Override
	public void failedToRetrieveHighScores(Throwable t) {
		Gdx.app.error("MainClass",
				"Something went wrong while getting high scores", t);
	}

	@Override
	public void failedToSendHighScore(Throwable t) {
		Gdx.app.error("MainClass",
				"Something went wrong while sending a high scoreField entry", t);
	}

	private void otherSetup() {
		skin = new Skin();
		skin = new Skin (Gdx.files.internal("uiskin.json"));
		stage = new Stage();
		Gdx.input.setInputProcessor(stage);
		content = new Table();
		createTable();
		stage.addActor(content);
	}

	private ArrayList<Label> scoreLabels;

	private void updateScores(List<HighScoreEntry> scores) {
		int i = 0;
		for (HighScoreEntry e : scores) {
			String entry = e.getScore() + " - " + e.getName();
			scoreLabels.get(i).setText(entry);
			i++;
		}
	}

	private TextField nameField;
	private TextField scoreField;

	private void createTable() {
		content.setFillParent(true);
		content.add(new Label("High Scores", skin)).colspan(2);

		scoreLabels = new ArrayList<>();


		for (int n = 0; n < 10; n++) {
			content.row();
			Label l = new Label("", skin);
			content.add(l).colspan(2);
			scoreLabels.add(l);
		}

		TextButton fetch = new TextButton("Fetch highscores", skin);
		fetch.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				fetchHighScores();
			}
		});

		TextButton newHighScore = new TextButton("Add new highscore", skin);
		newHighScore.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				createNewScore();
			}
		});

		content.row();
		content.add(fetch).colspan(2);
		content.row();
		content.add(new Label("Name:", skin));
		content.add(new Label("Score:", skin));
		content.row();

		nameField = new TextField("", skin);
		scoreField = new TextField("", skin);

		content.add(nameField);
		content.add(scoreField);

		content.row();
		content.add(newHighScore).colspan(2);
	}

	private void fetchHighScores() {
		HighScoreServer.fetchHighScores(this);
	}

	private void createNewScore() {
		String name = nameField.getText();
		int score = Integer.parseInt(scoreField.getText());
		HighScoreEntry scoreEntry = new HighScoreEntry(name, score);
		HighScoreServer.sendNewHighScore(scoreEntry, this);
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.draw();
	}

	@Override
	public void dispose () {
		skin.dispose();
	}
}

/*
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Hello extends ApplicationAdapter {
	SpriteBatch batch;
	BitmapFont font;

	@Override
	public void create () {
		batch = new SpriteBatch();
		font = new BitmapFont();
		font.setColor(Color.RED);
		System.out.println("Here here");
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();
		System.out.println("here here");
		font.draw(batch, "Hello World!", 200, 200);
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		font.dispose();
	}
}*/
