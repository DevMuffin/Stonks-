package com.anthony.stonks.entity;

import java.util.Random;

import com.anthony.stonks.Layer;
import com.anthonybhasin.nohp.Game;
import com.anthonybhasin.nohp.GameSettings;
import com.anthonybhasin.nohp.level.entity.Camera;
import com.anthonybhasin.nohp.level.entity.ImageEntity;
import com.anthonybhasin.nohp.math.Vector2D;
import com.anthonybhasin.nohp.texture.Sprite;

public class Tendie extends ImageEntity {

	public static final Random RANDOM = new Random();

	public static final Sprite TENDIE_SPRITE = new Sprite("/tendie.png"), TENDIE_SPRITE_2 = new Sprite("/tendie2.png");

	private int rotationDir;

	private Vector2D dir;

	public Tendie() {

		super(Tendie.RANDOM.nextInt(2) == 0 ? Tendie.TENDIE_SPRITE : Tendie.TENDIE_SPRITE_2);

		super.position.setZIndex(Layer.TENDIES_LAYER);
		
		this.rotationDir = Tendie.RANDOM.nextInt(2) == 0 ? +1 : -1;

		this.dir = new Vector2D(0, 4);

		Camera camera = Game.instance.level.camera;
		float percent = Math.max(0.25f, Tendie.RANDOM.nextFloat());
		if (percent > 0.6) {
			if (Tendie.RANDOM.nextInt(2) == 0) {
				percent = 0.5f;
			}
		}

		super.width *= percent;
		super.height *= percent;
		super.position.set(camera.getMinX() + Tendie.RANDOM.nextInt(GameSettings.width),
				camera.getMinY() - super.height / 2);
	}

	@Override
	public void tick() {

		super.position.move(this.dir);

		super.setSelfRotation(super.getSelfRotation() + this.rotationDir * 2);

		if (super.position.y >= GameSettings.height + 100) {

			Game.instance.level.removeEntity(this);
		}
	}
}
