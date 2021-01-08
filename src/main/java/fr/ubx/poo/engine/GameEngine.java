/*
 * Copyright (c) 2020. Laurent Réveillère
 */

package fr.ubx.poo.engine;

import fr.ubx.poo.game.Direction;
import fr.ubx.poo.model.go.Bombs;
import fr.ubx.poo.model.go.GameObject;
import fr.ubx.poo.view.sprite.Sprite;
import fr.ubx.poo.view.sprite.SpriteBomb;
import fr.ubx.poo.view.sprite.SpriteFactory;
import fr.ubx.poo.game.Game;
import fr.ubx.poo.game.Position;
import fr.ubx.poo.model.decor.*;
import fr.ubx.poo.model.go.character.Player;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;


public final class GameEngine {

    private static AnimationTimer gameLoop;
    private final String windowTitle;
    private final Game game;
    private final Player player;
    private final List<Sprite> sprites = new ArrayList<>();
    private final List<Sprite> spritesMonster= new ArrayList<>();
    private StatusBar statusBar;
    private Pane layer;
    private Input input;
    private Stage stage;
    private Sprite spritePlayer;
    private Sprite spriteBomb;
    private Bombs bombs;

    private long cpt;

    public GameEngine(final String windowTitle, Game game, final Stage stage) {
        this.windowTitle = windowTitle;
        this.game = game;
        this.player = game.getPlayer();
        initialize(stage, game);
        buildAndSetGameLoop();
    }

    private void initialize(Stage stage, Game game) {
        this.stage = stage;
        Group root = new Group();
        layer = new Pane();

        int height = game.getWorld().dimension.height;
        int width = game.getWorld().dimension.width;
        int sceneWidth = width * Sprite.size;
        int sceneHeight = height * Sprite.size;
        Scene scene = new Scene(root, sceneWidth, sceneHeight + StatusBar.height);
        scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());

        stage.setTitle(windowTitle);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        input = new Input(scene);
        root.getChildren().add(layer);
        statusBar = new StatusBar(root, sceneWidth, sceneHeight, game);
        // Create decor sprites
        game.getWorld().forEach( (pos,d) -> sprites.add(SpriteFactory.createDecor(layer, pos, d)));
        spritePlayer = SpriteFactory.createPlayer(layer, player);
        game.getWorld().getMonsters().stream().map(monster -> SpriteFactory.createMonster(layer, monster)).forEach(spritesMonster::add);
        cpt=0;
    }

    protected final void buildAndSetGameLoop() {
        gameLoop = new AnimationTimer() {
            public void handle(long now) {
                // Check keyboard actions
                processInput(now);

                // Do actions

                update(now);

                // Graphic update
                render();
                statusBar.update(game);
            }
        };
    }

    private void processInput(long now) {
        if (input.isExit()) {
            gameLoop.stop();
            Platform.exit();
            System.exit(0);
        }
        if (input.isMoveDown()) {
            player.requestMove(Direction.S);
        }
        if (input.isMoveLeft()) {
            player.requestMove(Direction.W);
        }
        if (input.isMoveRight()) {
            player.requestMove(Direction.E);
        }
        if (input.isMoveUp()) {
            player.requestMove(Direction.N);
        }
        if (input.isKey()){
            Direction direction=player.getDirection();
            Position nextPos = direction.nextPosition(player.getPosition());
            DoorNextOpened open =new DoorNextOpened();
            if (game.getWorld().get(nextPos) instanceof DoorClosed){
                if(game.getInitnbKey()==1){
                    game.getWorld().setChanged();
                    game.getWorld().set(nextPos, open);
                    game.initnbKey=0;
                }
            }
        }
        if (input.isBomb() && player.getNbBombs()>0 && player.getNbBombsfuse()< player.getNbBombs()){
            //game.getWorld().set(player.getPosition(), new BombNumberInc());
            player.setNbBombsfuse(player.getNbBombsfuse()+1);
            bombs= new Bombs(game, game.getPlayer().getPosition());
            spriteBomb= SpriteFactory.createBomb(layer, bombs);
            spriteBomb.render();
            //player.setNbBombs(player.getNbBombs()-1);
            //player.requestBombTimer(now);
        }
        input.clear();
    }

    private void showMessage(String msg, Color color) {
        Text waitingForKey = new Text(msg);
        waitingForKey.setTextAlignment(TextAlignment.CENTER);
        waitingForKey.setFont(new Font(60));
        waitingForKey.setFill(color);
        StackPane root = new StackPane();
        root.getChildren().add(waitingForKey);
        Scene scene = new Scene(root, 400, 200, Color.WHITE);
        stage.setTitle(windowTitle);
        stage.setScene(scene);
        input = new Input(scene);
        stage.show();
        new AnimationTimer() {
            public void handle(long now) {
                processInput(now);
            }
        }.start();
    }


    private void update(long now) {
        player.update(now);
        cpt++;
        if (cpt%60==0){
            game.getMonsterList().forEach(L -> L.forEach(monster -> {monster.requestMove(Direction.random()); monster.doMove(monster.getDirection()); } ));
            bombs.setStateBomb(bombs.getStateBomb()+1);
            if(bombs.getStateBomb()==1){
                spriteBomb.remove();
                spriteBomb=SpriteFactory.createBomb(layer, bombs);
                spriteBomb.render();
            }
            if(bombs.getStateBomb()==2){
                spriteBomb.remove();
                spriteBomb=SpriteFactory.createBomb(layer, bombs);
                spriteBomb.render();
            }
            if(bombs.getStateBomb()==3){
                spriteBomb.remove();
                spriteBomb=SpriteFactory.createBomb(layer, bombs);
                spriteBomb.render();
            }
            if(bombs.getStateBomb()==4){
                spriteBomb.remove();
                spriteBomb=SpriteFactory.createBomb(layer, bombs);
                bombs.doDestroy();
                game.getMonsterList().forEach(L -> L.removeIf(monster -> !monster.isAlive()));
                spritesMonster.forEach(Sprite::remove);
                spritesMonster.clear();
                game.getWorld().getMonsters().stream().map(monster -> SpriteFactory.createMonster(layer, monster)).forEach(spritesMonster::add);
                spriteBomb.render();
            }
            if(bombs.getStateBomb()>4){
                spriteBomb.remove();
                player.setNbBombsfuse(player.getNbBombsfuse()-1);
            }
            
        }
        if(game.getWorld().hasChanged()){
            DecreaseLifePlayer();
            sprites.forEach(Sprite::remove);
            sprites.clear();
            game.getWorld().forEach( (pos,d) -> sprites.add(SpriteFactory.createDecor(layer, pos, d)));
            game.getWorld().finishChanged();
        }

        if (game.getWorld().hasUp()){
            stage.close();
            sprites.forEach(Sprite::remove);
            sprites.clear();
            spritePlayer.remove();
            spritesMonster.forEach(Sprite::remove);
            spritesMonster.clear();
            initialize(stage, game);
            Position pos =game.getWorld().finDoorN();
            player.setPosition(pos);
            game.getWorld().SetUpfinish();
        }

        if (game.getWorld().hasDown()){
            stage.close();
            sprites.forEach(Sprite::remove);
            sprites.clear();
            spritePlayer.remove();
            spritesMonster.forEach(Sprite::remove);
            spritesMonster.clear();
            initialize(stage, game);
            Position pos =game.getWorld().finDoorP();
            player.setPosition(pos);
            game.getWorld().SetDownfinish();
        }

        if (player.isAlive() == false) {
            gameLoop.stop();
            showMessage("Perdu!", Color.RED);
        }
        if (player.isWinner()) {
            gameLoop.stop();
            showMessage("Gagné", Color.TURQUOISE);
        }
    }

    private void render() {
        sprites.forEach(Sprite::render);
        // last rendering to have player in the foreground
        spritePlayer.render();
        spritesMonster.forEach(Sprite::render);
        
    }

    public void start() {
        gameLoop.start();
    }


    public void DecreaseLifePlayer(){
        for (int i=0 ; i < game.getWorld().getMonsters().size(); i++){
            if ( game.getWorld().getMonsters().get(i).getPosition().equals(game.getPlayer().getPosition())){
                game.getPlayer().setLives(game.getPlayer().getLives()-1);;
            }
            else{ ;}
        }
    }

}
