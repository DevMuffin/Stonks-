package com.anthony.stonks;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.anthony.stonks.entity.LightsEffect;
import com.anthony.stonks.entity.Tendie;
import com.anthonybhasin.nohp.Game;
import com.anthonybhasin.nohp.GameSettings;
import com.anthonybhasin.nohp.Screen;
import com.anthonybhasin.nohp.io.Keyboard;
import com.anthonybhasin.nohp.io.Mouse;
import com.anthonybhasin.nohp.level.entity.Entity;
import com.anthonybhasin.nohp.math.Point2D;
import com.anthonybhasin.nohp.math.RotationMath;
import com.anthonybhasin.nohp.ui.Button;
import com.anthonybhasin.nohp.ui.Label;
import com.anthonybhasin.nohp.ui.TextField;
import com.anthonybhasin.nohp.ui.TextField.TextType;
import com.anthonybhasin.nohp.ui.UIElementBackground.BasicUIBackground;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockQuote;

public class StockReaderEntity extends Entity {

	private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#.00#");

	private static final Font TIME_FONT = new Font("Courier New", 1, 14);

	private boolean lastTickMouseWasPressed;
	private Point2D lastMousePos;

	private Stock stock;

	private int timer;
	private int lastNotifATimer, lastNotifBTimer, lastNotifCTimer, tendieTimer;
	private Candle on;
	private List<Candle> candles;

	private StockQuote latestQuote;

	private double displayLow, displayHigh;

	private double priceLow = Double.MAX_VALUE, priceHigh = Double.MIN_VALUE;

	private Label stockPriceLabel, lowPriceLabel, highPriceLabel;

	private boolean aboveAverageShare;
	private TextField averageShareField;

	private LightsEffect leEntity;

	private LocalTime startTime;

	public StockReaderEntity() throws IOException {

		super.persistent = true;
//		This will keep the stock reader entity on the screen for render persistence.
		super.position.set(10, 10);
		super.position.setZIndex(Layer.STOCK_CHART_LAYER);
		super.positionRelative = false;

		this.lastTickMouseWasPressed = false;
		this.lastMousePos = new Point2D();

		this.stock = YahooFinance.get("GME");

		this.displayLow = 0;
		this.displayHigh = 100;

		this.timer = 0;
		this.lastNotifATimer = 0;
		this.lastNotifBTimer = 0;
		this.lastNotifCTimer = 0;
		this.tendieTimer = 0;
		this.on = new Candle(Main.TEST_MODE ? 0 : this.stock.getQuote().getPrice().doubleValue());
		this.candles = new ArrayList<Candle>();

		BasicUIBackground stockPriceBg = new BasicUIBackground(Color.BLACK).withFont(new Font("Courier New", 0, 12));
		this.stockPriceLabel = new Label("", Color.WHITE, 125 / 2, 30 / 2, 125, 30, stockPriceBg);
		this.stockPriceLabel.position.setZIndex(Layer.UI_LAYER);
		Game.instance.level.addEntity(this.stockPriceLabel);

		BasicUIBackground lowPriceBg = new BasicUIBackground(Color.BLACK).withFont(new Font("Courier New", 0, 12));
		this.lowPriceLabel = new Label("", Color.WHITE, 125 + 125 / 2, 30 / 2, 125, 30, lowPriceBg);
		this.lowPriceLabel.position.setZIndex(Layer.UI_LAYER);
		Game.instance.level.addEntity(this.lowPriceLabel);

		BasicUIBackground highPriceBg = new BasicUIBackground(Color.BLACK).withFont(new Font("Courier New", 0, 12));
		this.highPriceLabel = new Label("", Color.WHITE, 125 * 2 + 125 / 2, 30 / 2, 125, 30, highPriceBg);
		this.highPriceLabel.position.setZIndex(Layer.UI_LAYER);
		Game.instance.level.addEntity(this.highPriceLabel);

		this.aboveAverageShare = false;
		int inputBoxWidth = 125, inputBoxHeight = 30;
		BasicUIBackground averageShareBg = new BasicUIBackground(new Color(0xFF00CC66)).withBorder(Color.BLACK, 1)
//				Since we're setting the text directly to the background, it wont be accounted for in the TextField object and therefore wont cause NumberFormatException problems since the TextField is of TextType DECIMAL_NUMERIC.
				.withText("Set Avg Share $", Color.BLACK, new Font("Courier New", 0, 12));
		this.averageShareField = new TextField(GameSettings.width - inputBoxWidth / 2, inputBoxHeight / 2,
				inputBoxWidth, inputBoxHeight, averageShareBg).withTextType(TextType.DECIMAL_NUMERIC);
		this.averageShareField.position.setZIndex(Layer.UI_LAYER);
		Game.instance.level.addEntity(this.averageShareField);

		Button livePriceButton = new Button(GameSettings.width - 125 - 125 / 2, 30 / 2, 125, 30,
				new BasicUIBackground(new Color(0xFF00CC66)).withBorder(Color.BLACK, 1).withText("Go to Live Price",
						Color.BLACK, new Font("Courier New", 0, 12))) {

			@Override
			public void onLeftClick() {

				jumpToLivePrice();
			}
		};
		livePriceButton.position.setZIndex(Layer.UI_LAYER);
		Game.instance.level.addEntity(livePriceButton);

		numSecs = GameSettings.maxTPS * 60;

		this.leEntity = new LightsEffect(this);
		Game.instance.level.addEntity(this.leEntity);

		LocalTime time = LocalTime.now();
		this.startTime = time;

		jumpToLivePrice();

		new Thread("request-thread") {
			@Override
			public void run() {

				while (true) {

//					System.out.println("Requesting refreshed quote from YahooFinance");

					try {

						latestQuote = stock.getQuote(true);

						Thread.sleep(100);
					} catch (InterruptedException | IOException e) {

						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	public void jumpToLivePrice() {

		double currentPrice = this.on.priceEnd;
		double halfDelta = (this.displayHigh - this.displayLow) / 2;

		this.displayLow = currentPrice - halfDelta;
		this.displayHigh = currentPrice + halfDelta;

		Game.instance.level.camera.position.x = 10 * (this.candles.size() + 1);
	}

	int numSecs;

	public void determineNotification(double price) {

		if (this.lastNotifATimer > 0) {

			this.lastNotifATimer--;
		} else {

//			Gets the candles for the last 10 * 60 seconds (10 minutes)
			double tenMinuteMin = Double.MAX_VALUE, tenMinuteMax = Double.MIN_VALUE;
			for (int i = this.candles.size() - 1; i >= Math
					.max(this.candles.size() - 1 - 10 * 6/*
															 * using 6 since the bars represent 10 seconds
															 */, 0); i--) {
				Candle candle = this.candles.get(i);
				tenMinuteMin = Math.min(tenMinuteMin, candle.positive ? candle.priceStart : candle.priceEnd);
				tenMinuteMax = Math.max(tenMinuteMax, candle.positive ? candle.priceEnd : candle.priceStart);
			}
			if (tenMinuteMax - tenMinuteMin >= 10) {
				TextThroughEmail.send("VOLATILITY", "Current price: $" + price
						+ ". In the last 10 mins there was a delta of $" + (tenMinuteMax - tenMinuteMin) + "!");
				this.lastNotifATimer = 15 * 60;
			}
		}

		if (this.lastNotifBTimer > 0) {
			this.lastNotifBTimer--;
		} else {
			if (price >= 220) {
				TextThroughEmail.send("GOOD NEWS :)", "Current price: $" + price + ".");
				this.lastNotifBTimer = this.numSecs;
			}
		}
		if (this.lastNotifCTimer > 0) {
			this.lastNotifCTimer--;
		} else {
			if (price <= 200) {
				TextThroughEmail.send("RUH ROH :(", "Current price: $" + price + ".");
				this.lastNotifCTimer = this.numSecs;
			}
		}
	}

	@Override
	public void tick() {

		if (this.latestQuote == null) {
			return;
		}

		this.displayLow -= 5 * Mouse.WHEEL_SCROLL_AMOUNT;
		this.displayHigh += 5 * Mouse.WHEEL_SCROLL_AMOUNT;

		if (Math.abs(this.displayHigh - this.displayLow) < 10) {

			double middle = (this.displayHigh + this.displayLow) / 2;

			this.displayLow = middle - 5;
			this.displayHigh = middle + 5;
		}

		if (Keyboard.getPressed(KeyEvent.VK_W)) {

			int tenPixelDollarAmount = (int) Math
					.ceil((10.0 / GameSettings.height) * (this.displayHigh - this.displayLow));

			this.displayLow += tenPixelDollarAmount;
			this.displayHigh += tenPixelDollarAmount;
		}
		if (Keyboard.getPressed(KeyEvent.VK_S)) {

			int tenPixelDollarAmount = (int) Math
					.ceil((10.0 / GameSettings.height) * (this.displayHigh - this.displayLow));

			this.displayLow -= tenPixelDollarAmount;
			this.displayHigh -= tenPixelDollarAmount;
		}
		if (Keyboard.getPressed(KeyEvent.VK_A)) {
			Game.instance.level.camera.position.translateX(-10);
		}
		if (Keyboard.getPressed(KeyEvent.VK_D)) {
			Game.instance.level.camera.position.translateX(10);
		}

		boolean mousePressed = Mouse.getPressed(Mouse.MOUSE_LEFT_BUTTON_CODE);
		if (mousePressed && this.lastTickMouseWasPressed) {

			Point2D diff = Point2D.subtract(this.lastMousePos, Mouse.position);

			double diffDollars = (diff.y / GameSettings.height) * (this.displayHigh - this.displayLow);

			this.displayLow -= diffDollars;
			this.displayHigh -= diffDollars;
			Game.instance.level.camera.position.translate(diff.x, 0);
		}
		this.lastTickMouseWasPressed = mousePressed;
		this.lastMousePos = new Point2D(Mouse.position);

		double price = Main.TEST_MODE ? ((Math.random() * 10) + 20) : this.latestQuote.getPrice().doubleValue();

		this.priceLow = this.latestQuote.getDayLow().doubleValue();
		this.priceHigh = this.latestQuote.getDayHigh().doubleValue();

		this.stockPriceLabel.setText("Price: " + StockReaderEntity.PRICE_FORMAT.format(price));

		this.lowPriceLabel.setText("Low: " + StockReaderEntity.PRICE_FORMAT.format(this.priceLow));
		this.highPriceLabel.setText("High: " + StockReaderEntity.PRICE_FORMAT.format(this.priceHigh));

		String averageShareStr = this.averageShareField.getText();

//		Check if above the average share price
		if (!averageShareStr.equals("") && price >= Double.parseDouble(averageShareStr)) {

			this.aboveAverageShare = true;
			this.leEntity.tick();

			if (this.tendieTimer == 0) {

				Game.instance.level.addEntity(new Tendie());
				this.tendieTimer = (int) (1 * GameSettings.maxTPS);
			} else {
				this.tendieTimer--;
			}
		} else {

			this.aboveAverageShare = false;
		}

		this.timer++;
		this.on.tick(price);

//		Every 1 second
		if (this.timer % GameSettings.maxTPS == 0) {

			this.determineNotification(price);
		}
		if (this.timer >= 10 * GameSettings.maxTPS) {

			this.timer = 0;
			this.candles.add(this.on);
			this.on = new Candle(price);

//			10 is one bar width.
			Game.instance.level.camera.position.translate(10, 0);
		}
	}

	@Override
	public void render() {

		double displayDelta = this.displayHigh - this.displayLow;

		int lineDollarAmt = 1;
		if (displayDelta <= 20) {
			lineDollarAmt = 1;
		} else if (displayDelta <= 40) {
			lineDollarAmt = 5;
		} else if (displayDelta <= 200) {
			lineDollarAmt = 10;
		} else if (displayDelta <= 500) {
			lineDollarAmt = 25;
		} else if (displayDelta <= 1000) {
			lineDollarAmt = 50;
		} else {
			lineDollarAmt = 100;
		}

//		Every lineDollarAmt of dollars draw a line
		for (int i = (int) (Math.round(this.displayLow)) / lineDollarAmt * lineDollarAmt; i < (int) Math
				.round(this.displayHigh); i += lineDollarAmt) {

			float y = GameSettings.height
					- (float) ((i - this.displayLow) / (this.displayHigh - this.displayLow) * GameSettings.height);

			Color col = Color.DARK_GRAY;
			if (lineDollarAmt < 10 && i % 10 != 0) {

				col = i % 5 == 0 ? Color.GRAY : Color.LIGHT_GRAY;
			}

			Screen.line().start(0, y).end(GameSettings.width, y).strokeWidth(1f).color(col).draw();
			Screen.text("$" + i).start(20, y).color(col).font(GameSettings.engineFont).draw();
		}

		float x = 0;

		int barWidth = 10;

		for (Candle candle : this.candles) {
			candle.draw(x, this.displayLow, this.displayHigh, barWidth, Color.GREEN, Color.RED);
			x += barWidth;
		}

		this.on.draw(x, this.displayLow, this.displayHigh, barWidth, Color.GREEN, Color.RED);

//		Since 10seconds = 10pixel width bar, 1 second = 1 pixel on screen.  So 60 pixels = 1 minute.
//		Also add 60 - startTime seconds to make sure it's at the :00 seconds mark.
		for (int timeX = Game.instance.level.camera.getMinXi() / 60 * 60
				+ (60 - this.startTime.getSecond()); timeX < Game.instance.level.camera.getMaxXi(); timeX += 60) {

			if (timeX < 0) {
				continue;
			}

			LocalTime newTime = this.startTime.plusSeconds(timeX);

			String timeStr = newTime.format(DateTimeFormatter.ofPattern("HH:mm"));

			float displayTimeX = timeX - Game.instance.level.camera.getMinX();

//			Prevent the times from overlapping the prices
			if (displayTimeX < 80) {
				continue;
			}

			boolean specialTime = newTime.getMinute() % 15 == 0;

			Screen.text(timeStr)
					.start(displayTimeX - Screen.getStringWidth(timeStr, StockReaderEntity.TIME_FONT) / 2 - 5,
							GameSettings.height - 10)
					.font(StockReaderEntity.TIME_FONT).color(specialTime ? Color.BLACK : Color.LIGHT_GRAY)
					.selfRotation(RotationMath.ROT_ANGLE[270]).draw();

			Screen.line().start(displayTimeX, specialTime ? 0 : GameSettings.height - 50)
					.end(displayTimeX, GameSettings.height).color(specialTime ? Color.BLACK : Color.LIGHT_GRAY)
					.strokeWidth(1f).draw();
		}
	}

	public boolean isAboveAverageShare() {

		return this.aboveAverageShare;
	}
}
