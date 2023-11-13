package com.anthony.stonks.entity;

import java.awt.Color;

import com.anthony.stonks.Layer;
import com.anthony.stonks.StockReaderEntity;
import com.anthonybhasin.nohp.GameSettings;
import com.anthonybhasin.nohp.Screen;
import com.anthonybhasin.nohp.level.entity.Entity;
import com.anthonybhasin.nohp.math.Point2D;

public class LightsEffect extends Entity {

	public static final Color[] BEAM_COLS = new Color[] { new Color(0, 204, 0, 50), new Color(102, 255, 103, 70) };

	public static final int NUM_BEAMS = 6;
	public static final float BEAM_WIDTH = 80f;

	private StockReaderEntity sre;

	private boolean[] beamRisings;
	private int[] beamSpeeds;
	private Point2D[] beamStarts, beamEnds;

	public LightsEffect(StockReaderEntity sre) {

		this.sre = sre;

		super.positionRelative = false;
		super.position.set(10, 10);
		super.position.setZIndex(Layer.EFFECTS_LAYER);
		super.persistent = true;

		this.beamRisings = new boolean[LightsEffect.NUM_BEAMS];
		this.beamSpeeds = new int[LightsEffect.NUM_BEAMS];
		this.beamStarts = new Point2D[LightsEffect.NUM_BEAMS];
		this.beamEnds = new Point2D[LightsEffect.NUM_BEAMS];

		for (int i = 0; i < this.beamStarts.length; i++) {

			this.beamRisings[i] = Math.random() < 0.5 ? true : false;
			this.beamSpeeds[i] = (int) (Math.random() * 5) + 5;

			float x = i * (GameSettings.width / LightsEffect.NUM_BEAMS) + LightsEffect.BEAM_WIDTH / 2;
			this.beamStarts[i] = new Point2D(x, GameSettings.height + 100);
			this.beamEnds[i] = new Point2D(x, 0 - 100);
		}
	}

	@Override
	public void tick() {

		if (!this.sre.isAboveAverageShare()) {

			return;
		}

		for (int i = 0; i < this.beamRisings.length; i++) {

			boolean rising = this.beamRisings[i];

			Point2D pointEnd = this.beamEnds[i];

			int moveX = this.beamSpeeds[i] * (rising ? 1 : -1);

			pointEnd.translateX(moveX);

			if (pointEnd.x < 0 || pointEnd.x >= GameSettings.width) {

				this.beamRisings[i] = !rising;
			}
		}
	}

	@Override
	public void render() {

		if (!this.sre.isAboveAverageShare()) {

			return;
		}

		for (int i = 0; i < this.beamStarts.length; i++) {

			Point2D beamStart = this.beamStarts[i], beamEnd = this.beamEnds[i];

			Screen.line().start(beamStart).end(beamEnd).strokeWidth(LightsEffect.BEAM_WIDTH)
					.color(LightsEffect.BEAM_COLS[i % LightsEffect.BEAM_COLS.length]).draw();
		}
	}
}
