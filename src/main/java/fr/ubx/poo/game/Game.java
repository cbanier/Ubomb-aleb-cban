/*
 * Copyright (c) 2020. Laurent Réveillère
 */

package fr.ubx.poo.game;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import fr.ubx.poo.model.go.character.Player;

public class Game {

    private final World world;
    private final Player player;
    private final String worldPath;
    public int initPlayerLives;
    public int initnbKey;

    private WorldEntity[][] raw;
    private World[] worlds = new World[3];

    public Game(String worldPath) {
        this.worldPath = worldPath;
        String current_level = worldPath + "/level1.txt";
        worlds[0] = new WorldFromFile(current_level);
        world = worlds[0];
        //world = new WorldStatic();
        loadConfig(worldPath);
        Position positionPlayer = null;
        try {
            positionPlayer = world.findPlayer();
            player = new Player(this, positionPlayer);
        } catch (PositionNotFoundException e) {
            System.err.println("Position not found : " + e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    public int getInitPlayerLives() {
        return initPlayerLives;
    }

    public int getInitnbKey() {
        return initnbKey;
    }

    private void loadConfig(String path) {
        try (InputStream input = new FileInputStream(new File(path, "config.properties"))) {
            Properties prop = new Properties();
            // load the configuration file
            prop.load(input);
            initPlayerLives = Integer.parseInt(prop.getProperty("lives", "3"));
            initnbKey = Integer.parseInt("0");
        } catch (IOException ex) {
            System.err.println("Error loading configuration");
        }
    }

    public World getWorld() {
        return world;
    }

    public Player getPlayer() {
        return this.player;
    }

    public String getWorldPath() {
        return worldPath;
    }
}
