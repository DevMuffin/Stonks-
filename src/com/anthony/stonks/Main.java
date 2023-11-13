package com.anthony.stonks;

import java.awt.Color;
import java.io.IOException;

import com.anthonybhasin.nohp.Game;
import com.anthonybhasin.nohp.GameSettings;
import com.anthonybhasin.nohp.level.Background.SolidColorBackground;
import com.anthonybhasin.nohp.level.Level;
import com.anthonybhasin.nohp.level.entity.Camera;

public class Main {

	public static boolean TEST_MODE = false;

	public static Color BG_COL = new Color(0xFFE1E3E4);

	public static void main(String[] args) {

		GameSettings.maxTPS = 25;
		GameSettings.windowTitle = "Stonks - they can only go up!";

		Game.create();

		Game.instance.level = new Level(new Camera(), new SolidColorBackground(Main.BG_COL));

		try {
			StockReaderEntity sre = new StockReaderEntity();
			Game.instance.level.addEntity(sre);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Game.instance.start();
	}
}
