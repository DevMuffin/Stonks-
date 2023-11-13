package com.anthony.stonks;

import java.awt.Color;

import com.anthonybhasin.nohp.Game;
import com.anthonybhasin.nohp.GameSettings;
import com.anthonybhasin.nohp.Screen;

public class Candle {

	/**
	 * Reflects whether the candle is a gain or loss in value. Note: equal start and
	 * end price is considered negative (loss in value).
	 */
	public boolean positive = false;

	public double priceStart, priceEnd;

	public Candle(double priceStart) {

		this.priceStart = priceStart;
		this.priceEnd = priceStart;
	}

	public void tick(double price) {

		this.priceEnd = price;

		this.positive = this.priceEnd > this.priceStart;
	}

	public void draw(float x, double displayLow, double displayHigh, int width, Color positiveColor,
			Color negativeColor) {

		float displayDelta = (float) (displayHigh - displayLow);

		float y = GameSettings.height - (float) ((this.priceStart - displayLow) / displayDelta * GameSettings.height);

		Color color = this.positive ? positiveColor : negativeColor;

		float height = -(float) ((this.priceEnd - this.priceStart) / displayDelta * GameSettings.height);

		int minHeight = 1;
		if (height > -minHeight && height < minHeight) {
			if (height < 0) {
				height = -minHeight;
				color = Color.YELLOW;
			} else {
				height = minHeight;
				color = Color.YELLOW;
			}
		}

		Screen.rect()
				.start(x - Game.instance.level.camera.getMinX(),
						y - Game.instance.level.camera.position.y + (height < 0 ? height : 0))
				.dimensions(width, (int) Math.abs(height)).color(color).fill(true).draw();
	}
}
